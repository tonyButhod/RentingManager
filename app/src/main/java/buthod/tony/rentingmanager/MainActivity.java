package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tony on 06/08/2017.
 */

public class MainActivity extends Activity {

    private TextView mTitle = null;
    private LinearLayout mRentsLayout = null;

    private String mLogin = null;
    private String mPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTitle = (TextView) findViewById(R.id.title);
        mRentsLayout = (LinearLayout) findViewById(R.id.rentsLayout);
        // Check preferences to automatic connection
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        String prefLogin = prefs.getString(SendPostRequest.LOGIN_KEY, null);
        String prefHash = prefs.getString(SendPostRequest.HASH_KEY, null);
        String script = SendPostRequest.GET_MAIN_RENTS;
        if (savedInstanceState == null && prefLogin != null && prefHash != null) {
            SendPostRequest req = new SendPostRequest(prefLogin, null, prefHash, script);
            req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
                @Override
                public void postExecute(boolean success, String result) {
                    // Parse JSON file
                    try {
                        JSONObject resObj = new JSONObject(result);
                        mLogin = resObj.getString(SendPostRequest.LOGIN_KEY);
                        mPassword = resObj.getString(SendPostRequest.PASSWORD_KEY);
                        JSONArray rents = resObj.getJSONArray(SendPostRequest.MAIN_RENTS_KEY);
                        mTitle.setText("Welcome back " + mLogin);
                        for (int i=0; i<rents.length(); i++) {
                            JSONObject rent = rents.getJSONObject(i);
                            Button button = new Button(getBaseContext());
                            button.setText(rent.getString(SendPostRequest.RENT_NAME_KEY));
                            LayoutParams params = new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT);
                            mRentsLayout.addView(button, params);
                        }
                        mTitle.invalidate();
                        mRentsLayout.invalidate();
                    }
                    catch (JSONException e) {
                        Intent intent = new Intent(getBaseContext(), ConnexionActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
            req.execute();
        }
        else {
            Intent intent = new Intent(getBaseContext(), ConnexionActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
