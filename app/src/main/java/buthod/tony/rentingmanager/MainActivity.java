package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tony on 06/08/2017.
 */

public class MainActivity extends Activity {

    private TextView mTitle = null;
    private LinearLayout mRentsLayout = null;
    private Button mSignOut = null;

    private String mUsername = null;
    private String mHash = null; // Contain the hash of the password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTitle = (TextView) findViewById(R.id.title);
        mRentsLayout = (LinearLayout) findViewById(R.id.rentsLayout);
        mSignOut = (Button) findViewById(R.id.sign_out);
        // Check preferences to automatic connection
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        String prefUsername = prefs.getString(SendPostRequest.USERNAME_KEY, null);
        String prefHash = prefs.getString(SendPostRequest.HASH_KEY, null);
        if (prefUsername != null && prefHash != null) {
            SendPostRequest req = new SendPostRequest(SendPostRequest.GET_MAIN_RENTS);
            req.addPostParam(SendPostRequest.USERNAME_KEY, prefUsername);
            req.addPostParam(SendPostRequest.HASH_KEY, prefHash);
            req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
                @Override
                public void postExecute(boolean success, String result) {
                    if (success) {
                        // Parse JSON file
                        try {
                            parseResult(result);
                        }
                        catch (JSONException e) {
                            // Go to connection activity
                            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                    else {
                        Toast.makeText(getBaseContext(), "Connection error : " + result,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            req.execute();
        }
        else {
            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
        // Listener for sign out
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
    }

    private void parseResult(String result) throws JSONException {
        JSONObject resObj = new JSONObject(result);
        mUsername = resObj.getString(SendPostRequest.USERNAME_KEY);
        mHash = resObj.getString(SendPostRequest.HASH_KEY);
        JSONArray rents = resObj.getJSONArray(SendPostRequest.RENTS_KEY);
        mTitle.setText(mUsername);
        for (int i=0; i<rents.length(); i++) {
            JSONObject rent = rents.getJSONObject(i);
            Button button = new Button(getBaseContext());
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
            // Add the button to the layout
            LayoutParams params = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            mRentsLayout.addView(button, params);
        }
        mTitle.invalidate();
        mRentsLayout.invalidate();
    }
}
