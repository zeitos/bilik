package com.tresmonos.calendar;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.collect.Maps;
import com.tresmonos.calendar.model.Reservation;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.Map;

public class ConfirmationManager {

    public static final Duration CONFIRMATION_START = Duration.standardMinutes(5);
    public static final Duration CONFIRMATION_END = Duration.standardMinutes(5);
    private static final String CONFIRMED_RESERVATIONS_FILE = "confirmedReservations";
    private final Map<String, Interval> reservationsInProgress = Maps.newHashMap();
    private final SharedPreferences sharedPreferences;

    public ConfirmationManager(Context context) {
        sharedPreferences = context.getSharedPreferences(CONFIRMED_RESERVATIONS_FILE, 0);
    }

    public Interval getConfirmationInterval(Reservation reservation) {
        String reservationId = getConfirmationId(reservation);
        DateTime now = DateTime.now();
        Interval interval = reservationsInProgress.get(reservationId);
        if (interval == null) {
            if (isConfirmed(reservation) || !(now.isAfter(reservation.getStartDate()) && now.isBefore(reservation.getEndDate()))) {
                interval = new Interval(reservation.getStartDate().minus(CONFIRMATION_START), reservation.getStartDate().plus(CONFIRMATION_END));
            } else {
                interval = new Interval(now, now.plus(CONFIRMATION_END));
            }
            reservationsInProgress.put(reservationId, interval);
        }
        return interval;
    }

    private String getConfirmationId(Reservation reservation) {
        return String.format("%s-%s-%s", reservation.getTitle(), reservation.getLocation(), reservation.getStartDate().getMillis());
    }

    public boolean isConfirmed(Reservation reservation) {
        return sharedPreferences.contains(getConfirmationId(reservation));
    }

    public void confirm(Reservation reservation) {
        String confirmationId = getConfirmationId(reservation);
        DateTime confirmationTime = DateTime.now();
        sharedPreferences.edit()
                .putLong(confirmationId, confirmationTime.getMillis())
                .commit();
    }

	public void unConfirm(Reservation reservation) {
		String confirmationId = getConfirmationId(reservation);
		sharedPreferences.edit()
				.remove(confirmationId)
				.commit();
	}

}
