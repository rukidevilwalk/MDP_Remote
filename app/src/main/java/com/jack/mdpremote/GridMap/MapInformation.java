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
    MapInformationView mapInformationView;
    Button obstacleBtn;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_information);

        connStatus = "None";

        intent = getIntent();
        // set TAG and Mode for shared preferences
        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        // get received map strings from main activity
        if (sharedPreferences.contains("mapJsonObject")) {
            mapString = sharedPreferences.getString("mapJsonObject", "");
            Log.d(TAG, mapString);

            try {
                mapJsonObject = new JSONObject(mapString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mapInformationView = new MapInformationView(this);
            mapInformationView = findViewById(R.id.mapInformationView);
            mapInformationView.mapJsonObject = mapJsonObject;
        }

        obstacleBtn = findViewById(R.id.obstacleBtn);

        // when obstacle toggle button is clicked
        obstacleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (obstacleBtn.getText().equals("Show Explored")) {
                    mapInformationView.plotObstacle = true;
                    Toast.makeText(getApplicationContext(), "Showing obstacle cells", Toast.LENGTH_SHORT).show();
                    mapInformationView.invalidate();
                } else if (obstacleBtn.getText().equals("Show Obstacle")) {
                    mapInformationView.plotObstacle = false;
                    Toast.makeText(getApplicationContext(), "Showing explored cells", Toast.LENGTH_SHORT).show();
                    mapInformationView.invalidate();
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

    }

}
