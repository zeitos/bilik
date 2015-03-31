package com.tresmonos.calendar.google.actions;

import android.content.Context;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.common.collect.Lists;
import com.tresmonos.calendar.CalendarAuthenticatorProvider;
import com.tresmonos.calendar.google.GAction;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;
import com.tresmonos.calendar.model.Utils;

import java.io.IOException;
import java.util.List;

public class DeleteReservationAction extends GAction<Reservation> {

    private final Context context;
    private final Reservation reservation;
    private final Resource resource;
    private final CalendarAuthenticatorProvider authenticatorProvider;

    public DeleteReservationAction(Context context, CalendarAuthenticatorProvider authenticatorProvider, Resource resource, Reservation reservation) {
        this.context = context;
        this.reservation = reservation;
        this.resource = resource;
        this.authenticatorProvider = authenticatorProvider;
    }

    @Override
    public String getAccountName() {
        return resource.getAccountName();
    }

    @Override
    public Reservation execute(Calendar client) throws IOException {
        CalendarListEntry calendar = findCalendar(client, resource);
        Event event = findEvent(client, calendar, reservation);
        if (event != null) {
            EventAttendee attendee = getResourceAttendee(client, event, resource);
            if (attendee != null) {
                attendee.setResponseStatus(INSTANCE_DECLINED_STATE);
            } else {
                event.setStatus(INSTANCE_CANCELLED_STATE);
            }
            updateEvent(client, calendar, event);
        }
        return reservation;
    }

    @Override
    public List<Reservation> transform(List<Reservation> reservations) {
        if (!reservations.contains(reservation))
            return reservations;

        List<Reservation> filtered = Lists.newArrayList(reservations);
        filtered.remove(reservation);
        return filtered;
    }

    @Override
    public boolean isProcessed(List<Reservation> reservations) {
        for (Reservation remote : reservations) {
            if (Utils.equals(reservation.getTitle(), remote.getTitle())
                    && Utils.equals(reservation.getStartDate(), remote.getStartDate())
                    && Utils.equals(reservation.getLocation(), remote.getLocation())) {
                return false;
            }
        }
        return true;
    }
}
