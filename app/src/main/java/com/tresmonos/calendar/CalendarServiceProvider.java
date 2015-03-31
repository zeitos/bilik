package com.tresmonos.calendar;

import com.tresmonos.calendar.google.ActionExecutorCallback;
import com.tresmonos.calendar.model.Account;
import com.tresmonos.calendar.model.LocalResource;
import com.tresmonos.calendar.model.RemoteResource;
import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;

import org.joda.time.DateTime;

import java.util.List;

public interface CalendarServiceProvider {
	/**
	 * @return an {@link Account} identified with the given name. See {@link #getAccounts()}
	 */
	Account getAccount(String name);

	/**
	 * @return all available {@link Account}s in this device.
	 */
	List<Account> getAccounts();

	/**
	 * @return all {@link com.tresmonos.calendar.model.LocalResource}s for the given {@link Account} as they are known on the server
	 */
	void getAllRemoteResources(Account account, ActionExecutorCallback<List<RemoteResource>> callback);

	/**
	 * @return the {@link com.tresmonos.calendar.model.LocalResource}s for the given {@link Account} that are already in this device
	 */
	List<LocalResource> getLocalResources(Account account);

	/**
	 * Creates a reservation on the given resource
	 * @return newly created reservation
	 */
	Reservation createReservation(Resource resource, String title, String description, DateTime startDate, DateTime endDate, ActionExecutorCallback<Reservation> callback, boolean executeNow);

	/**
	 * @return a {@link com.tresmonos.calendar.model.LocalResource} identified with the given name
	 */
	LocalResource getLocalResource(String name);

	/**
	 * Force a synchronization of the given account
	 */
	void sync(String accountName);

	/**
	 * Makes sure that the given resource is the only synced resource in this device
	 * @return if some of the settings were not correct, it returns a message describing the missing settings, otherwise
	 * it returns null
	 */
	String updateSyncableResources(String accountName, String resourceName);

	/**
	 * Declines a reservations for the given resource. Once the operation is completed the provided
	 * callback is invoked.
	 */
	void declineReservationAsync(Resource resource, Reservation reservation, ActionExecutorCallback<Reservation> callback);

	/**
	 * Updates the end time of the given resources on the given resource. Once the operation is completed
	 * the provided callback is invoked
	 */
	void updateEndDateAsync(LocalResource resource, Reservation currentReservation, DateTime newEndDate, ActionExecutorCallback<Reservation> callback);
}
