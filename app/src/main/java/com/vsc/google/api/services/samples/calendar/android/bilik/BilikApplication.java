package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.parse.Parse;
import com.tresmonos.calendar.PropertyManager;

/**
 * Bilik main application class
 */
public class BilikApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		Parse.initialize(this, PropertyManager.getPropertyValue(this, PropertyManager.PARSE_APPLICATION_ID), PropertyManager.getPropertyValue(this, PropertyManager.PARSE_CLIENT_KEY));

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(getNetworkBroadcastReceiver(), filter);

		Crashlytics.start(this);
	}

    private BroadcastReceiver getNetworkBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isNetworkConnected(context)) {
                    WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(false);
                    wifiManager.setWifiEnabled(true);
                    GeneralActivity.registerAnalyticsEvent(context, BaseActivity.Category.SYSTEM_ACTION, BaseActivity.Action.EVENT, BaseActivity.Label.MAIN_APPLICATION_WIFI_SERVICE_RESTARTED, 0l);
                }
            }
        };
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}
