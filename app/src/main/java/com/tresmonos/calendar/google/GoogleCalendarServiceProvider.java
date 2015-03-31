package com.tresmonos.calendar.google;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tresmonos.calendar.CalendarAuthenticatorProvider;
import com.tresmonos.calendar.CalendarServiceProvider;
import com.tresmonos.calendar.google.actions.CreateReservationAction;
import com.tresmonos.calendar.google.actions.DeleteReservationAction;
import com.tresmonos.calendar.google.actions.RetrieveResourcesAction;
import com.tresmonos.calendar.google.actions.UpdateReservationEndTimeAction;
import com.tresmonos.calendar.model.Account;
import com.tresmonos.calendar.model.LocalResource;
import com.tresmonos.calendar.model.RemoteResource;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;

import org.jcaki.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;

public class GoogleCalendarServiceProvider implements CalendarServiceProvider, CalendarAuthenticatorProvider {

    private static final int REQUEST_AUTHORIZATION = 1;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final EventBus eventBus = EventBus.getDefault();
    private final ContentResolver contentResolver;
    private final Context context;
    private Set<Reservation> reservationsInDeleteProcess = Sets.newConcurrentHashSet();
    private Map<Reservation, DateTime> endDateByReservation = Maps.newConcurrentMap();
    private ActionExecutor actionExecutor = null;

    public GoogleCalendarServiceProvider(Context context, ContentResolver contentResolver) {
        this.context = context;
        this.contentResolver = contentResolver;
    }

	@Override
	public Account getAccount(final String name) {
		if (!Strings.hasText(name))
			return null;
		return Iterables.find(getAccounts(), new Predicate<Account>() {
			@Override
			public boolean apply(Account account) {
				return name.equals(account.getName());
			}
		}, null);
	}

	@Override
	public List<Account> getAccounts() {
		android.accounts.Account[] accounts = AccountManager.get(context).getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		return Lists.transform(Arrays.asList(accounts), new Function<android.accounts.Account, Account>() {
			@Override
			public Account apply(android.accounts.Account account) {
				return new Account(account.name);
			}
		});
	}

	@Override
	public void getAllRemoteResources(Account account, final ActionExecutorCallback<List<RemoteResource>> callback) {
		if (account == null) {
			callback.onExecutionFinished(true, new ArrayList<RemoteResource>());
			return;
		}
        getActionExecutor().executeNow(new RetrieveResourcesAction(context, this, account.getName()), callback);
	}

	@Override
	public List<LocalResource> getLocalResources(Account account) {
		if (account == null)
			return new ArrayList<LocalResource>();
		Uri uri = CalendarContract.Calendars.CONTENT_URI;
		String selection = "(" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?)";
		String[] selectionArgs = new String[] {account.getName()};

