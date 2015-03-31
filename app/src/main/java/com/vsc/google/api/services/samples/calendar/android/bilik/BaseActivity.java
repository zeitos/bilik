package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.tresmonos.calendar.CalendarService;
import com.tresmonos.calendar.Configuration;
import com.tresmonos.calendar.google.GoogleCalendarServiceProvider;
import com.tresmonos.calendar.notifications.CalendarNotificationService;

import de.greenrobot.event.EventBus;

import static com.google.android.gms.common.GooglePlayServicesUtil.getErrorDialog;

/**
 * A base class for all activities
 */
public abstract class BaseActivity extends FragmentActivity {
	private boolean isBound;
	protected Configuration configuration;
	protected CalendarService calendarService;
	protected static final EventBus eventBus = EventBus.getDefault();

	/**
	 * Connection to the Calendar Service
	 */
	private ServiceConnection calendarServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			calendarService = ((CalendarService.CalendarBinder) service).getService();
			BaseActivity.this.checkServiceConnected();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			calendarService = null;
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
		eventBus.register(this);
        if (configuration.checkConfiguration() == null) {
            Crashlytics.setUserName(configuration.getResource() + ":" + configuration.getAccount());
            CalendarNotificationService.getOrSubscribe(this, configuration.getAccount(), configuration.getResource());
        }
	}

	@Override
	protected void onStop() {
		eventBus.unregister(this);
		EasyTracker.getInstance(this).activityStop(this);
		super.onStop();
	}

	void doBindServices() {
		bindService(new Intent(this, CalendarService.class), calendarServiceConnection, Context.BIND_AUTO_CREATE);
		isBound = true;
	}

	void doUnbindServices() {
		if (isBound) {
			// Detach our existing connection.
			if (calendarService != null)
				unbindService(calendarServiceConnection);
			isBound = false;
		}
	}

	private void checkServiceConnected() {
		if (calendarService != null)
			onServiceConnected();
	}

	protected void onServiceConnected() {
		// To be overridden
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		configuration = Configuration.getInstance(this);
		doBindServices();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
	}

	@Override
	protected void onDestroy() {
		doUnbindServices();
		super.onDestroy();
	}

    public void onEventMainThread(Exception exception) {
        Crashlytics.logException(exception);
    }

	public void onEventMainThread(String errorMessage) {
		Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
		Crashlytics.logException(new IllegalStateException(errorMessage));
	}

    public void onEventMainThread(final GoogleCalendarServiceProvider.GoogleAuthorizationRequest authRequest) {
        this.startActivityForResult(authRequest.getIntent(), authRequest.getAuthorizationCode());
    }

	public void onEventMainThread(final GoogleCalendarServiceProvider.GoogleAuthenticationRequest request) {
		runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog = getErrorDialog(request.getConnectionStatusCode(), BaseActivity.this, request.getRequestGooglePlayServices());
                dialog.show();
            }
        });
	}

	public enum Category {
		UI_ACTION,
		SYSTEM_ACTION,
	}

	public enum Action {
		// UI actions
		BUTTON_PRESS,
		ITEM_SELECTED,
		// System actions
		TIMEOUT,
		EVENT,
		VALIDATION_ERROR,
	}

	public enum Label {
		// UI elements
		MAIN_ACTIVITY_TAKE_NOW_BUTTON,
		MAIN_ACTIVITY_RELEASE_BUTTON,
		MAIN_ACTIVITY_CONFIRM_BUTTON,
		MAIN_ACTIVITY_ADD_TIME_BUTTON,
		MAIN_ACTIVITY_REPORT_ERROR_BUTTON,
		REPORT_ACTIVITY_CANCEL_BUTTON,
		REPORT_ACTIVITY_SETUP_BUTTON,
		REPORT_ACTIVITY_FEEDBACK_BUTTON,
		SETUP_ACTIVITY_READY_BUTTON,
		SETUP_ACTIVITY_SELECT_IMAGE_BUTTON,
		SETUP_ACTIVITY_ACCOUNT_SELECTOR,
		SETUP_ACTIVITY_RESOURCE_SELECTOR,
		SETUP_ACTIVITY_AREA_SELECTOR,
		// System elements
		SETUP_ACTIVITY_AUTO_DISMISS_TIMER,
		SETUP_ACTIVITY_ATTEMPT_SETUP,
		MAIN_ACTIVITY_AUTO_RELEASE_EVENT,
		MAIN_ACTIVITY_INITIAL_CONFIGURATION_EVENT,
		REPORT_ACTIVITY_AUTO_DISMISS_TIMER,
		CALENDAR_SERVICE_ADD_RESERVATION,
		CALENDAR_SERVICE_ADD_REMOTE_RESERVATION,
		CALENDAR_SERVICE_DELETE_RESERVATION,
		CALENDAR_SERVICE_CONFIRM_RESERVATION,
		CALENDAR_SERVICE_CHANGE_END_TIME_RESERVATION,
        CALENDAR_SERVICE_AUTO_DELETE_RESERVATION,
        CALENDAR_SERVICE_CONFIRM_NEW_RESERVATION_AFTER_DELETE,
        MAIN_APPLICATION_WIFI_SERVICE_RESTARTED,
		CALENDAR_SERVICE_SYNC_SETTINGS_RESET,
		EXCEEDED_LICENSE_QUOTE,
	}

	public void registerAnalyticsEvent(Category category, Action action, Label label, Long value) {
		registerAnalyticsEvent(this, category, action, label, value);
	}

	public static void registerAnalyticsEvent(Context ctx, Category category, Action action, Label label, Long value) {
		EasyTracker easyTracker = EasyTracker.getInstance(ctx);
		if (easyTracker != null) {
			easyTracker.send(MapBuilder.createEvent(category.name(), action.name(), label.name(), value).build());
		}
	}

    protected boolean supportsFullLayout() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

	protected Configuration getConfiguration() {
		return configuration;
	}

	protected CalendarService getCalendarService() {
		return calendarService;
	}
}
