package com.tresmonos.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vsc.google.api.services.samples.calendar.android.bilik.R;

public class DataUnitView extends LinearLayout {

    private final TextView dataView;
    private final TextView unitView;

    public DataUnitView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DataUnitOptions, 0, 0);
        String dataText = a.getString(R.styleable.DataUnitOptions_dataText);
        String unitText = a.getString(R.styleable.DataUnitOptions_unitText);

        //TODO ver el tema de SP... getDimension()... etc...
        float dataFontSize = a.getFloat(R.styleable.DataUnitOptions_dataFontSize, 50f);
        float unitFontSize = a.getFloat(R.styleable.DataUnitOptions_unitFontSize, 10f);
        a.recycle();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.data_unit_layout, this, true);

        dataView = (TextView) findViewById(R.id.data);
        unitView = (TextView) findViewById(R.id.unit);

        setDataText(dataText);
        setUnitText(unitText);
        setDataFontSize(dataFontSize);
        setUnitFontSize(unitFontSize);
    }


    public void setUnitFontSize(float unitFontSize) {
        unitView.setTextSize(unitFontSize);
    }

    public void setDataFontSize(float dataFontSize) {
        dataView.setTextSize(dataFontSize);
    }

    public void setUnitText(String unitText) {
        unitView.setText(unitText);
    }

    public void setDataText(String dataText) {
        dataView.setText(dataText);
    }

    public void clear() {
        setUnitText("");
        setDataText("");
    }
}
