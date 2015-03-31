package com.tresmonos.calendar.model;

import org.joda.time.DateTime;
import org.joda.time.Interval;

public class Reservation {

    private final String title;
    private final DateTime startDate;
    private final DateTime endDate;
    private final String location;
    private final String status;

    public Reservation( String title, String location, DateTime startDate, DateTime endDate, String status) {
        this.title = title;
        this.location = location;
        this.startDate = removeMilliseconds(startDate);
        this.endDate = removeMilliseconds(endDate);
        this.status = status;
    }

    private DateTime removeMilliseconds(DateTime dateTime) {
        return new DateTime(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), dateTime.getSecondOfMinute());
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getStatus() {
        return status;
    }

    public Interval getInterval() {
        return new Interval(startDate, endDate);
    }

    @Override
    public int hashCode() {
        return Utils.hash(startDate, title, location);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Reservation)) {
            return false;
        }
        Reservation reservation = (Reservation) o;
        return Utils.equals(reservation.title, title)
                && Utils.equals(reservation.startDate, startDate)
                && Utils.equals(reservation.endDate, endDate)
                && Utils.equals(reservation.location, location);
    }

	@Override
    public String toString() {
        return String.format("Reservation: %s - Location: %s - from: %s - until: %s - status: %s", title, location, startDate, endDate, status);
    }
}
