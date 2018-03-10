package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.Set;

/**
 * Activity showing prices for a whole rent and its sub-rents.
 * The user can edit prices.
 */

public class PricesActivity extends Activity {
    private final static int NB_WEEK = 53;

    private String mUsername = null;
    private String mHash = null;
    private String mWholeRent = null;
    private boolean mEditRight = false;
    private String mSelectedSubrent = null;
    private String mSelectedYear = null;

    private Spinner mSubrentsSpinner = null;
    private Spinner mYearSpinner = null;
    private TableLayout mTablePrices = null;
    private TextView[] mWeekViews = null;
    private EditText[] mPriceViews = null;
    private Button mCopyPrices = null;
    // Layout containing mSaveChangeButton and mCancelChangeButton
    private LinearLayout mBottomLayout = null;
    private Button mSaveChangesButton = null;
    private Button mCancelChangesButton = null;
    private ImageButton mBackButton = null;
    private Button mPostRequest = null;

    /* First key corresponds to sub-rent name. Second key is the year.
        The last one is the week number, and finally the price is obtained.
     */
    private HashMap<String, SparseArray<SparseArray<Integer>>> mPrices = new HashMap<>();
    private SparseArray<String> mIdMap = new SparseArray<>();
    /* Contains the changes made on prices
    If the user remove a price, it contains the value -1.
     */
    private HashMap<Integer, Integer> mChanges = new HashMap<>();

