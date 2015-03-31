package com.tresmonos.calendar;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Strings;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.tresmonos.calendar.google.ActionExecutorCallback;
import com.tresmonos.calendar.google.GoogleCalendarServiceProvider;
import com.tresmonos.calendar.model.Account;
import com.tresmonos.calendar.model.LocalResource;
import com.tresmonos.calendar.model.RemoteResource;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;
import com.tresmonos.calendar.model.ResourceState;
import com.vsc.google.api.services.samples.calendar.android.bilik.BaseActivity;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.joda.time.ReadableDuration;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nullable;

import de.greenrobot.event.EventBus;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class CalendarService extends Service {

    private static final EventBus eventBus = EventBus.getDefault();
	private static final long CALENDAR_TIMER_DELAY = Duration.standardSeconds(5).getMillis();
    private static final int CALENDAR_TIMER_START = 0;
	private static final long CALENDAR_SYNC_CHECK_TIMER_DELAY = Duration.standardSeconds(60).getMillis();
	private static final int CALENDAR_SYNC_CHECK_TIMER_START = 0;
    private static final long PARTIAL_RESOURCES_CHECK_TIMER_DELAY = Duration.standardMinutes(10).getMillis();
    private static final long PARTIAL_RESOURCES_CHECK_TIMER_START = 0;
    private static final ReadableDuration RESOURCE_UPDATE_DURATION_DELAY = Duration.standardMinutes(5);
	private static final ReadableDuration IN_PROGRESS_EXPIRATION_DURATION = Duration.standardMinutes(5);

	private DateTime nextResourceStateUpdate = DateTime.now();
    private final IBinder calendarBinder = new CalendarBinder();
    private final Timer calendarTimer = new Timer();
	private final Timer syncCheckTimer = new Timer();
    private final Timer syncPartialResourcesTimer = new Timer();

    private ResourceState lastResourceState = null;
    private ConfirmationManager confirmationManager;
    private CalendarServiceProvider googleCalendarService;
	private Configuration configuration;
    private Collection<RemoteResource> cachedResources = Lists.newArrayList();
	private HashMap<String, DateTime> mResourcesInProgress = Maps.newHashMap();

    /** This is the reservation that we expect to be confirmed next.*/
    private Reservation nextExpectedConfirmedReservation;

	public class CalendarBinder extends Binder {

        public CalendarService getService() {
            return CalendarService.this;
        }
    }

    @Override
    public void onDestroy() {
        calendarTimer.cancel();
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        googleCalendarService = new GoogleCalendarServiceProvider(this, getContentResolver());
        confirmationManager = new ConfirmationManager(this);
		configuration = Configuration.getInstance(this);

        calendarTimer.schedule(new TimerTask() {
            public void run() {
                refreshView();
            }
        }, CALENDAR_TIMER_START, CALENDAR_TIMER_DELAY);

		syncCheckTimer.schedule(new TimerTask() {
			public void run() {
				checkResourceSync();
			}
		}, CALENDAR_SYNC_CHECK_TIMER_START, CALENDAR_SYNC_CHECK_TIMER_DELAY);

        syncPartialResourcesTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                syncPartialResources(null);
            }
        }, PARTIAL_RESOURCES_CHECK_TIMER_START, PARTIAL_RESOURCES_CHECK_TIMER_DELAY);
    }

    public void syncPartialResources(final ActionExecutorCallback callback) {
        if (configuration.getAccount() == null) {
            cachedResources.clear();
            return;
        }

        getAllResources(getAccount(configuration.getAccount()), new ActionExecutorCallback<List<RemoteResource>>() {
            @Override
            public void onExecutionFinished(boolean success, List<RemoteResource> resources) {
                cachedResources = resources == null ? Lists.<RemoteResource>newArrayList() : Lists.newArrayList(resources);
	            if (callback != null) {
		            callback.onExecutionFinished(success, resources);
	            }
            }
        });
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //TODO only do this for our intent.
        syncPartialResources(new ActionExecutorCallback() {
	        @Override
	        public void onExecutionFinished(boolean success, Object result) {
		        // TODO extract this to a method that returns params received into the intent
		        if (intent != null && intent.getExtras() != null) {
			        String dataString = intent.getExtras().getString("com.parse.Data");
			        if (!Strings.isNullOrEmpty(dataString)) {
				        try {
					        JSONObject jsonObject = new JSONObject(dataString);
					        JSONObject params = (JSONObject) jsonObject.get("data");
					        if (params != null) {
						        String remoteResource = params.get("resource").toString();
						        if (!isNullOrEmpty(remoteResource)) {
							        removeRemoteResourceInProgress(remoteResource);
						        }
					        }
				        } catch (Exception e) {
					        Log.d("CalendarService", "JSONException when parsing data");
				        }
			        }
		        }
	        }
        });


	    // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

	private void removeRemoteResourceInProgress(String remoteResource) {
		mResourcesInProgress.remove(remoteResource);
	}

	public boolean isRemoteResourceInProgress(String fullResourceName) {
		DateTime expirationDateTime = mResourcesInProgress.get(fullResourceName);
		boolean inProgress = expirationDateTime != null;
		if (inProgress && expirationDateTime.isBeforeNow()) {
			inProgress = false;
			removeRemoteResourceInProgress(fullResourceName);
		}
		return inProgress;
	}

	private void addRemoteResourceInProgress(RemoteResource resource) {
		DateTime expirationDateTime = DateTime.now().plus(IN_PROGRESS_EXPIRATION_DURATION);
		mResourcesInProgress.put(resource.getFullName(), expirationDateTime);
	}

	@Override
    public boolean onUnbind(Intent intent) {
        calendarTimer.cancel();
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return calendarBinder;
    }

	/**
	 * Adds a reservation to a remote Resource, this action will be executed asynchronously but NOW
	 */
	public Reservation addRemoteReservation(RemoteResource resource, String title, String description, DateTime startDate, DateTime endDate) {
		BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_ADD_REMOTE_RESERVATION, null);
		addRemoteResourceInProgress(resource);
		return googleCalendarService.createReservation(resource, title, description, startDate, endDate, new ActionExecutorCallback<Reservation>() {
			@Override
			public void onExecutionFinished(boolean success, Reservation reservation) {
				if (!success) {
					reportError(String.format("Error when creating remote reservation: %s", reservation));
				}
			}
		}, true);
	}

	/**
	 * Adds a reservation in the current local Resource, this action will be executed asynchronously in the correct order.
	 */
    public void addReservation(String title, String description, DateTime startDate, DateTime endDate, final boolean isConfirmed) {
		BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_ADD_RESERVATION, null);
        Interval newInterval = new Interval(startDate, endDate);
	    LocalResource resource = getCurrentResource();
	    Reservation overlapReservation = getOverlapReservation(resource, newInterval);
        if (overlapReservation != null) {
            reportError(String.format("Reservation that attempts to create (%s - %s) overlaps with reservation: %s", startDate, endDate, overlapReservation));
            return;
        }
        Reservation temporalReservation = googleCalendarService.createReservation(resource, title, description, startDate, endDate, new ActionExecutorCallback<Reservation>() {
	        @Override
	        public void onExecutionFinished(boolean success, Reservation reservation) {
		        if (isConfirmed && success) {
			        confirmReservation(reservation);
		        } else {
			        unConfirmReservation(reservation);
		        }
		        refreshView();
	        }
        }, false);
	    confirmReservation(temporalReservation);
	    refreshView();
    }

    public void deleteReservation(Resource resource, final Reservation reservation) {
        Integer remainingMinutes = Minutes.minutesBetween(DateTime.now(), reservation.getEndDate()).getMinutes();
        BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_DELETE_RESERVATION, remainingMinutes.longValue());
        googleCalendarService.declineReservationAsync(resource, reservation, new ActionExecutorCallback<Reservation>() {
            @Override
            public void onExecutionFinished(boolean success, Reservation reservation) {
                if (!success) {
                    reportError("Error occurred declining reservation: " + reservation);
                }
            }
        });
        int nextReservationIndex = resource.getReservations().indexOf(reservation) + 1;
        boolean hasNextReservation = nextReservationIndex < resource.getReservations().size();
        nextExpectedConfirmedReservation = hasNextReservation ? resource.getReservations().get(nextReservationIndex) : null;
        refreshView();
    }

    public void confirmReservation(Reservation reservation) {
		BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_CONFIRM_RESERVATION, null);
        confirmationManager.confirm(reservation);
        boolean unexpectedMeeting = nextExpectedConfirmedReservation != null && !reservation.equals(nextExpectedConfirmedReservation);
        if (unexpectedMeeting) {
            Integer earnedMinutes = Minutes.minutesIn(reservation.getInterval()).getMinutes();
            BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_CONFIRM_NEW_RESERVATION_AFTER_DELETE, earnedMinutes.longValue());
        }
        nextExpectedConfirmedReservation = null;
        refreshView();
    }

	public void unConfirmReservation(Reservation reservation) {
		BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_CONFIRM_RESERVATION, null);
		confirmationManager.unConfirm(reservation);
		refreshView();
	}

    public void updateEndDateTime(final Reservation reservation, DateTime newEndDateTime) {
		BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_CHANGE_END_TIME_RESERVATION, null);
        LocalResource currentResource = getCurrentResource();
        Interval newInterval = new Interval(reservation.getStartDate(), newEndDateTime);
        Reservation overlapReservation = getOverlapReservation(currentResource, newInterval);
        if (overlapReservation != null && !reservation.equals(overlapReservation)) {
            reportError(String.format("EndDate (%s) overlaps with reservation: %s", newEndDateTime, overlapReservation));
            return;
        }
        googleCalendarService.updateEndDateAsync(currentResource, reservation, newEndDateTime, new ActionExecutorCallback<Reservation>() {
            @Override
            public void onExecutionFinished(boolean success, Reservation reservation) {
                if (!success) {
                    reportError("Error occurred modifying the reservation: " + reservation);
                }
            }
        });
        refreshView();
    }

    private Reservation getOverlapReservation(LocalResource currentResource, Interval newInterval) {
        for (Reservation actualReservation : currentResource.getReservations()) {
            if (newInterval.overlap(actualReservation.getInterval()) != null) {
                return actualReservation;
            }
        }
        return null;
    }

    public LocalResource getCurrentResource() {
		String resourceName = configuration.getResource();
        return resourceName != null ? googleCalendarService.getLocalResource(resourceName) : null;
    }

    private void refreshView() {
		Log.i(getClass().getName(), "Refreshing View");
        LocalResource currentResource = getCurrentResource();
		if (currentResource == null)
			return;
        googleCalendarService.sync(currentResource.getAccountName());
		ResourceState state = calculateResourceStatus(currentResource);
        Log.d("CalendarService", "---> posing state: " + state.getStatus().name() + " - with " + state.getResource().getReservations().size() + " reservations - for resourceState: " + state);
        eventBus.post(state);
		reportCurrentState(state);
    }

	private void checkResourceSync() {
		String accountName = configuration.getAccount();
		String resourceName = configuration.getResource();
		if (Strings.isNullOrEmpty(accountName) || Strings.isNullOrEmpty(resourceName))
			return;
		String result = googleCalendarService.updateSyncableResources(accountName, resourceName);
		if (result != null) {
			reportError(result);
		}
	}

    private void reportError(String message) {
        eventBus.post(message);
    }

    private ResourceState calculateResourceStatus(LocalResource resource) {
        Reservation currentReservation = null;
        Reservation nextReservation = null;
        Reservation afterNextReservation = null;
        DateTime dateTime = DateTime.now();

        // Assuming reservations sorted by start date.
        List<Reservation> reservations = resource.getReservations();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            DateTime reservationStart = reservation.getStartDate();
            DateTime reservationEnd = reservation.getEndDate();
            if (confirmationManager.isConfirmed(reservation))
                reservationStart = confirmationManager.getConfirmationInterval(reservation).getStart();
            if (dateTime.isAfter(reservationStart) && dateTime.isBefore(reservationEnd)) {
                currentReservation = reservation;
                nextReservation = i < reservations.size() - 1 ? reservations.get(i + 1) : null;
                afterNextReservation = i < reservations.size() - 2  ? reservations.get(i + 2) : null;
                break;
            } else if (dateTime.isBefore(reservationStart)) {
                nextReservation = reservation;
                afterNextReservation = i < reservations.size() - 1  ? reservations.get(i + 1) : null;
                currentReservation = null;
                break;
            }
        }

        if (nextReservation != null && !confirmationManager.isConfirmed(nextReservation) && confirmationManager.getConfirmationInterval(nextReservation).contains(dateTime)) {
            currentReservation = nextReservation;
            nextReservation = afterNextReservation;
        }

        if (currentReservation != null && !confirmationManager.isConfirmed(currentReservation)) {
            DateTime currentReservationEndDeletingDate = currentReservation.getStartDate().plus(ResourceState.confirmationDeletingEnd);
            if (dateTime.isAfter(currentReservationEndDeletingDate)) {
                confirmationManager.confirm(currentReservation);
            } else if (dateTime.isAfter(confirmationManager.getConfirmationInterval(currentReservation).getEnd()) && configuration.isMaster()) {
                BaseActivity.registerAnalyticsEvent(this, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.CALENDAR_SERVICE_AUTO_DELETE_RESERVATION, null);
	            deleteReservation(resource, currentReservation);
	            currentReservation = null;
            }
        }

        ResourceState.Status status;
        Interval confirmationInterval = null;
        if (currentReservation != null) {
            status = confirmationManager.isConfirmed(currentReservation) ? ResourceState.Status.busy : ResourceState.Status.waitingForConfirmation;
            // mhhh ver esto
            confirmationInterval = confirmationManager.getConfirmationInterval(currentReservation);
        } else {
            status = ResourceState.Status.empty;
        }

        return new ResourceState(resource, currentReservation, nextReservation, status, dateTime, confirmationInterval);
    }

	public List<Account> getAccounts() {
		return googleCalendarService.getAccounts();
	}

	public Account getAccount(String name) {
		return googleCalendarService.getAccount(name);
	}

	public void getAllResources(Account account, ActionExecutorCallback<List<RemoteResource>> callback) {
        googleCalendarService.getAllRemoteResources(account, callback);
	}

    public Collection<RemoteResource> getAllResources() {
        return Lists.newArrayList(cachedResources);
    }

    public Collection<RemoteResource> getAllResources(final String area) {
        return Lists.newArrayList(Iterables.filter(cachedResources, new Predicate<RemoteResource>() {
            @Override
            public boolean apply(@Nullable RemoteResource remoteResource) {
                return area.equalsIgnoreCase(nullToEmpty(remoteResource.getAreaName()));
            }
        }));
    }

	public LocalResource getResource(String name) {
		return googleCalendarService.getLocalResource(name);
	}

	/**
	 * Updates the status of this device on the central server. This is used
	 * for both interacting with other devices and for having a dashboard with the status
	 * of all devices in the system
	 * @param resourceState state to use
	 */
	private void reportCurrentState(final ResourceState resourceState) {
        if (!resourceState.equals(lastResourceState) || resourceState.getCurrentDateTime().isAfter(nextResourceStateUpdate)) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... voids) {
					nextResourceStateUpdate = resourceState.getCurrentDateTime().plus(RESOURCE_UPDATE_DURATION_DELAY);
					// Obtains battery status
					IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
					Intent batteryStatus = registerReceiver(null, intentFilter);

					int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
					boolean batteryIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
							status == BatteryManager.BATTERY_STATUS_FULL;
					int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
					int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
					float batteryLevel = level / (float) scale;

					try {
                        configuration.reportResourceInfo(resourceState);
                        configuration.reportDeviceInfo(batteryLevel, batteryIsCharging, resourceState);
					} catch (Configuration.ConfigurationException ex) {
						Crashlytics.logException(ex);
					}
					return null;
				}
			}.execute();
        }
        lastResourceState = resourceState;
	}

}
