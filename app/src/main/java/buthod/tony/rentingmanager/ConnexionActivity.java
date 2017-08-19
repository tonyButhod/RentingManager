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

public class ConnexionActivity extends Activity {

    private EditText mLogin = null;
    private EditText mPassword = null;
    private Button mConnexion = null;

    private boolean mPostRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connexion);
        // Initialize fields
        mLogin = (EditText) findViewById(R.id.login);
        mPassword = (EditText) findViewById(R.id.password);
        mConnexion = (Button) findViewById(R.id.connexion);
        mConnexion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Try to connect to the database.
                if (mPostRunning) {
                    Toast.makeText(getBaseContext(), "Connection already running",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mPostRunning = true;
                    String login = mLogin.getText().toString();
                    String password = mPassword.getText().toString();
                    String script = SendPostRequest.CONNEXION;
                    SendPostRequest req = new SendPostRequest(login, password, null, script);
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
            // Save login and hash in sharedPreferences
            SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            try {
                JSONObject resObj = new JSONObject(result);
                String login = resObj.getString(SendPostRequest.LOGIN_KEY);
                String password = resObj.getString(SendPostRequest.PASSWORD_KEY);
                editor.putString(SendPostRequest.LOGIN_KEY, login);
                editor.putString(SendPostRequest.HASH_KEY, password);
                // Need to write data immediately
                editor.commit();
                // Start main activity
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
            catch (JSONException e) {
                Toast.makeText(getBaseContext(), "Login or Password invalid",
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