package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity allowing the user to change his password.
 */

public class SettingsActivity extends Activity {

    private String mUsername = null;

    private Button mChangePassword = null;
    private ImageButton mBackButton = null;
    private TextView mUsernameView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        mChangePassword = (Button) findViewById(R.id.change_password);
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mUsernameView = (TextView) findViewById(R.id.username);
        // Recover username from extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUsername = extras.getString(SendPostRequest.USERNAME_KEY, null);
        }
        if (mUsername == null) {
            // Go back to main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        mUsernameView.setText(mUsername);
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
                        SharedPreferences prefs = getSharedPreferences(SendPostRequest.PREFS,
                                Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(SendPostRequest.HASH_KEY, result);
                        editor.apply();
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
}
