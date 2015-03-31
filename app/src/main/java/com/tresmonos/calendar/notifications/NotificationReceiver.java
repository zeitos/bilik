package com.tresmonos.calendar.notifications;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.tresmonos.calendar.CalendarService;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent2 = new Intent(context, CalendarService.class);
        intent2.putExtras(intent);
        context.startService(intent2);
    }
}