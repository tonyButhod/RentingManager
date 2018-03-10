package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText mUsername = null;
    private EditText mPassword = null;
    private Button mSignIn = null;

    private boolean mPostRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize fields
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mSignIn = (Button) findViewById(R.id.sign_in);
        mSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Try to connect to the database.
                if (mPostRunning) {
                    Toast.makeText(getBaseContext(), R.string.runningConnection,
                            Toast.LENGTH_SHORT).show();
                } else {
                    mPostRunning = true;
                    SendPostRequest req = new SendPostRequest(SendPostRequest.LOGIN);
                    req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername.getText().toString());
                    req.addPostParam(SendPostRequest.PASSWORD_KEY, mPassword.getText().toString());
                    req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
                        @Override
                        public void postExecute(boolean success, String result) {
                            checkResultRequest(success, result);
                        }
                    });
                    req.execute();
                }
            }
        });
    }

    private void checkResultRequest(boolean success, String result) {
        if (success) {
            // Save username and hash in sharedPreferences
            SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFERENCES_NAME,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            try {
                JSONObject resObj = new JSONObject(result);
                String username = resObj.getString(SendPostRequest.USERNAME_KEY);
                String hash = resObj.getString(SendPostRequest.HASH_KEY);
                editor.putString(SettingsActivity.PREF_USERNAME, username);
                editor.putString(SettingsActivity.PREF_HASH, hash);
                // Need to write data immediately because it is used in the next activity
                editor.commit();
                // Start main activity
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
            catch (JSONException e) {
                Toast.makeText(getBaseContext(), R.string.accessDeniedError,
                        Toast.LENGTH_SHORT).show();
                mPassword.setText("");
                mPassword.invalidate();
            }
        }
        else {
            Toast.makeText(getBaseContext(), R.string.connectionError,
                    Toast.LENGTH_SHORT).show();
        }
        mPostRunning = false;
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