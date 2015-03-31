package com.tresmonos.ui;

import android.view.View;

import com.tresmonos.calendar.CalendarService;
import com.tresmonos.calendar.model.ResourceState;
import com.vsc.google.api.services.samples.calendar.android.bilik.BaseActivity;

import org.joda.time.Duration;

public interface CustomizableResources {

    public static final Duration DEFAULT_EXTENSION_TIME = Duration.standardMinutes(15);

    int getBodyBarColor();

    int getStatusText();

	int getButtonBackground();

    View inflateRoomOperations(final BaseActivity activity, CalendarService calendarService, ResourceState resourceState);

}
