package com.tresmonos.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tresmonos.calendar.model.Reservation;
import com.vsc.google.api.services.samples.calendar.android.bilik.R;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ReservationSummaryView extends LinearLayout {

    private final static DateTimeFormatter hourFormatter = DateTimeFormat.forPattern("hh:mm a");
    private final static DateTimeFormatter dayFormatter = DateTimeFormat.forPattern("MM/dd");

    private TextView titleView;
    private TextView timeView;
    private View lineStateView;

    public ReservationSummaryView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ReservationOptions, 0, 0);
        int textPadding = a.getInteger(R.styleable.ReservationOptions_textPadding, 0);
        boolean highlight = a.getBoolean(R.styleable.ReservationOptions_highLight, false);
        boolean showLineState = a.getBoolean(R.styleable.ReservationOptions_showLineState, true);
        a.recycle();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.reservation_summary_layout, this, true);

        titleView = (TextView) findViewById(R.id.summary_title_1);
        timeView = (TextView) findViewById(R.id.summary_time_1);
        lineStateView = findViewById(R.id.line_state);

        timeView.setPadding(textPadding, timeView.getPaddingTop(), timeView.getPaddingRight(), timeView.getPaddingBottom());
        titleView.setPadding(textPadding, titleView.getPaddingTop(), titleView.getPaddingRight(), titleView.getPaddingBottom());

        setHightlight(highlight);

        lineStateView.setVisibility(showLineState ? VISIBLE : INVISIBLE);
    }

    public void setReservation(Reservation reservation) {
        String title = "";
        String time = "";

        if (reservation != null) {
            title = reservation.getTitle();
            time = String.format("%s to %s", hourFormatter.print(reservation.getStartDate()), hourFormatter.print(reservation.getEndDate()));
            // add more customization here like bulletState
        }

        titleView.setText(title);
        timeView.setText(time);
    }

    public void setHightlight(boolean hightlight) {
        int style = hightlight ? Typeface.BOLD : Typeface.NORMAL;
        titleView.setTypeface(null, style);
        timeView .setTypeface(null, style);
    }
}
