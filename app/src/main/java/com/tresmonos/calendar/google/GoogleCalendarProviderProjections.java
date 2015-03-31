package com.tresmonos.calendar.google;

import android.provider.CalendarContract;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

public class GoogleCalendarProviderProjections {

    private interface Projection {

        int getProjectionIndex();

        String getColumnProjectionName();
    }

    public static <E extends Enum<E>> String[] getProjectionColumns(Class<E> projectionEnum) {
        // TODO reveer y simplificar!!!!
        Iterator<String> projections = Iterators.transform(EnumSet.allOf(projectionEnum).iterator(), new Function<Enum<E>, String>() {
            @Override
            public String apply(Enum<E> enumItem) {
                return ((Projection)enumItem).getColumnProjectionName();
            }
        });

        ArrayList<String> projectionList = Lists.newArrayList(projections);
        return projectionList.toArray(new String[projectionList.size()]);
    }

    public enum EventProjection implements Projection {
        instanceId(CalendarContract.Instances._ID),
        eventId(CalendarContract.Instances.EVENT_ID),
        calendarId(CalendarContract.Instances.CALENDAR_ID),
        title(CalendarContract.Instances.TITLE),
        location(CalendarContract.Instances.EVENT_LOCATION),
        startDate(CalendarContract.Instances.BEGIN),
        endDate(CalendarContract.Instances.END),
        selfAttendeeStatus(CalendarContract.Instances.SELF_ATTENDEE_STATUS);

        private final String columnProjectionName;

        private EventProjection(String columnProjectionName) {
            this.columnProjectionName = columnProjectionName;
        }

        @Override
        public int getProjectionIndex() {
            return ordinal();
        }

        @Override
        public String getColumnProjectionName() {
            return this.columnProjectionName;
        }

    }

    public enum CalendarProjection implements Projection {
        id(CalendarContract.Calendars._ID),
        accountName(CalendarContract.Calendars.ACCOUNT_NAME),
        name(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
        email(CalendarContract.Calendars.OWNER_ACCOUNT),
		syncEvents(CalendarContract.Calendars.SYNC_EVENTS);

        private final String columnProjectionName;

        private CalendarProjection(String columnProjectionName) {
            this.columnProjectionName = columnProjectionName;
        }

        @Override
        public int getProjectionIndex() {
            return ordinal();
        }

        @Override
        public String getColumnProjectionName() {
            return columnProjectionName;
        }
    }

}