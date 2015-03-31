package com.tresmonos.calendar.google;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.Events;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;

public abstract class GAction<T> implements Action<T> {

    public static final String INSTANCE_DECLINED_STATE = "declined";
    public static final String INSTANCE_CANCELLED_STATE = "cancelled";
    public static final String INSTANCE_ACCEPTED_STATE = "accepted";


    /**
     * Creates the event received by parameter and associates it with a google calendar account.
     * @param accountName {@link String} google calendar account.
     * @param event {@link com.google.api.services.calendar.model.Event} event to insert.
     * @return the inserted {@link com.google.api.services.calendar.model.Event}.
     * @throws java.io.IOException if the operation failed.
     */
    protected Event createInstanceEvent(Calendar client, String accountName, Event event) throws IOException {
        client.events().insert(accountName, event).execute();
        return event;
    }

    /**
     * Searches for the calendar with name equals to the calendar name received by parameter.
     * @param feed {@link com.google.api.services.calendar.model.CalendarList} calendar list that contains all the calendars.
     * @param calendarName {@link String} name to match.
     * @return an instance of {@link com.google.api.services.calendar.model.CalendarListEntry} or null if the calendar does not exists.
     */
    protected CalendarListEntry findCalendar(CalendarList feed, final String calendarName) {
        return Iterables.tryFind(feed.getItems(), new Predicate<CalendarListEntry>() {
            @Override
            public boolean apply(CalendarListEntry calendarListEntry) {
                return calendarListEntry.getSummary().equals(calendarName);
            }
        }).orNull();
    }


    /**
     * Searchs for the {@link com.google.api.services.calendar.model.Event} associated with the reservation
     * received by parameter.
     * @param calendar {@link CalendarListEntry} calendar that contains all the reservations.
     * @param reservation {@link com.tresmonos.calendar.model.Reservation} that is going to be used map the event
     * @return the {@link com.google.api.services.calendar.model.Event} associated with the reservation
     * received by parameter.
     * @throws IOException
     */
    protected Event findEvent(Calendar client, CalendarListEntry calendar, final Reservation reservation) throws IOException {
        Events events = client.events().list(calendar.getId())
                .setTimeMin(new DateTime(reservation.getStartDate().getMillis()))
                .setTimeMax(new DateTime(reservation.getEndDate().getMillis()))
                .setSingleEvents(true)
                .execute();

        return Iterables.tryFind(events.getItems(), new Predicate<Event>() {
            @Override
            public boolean apply(Event e) {
                return nullToEmpty(e.getSummary()).equals(reservation.getTitle())
                        && nullToEmpty(e.getLocation()).equals(reservation.getLocation())
                        && e.getStart().getDateTime().getValue() == reservation.getStartDate().getMillis();
            }
        }).orNull();
    }

    /**
     * Updates the data of the {@link Event} received by parameter.
     * @param calendar {@link com.google.api.services.calendar.model.CalendarListEntry} calendar of the event.
     * @param event {@link com.google.api.services.calendar.model.Event} data to update.
     * @throws IOException if an IO exception is produced when updating the {@link com.google.api.services.calendar.model.Event}
     */
    protected void updateEvent(Calendar client, CalendarListEntry calendar, Event event) throws IOException {
        client.events().update(calendar.getId(), event.getId(), event).execute();
    }

    protected CalendarListEntry findCalendar(Calendar client, Resource resource) throws IOException {
        String resourceName = resource.getFullName();
        CalendarList feed = client.calendarList().list().setFields("items(id,summary)").execute();
        return findCalendar(feed, resourceName);
    }

    /**
     * @param event {@link com.google.api.services.calendar.model.Event} event.
     * @param resource {@link com.tresmonos.calendar.model.LocalResource} the resource.
     * @return the {@link com.google.api.services.calendar.model.EventAttendee} that represents the attendee of the resource name.
     * e.g. the room cancun will accept a reservation with the attendee 'cancun'
     */
    protected EventAttendee getResourceAttendee(Calendar client, Event event, Resource resource) {
        List<EventAttendee> attendees = event.getAttendees();
        final String resourceEmail = resource.getEmail();
        if (attendees == null) return null;
        return Iterables.tryFind(attendees, new Predicate<EventAttendee>() {
            @Override
            public boolean apply(EventAttendee attendee) {
                return attendee.getEmail().equalsIgnoreCase(resourceEmail);
            }
        }).orNull();
    }
}
