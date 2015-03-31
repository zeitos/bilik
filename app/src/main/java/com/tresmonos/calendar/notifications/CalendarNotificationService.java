package com.tresmonos.calendar.notifications;

import android.app.Activity;
import android.content.Context;

import com.google.api.client.util.Maps;
import com.google.common.base.Throwables;
import com.parse.ParsePush;
import com.parse.PushService;
import com.tresmonos.calendar.model.Account;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class CalendarNotificationService {

    private static final String NON_ALPHA_NUMERIC_REGULAR_EXPRESSION = "[^a-zA-Z0-9]";
    private static final String UPDATE_STATUS_INTENT_ACTION = "com.tresmonos.calendar.UPDATE_STATUS";
    private static Map<String, CalendarNotificationService> pushNotificationServiceMap = Maps.newHashMap();
    private final String mChannel;
    private final String mAccountName;
	private final String mResourceName;

    private CalendarNotificationService(Context context, String accountName, String resourceName) {
        this.mChannel = getNotificationChannel(accountName);
        this.mAccountName = accountName;
	    this.mResourceName = resourceName;
        //TODO: mhhhhhhhh this is really dirty
        PushService.subscribe(context, mChannel, Activity.class);
    }

    public static CalendarNotificationService getOrSubscribe(Context context, String accountName, String resourceName) {
	    String key = getKey(accountName, resourceName);
	    if (!pushNotificationServiceMap.containsKey(key)) {
            pushNotificationServiceMap.put(key, new CalendarNotificationService(context, accountName, resourceName));
        }
        return pushNotificationServiceMap.get(key);
    }

	private static String getKey(String accountName, String resourceName) {
		return accountName + "_" + resourceName;
	}

	private void pushNotification(String action) {
        try {
            JSONObject data = new JSONObject("{\"action\": \"" + action + "\", \"data\":{\"account\": \"" + mAccountName + "\", \"resource\": \"" + mResourceName + "\"}}");

            ParsePush push = new ParsePush();
            push.setData(data);
            push.setChannel(mChannel);
            push.sendInBackground();
        } catch (JSONException e) {
            throw Throwables.propagate(e);
        }
    }

    public void notifyDeviceStatusUpdated() {
        pushNotification(UPDATE_STATUS_INTENT_ACTION);
    }

    private static String getNotificationChannel(String accountName) {
        return Account.extractDomain(accountName).replaceAll(NON_ALPHA_NUMERIC_REGULAR_EXPRESSION, "");
    }

}
