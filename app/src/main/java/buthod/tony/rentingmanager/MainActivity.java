package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tony on 06/08/2017.
 */

public class MainActivity extends Activity {

    private Button mUserButton = null;
    private LinearLayout mRentsLayout = null;
    private Button mSignOut = null;
    private Button mPostRequest = null;

    private String mUsername = null;
    private String mHash = null; // Contain the hash of the password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mUserButton = (Button) findViewById(R.id.user_button);
        mRentsLayout = (LinearLayout) findViewById(R.id.rents_layout);
        mSignOut = (Button) findViewById(R.id.sign_out);
        mPostRequest = (Button) findViewById(R.id.post_request);
        // Hide user button at the beginning
        mUserButton.setVisibility(View.GONE);
        // Check preferences for automatic connection
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        mUsername = prefs.getString(SendPostRequest.USERNAME_KEY, null);
        mHash = prefs.getString(SendPostRequest.HASH_KEY, null);
        if (mUsername != null && mHash != null) {
            getMainRentsPostRequest();
        }
        else {
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
                startActivity(intent);
            }
        });
        // Listener to sign out
        mSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove username and hash in sharedPreferences
                SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS,
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(SendPostRequest.USERNAME_KEY);
                editor.remove(SendPostRequest.PASSWORD_KEY);
                editor.apply();
                // Start login activity
                Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                startActivity(intent);
                finish();
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

    private void parseResult(String result) throws JSONException {
        mRentsLayout.removeAllViews();
        JSONObject resObj = new JSONObject(result);
        JSONArray rents = resObj.getJSONArray(SendPostRequest.RENTS_KEY);
        mUserButton.setText(mUsername);
        for (int i=0; i<rents.length(); i++) {
            JSONObject rent = rents.getJSONObject(i);
            Button button = new Button(
                    new ContextThemeWrapper(getBaseContext(), R.style.CyanButton));
            button.setText(rent.getString(SendPostRequest.RENT_NAME_KEY));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getBaseContext(), RentActivity.class);
                    intent.putExtra(SendPostRequest.RENT_NAME_KEY,
                            ((Button) v).getText().toString());
                    startActivity(intent);
                }
            });
            button.setBackground(ContextCompat.getDrawable(getBaseContext(),
                    R.drawable.cyan_button));
            // Add the button to the layout
            LayoutParams params = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 5, 5, 5);
            mRentsLayout.addView(button, params);
        }
        mUserButton.invalidate();
        mRentsLayout.invalidate();
    }
}
