package com.tresmonos.ui.views;

import org.joda.time.Period;

public class DataUnitViewUtils {

    public static void updateRemainingTime(DataUnitView remainingHoursView, DataUnitView remainingMinutesView, Period period, String defaultTitle) {

        if (period != null) {
            Integer days = period.toStandardDays().getDays();
            if (days >= 1) {
                remainingHoursView.setUnitText("More than");
                remainingHoursView.setDataText("");
                remainingMinutesView.setUnitText(days == 1 ? "day" : "days");
                remainingMinutesView.setDataText("" + days);
            } else {
                Integer hours = period.toStandardHours().getHours();
                boolean displayHours = hours > 0;
                remainingHoursView.setUnitText(displayHours ? "Hrs" : "");
                remainingHoursView.setDataText(displayHours ? hours.toString() : "");

                Integer minutes = period.toStandardMinutes().getMinutes() % 60;
                boolean displayMinutes = minutes > 0;
                remainingMinutesView.setUnitText(displayMinutes ? "Min" : "");
                remainingMinutesView.setDataText(displayMinutes ? minutes.toString() : "");

                if (hours == 0 && minutes == 0) {
                    remainingHoursView.setUnitText("Less than");
                    remainingHoursView.setDataText("");
                    remainingMinutesView.setUnitText("min");
                    remainingMinutesView.setDataText("1");
                }
            }
        } else {
            remainingMinutesView.setDataText("");
            remainingMinutesView.setUnitText(defaultTitle);
            remainingHoursView.clear();
        }
    }
}
