package com.tresmonos.calendar.google.actions;

import android.util.Log;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.common.collect.ImmutableList;
import com.tresmonos.calendar.google.GAction;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;
import com.tresmonos.calendar.model.Utils;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

/**
 * Created by david on 8/27/14.
 */
public class CreateReservationAction extends GAction<Reservation> {

    private final Reservation mReservation;
    private final Resource resource;
    private final String title;
    private final String description;
    private final DateTime startDatetime;
    private final DateTime endDatetime;

    public CreateReservationAction(Resource resource, String title, String description, DateTime startDatetime, DateTime endDatetime) {
        this.resource = resource;
        this.title = title;
        this.description = description;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;

        this.mReservation = new Reservation(title, "", startDatetime, endDatetime, "");
    }

    @Override
    public String getAccountName() {
        return resource.getAccountName();
    }

    @Override
    public Reservation execute(Calendar client) throws IOException {
        Event event = new Event()
                .setAttendees(ImmutableList.of(new EventAttendee().setEmail(resource.getEmail())))
                .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startDatetime.getMillis())))
                .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endDatetime.getMillis())))
                .setDescription(description)
                .setSummary(title);
        createInstanceEvent(client, resource.getAccountName(), event);

        return mReservation;
    }

    @Override
    public List<Reservation> transform(List<Reservation> reservations) {
        List<Reservation> result = reservations.contains(mReservation) ? reservations : ImmutableList.<Reservation>builder().add(mReservation).addAll(reservations).build();
        Log.d("CalendarService", " ---- TRANSFORM WAS CALLED " + reservations.size() + " - " + result.size());
        return result;
    }

    @Override
    public boolean isProcessed(List<Reservation> reservations) {
        for (Reservation remote : reservations) {
            if (Utils.equals(mReservation.getTitle(), remote.getTitle())
                    && Utils.equals(mReservation.getStartDate(), remote.getStartDate())
                    && Utils.equals(mReservation.getLocation(), remote.getLocation())) {
                return true;
            }
        }
        return false;
    }

	public Reservation getReservation() {
		return mReservation;
	}
}
