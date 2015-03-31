package com.tresmonos.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.vsc.google.api.services.samples.calendar.android.bilik.R;

public class OneButtonView extends LinearLayout {

    private Button button;

    public OneButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        button = (Button) findViewById(R.id.take_now_button);
    }

    public void setButtonText(String text) {
        button.setText(text);
    }

    public void setButtonBackground(int drawableId) {
        button.setBackground(getResources().getDrawable(drawableId));
    }

    public void setOnClickButtonListener(final OnClickListener l) {
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    button.setEnabled(false);
                    l.onClick(button);
                } finally {
                    button.setEnabled(true);
                }
            }
        });
    }
}