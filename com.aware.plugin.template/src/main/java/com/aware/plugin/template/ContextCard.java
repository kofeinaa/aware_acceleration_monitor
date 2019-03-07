package com.aware.plugin.template;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.Locale;

public class ContextCard implements IContextCard {

    private static final String CURRENT_ACCELERATION = "Current acceleration";
    private static final String ACCELERATION_CHANGE = "Acceleration change";

    private TextView status = null;
    private LineChart chart;
    private LineChart changeChart;
    private Spinner edit;
    private float lastAccelerationValue;
    private Float maxAcceleration = Float.MIN_VALUE;

    //Constructor used to instantiate this card
    public ContextCard() {
    }

    @Override
    public View getContextCard(Context context) {
        View card = LayoutInflater.from(context).inflate(R.layout.card, null);
        status = card.findViewById(R.id.status);

        //spinner for setting maximum allowed acceleration
        edit = card.findViewById(R.id.setAcceleration);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.accelerations, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        edit.setAdapter(adapter);
        edit.setOnItemSelectedListener(new AccelerationSelectedListener());

        chart = (LineChart) card.findViewById(R.id.currentAccelerationChart);
        changeChart = (LineChart) card.findViewById(R.id.accelerationChangeChart);
        lastAccelerationValue = 0f;

        initializeChart(chart);
        initializeChart(changeChart);

        //Register the broadcast receiver that will update the UI from the background service (Plugin)
        IntentFilter filter = new IntentFilter("ACCELEROMETER_DATA");
        context.registerReceiver(accelerometerObserver, filter);

        return card;
    }

    private void initializeChart(LineChart chart) {
        //Configure chart
        chart.setDrawGridBackground(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);

        //data defaults
        LineData lineData = new LineData();
        chart.setData(lineData);
    }

    private void addData(Float newValue) {
        LineData data = chart.getData();

        int color = newValue > maxAcceleration ? Color.RED : Color.GREEN;
        if (data.getDataSetByIndex(0) == null) {
            data.addDataSet(createDataSet(CURRENT_ACCELERATION, color));
        }

        ((LineDataSet) data.getDataSetByLabel(CURRENT_ACCELERATION, false)).setColor(color);
        data.addEntry(new Entry(data.getEntryCount(), newValue), 0);

        LineData changeData = changeChart.getData();
        if (changeData.getDataSetByIndex(0) == null) {
            changeData.addDataSet(createDataSet(ACCELERATION_CHANGE, Color.BLUE));
        }

        float accelerationChange = calculateAccelerationChange(lastAccelerationValue, newValue);
        changeData.addEntry(new Entry(changeData.getEntryCount(), accelerationChange), 0);
        lastAccelerationValue = newValue;

        refreshChart(chart);
        refreshChart(changeChart);
    }

    private float calculateAccelerationChange(float lastAccelerationValue, float newValue) {
        return newValue - lastAccelerationValue;
    }

    private void refreshChart(LineChart chart) {
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(100);
        chart.moveViewToX(chart.getLineData().getEntryCount());
    }

    private LineDataSet createDataSet(String label, int color) {
        LineDataSet dataSet = new LineDataSet(null, label);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setLineWidth(3f);
        dataSet.setColor(color);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        return dataSet;
    }

    //This broadcast receiver is auto-unregistered because it's not static.
    private AccelerometerObserver accelerometerObserver = new AccelerometerObserver();

    public class AccelerometerObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (maxAcceleration > 0
                    && intent.getAction().equalsIgnoreCase("ACCELEROMETER_DATA")) {
                ContentValues data = intent.getParcelableExtra("data");
                Float zValue = (Float) data.get("double_values_2");

                status.setText(String.format(
                        Locale.ENGLISH,
                        "Current acceleration: %.2f",
                        zValue));
                addData(zValue);
            }
        }
    }

    public class AccelerationSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String selected = parent.getItemAtPosition(pos).toString();
            maxAcceleration = Float.valueOf(selected);
        }

        public void onNothingSelected(AdapterView parent) {
        }
    }
}
