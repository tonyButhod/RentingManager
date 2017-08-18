package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class ConnexionActivity extends Activity {

    private EditText mLogin = null;
    private EditText mPassword = null;
    private Button mConnexion = null;

    private boolean mPostRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check preferences to automatic connection
        SharedPreferences prefs = getSharedPreferences("autoConnection", Context.MODE_PRIVATE);
        String prefLogin = prefs.getString("login", null);
        String prefHash = prefs.getString("hash", null);
        if (savedInstanceState == null && prefLogin != null && prefHash != null) {
            new SendPostRequest(prefLogin, null, prefHash).execute();
        }
        else {
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
                        new SendPostRequest(mLogin.getText().toString(),
                                mPassword.getText().toString(), null).execute();
                    }
                }
            });
        }
    }

    // Public inner class of the main activity
    public class SendPostRequest extends AsyncTask<String, Void, String> {
        private String mLogin = null;
        private String mPassword = null;
        private String mHash = null;
        private boolean mSuccess = false;

        public SendPostRequest(String login, String password, String hash) {
            mLogin = login;
            mPassword = password;
            mHash = hash;
        }

        protected void onPreExecute() {
            mPostRunning = true;
            mSuccess = false;
        }

        protected String doInBackground(String... arg0) {
            try{

                URL url = new URL("https://website/access.php");

                JSONObject postDataParams = new JSONObject();
                postDataParams.put("login", mLogin);
                if (mPassword != null)
                    postDataParams.put("password", mPassword);
                if (mHash != null)
                    postDataParams.put("hash", mHash);
                Log.e("params",postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(5000 /* milliseconds */);
                conn.setConnectTimeout(5000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));
                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    BufferedReader in=new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuffer sb = new StringBuffer("");
                    String line = "";
                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }
                    in.close();
                    String res = sb.toString();
                    if (res.equals("{}")) {
                        res = "Access denied";
                    }
                    else {
                        mSuccess = true;
                    }
                    return res;

                }
                else {
                    return "Error : "+responseCode;
                }
            }
            catch(Exception e){
                return "Exception : " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (mSuccess) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("result", result);
                startActivity(intent);
                finish();
            }
            else {
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            }
            mPostRunning = false;
        }
    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }
}