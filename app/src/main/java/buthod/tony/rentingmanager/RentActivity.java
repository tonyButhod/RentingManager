package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tony on 15/08/2017.
 */

public class RentActivity extends Activity {

    private Spinner mListSubrentsView = null;
    private String mMainRent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.rent);

        mListSubrentsView = (Spinner) findViewById(R.id.list_subrents);
        // Recover the main rent name
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mMainRent = extras.getString(SendPostRequest.RENT_NAME_KEY, null);
        }
        // Send a post request to access information
        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS, Context.MODE_PRIVATE);
        String login = prefs.getString(SendPostRequest.LOGIN_KEY, null);
        String hash = prefs.getString(SendPostRequest.HASH_KEY, null);
        if (mMainRent != null && login != null && hash != null) {
            SendPostRequest req = new SendPostRequest(SendPostRequest.GET_RENT_INFO);
            req.addPostParam(SendPostRequest.LOGIN_KEY, login);
            req.addPostParam(SendPostRequest.HASH_KEY, hash);
            req.addPostParam(SendPostRequest.RENT_NAME_KEY, mMainRent);
            req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
                @Override
                public void postExecute(boolean success, String result) {
                    if (success) {
                        try {
                            parseInfo(result);
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

    private void parseInfo(String result) throws JSONException {
        // Populate the list of sub-rents
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item);
        adapter.add(mMainRent);
        // Parse the result
        JSONObject resObj = new JSONObject(result);
        JSONArray subrents = resObj.getJSONArray(SendPostRequest.SUBRENTS_KEY);
        for (int i=0; i<subrents.length(); i++) {
            JSONObject subrent = subrents.getJSONObject(i);
            adapter.add(subrent.getString(SendPostRequest.RENT_NAME_KEY));
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mListSubrentsView.setAdapter(adapter);
        mListSubrentsView.invalidate();
    }
}
