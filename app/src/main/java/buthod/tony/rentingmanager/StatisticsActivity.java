package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by Tony on 11/03/2018.
 */

public class StatisticsActivity extends Activity {

    private SharedPreferences mPreferences = null;

    private Button mPostRequest = null;
    private ImageButton mBackButton = null;
    private String mUsername = null;
    private String mHash = null; // Contains the hash of the password
    private int mStatistics; // Contains the statistics to display

    private BarChart mBarChart = null;
    private Spinner mRentsSpinner = null;
    private Button mYearButton = null;
    private ArrayList<RentsPerMonthsInformation> mRentsPerMonths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStatistics = getIntent().getIntExtra(SendPostRequest.STATISTICS_KEY, -1);
        mPreferences = getSharedPreferences(SettingsActivity.PREFERENCES_NAME,
                Context.MODE_PRIVATE);

        // Check preferences for automatic connection
        mUsername = mPreferences.getString(SettingsActivity.PREF_USERNAME, null);
        mHash = mPreferences.getString(SettingsActivity.PREF_HASH, null);
        if (mUsername == null || mHash == null) {
            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        switch (mStatistics) {
            case R.string.rentsPerMonth:
                onCreateRentsPerMonth();
                getRentsPerMonthsPostRequest();
                break;
        }
    }

