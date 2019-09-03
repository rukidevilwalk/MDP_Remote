package com.jack.mdpremote.GridMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jack.mdpremote.R;

import org.json.JSONException;
import org.json.JSONObject;

public class MapInformation extends AppCompatActivity {
    private final static String TAG = "MapInformation";

    Intent intent;
    String mapString;
    String connStatus;
    JSONObject mapJsonObject;
    GridView gridView;
    Button obstacleBtn;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        showLog("Entering onCreateView");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_information);

        connStatus = "None";

        intent = getIntent();
        // set TAG and Mode for shared preferences
        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        // get received map strings from main activity
        if (sharedPreferences.contains("mapJsonObject")) {
            mapString = sharedPreferences.getString("mapJsonObject", "");
            showLog(mapString);
            try {
                mapJsonObject = new JSONObject(mapString);
                showLog("mapJsonObject try success");
            } catch (JSONException e) {
                e.printStackTrace();
                showLog("mapJsonObject try fail");
            }
            gridView = new GridView(this);
            gridView = findViewById(R.id.mapInformationView);
            gridView.mapJsonObject = mapJsonObject;
        }

        obstacleBtn = findViewById(R.id.obstacleBtn);

        // when obstacle toggle button is clicked
        obstacleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (obstacleBtn.getText().equals("Show Explored")) {
                    gridView.plotObstacle = true;
                    Toast.makeText(getApplicationContext(), "Showing obstacle cells", Toast.LENGTH_SHORT).show();
                    gridView.invalidate();
                } else if (obstacleBtn.getText().equals("Show Obstacle")) {
                    gridView.plotObstacle = false;
                    Toast.makeText(getApplicationContext(), "Showing explored cells", Toast.LENGTH_SHORT).show();
                    gridView.invalidate();
                }
            }
        });

        // for toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView connStatusTextView = findViewById(R.id.connStatusTextView);

        if (sharedPreferences.contains("connStatus"))
            connStatus = sharedPreferences.getString("connStatus", "");
        connStatusTextView.setText(connStatus);
        showLog("Exiting onCreateView");
    }

    // show log message
    private void showLog(String message) {
        Log.d(TAG, message);
    }
}
