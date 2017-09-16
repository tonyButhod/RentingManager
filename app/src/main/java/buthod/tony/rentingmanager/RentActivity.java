package buthod.tony.rentingmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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

    private Spinner mSpinner = null;
    private CaldroidBookingFragment mCaldroidFragment = null;
    private FrameLayout mCaldroidContainer = null;
    private TableLayout mTenants = null;
    private Button mAddBooking = null;
    private Button mRemoveBooking = null;
    private Button mPrices = null;
    private ImageButton mBackButton = null;

    private RentalBooking mBooking = null;
    private String mWholeRent = null;
    private Date mSelectedDate = null;
    private Date mMinDate = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.rent);

        mSpinner = (Spinner) findViewById(R.id.list_subrents);
        mTenants = (TableLayout) findViewById(R.id.tenants);
        mAddBooking = (Button) findViewById(R.id.add_booking);
        mRemoveBooking = (Button) findViewById(R.id.remove_booking);
        mPrices = (Button) findViewById(R.id.prices);
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mCaldroidContainer = (FrameLayout) findViewById(R.id.caldroid_container);
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
                // Select the whole week and update tenant
                selectWeekFromDate(date);
                updateTenantInfo();
            }

            @Override
            public void onChangeMonth(int month, int year) {
                mSelectedDate = null;
                mCaldroidFragment.clearSelectedDates();
                mCaldroidFragment.refreshView();
                updateTenantInfo();
            }
        });
        // Commit changes
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.caldroid_container, mCaldroidFragment);
        t.commit();
        // Send a post request to access information
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        mUsername = prefs.getString(SendPostRequest.USERNAME_KEY, null);
        mHash = prefs.getString(SendPostRequest.HASH_KEY, null);
        if (mUsername != null && mHash != null) {
            // Hide views in the activity
            mCaldroidContainer.setVisibility(View.GONE);
            mAddBooking.setVisibility(View.GONE);
            mRemoveBooking.setVisibility(View.GONE);
            mPrices.setVisibility(View.GONE);
            mSpinner.setVisibility(View.GONE);
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
                            // Show calendar with buttons and spinner
                            mCaldroidContainer.setVisibility(View.VISIBLE);
                            mAddBooking.setVisibility(View.VISIBLE);
                            mRemoveBooking.setVisibility(View.VISIBLE);
                            mPrices.setVisibility(View.VISIBLE);
                            mSpinner.setVisibility(View.VISIBLE);
                            // Save result in local variables
                            parseInfo(result);
                            // Update the view with those updated variables
                            updateCaldroidView();
                        }
                        catch (JSONException e) {
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

    /**
     * Parse the string "result" to save booking information in variable "mBooking".
     * @param result The string to parse with JSON format.
     * @throws JSONException
     */
    private void parseInfo(String result) throws JSONException {
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
        if (mAccessLevel >= 1 || mOwners.contains(mUsername)) {
            mBookingRight = true;
        }
        /* Populate the list of rents */
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                R.layout.rent_spinner_item);
        JSONArray subrents = resObj.getJSONArray(SendPostRequest.SUBRENTS_KEY);
        for (int i=0; i<subrents.length(); i++) {
            JSONObject subrent = subrents.getJSONObject(i);
            String name = subrent.getString(SendPostRequest.RENT_NAME_KEY);
            int id = subrent.getInt(SendPostRequest.ID_KEY);
            mBooking.addSubrent(name, id);
            adapter.add(name);
        }
        mBooking.setWholeRent(mWholeRent);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.invalidate();

        /* Parse the result for booking */
        Calendar cal = Calendar.getInstance();
        // First calculate the number of sub-rents rented while parsing.
        JSONArray bookings = resObj.getJSONArray(SendPostRequest.BOOKING_KEY);
        for (int i=0; i<bookings.length(); i++) {
            JSONObject booking = bookings.getJSONObject(i);
            cal.clear();
            cal.set(Calendar.YEAR, booking.getInt(SendPostRequest.YEAR_KEY));
            cal.set(Calendar.WEEK_OF_YEAR, booking.getInt(SendPostRequest.WEEK_KEY));
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            int id_rent = booking.getInt(SendPostRequest.RENT_KEY);
            String tenant = booking.getString(SendPostRequest.TENANT_KEY);
            Date date = cal.getTime();
            mBooking.addBooking(id_rent, date, tenant);
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
        String selectedItem = mSpinner.getSelectedItem().toString();

        // Check which sub-rent is selected.
        // If the whole rent is selected, all sub-rent are selected
        Iterable<String> subrentSet;
        if (selectedItem.equals(mWholeRent))
            // If the whole location is selected, all sub-rents are selected
            subrentSet = mBooking.getSubrentSet();
        else {
            ArrayList<String> selectedSet = new ArrayList<String>();
            selectedSet.add(selectedItem);
            if (!selectedItem.equals(mWholeRent))
                selectedSet.add(mWholeRent);
            subrentSet = selectedSet;
        }

        // Iterate on sub-rents
        mCaldroidFragment.clearBooking();
        for (String subrent : subrentSet) {
            // Iterate on booking per sub-rent
            if (subrent.equals(selectedItem) || subrent.equals(mWholeRent)) {
                for (Map.Entry<Date, String> booking : mBooking.getBookingEntrySet(subrent))
                    mCaldroidFragment.setWeekBookingForDate(booking.getKey(),
                            CaldroidBookingFragment.FULL);
            }
            else {
                for (Map.Entry<Date, String> booking : mBooking.getBookingEntrySet(subrent)) {
                    // Count the number of sub-rents in order to put fullDraw
                    // if the whole location is rented.
                    Date date = booking.getKey();
                    Integer nb_booking = nbBookingMap.get(date);
                    int new_nb_booking = ((nb_booking!=null)?nb_booking:0) + 1;
                    nbBookingMap.put(date, new_nb_booking);
                    // Put the week state in CaldroidBookingFragment
                    int state = (new_nb_booking == nb_subrents)?
                            CaldroidBookingFragment.FULL : CaldroidBookingFragment.HALF_FULL;
                    mCaldroidFragment.setWeekBookingForDate(date, state);
                }
            }
        }
        mCaldroidFragment.refreshView();
    }

    private void updateTenantInfo() {
        // Update the tenant
        mTenants.removeAllViews();
        if (mSelectedDate != null) {
            Map<String, String> tenants = mBooking.getTenants(
                    mSpinner.getSelectedItem().toString(), mSelectedDate);
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

    private void selectWeekFromDate(Date date) {
        // Get the date of previous saturday
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int diffDate = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7;
        cal.add(Calendar.DATE, -diffDate);
        Date startDate = cal.getTime();
        cal.add(Calendar.DATE, 6);
        Date endDate = cal.getTime();
        // Put all the week as selected
        mCaldroidFragment.setSelectedDates(startDate, endDate);
        mCaldroidFragment.refreshView();
        mSelectedDate = startDate;
    }

    private void showAddBookingDialog() {
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
        // Check if at least one rent is free
        List<String> freeRents = mBooking.getFreeRentsForDate(mSelectedDate);
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
        cal.add(Calendar.DATE, 6);
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        dateView.setText(format.format(mSelectedDate) + " - " + format.format(cal.getTime()));
        // Populate the spinner of alert dialog with free rents.
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);
        adapter.addAll(freeRents);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rentChoice.setAdapter(adapter);
        // If the selected rent in the activity is free, set this rent as default in dialog.
        String selectedRent = mSpinner.getSelectedItem().toString();
        if (freeRents.contains(selectedRent)) {
            int defaultPos = adapter.getPosition(selectedRent);
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
                        errorView.setText("");
                        String rent = rentChoice.getSelectedItem().toString();
                        String tenant = tenantInput.getText().toString();
                        if (tenant.isEmpty()) {
                            errorView.setText(R.string.emptyTenant);
                        }
                        else {
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
        Calendar cal = Calendar.getInstance();
        cal.setTime(mSelectedDate);
        final Date dateRequest = mSelectedDate;
        final int rentId = mBooking.getIdRent(rent);
        SendPostRequest req = new SendPostRequest(SendPostRequest.ADD_BOOKING);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.WEEK_KEY, cal.get(Calendar.WEEK_OF_YEAR));
        req.addPostParam(SendPostRequest.YEAR_KEY, cal.get(Calendar.YEAR));
        req.addPostParam(SendPostRequest.RENT_KEY, rentId);
        req.addPostParam(SendPostRequest.TENANT_KEY, tenant);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    if (result.equals(SendPostRequest.ACTION_OK)) {
                        Toast.makeText(getBaseContext(), R.string.newBooking,
                                Toast.LENGTH_SHORT).show();
                        mBooking.addBooking(rentId, dateRequest, tenant);
                        mSelectedDate = null;
                        mCaldroidFragment.clearSelectedDates();
                        updateCaldroidView();
                        updateTenantInfo();
                    }
                    else if (result.equals(SendPostRequest.RENT_NOT_FREE)){
                        Toast.makeText(getBaseContext(), R.string.rentNotFreeError,
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
        // Set the text of the date view
        Calendar cal = Calendar.getInstance();
        cal.setTime(mSelectedDate);
        cal.add(Calendar.DATE, 6);
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        dateView.setText(format.format(mSelectedDate) + " - " + format.format(cal.getTime()));
        // Update the tenant depending on the selected sub-rent.
        rentChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRent = rentChoice.getSelectedItem().toString();
                tenantView.setText(mBooking.getTenant(selectedRent, mSelectedDate));
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
        String selectedRent = mSpinner.getSelectedItem().toString();
        if (busyRents.contains(selectedRent)) {
            int defaultPos = adapter.getPosition(selectedRent);
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
        Calendar cal = Calendar.getInstance();
        cal.setTime(mSelectedDate);
        final Date dateRequest = mSelectedDate;
        final int rentId = mBooking.getIdRent(rent);
        SendPostRequest req = new SendPostRequest(SendPostRequest.REMOVE_BOOKING);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.WEEK_KEY, cal.get(Calendar.WEEK_OF_YEAR));
        req.addPostParam(SendPostRequest.YEAR_KEY, cal.get(Calendar.YEAR));
        req.addPostParam(SendPostRequest.RENT_KEY, rentId);
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
}
