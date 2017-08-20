package buthod.tony.rentingmanager;

import android.os.AsyncTask;

import org.json.JSONException;
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

/**
 * Created by Tony on 14/08/2017.
 */

public class SendPostRequest extends AsyncTask<String, Void, String> {
    private final static String WEBSITE_URL = "https://website/";
    public final static String LOGIN_KEY = "login";
    public final static String PASSWORD_KEY = "password";
    public final static String HASH_KEY = "hash";
    public final static String PREFS = "connexionPrefs";
    public final static String MAIN_RENTS_KEY = "rents";
    public final static String SUBRENTS_KEY = "subrents";
    public final static String RENT_NAME_KEY = "name";

    public final static String CONNEXION = "connexion.php";
    public final static String GET_MAIN_RENTS = "getMainRents.php";
    public final static String GET_RENT_INFO = "getRentInfo.php";

    private JSONObject mPostDataParams = null;
    private String mScript = null;
    private boolean mSuccess = false;

    private OnPostExecute mOnPostExecute = null;

    public SendPostRequest(String script) {
        mPostDataParams = new JSONObject();
        mScript = script;
    }

    /**
     * Add a post param to the request.
     * @param key
     * @param value
     * @return Return true on success, false otherwise.
     */
    public boolean addPostParam(String key, Object value) {
        try {
            mPostDataParams.put(key, value);
            return true;
        }
        catch (JSONException e) {
            return false;
        }
    }

    public void setOnPostExecute(OnPostExecute onPostExecute) {
        mOnPostExecute = onPostExecute;
    }

    protected void onPreExecute() {
        mSuccess = false;
    }

    protected String doInBackground(String... arg0) {
        try{

            URL url = new URL(WEBSITE_URL + mScript);
            // Headers
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000 /* milliseconds */);
            conn.setConnectTimeout(5000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // Connection
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(mPostDataParams));
            writer.flush();
            writer.close();
            os.close();
            // Check the result
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                mSuccess = true;
                BufferedReader in=new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
                StringBuffer sb = new StringBuffer("");
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    break;
                }
                in.close();
                return sb.toString();
            }
            else {
                return String.valueOf(responseCode);
            }
        }
        catch(Exception e){
            return e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (mOnPostExecute != null)
            mOnPostExecute.postExecute(mSuccess, result);
    }

    private String getPostDataString(JSONObject params) throws Exception {
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

    public static class OnPostExecute {
        public void postExecute(boolean success, String result) {
            // To be overridden
        }
    }
}