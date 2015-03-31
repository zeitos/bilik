package com.tresmonos.calendar.google.actions;

import android.util.Log;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.tresmonos.calendar.google.GAction;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;
import com.tresmonos.calendar.model.Utils;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

public class UpdateReservationEndTimeAction extends GAction<Reservation> {

    private final Resource resource;
    private final Reservation reservation;
    private final DateTime endDate;
    private final Reservation transformedReservation;

    public UpdateReservationEndTimeAction(Resource resource, Reservation reservation, DateTime endDate) {
        this.resource = resource;
        this.reservation = reservation;
        this.endDate = endDate;
        this.transformedReservation = new Reservation(reservation.getTitle(), "", reservation.getStartDate(), endDate, reservation.getStatus());
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
            event.setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endDate.getMillis())));
            updateEvent(client, calendar, event);
        } else {
	        Log.d("UpdateReservationEndTimeAction", "Reservation attempt to updated doesn't exist.");
        }
        return transformedReservation;
    }

    @Override
    public List<Reservation> transform(List<Reservation> reservations) {
        return Lists.transform(reservations, new Function<Reservation, Reservation>() {
            @Override
            public Reservation apply(@Nullable Reservation input) {
                return reservation.equals(input) ? transformedReservation : input;
            }
        });
    }

    @Override
    public boolean isProcessed(List<Reservation> reservations) {
	    boolean reservationFound = false;
        for (Reservation remote : reservations) {
	        if (Utils.equals(reservation.getTitle(), remote.getTitle())
			        && Utils.equals(reservation.getStartDate(), remote.getStartDate())
			        && Utils.equals(reservation.getLocation(), remote.getLocation())) {
		        reservationFound = true;
		        if (remote.getEndDate().isAfter(reservation.getEndDate()) || remote.getEndDate().isEqual(transformedReservation.getEndDate())) {
			        return true;
		        }
	        }
        }

        return !reservationFound;
    }

}
