package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
                    Toast.makeText(getBaseContext(), "Connection already running",
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
            SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            try {
                JSONObject resObj = new JSONObject(result);
                String username = resObj.getString(SendPostRequest.USERNAME_KEY);
                String hash = resObj.getString(SendPostRequest.HASH_KEY);
                editor.putString(SendPostRequest.USERNAME_KEY, username);
                editor.putString(SendPostRequest.HASH_KEY, hash);
                // Need to write data immediately
                editor.commit();
                // Start main activity
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
            catch (JSONException e) {
                Toast.makeText(getBaseContext(), "Username or Password invalid",
                        Toast.LENGTH_SHORT).show();
                mPassword.setText("");
                mPassword.invalidate();
            }
        }
        else {
            Toast.makeText(getBaseContext(), "Connexion error : " + result,
                    Toast.LENGTH_SHORT).show();
        }
        mPostRunning = false;
    }
}