package com.vsc.google.api.services.samples.calendar.android.bilik;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.tresmonos.calendar.model.Reservation;
import com.tresmonos.calendar.model.Resource;
import com.tresmonos.calendar.model.ResourceState;
import com.tresmonos.ui.StatusResources;
import com.tresmonos.ui.views.DataUnitView;
import com.tresmonos.ui.views.DataUnitViewUtils;
import com.tresmonos.ui.views.ReservationSummaryView;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

/**
 * Main interface of the application. From here the users can confirm reservations,
 * make ad-hoc meetings, release meeting rooms and see the upcoming events.
 */
public class MainFragment extends BaseFragment {
	/** Factory that produces instances of this fragment */
	public static class Factory implements GeneralActivity.FragmentFactory {
		@Override
		public BaseFragment makeFragment() {
			return new MainFragment();
		}

		@Override
		public String title() {
			return "THIS ROOM";
		}

		@Override
		public int iconString() {
			return R.string.icon_home;
		}
	}

	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm");
	public static final DateTimeFormatter AM_PM_FORMATTER = DateTimeFormat.forPattern("a");
	private final int[] reservationListIds = {R.id.reservation_1, R.id.reservation_2, R.id.reservation_3};
	private TextView roomNameTextView;
	private TextView roomStatusTextView;
	private TextView untilTextView;
	private ReservationSummaryView currentReservationView;
	private ReservationSummaryView[] reservationSummary;
	private FrameLayout operationsLayout;

	public MainFragment() {
        // Required empty public constructor
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Obtain references to the elements of the screen
		roomNameTextView = (TextView) getActivity().findViewById(R.id.room_name);
		roomStatusTextView = (TextView) getActivity().findViewById(R.id.status);
		untilTextView = (TextView) getActivity().findViewById(R.id.until);
		currentReservationView = (ReservationSummaryView) getActivity().findViewById(R.id.current_reservation);
		reservationSummary = new ReservationSummaryView[reservationListIds.length];
		for (int i = 0; i < reservationListIds.length; i++)
			reservationSummary[i] = (ReservationSummaryView) getActivity().findViewById(reservationListIds[i]);
		operationsLayout = (FrameLayout) getActivity().findViewById(R.id.operations_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

	@Override
	public void onResume() {
		super.onResume();
		updateUIOnConfigurationChange();
		onClockTick(new DateTime());
	}

	@Override
	protected void onClockTick(DateTime currentDatetime) {
		DataUnitView actualTime = (DataUnitView) getActivity().findViewById(R.id.actual_time);
		String time = TIME_FORMATTER.print(currentDatetime);
		String ampmTime = AM_PM_FORMATTER.print(currentDatetime);
		actualTime.setDataText(time);
		actualTime.setUnitText(ampmTime);
	}

	/**
	 * Updates the UI every time the configuration changes
	 */
	private void updateUIOnConfigurationChange() {
		roomNameTextView.setText(getConfiguration().getAlternativeResourceName());
		roomStatusTextView.setText("");
		untilTextView.setText("");
		currentReservationView.setReservation(null);
		for (int i = 0 ; i < reservationListIds.length ; i++)
			reservationSummary[i].setReservation(null);
		operationsLayout.removeAllViews();
	}

	private String getUntilText(ResourceState resourceState) {
		ResourceState.Status status = resourceState.getStatus();
		int stringId = 0;
		switch (status) {
			case empty: stringId = R.string.until_busy; break;
			case busy: stringId = R.string.until_release; break;
			case waitingForConfirmation: stringId = R.string.until_auto_release; break;
		}
		return stringId != 0 ? getResources().getString(stringId) : "";
	}

	/**
	 * Update the UI with the current state of the resource associated with this device
	 * @param resourceState state to use
	 */
	@Override
	protected void onResourceStateChange(ResourceState resourceState) {
		DateTime currentDatetime = resourceState.getCurrentDateTime();
		Resource resource = resourceState.getResource();
		roomStatusTextView.setText(getResources().getString(StatusResources.getStatusResources(resourceState.getStatus()).getStatusText()));
		Period timeToNextEvent = resourceState.getStatus() == ResourceState.Status.waitingForConfirmation ?
				resourceState.getTimeToEndOfConfirmation(currentDatetime) :
				resourceState.getTimeToNextEvent(currentDatetime);
		updateRemainingTime(timeToNextEvent);
		untilTextView.setText(timeToNextEvent != null ? getUntilText(resourceState) : "");

		Reservation currentReservation = resourceState.getCurrentReservation();
		currentReservationView.setReservation(currentReservation);

		List<Reservation> reservations = resource.getReservations();
		int currentReservationIndex = reservations.indexOf(currentReservation);
		for (int i = 0 ; i < reservationListIds.length ; i++) {
			currentReservationIndex++;
			Reservation reservation = currentReservationIndex < reservations.size() ? reservations.get(currentReservationIndex) : null;
			reservationSummary[i].setReservation(reservation);
		}

        operationsLayout.removeAllViews();
		if (getConfiguration().isMaster()) {
			StatusResources statusResources = StatusResources.getStatusResources(resourceState.getStatus());
			View operationsView = statusResources.inflateRoomOperations(getBaseActivity(), getBaseActivity().getCalendarService(), resourceState);
			operationsLayout.addView(operationsView);
		}
	}

	private void updateRemainingTime(Period period) {
		DataUnitView remainingHoursView = (DataUnitView) getActivity().findViewById(R.id.remaining_hours);
		DataUnitView remainingMinutesView = (DataUnitView) getActivity().findViewById(R.id.remaining_minutes);
		DataUnitViewUtils.updateRemainingTime(remainingHoursView, remainingMinutesView, period, getResources().getString(R.string.no_more_reservations));
	}
}
