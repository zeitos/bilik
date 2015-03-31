package com.tresmonos.calendar.model;

import java.util.ArrayList;
import java.util.List;

public class LocalResource implements Resource {

    private final Long calendarId;
    private final String name;
    private final String accountName;
    private final String email;
    private List<Reservation> reservations;
	private final boolean syncEvents;

    public LocalResource(long calendarId, String fullName, String accountName, String email, List<Reservation> reservations, boolean syncEvents) {
        this.calendarId = calendarId;
        this.name = fullName;
        this.accountName = accountName;
        this.reservations = reservations;
        this.email = email;
		this.syncEvents = syncEvents;
    }

    public Long getId() {
        return calendarId;
    }

    @Override
    public String getShortName() {
        return name;
    }

    @Override
    public String getFullName() {
        return name;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    /**
     * @return a {@link List<com.tresmonos.calendar.model.Reservation>} sorted by {@link Reservation#getStartDate()}
     */
    @Override
    public List<Reservation> getReservations() {
        return reservations;
    }

	@Override
	public String toString() {
		return name;
	}

    @Override
    public String getEmail() {
        return email;
    }

	public boolean isSyncEvents() { return syncEvents; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Resource)) return false;

        Resource ro = (Resource)o;
        return Utils.equals(accountName, ro.getAccountName())
                && Utils.equals(email, ro.getEmail())
                && Utils.equals(name, ro.getFullName());
    }

    @Override
    public int hashCode() {
        return Utils.hash(accountName, email, name);
    }
}
