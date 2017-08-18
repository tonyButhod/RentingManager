package buthod.tony.rentingmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by Tony on 06/08/2017.
 */

public class MainActivity extends Activity {

    private TextView info = null;
    private TextView title = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        info = (TextView) findViewById(R.id.info);
        title = (TextView) findViewById(R.id.title);
        title.setText("Result :");
        // Recover previous results
        Intent intent = getIntent();
        String res = intent.getStringExtra("result");
        info.setText(res);
    }
}
