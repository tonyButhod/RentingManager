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
        // Recover previous results
        Bundle extras = getIntent().getExtras();
        String res = "{}";
        if (extras != null) {
            res = extras.getString("result", "{}");
        }
        // Parse JSON file
        try {
            JSONObject resObj = new JSONObject(res);
            mLogin = resObj.getString("login");
            mPassword = resObj.getString("password");
            JSONArray rents = resObj.getJSONArray("rents");
            mTitle.setText("Welcome back " + mLogin);
            for (int i=0; i<rents.length(); i++) {
                JSONObject rent = rents.getJSONObject(i);
                Button button = new Button(this);
                button.setText(rent.getString("name"));
                LayoutParams params = new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT);
                mRentsLayout.addView(button, params);
            }
        }
        catch (JSONException e) {
            Intent intent = new Intent(this, ConnexionActivity.class);
            startActivity(intent);
            finish();
        }
        SharedPreferences prefs = getSharedPreferences("autoConnection", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("login", mLogin);
        editor.putString("hash", mPassword);
        editor.apply();
    }
}