		Cursor cur = contentResolver.query(uri, GoogleCalendarProviderProjections.getProjectionColumns(GoogleCalendarProviderProjections.CalendarProjection.class), selection, selectionArgs, null);
		List<LocalResource> resources = new ArrayList<LocalResource>();
		while (cur.moveToNext()) {
			resources.add(createResource(cur));
		}
		cur.close();
		return resources;
	}

    @Override
    public LocalResource getLocalResource(String name) {
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "(" + CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " = ?)";
        String[] selectionArgs = new String[] {name};

        LocalResource resource = null;
        Cursor cur = contentResolver.query(uri, GoogleCalendarProviderProjections.getProjectionColumns(GoogleCalendarProviderProjections.CalendarProjection.class), selection, selectionArgs, null);
        while (cur.moveToNext()) {
            resource = createResource(cur);
        }
		cur.close();
        return resource;
    }

	private LocalResource createResource(Cursor cur) {
		long calendarID = cur.getLong(GoogleCalendarProviderProjections.CalendarProjection.id.getProjectionIndex());
		String displayName = cur.getString(GoogleCalendarProviderProjections.CalendarProjection.name.getProjectionIndex());
		String accountName = cur.getString(GoogleCalendarProviderProjections.CalendarProjection.accountName.getProjectionIndex());
        String email = cur.getString(GoogleCalendarProviderProjections.CalendarProjection.email.getProjectionIndex());
		Integer syncEvents = cur.getInt(GoogleCalendarProviderProjections.CalendarProjection.syncEvents.getProjectionIndex());
		return new LocalResource(calendarID, displayName, accountName, email, getReservations(calendarID), syncEvents > 0);
	}

	@Override
	public void sync(String accountName) {
		android.accounts.Account account = new android.accounts.Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		ContentResolver.requestSync(account, CalendarContract.AUTHORITY, new Bundle());
	}

	@Override
	public String updateSyncableResources(String accountName, String resourceName) {
		String result = "";

		// Check that only the Calendar sync is enabled
		android.accounts.Account account = new android.accounts.Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		if (ContentResolver.getIsSyncable(account, CalendarContract.AUTHORITY) <= 0) {
			result += "Calendar Provider was not enabled for sync. ";
		}
		if (!ContentResolver.getSyncAutomatically(account, CalendarContract.AUTHORITY)) {
			result += "Calendar Provider was not set for automatic sync. ";
		}

		SyncAdapterType[] types = ContentResolver.getSyncAdapterTypes();
		for (SyncAdapterType type : types) {
			if (account.type.equals(type.accountType)) {
				ContentResolver.setIsSyncable(account, type.authority, CalendarContract.AUTHORITY.equals(type.authority) ? 1 : 0);
				ContentResolver.setSyncAutomatically(account, type.authority, CalendarContract.AUTHORITY.equals(type.authority));
			}
		}

		// Check if sync events is set
		LocalResource localResource = getLocalResource(resourceName);
		if (localResource != null && !localResource.isSyncEvents()) {
			result += "Calendar '" + resourceName + "' was not configured to be synced";
			// Makes sure that the selected resource synced
			Uri uri = CalendarContract.Calendars.CONTENT_URI;
			String selection = "(" + CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " = ?)";
			String[] selectionArgs = new String[]{resourceName};
			ContentValues values = new ContentValues();
			values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
			contentResolver.update(uri, values, selection, selectionArgs);
		}

		return result.isEmpty() ? null : result;
	}

    private List<Reservation> getReservations(Long calendarID) {
        List<Reservation> reservations = Lists.newLinkedList();

        Long start = new DateTime(DateTimeZone.UTC).getMillis();
        Long end = new DateTime(DateTimeZone.UTC).plus(Days.days(2)).getMillis();

        Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, start);
        ContentUris.appendId(uriBuilder, end);
        String selection = "(" + CalendarContract.Instances.CALENDAR_ID + " = ?)";
        String[] selectionArgs = new String[] {calendarID.toString()};
        Cursor cur = contentResolver.query(uriBuilder.build(), GoogleCalendarProviderProjections.getProjectionColumns(GoogleCalendarProviderProjections.EventProjection.class), selection, selectionArgs, CalendarContract.Instances.BEGIN + " ASC");
        while (cur.moveToNext()) {
            Reservation reservation = getReservation(cur);

            if (endDateByReservation.containsKey(reservation)) {
                DateTime inProgressEndDate = endDateByReservation.get(reservation);
                reservation = new Reservation(reservation.getTitle(), "", reservation.getStartDate(), inProgressEndDate, reservation.getStatus());
            }

            if (!Integer.toString(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED).equals(reservation.getStatus())
                    && isInDeleteProcess(reservation)) {
                reservations.add(reservation);
            }
        }
		cur.close();
        return getActionExecutor().transformReservations(reservations);
    }

    private boolean isInDeleteProcess(Reservation reservation) {
        return !reservationsInDeleteProcess.contains(reservation);
    }

    private Reservation getReservation(Cursor cur) {
        String title = cur.getString(GoogleCalendarProviderProjections.EventProjection.title.getProjectionIndex());
        String location = cur.getString(GoogleCalendarProviderProjections.EventProjection.location.getProjectionIndex());
        String status = cur.getString(GoogleCalendarProviderProjections.EventProjection.selfAttendeeStatus.getProjectionIndex());
        DateTime startDate = normalizeDateTime(cur.getLong(GoogleCalendarProviderProjections.EventProjection.startDate.getProjectionIndex()));
        DateTime endDate = normalizeDateTime(cur.getLong(GoogleCalendarProviderProjections.EventProjection.endDate.getProjectionIndex()));
        return new Reservation(title, location, startDate, endDate, status);
    }

    private DateTime normalizeDateTime(long milliseconds) {
        return normalizeDateTime(new DateTime(milliseconds));
    }

    private DateTime normalizeDateTime(DateTime dateTime) {
        return dateTime.withMillisOfSecond(0);
    }

    @Override
    public void declineReservationAsync(final Resource resource, final Reservation reservation, final ActionExecutorCallback<Reservation> callback) {
	    getActionExecutor().execute(new DeleteReservationAction(context, this, resource, reservation), new ActionExecutorCallback<Reservation>() {
            @Override
            public void onExecutionFinished(boolean success, Reservation result) {
                sync(resource.getAccountName());
                if (!success) {
                    reservationsInDeleteProcess.remove(reservation);
                    Log.i(GoogleCalendarServiceProvider.class.getName(), String.format("Unable to decline reservation %s", reservation));
                }
                callback.onExecutionFinished(success, result);
            }
        });
    }

    @Override
    public Reservation createReservation(final Resource resource, final String title, final String description, final DateTime startDate, final DateTime endDate, final ActionExecutorCallback<Reservation> callback, boolean executeNow) {
	    CreateReservationAction action = new CreateReservationAction(resource, title, description, startDate, endDate);
	    if (executeNow)
		    getActionExecutor().executeNow(action, callback);
		else
		    getActionExecutor().execute(action, callback);
	    return action.getReservation();
    }

    private Reservation getReservationForCalendarAndEventId(LocalResource resource, Long eventID, DateTime startDate, DateTime endDate) {
        Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, startDate.getMillis());
        ContentUris.appendId(uriBuilder, endDate.getMillis());
        String selection = "(" + CalendarContract.Instances.CALENDAR_ID + " = ? AND " + CalendarContract.Instances.EVENT_ID + " = ? )";
        String[] selectionArgs = new String[] {resource.getId().toString(), eventID.toString()};
        Cursor cur = contentResolver.query(uriBuilder.build(), GoogleCalendarProviderProjections.getProjectionColumns(GoogleCalendarProviderProjections.EventProjection.class), selection, selectionArgs, CalendarContract.Instances.BEGIN + " ASC");
        cur.moveToNext();
		Reservation reservation = getReservation(cur);
		cur.close();
		return reservation;
    }

    @Override
    public void updateEndDateAsync(LocalResource resource, final Reservation currentReservation, DateTime newEndDate, final ActionExecutorCallback<Reservation> callback) {
        DateTime endDate = normalizeDateTime(newEndDate);
	    getActionExecutor().execute(new UpdateReservationEndTimeAction(resource, currentReservation, endDate), callback);
    }

    @Override
    public void requestAuthentication(int authInfo) {
        eventBus.post(new GoogleAuthenticationRequest(authInfo, REQUEST_GOOGLE_PLAY_SERVICES));
    }

    @Override
    public void requestAuthorization(Intent intent) {
        eventBus.post(new GoogleAuthorizationRequest(intent, REQUEST_AUTHORIZATION));
    }

    public static class GoogleAuthenticationRequest {
        private final int connectionStatusCode;
        private final int requestGooglePlayServices;

        public GoogleAuthenticationRequest(Integer connectionStatusCode, int requestGooglePlayServices) {
            this.connectionStatusCode = connectionStatusCode;
            this.requestGooglePlayServices = requestGooglePlayServices;
        }

        public int getConnectionStatusCode() {
            return connectionStatusCode;
        }

        public int getRequestGooglePlayServices() {
            return requestGooglePlayServices;
        }
    }

    public static class GoogleAuthorizationRequest {
        private final Intent intent;
        private final int authorizationCode;

        public GoogleAuthorizationRequest(Intent intent, int authorizationCode) {
            this.intent = intent;
            this.authorizationCode = authorizationCode;
        }

        public Intent getIntent() {
            return intent;
        }

        public int getAuthorizationCode() {
            return authorizationCode;
        }
    }

    private synchronized ActionExecutor getActionExecutor() {
        if (actionExecutor == null) {
            if (actionExecutor == null) {
                actionExecutor = new ActionExecutor(context, this).start();
            }
        }
        return actionExecutor;
    }

}
