package com.tresmonos.calendar.model;

import com.google.api.client.util.Lists;

import org.jcaki.Strings;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;

import java.util.List;

/**
 * A resource that has not yet been synchronized
 */
public class RemoteResource implements Resource {
    public static final Duration LAST_UPDATE_TIMEOUT = Minutes.minutes(30).toStandardDuration();
    private final String email;
    private final String accountName;
	private final String fullName;
	private final String description;
    private final ResourceState.Status status;
    private final DateTime lastUpdateDate;
    private final DateTime nextEventDate;
    private final boolean managedByBilik;
    private final String alternativeName;
    private String areaName;

    public RemoteResource(String accountName, String email, String fullName, String description, String status, DateTime lastUpdateDate, DateTime nextEventDate, boolean managedByBilik, String alternativeName, String areaName) {
        this.accountName = accountName;
        this.email = email;
		this.fullName = fullName;
		this.description = description;
        this.status = isPlugged(lastUpdateDate, nextEventDate) && Strings.hasText(status) ? ResourceState.Status.valueOf(status) : ResourceState.Status.unplugged;
        this.lastUpdateDate = lastUpdateDate;
        this.nextEventDate = nextEventDate;
        this.managedByBilik = managedByBilik;
        this.alternativeName = alternativeName;
        this.areaName = areaName;
    }

    private boolean isPlugged(DateTime lastUpdateDate, DateTime nextEventDate) {
        DateTime now = DateTime.now();
        return lastUpdateDate != null && lastUpdateDate.plus(LAST_UPDATE_TIMEOUT.toStandardMinutes()).isAfter(now) && (nextEventDate == null || nextEventDate.isAfter(now));
    }

	public String getDescription() {
		return description;
	}

	public String toString() { return fullName; }

    public ResourceState.Status getStatus() {
        return status;
    }

    public DateTime getNextEventDate() {
        return nextEventDate;
    }

    public DateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public boolean isManagedByBilik() {
        return managedByBilik;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getShortName() {
        return alternativeName;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public List<Reservation> getReservations() {
        //TODO
        return Lists.newArrayList();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Resource)) return false;

        Resource ro = (Resource)o;
        return Utils.equals(accountName, ro.getAccountName())
                && Utils.equals(email, ro.getEmail())
                && Utils.equals(fullName, ro.getFullName());
    }

    @Override
    public int hashCode() {
        return Utils.hash(accountName, email, fullName);
    }

    public String getAreaName() {
        return areaName;
    }
}
