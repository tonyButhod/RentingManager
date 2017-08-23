package buthod.tony.rentingmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tony on 15/08/2017.
 */

public class RentActivity extends FragmentActivity {

    private Spinner mSpinner = null;
    private CaldroidBookingFragment mCaldroidFragment = null;
    private TextView mTenant = null;

    private RentalBooking mBooking = null;
    private String mWholeRent = null;
    private Date mSelectedDate = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.rent);

        mSpinner = (Spinner) findViewById(R.id.list_subrents);
        mTenant = (TextView) findViewById(R.id.tenants);
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
        mBooking = new RentalBooking(mWholeRent);
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
        // Set calendar view
        mCaldroidFragment = new CaldroidBookingFragment();
        Bundle args = new Bundle();
        Calendar cal = Calendar.getInstance();
        args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        mCaldroidFragment.setArguments(args);
        // Set caldroid listener
        mCaldroidFragment.setCaldroidListener(new CaldroidListener() {
            @Override
            public void onSelectDate(Date date, View view) {
                // Select the whole week and update tenant
                selectWeekFromDate(date);
                updateTenantInfo();
            }
        });
        // Commit changes
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.caldroid_container, mCaldroidFragment);
        t.commit();
        // Send a post request to access information
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        String login = prefs.getString(SendPostRequest.LOGIN_KEY, null);
        String hash = prefs.getString(SendPostRequest.HASH_KEY, null);
        if (login != null && hash != null) {
            SendPostRequest req = new SendPostRequest(SendPostRequest.GET_RENT_INFO);
            req.addPostParam(SendPostRequest.LOGIN_KEY, login);
            req.addPostParam(SendPostRequest.HASH_KEY, hash);
            req.addPostParam(SendPostRequest.RENT_NAME_KEY, mWholeRent);
            req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
                @Override
                public void postExecute(boolean success, String result) {
                    if (success) {
                        try {
                            // Save result in local variables
                            parseInfo(result);
                            // Update the view with those updated variables
                            updateCaldroidView();
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

    /**
     * Parse the string "result" to save booking information in variable "mBooking".
     * @param result The string to parse with JSON format.
     * @throws JSONException
     */
    private void parseInfo(String result) throws JSONException {
        JSONObject resObj = new JSONObject(result);

        /* Populate the list of rents */
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);
        JSONArray subrents = resObj.getJSONArray(SendPostRequest.SUBRENTS_KEY);
        for (int i=0; i<subrents.length(); i++) {
            JSONObject subrent = subrents.getJSONObject(i);
            String name = subrent.getString(SendPostRequest.RENT_NAME_KEY);
            int id = subrent.getInt(SendPostRequest.ID_KEY);
            mBooking.addSubrent(name, id);
            adapter.add(name);
        }
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
        if (mSelectedDate != null) {
            Map<String, String> tenants = mBooking.getTenants(
                    mSpinner.getSelectedItem().toString(), mSelectedDate);
            String text = "";
            for (Map.Entry<String, String> entry : tenants.entrySet()) {
                text += entry.getKey() + " : " + entry.getValue() + "\n";
            }
            mTenant.setText(text);
        }
        else {
            mTenant.setText("");
        }
        mTenant.invalidate();
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
}
