package com.tresmonos.calendar.model;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;

public class ResourceState {

    public enum Status {
        busy,
        empty,
        waitingForConfirmation,
        unplugged;
    }

    private DateTime currentDateTime;
    private final Resource resource;
    private final Reservation currentReservation;
    private final Reservation nextReservation;
    private final Status status;
    private final Interval currentConfirmationInterval;

    public static final Duration confirmationDeletingEnd = Duration.standardMinutes(7);

    public ResourceState(Resource resource, Reservation currentReservation, Reservation nextReservation, Status status, DateTime currentDateTime, Interval currentConfirmationInterval) {
        this.resource = resource;
        this.currentReservation = currentReservation;
        this.nextReservation = nextReservation;
        this.status = status;
        this.currentDateTime = currentDateTime;
        this.currentConfirmationInterval = currentConfirmationInterval;
    }

    public DateTime getCurrentDateTime() {
        return currentDateTime;
    }

    public Reservation getCurrentReservation() {
        return currentReservation;
    }

    public Reservation getNextReservation() {
        return nextReservation;
    }

	public Resource getResource() {
        return resource;
    }

    public Status getStatus() {
        return status;
    }

    public Duration getCurrentExtensionTime(Duration maxExtensionTime) {
        DateTime correctedFrom = currentReservation == null ? currentDateTime : currentReservation.getEndDate();
        return getExtensionTime(nextReservation, correctedFrom, maxExtensionTime);
    }

    private Duration getExtensionTime(Reservation nextReservation, DateTime from, Duration maxExtensionTime) {
        if (nextReservation != null) {
            long availableExtensionTime = nextReservation.getStartDate().getMillis() - from.getMillis();
            return new Duration(Math.min(availableExtensionTime, maxExtensionTime.getMillis()));
        } else {
            return maxExtensionTime;
        }
    }

    public Period getTimeToNextEvent(DateTime from) {
        if (currentReservation != null)
            return new Period(from, currentReservation.getEndDate());

        if (nextReservation == null)
            return null;

        return new Period(from, nextReservation.getStartDate());
    }

	public Period getTimeToEndOfConfirmation(DateTime from) {
		if (currentReservation != null)
			return new Period(from, currentConfirmationInterval.getEnd());
		return null;
	}

    @Override
    public int hashCode() {
        return Utils.hash(resource, currentReservation, nextReservation, status);
    }

    @Override
    public boolean equals(Object o) {
		if (o == null) return false;
        if (!(o instanceof ResourceState)) return false;
        ResourceState rs = (ResourceState) o;
        return Utils.equals(status, rs.status)
                && Utils.equals(nextReservation, rs.nextReservation)
                && Utils.equals(resource, rs.resource)
                && Utils.equals(currentReservation, rs.currentReservation)
                && Utils.equals(nextReservation, rs.nextReservation);
    }

}
