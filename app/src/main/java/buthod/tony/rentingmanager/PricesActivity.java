package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private int mCountChanges = 0;

    private Spinner mSubrentsSpinner = null;
    private Spinner mYearSpinner = null;
    private TableLayout mTablePrices = null;
    private TextView[] mWeekViews = null;
    private EditText[] mPriceViews = null;
    private LinearLayout mBottomLayout = null;
    private Button mSaveChangesButton = null;
    private Button mCancelChangesButton = null;

    private HashMap<String, HashMap<Integer, Integer>> mPrices = new HashMap<>();
    private SparseArray<String> mIdMap = new SparseArray<>();
    /* Contains the changes made on prices
    If the user remove a price, it contains the value -1.
     */
    private HashMap<Integer, Integer> mChanges = new HashMap<>();
    /* Indicate if a post request is running. Allow to have only 1 post request. */
    private boolean mRequestRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.prices);

        // Recover useful views
        mSubrentsSpinner = (Spinner) findViewById(R.id.list_subrents);
        mYearSpinner = (Spinner) findViewById(R.id.year);
        mTablePrices = (TableLayout) findViewById(R.id.table_prices);
        mSaveChangesButton = (Button) findViewById(R.id.save_changes);
        mCancelChangesButton = (Button) findViewById(R.id.cancel_changes);
        mBottomLayout = (LinearLayout) findViewById(R.id.bottom_layout);
        // Hide the bottom layout
        mBottomLayout.setVisibility(View.GONE);
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
        // Add action to buttons
        mCancelChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChanges.clear();
                updatePriceViews();
            }
        });
        mSaveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChangesPostRequest();
            }
        });
        // Send a post request to access information
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        mUsername = prefs.getString(SendPostRequest.USERNAME_KEY, null);
        mHash = prefs.getString(SendPostRequest.HASH_KEY, null);
        if (mUsername != null && mHash != null) {
            SendPostRequest req = new SendPostRequest(SendPostRequest.SET_AND_GET_PRICES);
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
        mSubrentsSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && mChanges.size() > 0) {
                    // Initialize an alert dialog
                    AlertDialog.Builder alert = new AlertDialog.Builder(PricesActivity.this);
                    alert.setTitle(R.string.changeRentTitle);
                    alert.setMessage(R.string.changesWillBeLost);
                    // Set up the buttons
                    alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSubrentsSpinner.performClick();
                        }
                    });
                    alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    alert.create().show();
                    return true;
                }
                return false;
            }
        });
        mSubrentsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mChanges.clear();
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
        mPriceViews = new EditText[NB_WEEK];
        mWeekViews = new TextView[NB_WEEK];
        for (int i=0; i<NB_WEEK; i++) {
            TableRow row = new TableRow(getBaseContext());
            if (i%2 == 0)
                row.setBackgroundColor(Color.LTGRAY);
            else
                row.setBackgroundColor(Color.WHITE);
            TextView weekView = new TextView(getBaseContext());
            EditText priceView = new EditText(getBaseContext());
            mPriceViews[i] = priceView;
            mWeekViews[i] = weekView;
            weekView.setTextColor(Color.BLACK);
            weekView.setPadding(15,5,15,5);
            priceView.setTextColor(Color.BLACK);
            priceView.setPadding(15,5,15,5);
            priceView.setMinEms(3);
            priceView.setInputType(InputType.TYPE_CLASS_NUMBER);
            // Disable changes if the user has no rights
            priceView.setFocusable(mEditRight);
            if (!mEditRight) {
                priceView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getBaseContext(), R.string.noEditPriceRight,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            // Save changes made in a HashMap to update the database later.
            final int week = i+1;
            priceView.addTextChangedListener(new TextWatcher() {
                private int mWeek = week;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String rent = mSubrentsSpinner.getSelectedItem().toString();
                    Integer oldPrice = mPrices.get(rent).get(mWeek);
                    String newString = s.toString();
                    if (newString.isEmpty()) {
                        if (oldPrice == null)
                            mChanges.remove(mWeek);
                        else
                            // Put -1 to know the user remove this value
                            mChanges.put(mWeek, -1);
                    }
                    else {
                        // newPrice is a positive integer since TYPE_CLASS_NUMBER is used.
                        Integer newPrice = Integer.valueOf(newString);
                        if (!newPrice.equals(oldPrice))
                            mChanges.put(mWeek, newPrice);
                        else
                            mChanges.remove(mWeek);
                    }
                    // Show or hide the bottom layout
                    if (mChanges.size() > 0)
                        mBottomLayout.setVisibility(View.VISIBLE);
                    else
                        mBottomLayout.setVisibility(View.GONE);
                }
            });
            row.addView(weekView, params);
            row.addView(priceView, params);
            mTablePrices.addView(row);
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

    private void saveChangesPostRequest() {
        // At most 1 post request is running
        if (mRequestRunning) {
            Toast.makeText(getBaseContext(), "A request is already running",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Disable the sub-rents spinner during the post request
        mSubrentsSpinner.setEnabled(false);
        // Launch the post request
        mRequestRunning = true;
        SendPostRequest req = new SendPostRequest(SendPostRequest.SET_AND_GET_PRICES);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.RENT_NAME_KEY, mWholeRent);
        // Sub-rent to update prices
        req.addPostParam(SendPostRequest.SUBRENT_KEY,
                mIdMap.keyAt(mIdMap.indexOfValue(mSubrentsSpinner.getSelectedItem().toString())));
        req.addPostParam(SendPostRequest.PRICES_KEY, mChanges.toString());
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    try {
                        mChanges.clear();
                        mIdMap.clear();
                        mPrices.clear();
                        // Save new prices in local variables
                        parsePrices(result);
                        // Update the table view
                        updatePriceViews();
                        Toast.makeText(getBaseContext(), "Changes saved", Toast.LENGTH_SHORT).show();
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
                // Enable the sub-rents spinner
                mSubrentsSpinner.setEnabled(true);
            }
        });
        req.execute();
    }
}