    /**
     * Common function called after a setContentView was called.
     * It initializing common listeners and fields.
     */
    private void onCreateCommon() {

        mPostRequest = (Button) findViewById(R.id.post_request);
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        // Listener to send a new post request or go back to main activity
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mPostRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRentsPerMonthsPostRequest();
                mPostRequest.setVisibility(View.GONE);
            }
        });
    }

    /// RENTS PER MONTH PART ///

    private class RentsPerMonthsInformation {
        public int rentId;
        public String rentName;
        public int capacity;
        // Rented days times capacity for each month of the year.
        public int[] rentedDaysTimesCapacity = new int[12];

        public RentsPerMonthsInformation() {
        }
    }

    private void onCreateRentsPerMonth() {
        setContentView(R.layout.rents_per_month);
        onCreateCommon();

        // Get useful widgets
        mBarChart = (BarChart) findViewById(R.id.barChart);
        mRentsSpinner = (Spinner) findViewById(R.id.list_rents);
        mYearButton = (Button) findViewById(R.id.yearButton);
        // Set bar chart parameters
        mBarChart.getDescription().setEnabled(false);
        mBarChart.setOnClickListener(null);
        mBarChart.setOnLongClickListener(null);
        mBarChart.setOnChartGestureListener(null);
        mBarChart.setOnChartValueSelectedListener(null);
        mBarChart.setOnSystemUiVisibilityChangeListener(null);
        mBarChart.setOnHoverListener(null);
        mBarChart.setOnTouchListener(null);
        // Defining X-axis labels
        final String[] labels = getResources().getStringArray(R.array.monthNames);
        IAxisValueFormatter xFormatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < labels.length)
                    return labels[index];
                else
                    return null;
            }
        };
        mBarChart.getXAxis().setValueFormatter(xFormatter);
        // Set rents spinner listener
        mRentsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRentsPerMonthsBarChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        // Set year button listener and message popup
        setYearButtonListener();

        // Hide widgets
        mBarChart.setVisibility(View.GONE);
        mRentsSpinner.setVisibility(View.GONE);
        mYearButton.setVisibility(View.GONE);
    }

    private void setYearButtonListener() {
        Calendar cal = Calendar.getInstance();
        final int currentYear = cal.get(Calendar.YEAR);
        mYearButton.setText(String.valueOf(currentYear));
        mYearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a popup with a number picker, and update the year once selected.
                AlertDialog.Builder builder = new AlertDialog.Builder(StatisticsActivity.this);
                Resources res = getResources();
                // Set the view of the alert dialog
                final NumberPicker numberPicker = new NumberPicker(getBaseContext());
                numberPicker.setMinValue(2000);
                int selectedYear = Integer.parseInt(mYearButton.getText().toString());
                numberPicker.setMaxValue(currentYear + 3);
                numberPicker.setValue(selectedYear);
                builder.setView(numberPicker);
                // Set up the buttons
                builder.setNegativeButton(res.getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                builder.setPositiveButton(res.getString(R.string.ok), null);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        b.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View view) {
                                alertDialog.dismiss();
                                mYearButton.setText(String.valueOf(numberPicker.getValue()));
                                getRentsPerMonthsPostRequest();
                            }
                        });
                    }
                });
                alertDialog.show();
            }
        });
    }

    private void getRentsPerMonthsPostRequest() {
        SendPostRequest req = new SendPostRequest(SendPostRequest.STATISTICS_MANAGER);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.YEAR_KEY,
                Integer.parseInt(mYearButton.getText().toString()));
        req.addPostParam(SendPostRequest.STATISTICS_KEY, 0); // 0 is the statistics' id on server
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    // Parse JSON file
                    try {
                        parseResultRentsPerMonths(result);
                        populateRentsPerMonthsSpinner();
                        mYearButton.setVisibility(View.VISIBLE);
                        updateRentsPerMonthsBarChart();
                    }
                    catch (JSONException e) {
                        // Username and password in preferences are not valid.
                        Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_LONG).show();
                    mPostRequest.setVisibility(View.VISIBLE);
                }
            }
        });
        req.execute();
    }

    /**
     * Parse the result from the server and save it in private field mRentsPerMonths.
     * @param result The result to parse.
     * @throws JSONException
     */
    private void parseResultRentsPerMonths(String result) throws JSONException {
        mRentsPerMonths.clear();
        JSONObject resultObj = new JSONObject(result);
        Iterator<String> keysIterator = resultObj.keys();
        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            JSONObject rentInfo = resultObj.getJSONObject(key);
            RentsPerMonthsInformation info = new RentsPerMonthsInformation();
            info.rentId = Integer.parseInt(key);
            info.rentName = rentInfo.getString(SendPostRequest.RENT_NAME_KEY);
            info.capacity = rentInfo.getInt(SendPostRequest.CAPACITY_KEY);
            JSONArray rentedDaysTimesCapacity = rentInfo.getJSONArray(
                    SendPostRequest.RENTED_DAYS_TIMES_CAPACITY_KEY);
            for (int i = 0; i < rentedDaysTimesCapacity.length(); ++i) {
                info.rentedDaysTimesCapacity[i] = rentedDaysTimesCapacity.getInt(i);
            }

            mRentsPerMonths.add(info);
        }
    }

    /**
     * Populate the spinner for the rents per month.
     */
    private void populateRentsPerMonthsSpinner() {
        // Save the default selected rent
        String selectedRent = (mRentsSpinner.getSelectedItem() != null) ?
                mRentsSpinner.getSelectedItem().toString() : null;

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                R.layout.rent_spinner_item);
        // Save the previous selected rent position
        adapter.add(getResources().getString(R.string.globalStatistics));
        int selectedPosition = 0; // default selection is global statistic
        for (int i = 0; i < mRentsPerMonths.size(); i++) {
            String name = mRentsPerMonths.get(i).rentName;
            if (name.equals(selectedRent)) selectedPosition = i + 1;
            adapter.add(name);
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRentsSpinner.setAdapter(adapter);
        mRentsSpinner.setSelection(selectedPosition);
        mRentsSpinner.setVisibility(View.VISIBLE);
    }

    /**
     * Update the bar chart mBarChart with the rent rate per month.
     */
    private void updateRentsPerMonthsBarChart() {
        // Populate the bar chart with rents rate per months
        int year = Integer.valueOf(mYearButton.getText().toString());
        String selectedRent = mRentsSpinner.getSelectedItem().toString();
        Calendar cal = Calendar.getInstance();
        String globalStatistics = getResources().getString(R.string.globalStatistics);

        // Get the number of rented days for each months.
        // Takes into account the capacity of the rent.
        float[] rentedDays = {0,0,0,0,0,0,0,0,0,0,0,0};
        int totalCapacity = 0;
        for (int i = 0; i < mRentsPerMonths.size(); i++) {
            RentsPerMonthsInformation rentInfo = mRentsPerMonths.get(i);
            if (selectedRent.equals(rentInfo.rentName) ||
                    selectedRent.equals(globalStatistics)) {
                totalCapacity += rentInfo.capacity;
                for (int j = 0; j < 12; ++j) {
                    rentedDays[j] += rentInfo.rentedDaysTimesCapacity[j];
                }
            }
        }

        // Divide by the total capacity
        if (totalCapacity != 0)
            for (int i = 0; i < 12; i++)
                rentedDays[i] /= totalCapacity;

        // Create the dataset
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 12; ++i) {
            // Get the number of days in the month
            cal.set(year, i, 1);
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            // Add a field in entries
            entries.add(new BarEntry(i, rentedDays[i]/daysInMonth));
        }
        BarDataSet dataset = new BarDataSet(entries,
                getResources().getString(R.string.rentsPerMonth) + " (%)");
        dataset.setColor(ContextCompat.getColor(getBaseContext(), R.color.lightOrange));
        // Set the data
        BarData data = new BarData(dataset);
        mBarChart.setData(data);
        mBarChart.setVisibility(View.VISIBLE);
        mBarChart.invalidate();
    }
}
