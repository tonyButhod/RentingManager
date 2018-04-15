package buthod.tony.rentingmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity showing booking made on a whole rent and its sub-rents.
 * Show the results in a Calendar.
 */

public class RentActivity extends FragmentActivity {

    private String mUsername = null;
    private String mHash = null; // Contain the hash of the password
    private int mAccessLevel = 0; // User access level
    /* 0 : read access for everything, edit only if the user is owner of the rent.
       1 : add booking every where even if the user is not the owner.
       2 : edit everything.
     */
    private List<String> mOwners = new ArrayList<>();
    private boolean mBookingRight = false;
    private String mSelectedSubrent = null;

    private Spinner mSpinner = null;
    private CaldroidBookingFragment mCaldroidFragment = null;
    private FrameLayout mCaldroidContainer = null;
    private TextView mTenantsLabel = null;
    private TableLayout mTenants = null;
    private TextView mSelectDateLabel = null;
    private Button mAddBooking = null;
    private Button mRemoveBooking = null;
    private Button mModifyBooking = null;
    private Button mPrices = null;
    private ImageButton mBackButton = null;
    private Button mPostRequest = null;

    private RentalBooking mBooking = null;
    private String mWholeRent = null;
    private Date mSelectedDate = null;
    private int mSelectedDuration = 0;
    private boolean mIsOnSelection = false;
    private Date mMinDate = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.rent);

        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mSpinner = (Spinner) findViewById(R.id.list_subrents);
        mCaldroidContainer = (FrameLayout) findViewById(R.id.caldroid_container);
        mSelectDateLabel = (TextView) findViewById(R.id.select_date_label);
        mAddBooking = (Button) findViewById(R.id.add_booking);
        mRemoveBooking = (Button) findViewById(R.id.remove_booking);
        mModifyBooking = (Button) findViewById(R.id.modify_booking);
        mPrices = (Button) findViewById(R.id.prices);
        mTenantsLabel = (TextView) findViewById(R.id.tenants_label);
        mTenants = (TableLayout) findViewById(R.id.tenants);
        mPostRequest = (Button) findViewById(R.id.post_request);
        // Hide views in the activity, show again once a post request succeeds
        mSpinner.setVisibility(View.GONE);
        mCaldroidContainer.setVisibility(View.GONE);
        mSelectDateLabel.setVisibility(View.GONE);
        mAddBooking.setVisibility(View.GONE);
        mRemoveBooking.setVisibility(View.GONE);
        mModifyBooking.setVisibility(View.GONE);
        mPrices.setVisibility(View.GONE);
        mTenantsLabel.setVisibility(View.GONE);
        mTenants.setVisibility(View.GONE);
        // Recover the main rent name
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mWholeRent = extras.getString(SendPostRequest.RENT_NAME_KEY, null);
        }
        // If main rent is null, go back to MainActivity
        if (mWholeRent == null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // Initialize mBooking attribute
        mBooking = new RentalBooking();
        // OnClickListener for the spinner
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedSubrent = mSpinner.getItemAtPosition(position).toString();
                updateCaldroidView();
                updateTenantInfo();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        // Add event listener to buttons
        mAddBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddBookingDialog();
            }
        });
        mRemoveBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRemoveBookingDialog();
            }
        });
        mModifyBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showModifyBookingDialog();
            }
        });
        mPrices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PricesActivity.class);
                intent.putExtra(SendPostRequest.RENT_NAME_KEY, mWholeRent);
                intent.putExtra("editPricesRight",
                        mAccessLevel >= 2 || mOwners.contains(mUsername));
                startActivity(intent);
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
                getRentInfoPostRequest();
                mPostRequest.setVisibility(View.GONE);
            }
        });
        // Set calendar view
        mCaldroidFragment = new CaldroidBookingFragment();
        Bundle args = new Bundle();
        Calendar cal = Calendar.getInstance();
        args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        mCaldroidFragment.setArguments(args);
        // Set the minimum date as 3 months before the current date.
        // This limits the data sent during the request.
        cal.add(Calendar.MONTH, -3);
        mMinDate = cal.getTime();
        mCaldroidFragment.setMinDate(mMinDate);
        // Set caldroid listener
        mCaldroidFragment.setCaldroidListener(new CaldroidListener() {
            @Override
            public void onSelectDate(Date date, View view) {
                mCaldroidFragment.clearSelectedDates();
                if (mIsOnSelection) {
                    // The user did a long click, several date are selected
                    mCaldroidFragment.clearBackgroundDrawableForDate(mSelectedDate);
                    Calendar clickedDate = Calendar.getInstance();
                    clickedDate.setTime(date);
                    Calendar currDate = Calendar.getInstance();
                    currDate.setTime(mSelectedDate);
                    // Estimation of days between 2 dates
                    // This estimation is used to reduce computation time
                    int daysBetween = (int) ((date.getTime()-mSelectedDate.getTime())/86400000);
                    // Calculate the exact number of days between 2 dates using the previous value
                    currDate.add(Calendar.DATE, daysBetween);
                    while (currDate.compareTo(clickedDate) < 0) {
                        currDate.add(Calendar.DATE, 1);
                        daysBetween++;
                    }
                    while (currDate.compareTo(clickedDate) > 0) {
                        currDate.add(Calendar.DATE, -1);
                        daysBetween--;
                    }
                    if (daysBetween > 0) {
                        mSelectedDuration = daysBetween;
                        mCaldroidFragment.setSelectedDates(mSelectedDate, date);
                    }
                    else {
                        mSelectedDuration = -daysBetween;
                        mCaldroidFragment.setSelectedDates(date, mSelectedDate);
                        mSelectedDate = date;
                    }
                }
                else {
                    // Otherwise, we select the clicked date
                    mSelectedDuration = 0;
                    mSelectedDate = date;
                    mCaldroidFragment.setSelectedDate(mSelectedDate);
                    updateTenantInfo();
                }
                mIsOnSelection = false;
                mCaldroidFragment.refreshView();

                // Show only useful buttons depending on the number of date selected
                mSelectDateLabel.setVisibility(View.GONE);
                if (mSelectedDuration > 0) {
                    mAddBooking.setVisibility(View.VISIBLE);
                    mRemoveBooking.setVisibility(View.GONE);
                    mModifyBooking.setVisibility(View.GONE);
                }
                else {
                    mAddBooking.setVisibility(View.GONE);
                    mRemoveBooking.setVisibility(View.VISIBLE);
                    mModifyBooking.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onChangeMonth(int month, int year) {
            }

            @Override
            public void onLongClickDate(Date date, View view) {
                if (mSelectedDate != null && mIsOnSelection)
                    mCaldroidFragment.clearBackgroundDrawableForDate(mSelectedDate);
                mSelectedDate = date;
                mSelectedDuration = 0;
                mIsOnSelection = true;
                mCaldroidFragment.clearSelectedDates();
                // Draw a green border on the long clicked date
                Drawable greenBorder = ContextCompat.getDrawable(getBaseContext(),
                        R.drawable.custom_cell_view_green);
                mCaldroidFragment.setBackgroundDrawableForDate(greenBorder, mSelectedDate);
                mCaldroidFragment.refreshView();
                // Remove all tenants in the view
                mTenants.removeAllViews();
                mTenants.invalidate();
                // Hide all action buttons (add, remove, modify booking)
                mSelectDateLabel.setVisibility(View.VISIBLE);
                mAddBooking.setVisibility(View.GONE);
                mRemoveBooking.setVisibility(View.GONE);
                mModifyBooking.setVisibility(View.GONE);
            }
        });
        // Commit changes
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.caldroid_container, mCaldroidFragment);
        t.commit();
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
        mSelectedDate = null;
        mSelectedDuration = 0;
        mCaldroidFragment.clearSelectedDates();
        updateTenantInfo();
        mSelectDateLabel.setVisibility(View.GONE);
        mAddBooking.setVisibility(View.GONE);
        mRemoveBooking.setVisibility(View.GONE);
        mModifyBooking.setVisibility(View.GONE);
        getRentInfoPostRequest();
    }

    /**
     * Get all bookings of one whole rent and its sub-rents with a post request.
     */
    private void getRentInfoPostRequest() {
        // Send a post request
        SendPostRequest req = new SendPostRequest(SendPostRequest.GET_RENT_INFO);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.RENT_NAME_KEY, mWholeRent);
        // Add a minimum date to limit the flow of data.
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        req.addPostParam(SendPostRequest.MIN_DATE_KEY, format.format(mMinDate));
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    try {
                        // Save result in local variables
                        parseInfo(result);
                        // Show calendar, buttons, spinner and tenants
                        mSelectDateLabel.setVisibility(View.VISIBLE);
                        mSpinner.setVisibility(View.VISIBLE);
                        mCaldroidContainer.setVisibility(View.VISIBLE);
                        mPrices.setVisibility(View.VISIBLE);
                        mTenantsLabel.setVisibility(View.VISIBLE);
                        mTenants.setVisibility(View.VISIBLE);
                        // Update views with new variables
                        updateCaldroidView();
                        updateTenantInfo();
                    }
                    catch (JSONException | ParseException e) {
                        // Invalid username or password or rent name.
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
     * Parse the string "result" to save booking information in variable "mBooking".
     * Update also the spinner and the booking right.
     * @param result The string to parse with JSON format.
     */
    private void parseInfo(String result) throws JSONException, ParseException {
        mBooking.clear();

        JSONObject resObj = new JSONObject(result);
        /* Get the access level and owners of the rent */
        mAccessLevel = resObj.getInt(SendPostRequest.ACCESS_KEY);
        JSONArray owners = resObj.getJSONArray(SendPostRequest.OWNERS_KEY);
        for (int i=0; i<owners.length(); i++) {
            JSONObject owner = owners.getJSONObject(i);
            String ownerUsername = owner.getString(SendPostRequest.USERNAME_KEY);
            mOwners.add(ownerUsername);
        }
        // Update the booking right of the user
        mBookingRight = mAccessLevel >= 1 || mOwners.contains(mUsername);

        /* Populate the list of rents */
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                R.layout.rent_spinner_item);
        JSONArray subrents = resObj.getJSONArray(SendPostRequest.SUBRENTS_KEY);
        // Save the previous selected rent position
        int previousRentPosition = -1;
        for (int i=0; i<subrents.length(); i++) {
            JSONObject subrent = subrents.getJSONObject(i);
            String name = subrent.getString(SendPostRequest.RENT_NAME_KEY);
            // If the name corresponds to the previous selected rent, save the position
            if (mSelectedSubrent != null && mSelectedSubrent.equals(name))
                previousRentPosition = i;
            int id = subrent.getInt(SendPostRequest.ID_KEY);
            mBooking.addSubrent(name, id);
            adapter.add(name);
        }
        mBooking.setWholeRent(mWholeRent);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        // Recover the previous selected rent
        if (previousRentPosition != -1)
            mSpinner.setSelection(previousRentPosition);
        mSpinner.invalidate();
        mSelectedSubrent = mSpinner.getSelectedItem().toString();

        /* Parse the result for booking */
        // First calculate the number of sub-rents rented while parsing.
        JSONArray bookings = resObj.getJSONArray(SendPostRequest.BOOKING_KEY);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        for (int i=0; i<bookings.length(); i++) {
            JSONObject booking = bookings.getJSONObject(i);
            long bookingId = booking.getLong(SendPostRequest.ID_KEY);
            int rentId = booking.getInt(SendPostRequest.RENT_KEY);
            Date date = format.parse(booking.getString(SendPostRequest.DATE_KEY));
            int duration = booking.getInt(SendPostRequest.DURATION_KEY);
            String tenant = booking.getString(SendPostRequest.TENANT_KEY);
            mBooking.addBooking(bookingId, rentId, date, duration, tenant);
        }
    }

    /**
     * Update Caldroid View. Reads the selected rent in the spinner,
     * and change the background drawable.
     * Specific color are used depending on if the whole rent is rented or only a part.
     */
    private void updateCaldroidView() {
        int nb_subrents = mBooking.getNumberSubrent()-1; // Doesn't count the whole rent.
        HashMap<Date, Integer> nbBookingMap = new HashMap<>();

        // Check which sub-rent is selected.
        // If the whole rent is selected, all sub-rent are selected
        Iterable<String> subrentSet;
        if (mSelectedSubrent.equals(mWholeRent))
            // If the whole location is selected, all sub-rents are selected
            subrentSet = mBooking.getSubrentSet();
        else {
            // Otherwise only the sub-rent and the whole rent are selected
            ArrayList<String> selectedSet = new ArrayList<String>();
            selectedSet.add(mSelectedSubrent);
            selectedSet.add(mWholeRent);
            subrentSet = selectedSet;
        }

        // Iterate on sub-rents
        mCaldroidFragment.clearBooking();
        for (String subrent : subrentSet) {
            // Iterate on booking per sub-rent
            if (subrent.equals(mSelectedSubrent) || subrent.equals(mWholeRent)) {
                // The rent is full
                for (RentalBooking.BookingInformation b : mBooking.getAllBookings(subrent)) {
                    mCaldroidFragment.addBooking(b.date, b.duration, CaldroidBookingFragment.FULL);
                    mCaldroidFragment.addDifferentTenants(b.date, b.duration);
                }
            }
            else {
                // The rent can be full or partially rented depending on whether all sub-rents
                // are rented or not.
                for (RentalBooking.BookingInformation b : mBooking.getAllBookings(subrent)) {
                    // Count the number of sub-rents to know if the whole rent is rented.
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(b.date);
                    for (int i=0; i<b.duration; i++) {
                        Date date = cal.getTime();
                        Integer nb_booking = nbBookingMap.get(date);
                        int new_nb_booking = ((nb_booking != null) ? nb_booking : 0) + 1;
                        nbBookingMap.put(date, new_nb_booking);
                        // Put the week state in CaldroidBookingFragment
                        int state = (new_nb_booking == nb_subrents) ?
                                CaldroidBookingFragment.FULL : CaldroidBookingFragment.HALF_FULL;
                        mCaldroidFragment.addBooking(date, state);
                        cal.add(Calendar.DATE, 1);
                    }
                    mCaldroidFragment.addDifferentTenants(b.date, b.duration);
                }
            }
        }
        mCaldroidFragment.refreshView();
    }

    /**
     * Show tenants of the selected date in the table layout.
     */
    private void updateTenantInfo() {
        mTenants.removeAllViews();
        if (mSelectedDate != null) {
            Map<String, String> tenants = mBooking.getTenants(mSelectedSubrent, mSelectedDate);
            int darkBlueColor = ContextCompat.getColor(getBaseContext(), R.color.darkBlue);
            for (Map.Entry<String, String> entry : tenants.entrySet()) {
                TextView tenant = new TextView(getBaseContext());
                tenant.setText("-" + entry.getKey() + " : " + entry.getValue());
                tenant.setTextColor(darkBlueColor);
                tenant.setTextSize(18);
                mTenants.addView(tenant);
            }
        }
        mTenants.invalidate();
    }

    private void showAddBookingDialog() {
        // Check if the user has the right to add a booking
        if (!mBookingRight) {
            Toast.makeText(getBaseContext(), R.string.noBookingRight,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if at least one night is selected
        if (mSelectedDate == null || mSelectedDuration == 0) {
            Toast.makeText(getBaseContext(), R.string.noDateSelected,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if at least one rent is free
        List<String> freeRents = mBooking.getFreeRentsForDate(mSelectedDate, mSelectedDuration);
        if (freeRents.size() == 0) {
            Toast.makeText(getBaseContext(), R.string.noRentFree,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.addBookingTitle);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.add_booking, null);
        builder.setView(alertView);
        // Get useful view
        final EditText tenantInput = (EditText) alertView.findViewById(R.id.tenant_edit);
        final Spinner rentChoice = (Spinner) alertView.findViewById(R.id.rent_choice);
        final TextView dateView = (TextView) alertView.findViewById(R.id.date);
        final TextView errorView = (TextView) alertView.findViewById(R.id.error);
        // Set the text of the date view
        Calendar cal = Calendar.getInstance();
        cal.setTime(mSelectedDate);
        cal.add(Calendar.DATE, mSelectedDuration);
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        dateView.setText(format.format(mSelectedDate) + " - " + format.format(cal.getTime()));
        // Populate the spinner of alert dialog with free rents.
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);
        adapter.addAll(freeRents);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rentChoice.setAdapter(adapter);
        // If the selected rent in the activity is free, set this rent as default in dialog.
        if (freeRents.contains(mSelectedSubrent)) {
            int defaultPos = adapter.getPosition(mSelectedSubrent);
            rentChoice.setSelection(defaultPos);
        }
        // Set up the buttons
        Resources res = getResources();
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton(res.getString(R.string.add), null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        String rent = rentChoice.getSelectedItem().toString();
                        String tenant = tenantInput.getText().toString();
                        if (tenant.isEmpty())
                            errorView.setVisibility(View.VISIBLE);
                        else {
                            errorView.setVisibility(View.GONE);
                            addBookingPostRequest(rent, tenant);
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void addBookingPostRequest(final String rent, final String tenant) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        final Date dateRequest = mSelectedDate;
        final int durationRequest = mSelectedDuration;
        final int rentId = mBooking.getIdRent(rent);

        SendPostRequest req = new SendPostRequest(SendPostRequest.ADD_BOOKING);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.DATE_KEY, format.format(mSelectedDate));
        req.addPostParam(SendPostRequest.DURATION_KEY, mSelectedDuration);
        req.addPostParam(SendPostRequest.RENT_KEY, rentId);
        req.addPostParam(SendPostRequest.TENANT_KEY, tenant);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    // Try to parse the result as a long value.
                    try {
                        // The received value is the booking id of the new booking added.
                        long bookingId = Long.parseLong(result);
                        Toast.makeText(getBaseContext(), R.string.newBooking,
                                Toast.LENGTH_SHORT).show();
                        mBooking.addBooking(bookingId, rentId, dateRequest, durationRequest, tenant);
                        mSelectedDate = null;
                        mCaldroidFragment.clearSelectedDates();
                        updateCaldroidView();
                        updateTenantInfo();
                    }
                    catch (NumberFormatException e) {
                        // An error occurred, the booking was not added.
                        // Display an error message depending on the result.
                        if (result.equals(SendPostRequest.RENT_NOT_FREE)){
                            Toast.makeText(getBaseContext(), R.string.rentNotFreeError,
                                    Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(getBaseContext(), R.string.internalError,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        req.execute();
    }

    private void showRemoveBookingDialog() {
        // Check if the user has the right to add a booking
        if (!mBookingRight) {
            Toast.makeText(getBaseContext(), R.string.noBookingRight,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if a date is selected
        if (mSelectedDate == null) {
            Toast.makeText(getBaseContext(), R.string.noDateSelected,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if at least one rent is busy
        List<String> busyRents = mBooking.getBusyRentsForDate(mSelectedDate);
        if (busyRents.size() == 0) {
            Toast.makeText(getBaseContext(), R.string.noRentBusy,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Initialize an alert dialog
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.removeBookingTitle);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.remove_booking, null);
        alert.setView(alertView);
        // Get useful view
        final TextView tenantView = (TextView) alertView.findViewById(R.id.tenant);
        final Spinner rentChoice = (Spinner) alertView.findViewById(R.id.rent_choice);
        final TextView dateView = (TextView) alertView.findViewById(R.id.date);
        // Update the tenant depending on the selected sub-rent.
        rentChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRent = rentChoice.getSelectedItem().toString();
                // Set text for tenant
                tenantView.setText(mBooking.getTenant(selectedRent, mSelectedDate));
                // Set text for booking date
                RentalBooking.BookingInformation info =
                        mBooking.getBookingInformation(selectedRent, mSelectedDate);
                Calendar cal = Calendar.getInstance();
                cal.setTime(info.date);
                cal.add(Calendar.DATE, info.duration);
                SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                dateView.setText(format.format(info.date)+" - "+format.format(cal.getTime()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Populate the spinner of alert dialog with free rents.
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        adapter.addAll(busyRents);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rentChoice.setAdapter(adapter);
        // If the selected rent in the activity is free, set this rent as default in dialog.
        if (busyRents.contains(mSelectedSubrent)) {
            int defaultPos = adapter.getPosition(mSelectedSubrent);
            rentChoice.setSelection(defaultPos);
        }
        // Set up the buttons
        Resources res = getResources();
        alert.setPositiveButton(res.getString(R.string.remove),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeBookingPostRequest(rentChoice.getSelectedItem().toString());
            }
        });
        alert.setNegativeButton(res.getString(R.string.cancel) ,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    private void removeBookingPostRequest(final String rent) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        final int rentId = mBooking.getIdRent(rent);
        SendPostRequest req = new SendPostRequest(SendPostRequest.REMOVE_BOOKING);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.RENT_KEY, rentId);
        final Date dateRequest = mBooking.getBookingInformation(rent, mSelectedDate).date;
        req.addPostParam(SendPostRequest.DATE_KEY, format.format(dateRequest));
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    if (result.equals(SendPostRequest.ACTION_OK)) {
                        Toast.makeText(getBaseContext(), R.string.bookingRemoved,
                                Toast.LENGTH_SHORT).show();
                        mBooking.removeBooking(rentId, dateRequest);
                        mSelectedDate = null;
                        mCaldroidFragment.clearSelectedDates();
                        updateCaldroidView();
                        updateTenantInfo();
                    }
                    else if (result.equals(SendPostRequest.BOOKING_NOT_EXIST)) {
                        Toast.makeText(getBaseContext(), R.string.bookingNotExistError,
                                Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getBaseContext(), R.string.internalError,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        req.execute();
    }

    private void showModifyBookingDialog() {
        // Check if the user has the right to modify a booking
        if (!mBookingRight) {
            Toast.makeText(getBaseContext(), R.string.noBookingRight,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if a date is selected
        if (mSelectedDate == null) {
            Toast.makeText(getBaseContext(), R.string.noDateSelected,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if at least one rent is busy
        List<String> busyRents = mBooking.getBusyRentsForDate(mSelectedDate);
        if (busyRents.size() == 0) {
            Toast.makeText(getBaseContext(), R.string.noRentBusy,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Initialize an alert dialog
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.modifyBookingTitle);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.modify_booking, null);
        alert.setView(alertView);
        // Get useful view
        final EditText tenantView = (EditText) alertView.findViewById(R.id.tenant);
        final Spinner rentChoice = (Spinner) alertView.findViewById(R.id.rent_choice);
        final TextView dateView = (TextView) alertView.findViewById(R.id.date);
        // Update the tenant depending on the selected sub-rent.
        rentChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRent = rentChoice.getSelectedItem().toString();
                // Set text for tenant
                tenantView.setText(mBooking.getTenant(selectedRent, mSelectedDate));
                // Set text for booking date
                RentalBooking.BookingInformation info =
                        mBooking.getBookingInformation(selectedRent, mSelectedDate);
                Calendar cal = Calendar.getInstance();
                cal.setTime(info.date);
                cal.add(Calendar.DATE, info.duration);
                SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                dateView.setText(format.format(info.date)+" - "+format.format(cal.getTime()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Populate the spinner of alert dialog with free rents.
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        adapter.addAll(busyRents);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rentChoice.setAdapter(adapter);
        // If the selected rent in the activity is busy, set this rent as default in dialog.
        if (busyRents.contains(mSelectedSubrent)) {
            int defaultPos = adapter.getPosition(mSelectedSubrent);
            rentChoice.setSelection(defaultPos);
        }
        // Set up the buttons
        Resources res = getResources();
        alert.setPositiveButton(res.getString(R.string.modify),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedRent = rentChoice.getSelectedItem().toString();
                        RentalBooking.BookingInformation info =
                                mBooking.getBookingInformation(selectedRent, mSelectedDate);
                        long bookingId = info.id;
                        modifyBookingPostRequest(bookingId, tenantView.getText().toString(),
                                selectedRent, info.date);
                    }
                });
        alert.setNegativeButton(res.getString(R.string.cancel) ,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alert.show();
    }

    /**
     * Modify a booking on the server.
     * @param bookingId The booking id, used to identify the booking to modify on the server.
     * @param newTenant The new tenant name.
     * @param rent The rent name, used to modify booking internally.
     * @param date The starting date of the booking, used to modify booking internally.
     */
    private void modifyBookingPostRequest(final long bookingId, final String newTenant,
                                          final String rent, final Date date) {
        SendPostRequest req = new SendPostRequest(SendPostRequest.MODIFY_BOOKING);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.BOOKING_KEY, bookingId);
        req.addPostParam(SendPostRequest.TENANT_KEY, newTenant);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    if (result.equals(SendPostRequest.ACTION_OK)) {
                        Toast.makeText(getBaseContext(), R.string.bookingModified,
                                Toast.LENGTH_SHORT).show();
                        mBooking.modifyBooking(rent, date, newTenant);
                        updateCaldroidView();
                        updateTenantInfo();
                    }
                    else if (result.equals(SendPostRequest.BOOKING_NOT_EXIST)) {
                        Toast.makeText(getBaseContext(), R.string.bookingNotExistError,
                                Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getBaseContext(), R.string.internalError,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        req.execute();
    }
}
