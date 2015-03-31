package com.tresmonos.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.tresmonos.calendar.CalendarService;
import com.tresmonos.calendar.model.ResourceState;
import com.tresmonos.ui.views.OneButtonView;
import com.tresmonos.ui.views.TwoButtonsView;
import com.vsc.google.api.services.samples.calendar.android.bilik.BaseActivity;
import com.vsc.google.api.services.samples.calendar.android.bilik.R;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.joda.time.Period;

public enum StatusResources implements CustomizableResources {

    busy(ResourceState.Status.busy) {

        @Override
        public int getBodyBarColor() {
            return R.color.busy_room_body_bar;
        }

        @Override
        public int getStatusText() {
            return R.string.busy_status;
        }

		@Override
		public int getButtonBackground() {
			return R.drawable.busy_button_selector;
		}

        @Override
        public View inflateRoomOperations(final BaseActivity activity, final CalendarService calendarService, final ResourceState resourceState) {
            TwoButtonsView view = (TwoButtonsView) getLayoutInflater(activity).inflate(R.layout.two_buttons_view_layout, null);
            view.setButton1Text(activity.getString(R.string.relase_button_text));
            final Long freeMinutes = resourceState.getCurrentExtensionTime(DEFAULT_EXTENSION_TIME).getStandardMinutes();
            String addMoreTimeText = freeMinutes > 0 ? String.format(activity.getString(R.string.add_more_time_button_text), freeMinutes) : activity.getString(R.string.no_more_time_button_text);
            view.setButton2Text(addMoreTimeText);
            view.setButtonsBackground(R.drawable.busy_button_selector);
            view.setOnClickButton1Listener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
	                activity.registerAnalyticsEvent(BaseActivity.Category.UI_ACTION, BaseActivity.Action.BUTTON_PRESS, BaseActivity.Label.MAIN_ACTIVITY_RELEASE_BUTTON, null);
	                calendarService.deleteReservation(resourceState.getResource(), resourceState.getCurrentReservation());
                }
            });
            view.setOnClickButton2Listener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Quick and dirty
                    if (freeMinutes > 0) {
						activity.registerAnalyticsEvent(BaseActivity.Category.UI_ACTION, BaseActivity.Action.BUTTON_PRESS, BaseActivity.Label.MAIN_ACTIVITY_ADD_TIME_BUTTON, null);
                        DateTime newEndDate = resourceState.getCurrentReservation().getEndDate().withDurationAdded(Minutes.minutes(freeMinutes.intValue()).toStandardDuration(), 1);
                        calendarService.updateEndDateTime(resourceState.getCurrentReservation(), newEndDate);
                    }
                }
            });
            return view;
        }
    },
    empty(ResourceState.Status.empty) {

        //TODO quitar de aca
        public final Minutes MIN_NEW_RESERVATION_MINUTES_DURATION = Minutes.minutes(10);

        @Override
        public int getBodyBarColor() {
            return R.color.available_room_body_bar;
        }

        @Override
        public int getStatusText() {
            return R.string.empty_status;
        }

		@Override
		public int getButtonBackground() {
			return R.drawable.empty_button_selector;
		}

		@Override
        public View inflateRoomOperations(final BaseActivity activity, final CalendarService calendarService, final ResourceState resourceState) {
            OneButtonView view = (OneButtonView) getLayoutInflater(activity).inflate(R.layout.one_button_view_layout, null);
            view.setButtonText(activity.getString(R.string.take_room_now_button_text));
            view.setButtonBackground(R.drawable.empty_button_selector);
            view.setOnClickButtonListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
					activity.registerAnalyticsEvent(BaseActivity.Category.UI_ACTION, BaseActivity.Action.BUTTON_PRESS, BaseActivity.Label.MAIN_ACTIVITY_TAKE_NOW_BUTTON, null);
                    DateTime startDate = DateTime.now();

                    Minutes minutesToNextQuarterSlot = getMinutesToNextQuarterSlot(startDate, MIN_NEW_RESERVATION_MINUTES_DURATION);

                    Period timeToNextEvent = resourceState.getTimeToNextEvent(startDate);
                    Duration extensionTime = timeToNextEvent == null ? minutesToNextQuarterSlot.toStandardDuration() : Duration.standardMinutes(Math.min(timeToNextEvent.toStandardMinutes().getMinutes(), minutesToNextQuarterSlot.getMinutes()));
                    DateTime endDate = startDate.plus(extensionTime);
                    calendarService.addReservation("Adhoc reservation", "Reservation created by Bilik application.", startDate, endDate, true);
                }

                private Minutes getMinutesToNextQuarterSlot(DateTime startDate, Minutes minMinutesToExtend) {
                    int currentMinutes = startDate.getMinuteOfHour();
                    int minutestToNextQuarter = 15 - (currentMinutes % 15);
                    if (minutestToNextQuarter < minMinutesToExtend.getMinutes()) {
                        minutestToNextQuarter = 15 + minutestToNextQuarter;
                    }
                    return Minutes.minutes(minutestToNextQuarter);
                }
            });
            return view;
        }
    },
    waitingForConfirmation(ResourceState.Status.waitingForConfirmation) {
        @Override
        public int getBodyBarColor() {
            return R.color.waiting_room_body_bar;
        }

        @Override
        public int getStatusText() {
            return R.string.waiting_status;
        }

		@Override
		public int getButtonBackground() {
			return R.drawable.waiting_button_selector;
		}

		@Override
        public View inflateRoomOperations(final BaseActivity activity, final CalendarService calendarService, final ResourceState resourceState) {
            OneButtonView view = (OneButtonView) getLayoutInflater(activity).inflate(R.layout.one_button_view_layout, null);
            view.setButtonText(activity.getString(R.string.confirm_room_button_text));
            view.setButtonBackground(R.drawable.waiting_button_selector);
            view.setOnClickButtonListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
	                activity.registerAnalyticsEvent(BaseActivity.Category.UI_ACTION, BaseActivity.Action.BUTTON_PRESS, BaseActivity.Label.MAIN_ACTIVITY_CONFIRM_BUTTON, null);
	                calendarService.confirmReservation(resourceState.getCurrentReservation());
                }
            });
            return view;
        }
    };

    private static LayoutInflater getLayoutInflater(Context context) {
        return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private final ResourceState.Status status;

    StatusResources(ResourceState.Status status) {
        this.status = status;
    }

    public static StatusResources getStatusResources(ResourceState.Status status) {
        return StatusResources.valueOf(status.name());
    }

	public ResourceState.Status getStatus() {
		return status;
	}
}
