package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity allowing the user to change his password.
 */

public class SettingsActivity extends Activity {

    public static String PREFERENCES_NAME = "RentingManagerPreferences";
    public static String
            PREF_USERNAME = "username",
            PREF_HASH = "hash",
            PREF_STAT_ACTIVATED = "statisticsActivated";

    private String mUsername = null;
    private String mHash = null;
    private int mAccessLevel = 0;
    private String mMessage = null;

    private Button mChangePassword = null;
    private ImageButton mBackButton = null;
    private Button mSignOut = null;
    private CheckBox mActivateStat = null;
    private Button mMessageButton = null;

    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        mChangePassword = (Button) findViewById(R.id.change_password);
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mSignOut = (Button) findViewById(R.id.sign_out);
        mActivateStat = (CheckBox) findViewById(R.id.activate_statistics);
        mMessageButton = (Button) findViewById(R.id.message_button);
        // Recover username from extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUsername = extras.getString(SendPostRequest.USERNAME_KEY, null);
            mHash = extras.getString(SendPostRequest.HASH_KEY, null);
        }
        if (mUsername == null || mHash == null) {
            // Go back to main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // Listener to go back to main activity
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Listener to change password
        mChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open dialog to change password
                showChangePasswordDialog();
            }
        });
        // Listener to sign out
        mSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove username and hash in sharedPreferences
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.remove(PREF_USERNAME);
                editor.remove(PREF_HASH);
                editor.apply();
                // Finish main activity
                if (MainActivity.instance != null)
                    MainActivity.instance.finish();
                // Start login activity
                Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        // Listener to update the shared preferences on statistics
        mActivateStat.setChecked(mPreferences.getBoolean(PREF_STAT_ACTIVATED, false));
        mActivateStat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(PREF_STAT_ACTIVATED, isChecked);
                editor.apply();
            }
        });
        // Listener to remove or add a new message
        mMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessageDialog(false);
            }
        });
        // Send a post request to get user access and the message if it exists
        getMessagePostRequest();
    }

    private void showChangePasswordDialog() {
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.changePassword);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.change_password, null);
        builder.setView(alertView);
        // Set up the buttons
        Resources res = getResources();
        builder.setPositiveButton(res.getString(R.string.save), null);
        builder.setNegativeButton(res.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        // Get useful view
        final EditText oldPasswordEdit = (EditText) alertView.findViewById(R.id.old_password);
        final EditText newPasswordEdit = (EditText) alertView.findViewById(R.id.new_password);
        final EditText confirmationEdit = (EditText) alertView.findViewById(R.id.confirmation);
        final TextView errorsView = (TextView) alertView.findViewById(R.id.errors);
        // Create alertDialog and set on click listener
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        errorsView.setVisibility(View.GONE);
                        String oldPassword = oldPasswordEdit.getText().toString();
                        String newPassword = newPasswordEdit.getText().toString();
                        String confirmation = confirmationEdit.getText().toString();
                        if (!newPassword.equals(confirmation)) {
                            errorsView.setText(R.string.confirmationIncorrect);
                            errorsView.setVisibility(View.VISIBLE);
                        }
                        else if (newPassword.length() < 8) {
                            errorsView.setText(R.string.atLeast8Char);
                            errorsView.setVisibility(View.VISIBLE);
                        }
                        else {
                            changePasswordPostRequest(oldPassword, newPassword, confirmation);
                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void changePasswordPostRequest(String oldPassword, String newPassword,
                                           String confirmation) {
        mChangePassword.setEnabled(false);
        SendPostRequest req = new SendPostRequest(SendPostRequest.CHANGE_PASSWORD);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.PASSWORD_KEY, oldPassword);
        req.addPostParam(SendPostRequest.NEW_PASSWORD_KEY, newPassword);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                mChangePassword.setEnabled(true);
                if (success) {
                    if (result.equals(SendPostRequest.PASSWORD_INCORRECT)){
                        Toast.makeText(getBaseContext(), R.string.passwordIncorrect,
                                Toast.LENGTH_SHORT).show();
                    }
                    else if (result.equals(SendPostRequest.AT_LEAST_8_CHAR)) {
                        Toast.makeText(getBaseContext(), R.string.atLeast8Char,
                                Toast.LENGTH_LONG).show();
                    }
                    else {
                        // "result" contains the new hash, so shared preferences are updated
                        SharedPreferences.Editor editor = mPreferences.edit();
                        editor.putString(SettingsActivity.PREF_HASH, result);
                        editor.commit();
                        Toast.makeText(getBaseContext(), R.string.newPasswordSaved,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        req.execute();
    }

    private void getMessagePostRequest() {
        SendPostRequest req = new SendPostRequest(SendPostRequest.MESSAGE_MANAGER);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.MESSAGE_ACTION, SendPostRequest.GET_MESSAGE);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    // Save the user access right and the message
                    try {
                        JSONObject resObj = new JSONObject(result);
                        mAccessLevel = resObj.getInt(SendPostRequest.ACCESS_KEY);
                        if (resObj.has(SendPostRequest.MESSAGE_KEY))
                            mMessage = resObj.getString(SendPostRequest.MESSAGE_KEY);
                        else
                            mMessage = null;
                        // If the user has the right access level, display message button
                        if (mAccessLevel >= 3) {
                            mMessageButton.setVisibility(View.VISIBLE);
                        }
                    }
                    catch (JSONException e) {
                        // The user do not have the good access right, then do nothing.
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        req.execute();
    }

    private void showMessageDialog(final boolean editMode) {
        Resources res = getResources();
        // Initialize an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(editMode ? R.string.add_message : R.string.add_remove_message);
        // Set the view of the alert dialog
        LayoutInflater inflater = getLayoutInflater();
        View alertView;
        final TextView messageView;
        if (editMode) {
            alertView = inflater.inflate(R.layout.edit_message, null);
            messageView = (TextView) alertView.findViewById(R.id.message_view);
        }
        else {
            alertView = inflater.inflate(R.layout.add_remove_message, null);
            messageView = (TextView) alertView.findViewById(R.id.message_view);
            if (mMessage != null) {
                messageView.setText(mMessage);
            }
            else {
                messageView.setText(res.getString(R.string.noMessage));
                messageView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            }
            messageView.setPadding(50, 10, 50, 10);
        }
        builder.setView(alertView);
        // Set up the buttons
        if (!editMode && mMessage != null) {
            builder.setNegativeButton(res.getString(R.string.remove),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeMessagePostRequest();
                            dialog.cancel();
                        }
                    });
        }
        if (!editMode) {
            builder.setPositiveButton(res.getString(R.string.add), null);
        }
        else {
            builder.setPositiveButton(res.getString(R.string.send), null);
        }
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                        if (!editMode)
                            // Enter edit mode for the message
                            showMessageDialog(true);
                        else
                            // Send the message to other users
                            addMessagePostRequest(messageView.getText().toString());
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void removeMessagePostRequest() {
        SendPostRequest req = new SendPostRequest(SendPostRequest.MESSAGE_MANAGER);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.MESSAGE_ACTION, SendPostRequest.REMOVE_MESSAGE);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    // Check if the message was removed
                    if (result.compareTo(SendPostRequest.ACTION_OK) == 0) {
                        mMessage = null;
                        Toast.makeText(getBaseContext(), R.string.messageRemoved,
                                Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getBaseContext(), R.string.anErrorOccurred,
                                Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        req.execute();
    }

    private void addMessagePostRequest(final String messageToAdd) {
        SendPostRequest req = new SendPostRequest(SendPostRequest.MESSAGE_MANAGER);
        req.addPostParam(SendPostRequest.USERNAME_KEY, mUsername);
        req.addPostParam(SendPostRequest.HASH_KEY, mHash);
        req.addPostParam(SendPostRequest.MESSAGE_ACTION, SendPostRequest.ADD_MESSAGE);
        req.addPostParam(SendPostRequest.MESSAGE_KEY, messageToAdd);
        req.setOnPostExecute(new SendPostRequest.OnPostExecute() {
            @Override
            public void postExecute(boolean success, String result) {
                if (success) {
                    // Check if the message was added
                    if (result.compareTo(SendPostRequest.ACTION_OK) == 0) {
                        mMessage = messageToAdd;
                        Toast.makeText(getBaseContext(), R.string.messageSent,
                                Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getBaseContext(), R.string.anErrorOccurred,
                                Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    // A connection error occurred
                    Toast.makeText(getBaseContext(), R.string.connectionError,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        req.execute();
    }
}