    /* Listener for spinners */
    private AdapterView.OnItemSelectedListener mSubrentSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                    mSelectedSubrent = mSubrentsSpinner.getItemAtPosition(position).toString();
                    mChanges.clear();
                    mBottomLayout.setVisibility(View.GONE);
                    updatePriceViews();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };

    private AdapterView.OnItemSelectedListener mYearSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                    mSelectedYear = mYearSpinner.getItemAtPosition(position).toString();
                    mChanges.clear();
                    mBottomLayout.setVisibility(View.GONE);
                    updateWeekViews();
                    updatePriceViews();
                    // Unable copy prices button if the selected year is the last one
                    if (position == mYearSpinner.getAdapter().getCount() - 1)
                        mCopyPrices.setEnabled(false);
                    else
                        mCopyPrices.setEnabled(true);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };

    private View.OnTouchListener mChangesLostDialog = new View.OnTouchListener() {
        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && mChanges.size() > 0) {
                // Initialize an alert dialog
                AlertDialog.Builder alert = new AlertDialog.Builder(PricesActivity.this);
                alert.setTitle(R.string.changeSpinnerTitle);
                alert.setMessage(R.string.changesWillBeLost);
                // Set up the buttons
                Resources res = getResources();
                alert.setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        v.performClick();
                    }
                });
                alert.setNegativeButton(res.getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                // Show the alert dialog
                alert.create().show();
                return true;
            }
            return false;
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.prices);

        // Recover useful views
        mSubrentsSpinner = (Spinner) findViewById(R.id.list_subrents);
        mYearSpinner = (Spinner) findViewById(R.id.year);
        mTablePrices = (TableLayout) findViewById(R.id.table_prices);
        mCopyPrices = (Button) findViewById(R.id.copy_prices);
        mSaveChangesButton = (Button) findViewById(R.id.save_changes);
        mCancelChangesButton = (Button) findViewById(R.id.cancel_changes);
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mBottomLayout = (LinearLayout) findViewById(R.id.bottom_layout);
        mPostRequest = (Button) findViewById(R.id.post_request);
        // Hide views in the layout, show again once a post request succeeds
        mSubrentsSpinner.setVisibility(View.GONE);
        mCopyPrices.setVisibility(View.GONE);
        mYearSpinner.setVisibility(View.GONE);
        mTablePrices.setVisibility(View.GONE);
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
        // Warn the user that changes might be lost if he change the selected rent
        mSubrentsSpinner.setOnTouchListener(mChangesLostDialog);
        // Clear changes and update price views when the user change of rent
        mSubrentsSpinner.setOnItemSelectedListener(mSubrentSpinnerListener);
        // Warn the user that changes might be lost if he change the selected year
        mYearSpinner.setOnTouchListener(mChangesLostDialog);
        // Update weeks and prices views when the selected year changes
        mYearSpinner.setOnItemSelectedListener(mYearSpinnerListener);
        // Add action to buttons
        mCopyPrices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEditRight) {
                    Toast.makeText(getBaseContext(), R.string.noEditPriceRight,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mChanges.size() > 0) {
                    // Initialize an alert dialog
                    AlertDialog.Builder alert = new AlertDialog.Builder(PricesActivity.this);
                    alert.setTitle(R.string.changeSpinnerTitle);
                    alert.setMessage(R.string.changesWillBeLost);
                    // Set up the buttons
                    Resources res = getResources();
                    alert.setPositiveButton(res.getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            copyPricesPreviousYear();
                        }
                    });
                    alert.setNegativeButton(res.getString(R.string.no),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    alert.create().show();
                }
                else {
                    copyPricesPreviousYear();
                }
            }
        });
        mCancelChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChanges.clear();
                mBottomLayout.setVisibility(View.GONE);
                updatePriceViews();
            }
        });
        mSaveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChangesPostRequest();
            }
        });
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mPostRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPricesPostRequest();
                mPostRequest.setVisibility(View.GONE);
            }
        });
        // Populate the table of prices by week
        populatePricesTable();
        // Send a post request to access information
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        mUsername = prefs.getString(SettingsActivity.PREF_USERNAME, null);
        mHash = prefs.getString(SettingsActivity.PREF_HASH, null);
        if (mUsername == null || mHash == null) {
            // Go to main activity
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getPricesPostRequest();
    }

    /**
     * Get prices of the whole rent and its sub-rents with a post request.
     */
    private void getPricesPostRequest() {
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
                        // Show views previously hidden
                        mSubrentsSpinner.setVisibility(View.VISIBLE);
                        mCopyPrices.setVisibility(View.VISIBLE);
                        mYearSpinner.setVisibility(View.VISIBLE);
                        mTablePrices.setVisibility(View.VISIBLE);
                        // Populate spinners
                        populateSpinners();
                        // Update the table view
                        updatePriceViews();
                        updateWeekViews();
                    }
                    catch (JSONException e) {
                        // Username or password or rent name is not valid
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
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
     * Parse the resulting string of the post request.
     * Update mPrices and mIdMap with new values.
     * @param result The string to parse.
     */
    private void parsePrices(String result) throws JSONException {
        // Clear fields to update
        mIdMap.clear();
        mPrices.clear();
        // Parse post request result
        JSONObject resObj = new JSONObject(result);
        JSONArray subrents = resObj.getJSONArray(SendPostRequest.SUBRENTS_KEY);
        for (int i = 0; i < subrents.length(); i++) {
            // Browse the sub-rents
            JSONObject subrent = subrents.getJSONObject(i);
            String rentName = subrent.getString(SendPostRequest.RENT_NAME_KEY);
            int rentId = subrent.getInt(SendPostRequest.ID_KEY);
            mIdMap.put(rentId, rentName);
            SparseArray<SparseArray<Integer>> pricesSubrent = new SparseArray<>();
            JSONObject prices = subrent.getJSONObject(SendPostRequest.PRICES_KEY);
            JSONArray years = prices.names();
            for (int j = 0; j < years.length(); j++) {
                // Browse the years
                int year = years.getInt(j);
                pricesSubrent.put(year, new SparseArray<Integer>());
                JSONArray yearPrices = prices.getJSONArray(years.getString(j));
                for (int k = 0; k < yearPrices.length(); k++) {
                    JSONObject weekPrice = yearPrices.getJSONObject(k);
                    int week = weekPrice.names().getInt(0);
                    int price = weekPrice.getInt(weekPrice.names().getString(0));
                    pricesSubrent.get(year).put(week, price);
                }
            }
            mPrices.put(rentName, pricesSubrent);
        }
    }

    /**
     * Populate sub-rents spinner and years spinner with parsed data.
     */
    private void populateSpinners() {
        /* Disable onItemSelectedListener of spinners */
        mSubrentsSpinner.setOnItemSelectedListener(null);
        mYearSpinner.setOnItemSelectedListener(null);

        /* Populate the list of rents */
        ArrayAdapter<CharSequence> adapterSubrents = new ArrayAdapter<CharSequence>(this,
                R.layout.rent_spinner_item);
        // Save the previous selected rent position if it exists
        int previousRentPosition = -1;
        // Iterate on mIdMap to keep the same rent order
        for (int i = 0; i < mIdMap.size(); i++) {
            String name = mIdMap.valueAt(i);
            adapterSubrents.add(name);
            if (mSelectedSubrent != null && mSelectedSubrent.equals(name))
                previousRentPosition = i;
        }
        adapterSubrents.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSubrentsSpinner.setAdapter(adapterSubrents);
        // Recover the previous selected rent or clear changes
        if (previousRentPosition != -1)
            mSubrentsSpinner.setSelection(previousRentPosition, true);
        else {
            mChanges.clear();
            mBottomLayout.setVisibility(View.GONE);
        }
        mSubrentsSpinner.invalidate();
        mSelectedSubrent = mSubrentsSpinner.getSelectedItem().toString();

        /* Populate the list of year */
        ArrayAdapter<CharSequence> adapterYears = new ArrayAdapter<CharSequence>(this,
                R.layout.year_spinner_item);
        SparseArray<SparseArray<Integer>> years = mPrices.get(mIdMap.valueAt(0));
        // Save the previous selected year position if it exists
        int previousYearPosition = -1;
        int currPos = 0;
        for (int i=years.size()-1; i>=0; --i) {
            int year = years.keyAt(i);
            String yearStr = String.valueOf(year);
            adapterYears.add(yearStr);
            if (mSelectedYear != null && mSelectedYear.equals(yearStr))
                previousYearPosition = currPos;
            currPos++;
        }
        adapterYears.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mYearSpinner.setAdapter(adapterYears);
        // Recover the previous selected year or clear changes
        if (previousYearPosition != -1)
            mYearSpinner.setSelection(previousYearPosition, true);
        else {
            mChanges.clear();
            mBottomLayout.setVisibility(View.GONE);
        }
        mYearSpinner.invalidate();
        mSelectedYear = mYearSpinner.getSelectedItem().toString();

        /* Enable onItemSelectedListener of spinners */
        mSubrentsSpinner.setOnItemSelectedListener(mSubrentSpinnerListener);
        mYearSpinner.setOnItemSelectedListener(mYearSpinnerListener);
    }

    /**
     * Array containing all text watchers for all EditText.
     * This array is initialized by populatePriceTable method.
     * It is useful using removeTextChangedListener to prevent triggering
     * when the text is set programmatically.
     */
    private TextWatcher[] mPriceTextWatchers = null;

    /**
     * Populate the table of prices.
     * Add NB_WEEK rows with an empty TextView representing the week and
     * an empty EditText representing the price.
     */
    private void populatePricesTable() {
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        mPriceViews = new EditText[NB_WEEK];
        mWeekViews = new TextView[NB_WEEK];
        int lightCyanColor = ContextCompat.getColor(getBaseContext(), R.color.lightCyan);
        mPriceTextWatchers = new TextWatcher[NB_WEEK];
        for (int i=0; i<NB_WEEK; i++) {
            TableRow row = new TableRow(getBaseContext());
            // The background color is changed one row out of two
            if (i%2 == 0)
                row.setBackgroundColor(lightCyanColor);
            else
                row.setBackgroundColor(Color.WHITE);
            // Create new views
            TextView weekView = new TextView(getBaseContext());
            EditText priceView = new EditText(getBaseContext());
            mPriceViews[i] = priceView;
            mWeekViews[i] = weekView;
            // Change style
            int darkBlueColor = ContextCompat.getColor(getBaseContext(), R.color.darkBlue);
            weekView.setTextColor(darkBlueColor);
            weekView.setPadding(20,10,20,10);
            weekView.setTextSize(20);
            priceView.setTextColor(darkBlueColor);
            priceView.setPadding(20,10,20,10);
            priceView.setTextSize(20);
            priceView.setMinEms(3);
            priceView.setInputType(InputType.TYPE_CLASS_NUMBER);
            // Disable changes if the user has no rights
            priceView.setFocusable(mEditRight);
            priceView.setFocusableInTouchMode(mEditRight);
            priceView.setLongClickable(mEditRight);
            if (!mEditRight) {
                priceView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getBaseContext(), R.string.noEditPriceRight,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            /* Create a new text watcher.
                It will save changes made by the user in mChanges,
                and will hide or show mBottomLayout to cancel or save changes.
             */
            final int week = i+1;
            mPriceTextWatchers[i] = new TextWatcher() {
                private int mWeek = week;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    int year = Integer.valueOf(mSelectedYear);
                    Integer oldPrice = mPrices.get(mSelectedSubrent).get(year).get(mWeek);
                    String newString = s.toString();
                    if (newString.isEmpty()) {
                        if (oldPrice == null)
                            mChanges.remove(mWeek);
                        else
                            // Put -1 to know the user removed this value
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
            };
            priceView.addTextChangedListener(mPriceTextWatchers[i]);
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
        cal.set(Calendar.YEAR, Integer.valueOf(mSelectedYear));
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
     * Update EditText of the second column in the TableLayout
     * and display the prices depending on the selected rent.
     */
    private void updatePriceViews() {
        if (mPriceViews == null) {
            return;
        }
        int selectedYear = Integer.valueOf(mSelectedYear);
        // Disable onTextChangedListener of EditText */
        for (int i=0; i<NB_WEEK; i++) {
            mPriceViews[i].removeTextChangedListener(mPriceTextWatchers[i]);
        }
        // Update prices with those saved in the database.
        SparseArray<Integer> prices = mPrices.get(mSelectedSubrent).get(selectedYear);
        for (int week=1; week<=NB_WEEK; week++) {
            TextView priceView = mPriceViews[week-1];
            Integer price = prices.get(week);
            priceView.setText((price!=null) ? String.valueOf(price) : "");
            priceView.invalidate();
        }
        // Update prices with changes made.
        for (int week : mChanges.keySet()) {
            TextView priceView = mPriceViews[week-1];
            int price = mChanges.get(week);
            priceView.setText((price!=-1) ? String.valueOf(price) : "");
            priceView.invalidate();
        }
        // Enable onTextChangedListener of EditText */
        for (int i=0; i<NB_WEEK; i++) {
            mPriceViews[i].addTextChangedListener(mPriceTextWatchers[i]);
        }
    }

    /**
     * Copy prices from the previous year to mChanges if the previous year is defined.
     * mChanges is cleared before the copy.
     */
    private void copyPricesPreviousYear() {
        int selectedYear = Integer.valueOf(mSelectedYear);
        SparseArray<Integer> newChanges = mPrices.get(mSelectedSubrent).get(selectedYear-1);
        if (newChanges != null) {
            mChanges.clear();
            // First remove all prices set for the selected year
            SparseArray<Integer> pricesPerWeek = mPrices.get(mSelectedSubrent).get(selectedYear);
            for (int i=0; i<pricesPerWeek.size(); ++i)
                mChanges.put(pricesPerWeek.keyAt(i), -1);
            // Then add prices of the previous year to mChanges
            for (int i=0; i<newChanges.size(); ++i) {
                int week = newChanges.keyAt(i);
                mChanges.put(week, newChanges.get(week));
            }
            // Finally, update prices views
            for (int week = 1; week <= NB_WEEK; week++) {
                Integer price = newChanges.get(week);
                mPriceViews[week-1].setText( price!=null ? String.valueOf(price) : "");
                mPriceViews[week-1].invalidate();
            }
            // Show or hide the bottom layout
            if (mChanges.size() > 0)
                mBottomLayout.setVisibility(View.VISIBLE);
            else
                mBottomLayout.setVisibility(View.GONE);
        }
    }

    private void saveChangesPostRequest() {
        // Disable the button to save changes
        mSaveChangesButton.setEnabled(false);
        // Launch the post request
        SendPostRequest req = new SendPostRequest(SendPostRequest.SET_AND_GET_PRICES);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.RENT_NAME_KEY, mWholeRent);
        // Sub-rent to update prices
        int key = -1;
        int idMapSize = mIdMap.size();
        for (int i=0; i<idMapSize; ++i) {
            // Need to browse manually because SparseArray uses references for comparison.
            if (mIdMap.valueAt(i).equals(mSelectedSubrent))
                key = mIdMap.keyAt(i);
        }
        req.addPostParam(SendPostRequest.SUBRENT_KEY, key);
        req.addPostParam(SendPostRequest.YEAR_KEY, mSelectedYear);
        req.addPostParam(SendPostRequest.PRICES_KEY, mChanges.toString());
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    try {
                        mIdMap.clear();
                        mPrices.clear();
                        mChanges.clear();
                        mBottomLayout.setVisibility(View.GONE);
                        // Save new prices in local variables
                        parsePrices(result);
                        // Update the table view
                        updatePriceViews();
                        Toast.makeText(getBaseContext(), R.string.changeSaved,
                                Toast.LENGTH_SHORT).show();
                    }
                    catch (JSONException e) {
                        // Go to main activity if an error occurs.
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
                else {
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_SHORT).show();
                }
                // Enable the button to save changes
                mSaveChangesButton.setEnabled(true);
            }
        });
        req.execute();
    }

    /**
     * Clear focus of any edit text when the user click next to it.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
