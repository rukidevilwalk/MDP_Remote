package com.jack.mdpremote;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jack.mdpremote.Bluetooth.BluetoothConnectionService;
import com.jack.mdpremote.Bluetooth.BluetoothSettings;
import com.jack.mdpremote.GridMap.GridMap;
import com.jack.mdpremote.SendReceive.SendReceive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SensorEventListener {

    private static SharedPreferences sharedPreferences;

    private static SharedPreferences.Editor editor;

    private static Context context;

    private static long exploreTimer;

    private static long fastestTimer;

    private static boolean startActivityStatus = true;

    public String connStatus = "None";

    String[] direction = {"None", "up", "down", "left", "right"};


    GridMap gridMap;

    SendReceive sendReceive;

    TextView connStatusTextView;

    MenuItem bluetoothMenuItem, sendReceiveMenuItem;

    TextView exploreTimeTextView, fastestTimeTextView;

    ToggleButton exploreToggleBtn, fastestToggleBtn;

    ImageButton exploreResetImageBtn, fastestResetImageBtn;

    TextView robotStatusTextView;

    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackwardImageBtn, turnLeftImageBtn;

    Switch phoneTiltSwitch;

    Button resetMapBtn;

    ToggleButton setSPToggle, setWPToggle;

    TextView xAxisTextView, yAxisTextView;

    Button exploredImageBtn, obstacleImageBtn, clearImageBtn;

    Spinner directionDropdown;

    TextView sentMessageText;

    TextView receivedMessageText;

    ToggleButton manualAutoToggleBtn;

    Button manualUpdateBtn;

    Intent intent;

    BluetoothConnectionService BluetoothConnection;

    BluetoothDevice BTDevice;

    ProgressDialog progressDialog;


    private Sensor sensor;

    private SensorManager sensorManager;

    private UUID myUUID;

    Handler timerHandler = new Handler();

    Runnable timerRunnableExplore = new Runnable() {
        @Override
        public void run() {

            long millisExplore = System.currentTimeMillis() - exploreTimer;

            int secondsExplore = (int) (millisExplore / 1000);

            int minutesExplore = secondsExplore / 60;

            secondsExplore = secondsExplore % 60;

            exploreTimeTextView.setText(String.format("%02d:%02d", minutesExplore, secondsExplore));

            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timerRunnableFastest = new Runnable() {
        @Override
        public void run() {

            long millisFastest = System.currentTimeMillis() - fastestTimer;

            int secondsFastest = (int) (millisFastest / 1000);

            int minutesFastest = secondsFastest / 60;

            secondsFastest = secondsFastest % 60;

            fastestTimeTextView.setText(String.format("%02d:%02d", minutesFastest, secondsFastest));

            timerHandler.postDelayed(this, 500);
        }
    };


    Runnable timedMessage = new Runnable() {
        @Override
        public void run() {

            refreshMessage();

            timerHandler.postDelayed(timedMessage, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        gridMap = new GridMap(this);

        sendReceive = new SendReceive();


        exploreTimer = 0;

        fastestTimer = 0;


        gridMap = findViewById(R.id.mapView);

        xAxisTextView = findViewById(R.id.xAxisTextView);

        exploreTimeTextView = findViewById(R.id.exploreTimeTextView);

        exploreToggleBtn = findViewById(R.id.exploreToggleBtn);

        exploreResetImageBtn = findViewById(R.id.exploreResetImageBtn);

        fastestToggleBtn = findViewById(R.id.fastestToggleBtn);

        fastestResetImageBtn = findViewById(R.id.fastestResetImageBtn);

        robotStatusTextView = findViewById(R.id.robotStatusTextView);

        yAxisTextView = findViewById(R.id.yAxisTextView);

        resetMapBtn = findViewById(R.id.resetMapBtn);

        setSPToggle = findViewById(R.id.setStartPointToggleBtn);

        setWPToggle = findViewById(R.id.setWaypointToggleBtn);

        turnRightImageBtn = findViewById(R.id.turnRightImageBtn);

        moveBackwardImageBtn = findViewById(R.id.moveBackwardImageBtn);

        turnLeftImageBtn = findViewById(R.id.turnLeftImageBtn);

        phoneTiltSwitch = findViewById(R.id.phoneTiltSwitch);

        sendReceiveMenuItem = findViewById(R.id.sendReceiveMenuItem);

        bluetoothMenuItem = findViewById(R.id.bluetoothMenuItem);

        connStatusTextView = findViewById(R.id.connStatusTextView);

        exploredImageBtn = findViewById(R.id.exploredImageBtn);

        obstacleImageBtn = findViewById(R.id.obstacleImageBtn);

        clearImageBtn = findViewById(R.id.unexploredImageBtn);

        directionDropdown = findViewById(R.id.directionDropdown);

        moveForwardImageBtn = findViewById(R.id.moveForwardImageBtn);

        fastestTimeTextView = findViewById(R.id.fastestTimeTextView);

        sentMessageText = findViewById(R.id.imagesTextView);

        receivedMessageText = findViewById(R.id.messageReceivedTextView);

        manualAutoToggleBtn = findViewById(R.id.manualAutoToggleBtn);

        manualUpdateBtn = findViewById(R.id.manualUpdateBtn);


        MainActivity.context = getApplicationContext();

        this.sharedPreferences();

        editor.putString("sentText", "");

        editor.putString("receivedText", "");

        editor.putString("image", "");

        editor.putString("direction", "None");

        editor.putString("connStatus", connStatus);

        editor.commit();


        timerHandler.post(timedMessage);


        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        robotStatusTextView.setMovementMethod(new ScrollingMovementMethod());

        sentMessageText.setMovementMethod(new ScrollingMovementMethod());

        receivedMessageText.setMovementMethod(new ScrollingMovementMethod());


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        exploreToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Button exploreToggleBtn = (Button) view;

                if (exploreToggleBtn.getText().equals("EXPLORE")) {

                    timerHandler.removeCallbacks(timerRunnableExplore);

                } else if (exploreToggleBtn.getText().equals("STOP")) {

                    sendMessage("B1:0");

                    exploreTimer = System.currentTimeMillis();

                    timerHandler.postDelayed(timerRunnableExplore, 0);

                }

            }
        });


        exploreResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                exploreTimeTextView.setText("00:00");

                if (exploreToggleBtn.isChecked())

                    exploreToggleBtn.toggle();

                timerHandler.removeCallbacks(timerRunnableExplore);

            }
        });


        fastestToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Button fastestToggleBtn = (Button) view;

                if (fastestToggleBtn.getText().equals("FASTEST")) {

                    timerHandler.removeCallbacks(timerRunnableFastest);

                } else if (fastestToggleBtn.getText().equals("STOP")) {

                    sendMessage("B1:1");

                    fastestTimer = System.currentTimeMillis();

                    timerHandler.postDelayed(timerRunnableFastest, 0);
                }

            }
        });


        fastestResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                fastestTimeTextView.setText("00:00");

                if (fastestToggleBtn.isChecked())
                    fastestToggleBtn.toggle();

                timerHandler.removeCallbacks(timerRunnableFastest);

            }
        });


        moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (gridMap.getAutoUpdate())
                    updateStatus("SET TO MANUAL MODE FIRST");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");

                    refreshLabel();

                    if (gridMap.getValidPosition())
                        updateStatus("MOVE: FORWARD");
                    else
                        updateStatus("MOVE: FORWARD IS BLOCKED");
                    sendMessage("forward");
                } else
                    updateStatus("SET STARTING POINT FIRST");

            }
        });


        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (gridMap.getAutoUpdate())
                    updateStatus("SET TO MANUAL MODE FIRST'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    refreshLabel();
                    updateStatus("TURN: RIGHT");
                    sendMessage("right");
                } else
                    updateStatus("SET STARTING POINT FIRST");

            }
        });


        moveBackwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (gridMap.getAutoUpdate())
                    updateStatus("SET TO MANUAL MODE FIRST'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    refreshLabel();
                    if (gridMap.getValidPosition())
                        updateStatus("MOVE: REVERSE");
                    else
                        updateStatus("MOVE: REVERSE IS BLOCKED");
                    sendMessage("reverse");
                } else
                    updateStatus("SET STARTING POINT FIRST");

            }
        });


        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (gridMap.getAutoUpdate())
                    updateStatus("SET TO MANUAL MODE FIRST");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    refreshLabel();
                    updateStatus("TURN: LEFT");
                    sendMessage("left");
                } else
                    updateStatus("SET STARTING POINT FIRST");

            }
        });


        phoneTiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (gridMap.getAutoUpdate()) {

                    updateStatus("SET TO MANUAL MODE FIRST");

                    phoneTiltSwitch.setChecked(false);

                    compoundButton.setText("TILT OFF");

                } else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {

                    if (phoneTiltSwitch.isChecked()) {

                        compoundButton.setText("TILT ON");

                        phoneTiltSwitch.setPressed(true);

                        sensorManager.registerListener(MainActivity.this, sensor, sensorManager.SENSOR_DELAY_NORMAL);

                        sensorHandler.post(sensorDelay);

                    } else {

                        try {

                            sensorManager.unregisterListener(MainActivity.this);

                        } catch (IllegalArgumentException e) {

                            e.printStackTrace();

                        }

                        sensorHandler.removeCallbacks(sensorDelay);

                        compoundButton.setText("TILT OFF");
                    }
                } else {

                    updateStatus("SET STARTING POINT FIRST");

                    phoneTiltSwitch.setChecked(false);

                    compoundButton.setText("TILT OFF");
                }
            }
        });


        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gridMap.resetMap();
            }
        });

        setSPToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (setSPToggle.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {

                    gridMap.setStartCoordinatesStatus(true);

                    gridMap.toggleCheckedBtn("setSPToggle");

                }

            }
        });


        setWPToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (setWPToggle.getText().equals("CANCEL")) {

                    gridMap.setWPStatus(true);

                    gridMap.toggleCheckedBtn("setWPToggle");

                }
            }
        });


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, direction);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        directionDropdown.setAdapter(adapter);

        directionDropdown.setOnItemSelectedListener(this);

        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!gridMap.getExploredStatus()) {

                    gridMap.setExploredStatus(true);

                    gridMap.toggleCheckedBtn("exploredImageBtn");

                } else
                    gridMap.setSetObstacleStatus(false);

            }
        });


        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!gridMap.getSetObstacleStatus()) {

                    gridMap.setSetObstacleStatus(true);

                    gridMap.toggleCheckedBtn("obstacleImageBtn");

                } else
                    gridMap.setSetObstacleStatus(false);

            }
        });


        clearImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!gridMap.getUnSetCellStatus()) {

                    gridMap.setUnSetCellStatus(true);

                    gridMap.toggleCheckedBtn("clearImageBtn");

                } else
                    gridMap.setUnSetCellStatus(false);

            }
        });


        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (manualAutoToggleBtn.getText().equals("AUTO")) {

                    if (phoneTiltSwitch.isChecked()) {

                        phoneTiltSwitch.setChecked(false);

                        try {

                            sensorManager.unregisterListener(MainActivity.this);

                        } catch (IllegalArgumentException e) {

                            e.printStackTrace();

                        }

                        sensorHandler.removeCallbacks(sensorDelay);
                    }

                    try {
                        gridMap.setAutoUpdate(true);

                        gridMap.toggleCheckedBtn("None");

                        manualUpdateBtn.setEnabled(false);

                    } catch (JSONException e) {

                        e.printStackTrace();

                    }

                } else if (manualAutoToggleBtn.getText().equals("MANUAL")) {


                    try {
                        gridMap.setAutoUpdate(false);

                        gridMap.toggleCheckedBtn("None");
                        manualUpdateBtn.setEnabled(true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }
        });


        manualUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    gridMap.updateMapInformation();
                } catch (JSONException e) {

                }

            }
        });


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Trying to reconnect..");
        progressDialog.setCancelable(true);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.sharedPreferences();
        switch (item.getItemId()) {
            case R.id.bluetoothMenuItem:
                intent = new Intent(MainActivity.this, BluetoothSettings.class);
                startActivityStatus = false;
                startActivityForResult(intent, 1);
                break;
            case R.id.sendReceiveMenuItem:
                intent = new Intent(MainActivity.this, SendReceive.class);
                editor.putString("receivedText", receivedMessageText.getText().toString());
                break;

            default:
                return false;
        }

        editor.putString("mapJsonObject", String.valueOf(gridMap.getCreateJsonObject()));
        editor.putString("connStatus", connStatusTextView.getText().toString());
        editor.commit();
        if (startActivityStatus)
            startActivity(intent);
        startActivityStatus = true;
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        editor.putString("direction", direction[position]);
        refreshDirection(direction[position]);
        editor.commit();

    }


    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    private void refreshLabel() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurrentCoordinates()[0] - 1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurrentCoordinates()[1] - 1));


    }


    public void refreshMessage() {

        receivedMessageText.setText(sharedPreferences.getString("receivedText", ""));

        sentMessageText.setText(sharedPreferences.getString("image", ""));
        connStatusTextView.setText(sharedPreferences.getString("connStatus", ""));

    }


    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
        if (!(direction.equals("None"))) {
            switch (direction) {
                case "up":
                    sendMessage("B3:0");
                    break;
                case "right":
                    sendMessage("B3:1");
                    break;
                case "down":
                    sendMessage("B3:2");
                    break;
                case "left":
                    sendMessage("B3:3");
                    break;
            }
        }

    }


    private void updateStatus(String message) {
        robotStatusTextView.setText(message);
    }


    public static void setSPWP(String type, String x, String y) {

        sharedPreferences();

        String message;
        message = type + "" + x + "" + y;

        editor.putString("sentText", sharedPreferences.getString("sentText", "") + "\n " + message);
        editor.commit();
        sendMessage("B2:" + message);

    }

    public static void sendMessage(String message) {

        sharedPreferences();

        if (BluetoothConnectionService.BluetoothConnectionStatus) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }

        editor.putString("sentText", sharedPreferences.getString("sentText", "") + "\n " + message);
        editor.commit();

    }


    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    public static void sharedPreferences() {

        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);

        editor = sharedPreferences.edit();
    }

    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BluetoothDevice mDevice = intent.getParcelableExtra("Device");

            String status = intent.getStringExtra("Status");

            sharedPreferences();

            if (status.equals("connected")) {

                try {

                    progressDialog.dismiss();

                } catch (NullPointerException e) {

                    e.printStackTrace();

                }

                Toast.makeText(MainActivity.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();

                editor.putString("connStatus", mDevice.getName());

                connStatusTextView.setText(mDevice.getName());

            } else if (status.equals("disconnected")) {

                Toast.makeText(MainActivity.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();

                BluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                BluetoothConnection.connectionLost(BTDevice);


                editor.putString("connStatus", "None");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("None");

                progressDialog.show();
            }
            editor.commit();
        }
    };


    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String payload = intent.getStringExtra("receivedMessage");

            try {


                if (payload.length() == 158 && payload.substring(0, 3).equals("B4:")) {

                    String robotPositionX = payload.substring(3, 5);
                    String robotPositionY = payload.substring(5, 7);
                    String robotDirection = payload.substring(7, 8);
                    String mapInfo = payload.substring(8);

                    JSONObject payloadObject = new JSONObject();
                    payloadObject.put("explored", mapInfo);
                    payloadObject.put("robotX", robotPositionX);
                    payloadObject.put("robotY", robotPositionY);
                    payloadObject.put("robotDirection", robotDirection);

                    JSONArray payloadArray = new JSONArray();
                    payloadArray.put(payloadObject);

                    JSONObject payloadBody = new JSONObject();
                    payloadBody.put("map", payloadArray);

                    payload = String.valueOf(payloadBody);

                    sharedPreferences();
                    String receivedText = sharedPreferences.getString("receivedText", "") + "\n " + payload;
                    editor.putString("receivedText", receivedText);
                    editor.commit();


                } else if (payload.substring(0, 3).equals("B5:")) {
                    String MDF;
                    int indexOfImage = payload.indexOf("|");
                    if (indexOfImage != -1) {
                        MDF = payload.substring(3, indexOfImage);
                        String imageBody = payload.substring(indexOfImage + 1);

                        JSONArray payloadArray = new JSONArray();
                        JSONObject payloadObject = new JSONObject();

                        payloadObject.put("imageString", imageBody);
                        payloadArray.put(payloadObject);
                        JSONObject payloadBody = new JSONObject();
                        payloadBody.put("image", payloadArray);

                        payload = String.valueOf(payloadBody);
                    } else {
                        MDF = payload.substring(3);
                    }


                    sharedPreferences();
                    String receivedText = sharedPreferences.getString("receivedText", "") + "\n " + MDF;
                    editor.putString("receivedText", receivedText);
                    editor.commit();

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (gridMap.getAutoUpdate()) {

                try {

                    gridMap.setReceivedPayload(new JSONObject(payload));

                    gridMap.updateMapInformation();

                } catch (JSONException e) {

                }
            } else {

                try {
                    gridMap.setReceivedPayload(new JSONObject(payload));

                } catch (JSONException e) {

                }
            }


        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {

                    BTDevice = data.getExtras().getParcelable("BTDevice");

                    myUUID = (UUID) data.getSerializableExtra("deviceUUID");

                }
        }
    }


    Handler sensorHandler = new Handler();

    boolean sensorFlag = false;

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {

            sensorFlag = true;

            sensorHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];

        float y = event.values[1];

        float z = event.values[2];

        if (sensorFlag) {

            if (y < -2) {

                gridMap.moveRobot("forward");

                refreshLabel();

                sendMessage("forward");

            } else if (y > 2) {

                gridMap.moveRobot("back");

                refreshLabel();

                sendMessage("reverse");

            } else if (x > 2) {

                gridMap.moveRobot("left");

                refreshLabel();

                sendMessage("left");

            } else if (x < -2) {

                gridMap.moveRobot("right");

                refreshLabel();

                sendMessage("right");
            }
        }

        sensorFlag = false;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

            LocalBroadcastManager.getInstance(this).unregisterReceiver(mainReceiver);

            sensorManager.unregisterListener(this);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();

        }
    }


    @Override
    protected void onPause() {

        super.onPause();

        try {

            LocalBroadcastManager.getInstance(this).unregisterReceiver(mainReceiver);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();

        }
    }


    @Override
    protected void onResume() {

        super.onResume();

        try {

            IntentFilter filter2 = new IntentFilter("ConnectionStatus");

            LocalBroadcastManager.getInstance(this).registerReceiver(mainReceiver, filter2);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();

        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

    }

}
