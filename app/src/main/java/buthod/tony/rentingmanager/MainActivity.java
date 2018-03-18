package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Tony on 06/08/2017.
 */

public class MainActivity extends Activity {

    // Variable used to finish this activity when on sign out.
    public static Activity instance = null;

    private Button mUserButton = null;
    private Button mPostRequest = null;
    private ViewPager mViewPager = null;
    private CustomPagerAdapter mPagerAdapter = null;
    // 2 layouts used for ViewPager
    private LinearLayout mRentsLayout = null;
    private LinearLayout mStatisticLayout = null;

    private String mUsername = null;
    private String mHash = null; // Contain the hash of the password

    private SharedPreferences mPreferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mPreferences = getSharedPreferences(SettingsActivity.PREFERENCES_NAME,
                Context.MODE_PRIVATE);

        // Update instance of this activity
        instance = this;

        mUserButton = (Button) findViewById(R.id.user_button);
        mPostRequest = (Button) findViewById(R.id.post_request);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);

        // Initialize variables for ViewPager
        mRentsLayout = new LinearLayout(getBaseContext());
        mRentsLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mRentsLayout.setOrientation(LinearLayout.VERTICAL);
        mRentsLayout.setPadding(10, 5, 10, 5);
        mStatisticLayout = new LinearLayout(getBaseContext());
        mStatisticLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mStatisticLayout.setOrientation(LinearLayout.VERTICAL);
        mStatisticLayout.setPadding(10, 5, 10, 5);
        mPagerAdapter = new CustomPagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);

        // Populate the statistics layout
        populateStatisticsLayout();

        // Hide user button at the beginning
        mUserButton.setVisibility(View.GONE);
        // Check preferences for automatic connection
        mUsername = mPreferences.getString(SettingsActivity.PREF_USERNAME, null);
        mHash = mPreferences.getString(SettingsActivity.PREF_HASH, null);
        if (mUsername == null || mHash == null) {
            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
        // Listener to open settings when the user click on his username
        mUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                intent.putExtra(SendPostRequest.USERNAME_KEY, mUsername);
                intent.putExtra(SendPostRequest.HASH_KEY, mHash);
                startActivity(intent);
            }
        });
        // Listener to send a new post request
        mPostRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainRentsPostRequest();
                mPostRequest.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check preferences in case the password has been changed
        String hashPref = mPreferences.getString(SettingsActivity.PREF_HASH, null);
        if (hashPref != null && !hashPref.equals(mHash))
            mHash = hashPref;

        getMainRentsPostRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the number of pages of the ViewPagerAdapter if the user change settings
        if (mPreferences.getBoolean(SettingsActivity.PREF_STAT_ACTIVATED, false))
            mPagerAdapter.setNumberPages(2);
        else
            mPagerAdapter.setNumberPages(1);
        mViewPager.setAdapter(mPagerAdapter);
    }

    @Override
    protected void onDestroy() {
        MainActivity.instance = null;
        super.onDestroy();
    }

    private void getMainRentsPostRequest() {
        SendPostRequest req = new SendPostRequest(SendPostRequest.GET_MAIN_RENTS);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    // Parse JSON file
                    try {
                        parseResult(result);
                        // Show the user button to access settings
                        mUserButton.setVisibility(View.VISIBLE);
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
     * Parse the resulting string of the post request,
     * and add one button per rent to the linear layout.
     * @param result The string to parse.
     */
    private void parseResult(String result) throws JSONException {
        mRentsLayout.removeAllViews();
        JSONObject resObj = new JSONObject(result);
        JSONArray rents = resObj.getJSONArray(SendPostRequest.RENTS_KEY);
        mUserButton.setText(mUsername);
        // Parameters and listeners used
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 5, 5, 5);
        View.OnClickListener buttonOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), RentActivity.class);
                intent.putExtra(SendPostRequest.RENT_NAME_KEY,
                        ((Button) v).getText().toString());
                startActivity(intent);
            }
        };

        for (int i=0; i<rents.length(); i++) {
            JSONObject rent = rents.getJSONObject(i);
            Button button = new Button(
                    new ContextThemeWrapper(getBaseContext(), R.style.CyanButton));
            button.setText(rent.getString(SendPostRequest.RENT_NAME_KEY));
            button.setOnClickListener(buttonOnClickListener);
            // In case some styles are not working
            button.setBackground(ContextCompat.getDrawable(getBaseContext(),
                    R.drawable.cyan_button));
            button.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.darkBlue));
            // Add the button to the layout
            mRentsLayout.addView(button, params);
        }
        mUserButton.invalidate();
        mRentsLayout.invalidate();
        // Check if their is a message to display
        if (resObj.has(SendPostRequest.MESSAGE_KEY)) {
            String message = resObj.getString(SendPostRequest.MESSAGE_KEY);
            showPopupMessage(message);
        }
    }

    private void showPopupMessage(String message) {
        message = processMessage(message);
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.popup_message_title);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.popup_message, null);
        builder.setView(alertView);
        // Get useful view
        TextView messageView = (TextView) alertView.findViewById(R.id.message);
        messageView.setText(message);
        // Set up the buttons
        Resources res = getResources();
        builder.setPositiveButton(res.getString(R.string.ok), null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        // Update the database so that the message do not show anymore
                        SendPostRequest req = new SendPostRequest(SendPostRequest.MESSAGE_MANAGER);
                        req.addPostParam(SendPostRequest.MESSAGE_ACTION,
                                SendPostRequest.MESSAGE_NOT_SHOW);
                        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
                        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
                        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
                            @Override
                            public void postExecute(boolean success, String result) {
                                if (!success) {
                                    // A connection error occurred
                                    Toast.makeText(getBaseContext(), R.string.connectionError,
                                            Toast.LENGTH_LONG).show();
                                    mPostRequest.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                        req.execute();
                        alertDialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }

    /**
     * Function used to process the message by replacing some patterns by others using regex.
     * @param message The message to change.
     * @return The processed message.
     */
    private String processMessage(String message) {
        // Replace {%NAME%} by the user name
        String regexName = "\\{%NAME%\\}";
        return message.replaceAll(regexName, mUsername);
    }

    /**
     * Method used to add buttons in the statistics layout
     */
    private void populateStatisticsLayout() {
        ArrayList<String> buttonNames = new ArrayList<>();
        buttonNames.add(getResources().getString(R.string.rentsPerMonth));
        // Parameters and listeners used
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 5, 5, 5);
        View.OnClickListener buttonOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), StatisticsActivity.class);
                intent.putExtra(SendPostRequest.STATISTICS_KEY, R.string.rentsPerMonth);
                startActivity(intent);
            }
        };

        for (final String name : buttonNames) {
            Button button = new Button(
                    new ContextThemeWrapper(getBaseContext(), R.style.OrangeButton));
            // In case some styles are not working
            button.setBackground(ContextCompat.getDrawable(getBaseContext(),
                    R.drawable.orange_button));
            button.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.darkOrange));
            button.setText(name);
            button.setOnClickListener(buttonOnClickListener);
            mStatisticLayout.addView(button, params);
        }
    }


    public class CustomPagerAdapter extends PagerAdapter {

        private int mNumberPages = 1;

        public CustomPagerAdapter() {
        }

        public void setNumberPages(int numberPages) {
            mNumberPages = numberPages;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            switch (position) {
                case 0:
                    collection.addView(mRentsLayout);
                    return mRentsLayout;
                case 1:
                    collection.addView(mStatisticLayout);
                    return mStatisticLayout;
                default:
                    return null;
            }
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            switch (position) {
                case 0:
                    collection.removeView(mRentsLayout);
                    break;
                case 1:
                    collection.removeView(mStatisticLayout);
                    break;
            }
        }

        @Override
        public int getCount() {
            return mNumberPages;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Title custom view pager";
        }
    }
}
