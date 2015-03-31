package com.tresmonos.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.vsc.google.api.services.samples.calendar.android.bilik.R;

public class TwoButtonsView extends LinearLayout {

    private Button button1;
    private Button button2;

    public TwoButtonsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
    }

    public void setButton1Text(String text) {
        button1.setText(text);
    }

    public void setButton2Text(String text) {
        button2.setText(text);
    }

    public void setButtonsBackground(int drawableId) {
        button1.setBackground(getResources().getDrawable(drawableId));
        button2.setBackground(getResources().getDrawable(drawableId));
    }

    public void setOnClickButton1Listener(final OnClickListener l) {
        button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    button1.setEnabled(false);
                    l.onClick(button1);
                } finally {
                    button1.setEnabled(true);
                }
            }
        });
    }

    public void setOnClickButton2Listener(final OnClickListener l) {
        button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    button2.setEnabled(false);
                    l.onClick(button2);
                } finally {
                    button2.setEnabled(true);
                }
            }
        });
    }
}
