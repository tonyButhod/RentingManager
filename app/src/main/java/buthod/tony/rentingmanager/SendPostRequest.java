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
    // Key for post requests
    public final static String
            ID_KEY = "id",
            USERNAME_KEY = "username",
            PASSWORD_KEY = "password",
            HASH_KEY = "hash",
            ACCESS_KEY = "access",
            OWNERS_KEY = "owners",
            RENTS_KEY = "rents",
            SUBRENTS_KEY = "subrents",
            RENT_NAME_KEY = "name",
            BOOKING_KEY = "booking",
            YEAR_KEY = "year",
            RENT_KEY = "rent",
            TENANT_KEY = "tenant",
            PRICES_KEY = "prices",
            SUBRENT_KEY = "subrent",
            MIN_DATE_KEY = "minDate",
            NEW_PASSWORD_KEY = "newPassword",
            DATE_KEY = "date",
            DURATION_KEY = "duration",
            MESSAGE_KEY = "message",
            MESSAGE_ACTION = "messageAction",
            MESSAGE_NOT_SHOW = "messageNotShow",
            GET_MESSAGE = "getMessage",
            REMOVE_MESSAGE = "removeMessage",
            ADD_MESSAGE = "addMessage";
    // Web pages
    public final static String
            LOGIN = "login.php",
            GET_MAIN_RENTS = "getMainRents.php",
            GET_RENT_INFO = "getRentInfo.php",
            ADD_BOOKING = "addBooking.php",
            REMOVE_BOOKING = "removeBooking.php",
            SET_AND_GET_PRICES = "setAndGetPrices.php",
            CHANGE_PASSWORD = "changePassword.php",
            MESSAGE_MANAGER = "messageManager.php";
    // Connection results
    public final static String
            CONNEXION_ERROR = "Connexion error",
            ACTION_OK = "OK",
            RENT_NOT_FREE = "Rent not free",
            BOOKING_NOT_EXIST = "Booking doesn't exist",
            PASSWORD_INCORRECT = "Password incorrect",
            AT_LEAST_8_CHAR = "At least 8 characters";

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
        HttpURLConnection conn = null;
        String outputString = "";
        try{

            URL url = new URL(WEBSITE_URL + mScript);
            // Headers
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(10000 /* milliseconds */);
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
            StringBuffer sb = new StringBuffer("");
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                mSuccess = true;
                BufferedReader in=new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    break;
                }
                in.close();
            }
            else {
                sb.append(String.valueOf(responseCode));
            }
            conn.disconnect();
            outputString = sb.toString();
        }
        catch(Exception e){
            outputString = CONNEXION_ERROR;
        }
        finally {
            if (conn != null)
                conn.disconnect();
        }
        return outputString;
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