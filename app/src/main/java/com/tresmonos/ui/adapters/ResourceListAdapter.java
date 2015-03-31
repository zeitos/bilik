package com.tresmonos.ui.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.vsc.google.api.services.samples.calendar.android.bilik.R;
import com.tresmonos.calendar.model.Reservation;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

public class ResourceListAdapter extends BaseAdapter {

    private final static DateTimeFormatter hourFormatter = DateTimeFormat.forPattern("hh:mm a");
    private final static DateTimeFormatter dayFormatter = DateTimeFormat.forPattern("MM/dd");

    private Context context;
    private List<Reservation> events;
    private Reservation currentReservation;
    private DateTime dateTime;

    public ResourceListAdapter(Context context, List<Reservation> events, Reservation currentReservation, DateTime dateTime) {
        this.context = context;
        this.events = events;
        this.currentReservation = currentReservation;
        this.dateTime = dateTime;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Object getItem(int position) {
        return events.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TwoLineListItem twoLineListItem;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            twoLineListItem = (TwoLineListItem) inflater.inflate(
                    android.R.layout.simple_list_item_2, null);
        } else {
            twoLineListItem = (TwoLineListItem) convertView;
        }

        twoLineListItem.setMinimumWidth(400);
        TextView text1 = twoLineListItem.getText1();
        TextView text2 = twoLineListItem.getText2();

        Reservation reservation = events.get(position);
        Resources resources = context.getResources();
        if (isCurrentReservation(reservation)) {
            text1.setTypeface(null, Typeface.BOLD);
            text2.setTypeface(null, Typeface.BOLD);
        }
        text1.setTextColor(resources.getColor(R.color.white));
        text2.setTextColor(resources.getColor(R.color.white));

        text1.setText(reservation.getTitle());
        DateTime startDate = reservation.getStartDate();
        String dayLabel = startDate.getDayOfYear() == dateTime.getDayOfYear() ? "" : dayFormatter.print(startDate) + ": ";
        text2.setText(String.format("%s%s to %s", dayLabel, hourFormatter.print(startDate), hourFormatter.print(reservation.getEndDate())));

        return twoLineListItem;
    }

    private boolean isCurrentReservation(Reservation reservation) {
        return currentReservation != null && currentReservation.equals(reservation);
    }
}