package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Tony on 27/08/2017.
 */

public class PricesActivity extends Activity {
    private final static int NB_WEEK = 53;

    private String mUsername = null;
    private String mHash = null;
    private String mWholeRent = null;
    private boolean mEditRight = false;

    private Spinner mSubrentsSpinner = null;
    private Spinner mYearSpinner = null;
    private TableLayout mTablePrices = null;
    private TextView[] mWeekViews = null;
    private TextView[] mPriceViews = null;

    private HashMap<String, HashMap<Integer, Integer>> mPrices = new HashMap<>();
    private SparseArray<String> mIdMap = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.prices);

        // Recover useful views
        mSubrentsSpinner = (Spinner) findViewById(R.id.list_subrents);
        mYearSpinner = (Spinner) findViewById(R.id.year);
        mTablePrices = (TableLayout) findViewById(R.id.table_prices);
        // Recover the main rent name and the access level
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mWholeRent = extras.getString(SendPostRequest.RENT_NAME_KEY, null);
            mEditRight = extras.getBoolean("editPricesRight", false);
        }
        // If main rent is null, go back to main activity
        if (mWholeRent == null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // Populate the year spinner with the current year and the next year
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);
        Calendar cal = Calendar.getInstance();
        int currYear = cal.get(Calendar.YEAR);
        adapter.add(String.valueOf(currYear));
        adapter.add(String.valueOf(currYear+1));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mYearSpinner.setAdapter(adapter);
        mYearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateWeekViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Send a post request to access information
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        mUsername = prefs.getString(SendPostRequest.USERNAME_KEY, null);
        mHash = prefs.getString(SendPostRequest.HASH_KEY, null);
        if (mUsername != null && mHash != null) {
            SendPostRequest req = new SendPostRequest(SendPostRequest.GET_PRICES);
            req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
            req.addPostParam(SendPostRequest.HASH_KEY, mHash);
            req.addPostParam(SendPostRequest.RENT_NAME_KEY, mWholeRent);
            req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
                @Override
                public void postExecute(boolean success, String result) {
                    if (success) {
                        try {
                            // Save result in local variables
                            parsePrices(result);
                            // Populate the spinner and the table
                            populateView();
                            // Update the table view
                            updatePriceViews();
                            updateWeekViews();
                        }
                        catch (JSONException e) {
                            // Go to main activity
                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                    else {
                        Toast.makeText(getBaseContext(), "Connexion error : " + result,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            req.execute();
        }
        else {
            // Go to main activity
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void parsePrices(String result) throws JSONException {
        JSONObject resObj = new JSONObject(result);

        JSONArray pricesSubrents = resObj.getJSONArray(SendPostRequest.PRICES_KEY);
        for (int i = 0; i < pricesSubrents.length(); i++) {
            JSONObject pricesSubrent = pricesSubrents.getJSONObject(i);
            String rentName = pricesSubrent.getString(SendPostRequest.RENT_NAME_KEY);
            int rentId = pricesSubrent.getInt(SendPostRequest.ID_KEY);
            mIdMap.put(rentId, rentName);
            HashMap<Integer, Integer> pricesSubrentHash = new HashMap<Integer, Integer>();
            JSONArray prices = pricesSubrent.getJSONArray(SendPostRequest.PRICES_KEY);
            JSONArray weeks = pricesSubrent.getJSONArray(SendPostRequest.WEEKS_KEY);
            for (int j = 0; j < prices.length(); j++) {
                int price = prices.getInt(j);
                int week = weeks.getInt(j);
                pricesSubrentHash.put(week, price);
            }
            mPrices.put(rentName, pricesSubrentHash);
        }
    }

    /**
     * Populate the spinner of sub-rents and
     * populate the table layout with empty TextView.
     */
    private void populateView() {
        /* Populate the list of rents */
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);
        // Iterate on mIdMap to keep the same rent order
        for (int i=0; i<mIdMap.size(); i++) {
            adapter.add(mIdMap.valueAt(i));
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSubrentsSpinner.setAdapter(adapter);
        mSubrentsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePriceViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mSubrentsSpinner.invalidate();
        /* Populate the prices table */
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        mPriceViews = new TextView[NB_WEEK];
        mWeekViews = new TextView[NB_WEEK];
        for (int week=0; week<NB_WEEK; week++) {
            TableRow row = new TableRow(getBaseContext());
            if (week%2 == 0)
                row.setBackgroundColor(Color.LTGRAY);
            else
                row.setBackgroundColor(Color.WHITE);
            TextView weekView = new TextView(getBaseContext());
            weekView.setTextColor(Color.BLACK);
            weekView.setPadding(15,5,15,5);
            TextView priceView = new TextView(getBaseContext());
            priceView.setTextColor(Color.BLACK);
            priceView.setPadding(15,5,15,5);
            row.addView(weekView, params);
            row.addView(priceView, params);
            mTablePrices.addView(row);
            mPriceViews[week] = priceView;
            mWeekViews[week] = weekView;
        }
    }

    /**
     * Update TextView of the first column in the TableLayout
     * and display the week depending on the selected year.
     */
    private void updateWeekViews() {
        if (mWeekViews == null) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("MMMM", Locale.getDefault());
        cal.set(Calendar.YEAR, Integer.valueOf(mYearSpinner.getSelectedItem().toString()));
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        for (int week=1; week<=NB_WEEK; week++) {
            // Update the dates
            TextView weekView = mWeekViews[week-1];
            cal.set(Calendar.WEEK_OF_YEAR, week-1);
            String weekText = String.format("%02d", cal.get(Calendar.DATE));
            cal.add(Calendar.DATE, 7);
            weekText += " - " + String.format("%02d", cal.get(Calendar.DATE));
            weekText += "  " + format.format(cal.getTime());
            weekView.setText(weekText);
            weekView.invalidate();
        }
    }

    /**
     * Update TextView of the second column in the TableLayout
     * and display the prices depending on the selected rent.
     */
    private void updatePriceViews() {
        if (mPriceViews == null) {
            return;
        }
        HashMap<Integer, Integer> prices =
                mPrices.get(mSubrentsSpinner.getSelectedItem().toString());
        for (int week=1; week<=NB_WEEK; week++) {
            // Update prices
            TextView priceView = mPriceViews[week-1];
            Integer price = prices.get(week);
            priceView.setText((price!=null) ? String.valueOf(price) : "");
            priceView.invalidate();
        }
    }
}
