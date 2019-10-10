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
import android.util.Log;
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
import com.jack.mdpremote.GridMap.MapInformation;
import com.jack.mdpremote.SendReceive.SendReceive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener , SensorEventListener{
    private static final String TAG = "MainActivity";

    // for transferring of information between activities
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;
    private static boolean autoUpdate = false;

    // declaration of variable
    private static long exploreTimer;          // for exploration timer
    private static long fastestTimer;          // for fastest timer
    private static boolean startActivityStatus = true;  // to indicate whether an intent should be started
    public String connStatus = "None";
    String[] direction = { "None","up","down","left","right"};

    // for view by id
    GridMap gridMap;
    SendReceive sendReceive;
    TextView connStatusTextView;
    MenuItem bluetoothMenuItem, sendReceiveMenuItem, getMapMenuItem;
    TextView exploreTimeTextView, fastestTimeTextView;
    ToggleButton exploreToggleBtn, fastestToggleBtn;
    ImageButton exploreResetImageBtn, fastestResetImageBtn;
    TextView robotStatusTextView;
    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackwardImageBtn, turnLeftImageBtn;
    Switch phoneTiltSwitch;
    Button resetMapBtn ;
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn;
    TextView xAxisTextView, yAxisTextView;
    Button exploredImageBtn, obstacleImageBtn, clearImageBtn;
    Spinner directionDropdown;
    static TextView messageSentTextView;
    TextView messageReceivedTextView;
    ToggleButton manualAutoToggleBtn;
    Button manualUpdateBtn;

    Intent intent;

    // for bluetooth
    StringBuilder message;
    BluetoothConnectionService mBluetoothConnection;
    private static UUID myUUID;
    BluetoothDevice mBTDevice;
    ProgressDialog myDialog;

    //Sensors for accelerometer
    private Sensor mSensor;
    private SensorManager mSensorManager;

    // runs without a timer by reposting this handler at the end of the runnable
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

    // set a timer to refresh the message sent
    Runnable timedMessage = new Runnable() {
        @Override
        public void run() {
            refreshMessage();
            timerHandler.postDelayed(timedMessage, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        showLog("Entering onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create a new map
        gridMap = new GridMap(this);
        // create a new message box to sync messages
        sendReceive = new SendReceive();

        // default value for timer is 0
        exploreTimer = 0;
        fastestTimer = 0;

        // View ID Initialization
        gridMap = findViewById(R.id.mapView);
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
        resetMapBtn = findViewById(R.id.resetMapBtn);
        setStartPointToggleBtn = findViewById(R.id.setStartPointToggleBtn);
        setWaypointToggleBtn = findViewById(R.id.setWaypointToggleBtn);
        exploredImageBtn = findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = findViewById(R.id.obstacleImageBtn);
        clearImageBtn = findViewById(R.id.unexploredImageBtn);
        directionDropdown = findViewById(R.id.directionDropdown);

        sendReceiveMenuItem = findViewById(R.id.sendReceiveMenuItem);
        getMapMenuItem = findViewById(R.id.getMapMenuItem);
        bluetoothMenuItem = findViewById(R.id.bluetoothMenuItem);
        connStatusTextView = findViewById(R.id.connStatusTextView);

        fastestTimeTextView = findViewById(R.id.fastestTimeTextView);
        exploreTimeTextView = findViewById(R.id.exploreTimeTextView);
        exploreToggleBtn = findViewById(R.id.exploreToggleBtn);
        exploreResetImageBtn = findViewById(R.id.exploreResetImageBtn);
        fastestToggleBtn = findViewById(R.id.fastestToggleBtn);
        fastestResetImageBtn = findViewById(R.id.fastestResetImageBtn);

        robotStatusTextView = findViewById(R.id.robotStatusTextView);

        moveForwardImageBtn = findViewById(R.id.moveForwardImageBtn);
        turnRightImageBtn = findViewById(R.id.turnRightImageBtn);
        moveBackwardImageBtn = findViewById(R.id.moveBackwardImageBtn);
        turnLeftImageBtn = findViewById(R.id.turnLeftImageBtn);

        phoneTiltSwitch = findViewById(R.id.phoneTiltSwitch);

        messageSentTextView = findViewById(R.id.imagesTextView);
        messageReceivedTextView = findViewById(R.id.messageReceivedTextView);

        manualAutoToggleBtn = findViewById(R.id.manualAutoToggleBtn);
        manualUpdateBtn = findViewById(R.id.manualUpdateBtn);

        MainActivity.context = getApplicationContext();
        this.sharedPreferences();
        // clearing text messages in shared preferences
        editor.putString("sentText", "");
        editor.putString("receivedText", "");
        editor.putString("image", "");
        editor.putString("direction", "None");
        editor.putString("connStatus", connStatus);
        editor.commit();

        // start the timer for the message to be refreshed after every second
        timerHandler.post(timedMessage);

        // for bluetooth
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        // not used, restore state for states when tilting devices
        if (savedInstanceState != null) {
            showLog("Entering savedInstanceState");
        }

        // allows scrolling of text view
        robotStatusTextView.setMovementMethod(new ScrollingMovementMethod());
        messageSentTextView.setMovementMethod(new ScrollingMovementMethod());
        messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());

        //Create Sensor Manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //accelerometer sensor
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // when fastest toggle button clicked
        exploreToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploreToggleBtn");
                Button exploreToggleBtn = (Button) view;
                if (exploreToggleBtn.getText().equals("EXPLORE")) {
                    showToast("Exploration timer stop!");
                    timerHandler.removeCallbacks(timerRunnableExplore);
                } else if (exploreToggleBtn.getText().equals("STOP")) {
                    showToast("Exploration timer start!");
                    sendMessage("B1:0");
                    exploreTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableExplore, 0);
                } else {
                    showToast("Else statement: " + exploreToggleBtn.getText());
                }
                showLog("Exiting exploreToggleBtn");
            }
        });

        // when explore reset image button clicked
        exploreResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploreResetImageBtn");
                showToast("Reseting exploration time...");
                exploreTimeTextView.setText("00:00");
                if (exploreToggleBtn.isChecked())
                    exploreToggleBtn.toggle();
                timerHandler.removeCallbacks(timerRunnableExplore);
                showLog("Exiting exploreResetImageBtn");
            }
        });

        // when fastest toggle button clicked
        fastestToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked fastestToggleBtn");
                Button fastestToggleBtn = (Button) view;
                if (fastestToggleBtn.getText().equals("FASTEST")) {
                    showToast("Fastest timer stop!");
                    timerHandler.removeCallbacks(timerRunnableFastest);
                } else if (fastestToggleBtn.getText().equals("STOP")) {
                    showToast("Fastest timer start!");
                    sendMessage("B1:1");
                    fastestTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableFastest, 0);
                } else
                    showToast(fastestToggleBtn.getText().toString());
                showLog("Exiting fastestToggleBtn");
            }
        });

        // when fastest reset image button clicked
        fastestResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked fastestResetImageBtn");
                showToast("Reseting fastest time...");
                fastestTimeTextView.setText("00:00");
                if (fastestToggleBtn.isChecked())
                    fastestToggleBtn.toggle();
                timerHandler.removeCallbacks(timerRunnableFastest);
                showLog("Exiting fastestResetImageBtn");
            }
        });

        // when move forward image button clicked
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
                showLog("Exiting moveForwardImageBtn");
            }
        });

        // when turn right image button clicked
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
                showLog("Exiting turnRightImageBtn");
            }
        });

        // when move backward image button clicked
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
                showLog("Exiting moveBackwardImageBtn");
            }
        });

        // when turn left image button clicked
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
                showLog("Exiting turnLeftImageBtn");
            }
        });

        // when phone tilt switch button clicked
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
                        showToast("Tilt motion control: ON");
                        phoneTiltSwitch.setPressed(true);

                        //register sensor when toggled ON
                        mSensorManager.registerListener(MainActivity.this, mSensor, mSensorManager.SENSOR_DELAY_NORMAL);
                        //start a runnable that will change boolean flag to true to allow onSensorChanged code to execute every 1-2 seconds
                        sensorHandler.post(sensorDelay);
                    } else {
                        showToast("Tilt motion control: OFF");
                        showLog("unregistering Sensor Listener");
                        try {
                            //unregister when button clicked to save battery since the sensor is very power consuming.
                            mSensorManager.unregisterListener(MainActivity.this);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        //stops the runnable loop
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

        // Reset Map button listener
        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gridMap.resetMap();
            }
        });

        // when set starting point toggle button clicked
        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setStartPointToggleBtn");
                if (setStartPointToggleBtn.getText().equals("STARTING POINT"))
                    showToast("Cancelled selecting starting point");
                else if (setStartPointToggleBtn.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setStartPointToggleBtn");
            }
        });

        // when set waypoint toggle button clicked
        setWaypointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setWaypointToggleBtn");
                if (setWaypointToggleBtn.getText().equals("WAYPOINT"))
                    showToast("Cancelled selecting waypoint");
                else if (setWaypointToggleBtn.getText().equals("CANCEL")) {
                    showToast("Please select waypoint");
                    gridMap.setWaypointStatus(true);
                    gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setWaypointToggleBtn");
            }
        });

        // Direction dropdown box
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_item, direction);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directionDropdown.setAdapter(adapter);
        directionDropdown.setOnItemSelectedListener(this);

        // when explored button clicked
        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploredImageBtn");
                if (!gridMap.getExploredStatus()) {
                    showToast("Please check cell");
                    gridMap.setExploredStatus(true);
                    gridMap.toggleCheckedBtn("exploredImageBtn");
                } else if (gridMap.getExploredStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting exploredImageBtn");
            }
        });

        // when obstacle plot button clicked
        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked obstacleImageBtn");
                if (!gridMap.getSetObstacleStatus()) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                } else if (gridMap.getSetObstacleStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting obstacleImageBtn");
            }
        });

        // when clear button clicked
        clearImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked clearImageBtn");
                if (!gridMap.getUnSetCellStatus()) {
                    showToast("Please remove cells");
                    gridMap.setUnSetCellStatus(true);
                    gridMap.toggleCheckedBtn("clearImageBtn");
                } else if (gridMap.getUnSetCellStatus())
                    gridMap.setUnSetCellStatus(false);
                showLog("Exiting clearImageBtn");
            }
        });

        // when manual auto button clicked
        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked manualAutoToggleBtn");
                if (manualAutoToggleBtn.getText().equals("AUTO")) {

                    // Turn off tilt sensor if it's on
                    if (phoneTiltSwitch.isChecked()){
                        phoneTiltSwitch.setChecked(false);
                        showToast("Tilt motion control: OFF");
                        try {
                            //unregister when button clicked to save battery since the sensor is very power consuming.
                            mSensorManager.unregisterListener(MainActivity.this);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }

                        sensorHandler.removeCallbacks(sensorDelay);
                    }


                    try {
                        gridMap.setAutoUpdate(true);
                        autoUpdate = true;
                        gridMap.toggleCheckedBtn("None");
                        manualUpdateBtn.setEnabled(false);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("AUTO mode");
                } else if (manualAutoToggleBtn.getText().equals("MANUAL")) {



                    try {
                        gridMap.setAutoUpdate(false);
                        autoUpdate = false;
                        gridMap.toggleCheckedBtn("None");
                        manualUpdateBtn.setEnabled(true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("MANUAL mode");
                }
                showLog("Exiting manualAutoToggleBtn");
            }
        });

        // Reset Map button listener
        manualUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    gridMap.updateMapInformation();
                } catch (JSONException e){
                    showLog("Manual update error" + e);
                }

            }
        });

        // for toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Progress dialog to show when the bluetooth is disconnected
        myDialog = new ProgressDialog(MainActivity.this);
        myDialog.setMessage("Trying to reconnect..");
        myDialog.setCancelable(true);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    // For creating dropdown menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    // For populating dropdown menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.sharedPreferences();
        switch (item.getItemId()) {
            case R.id.bluetoothMenuItem:
                //currentActivity = false;
                showToast("Entering Bluetooth Configuration...");
                intent = new Intent(MainActivity.this, BluetoothSettings.class);
                startActivityStatus = false;
                startActivityForResult(intent, 1);
                break;
            case R.id.sendReceiveMenuItem:
                showToast("Message Box selected");
                intent = new Intent(MainActivity.this, SendReceive.class);
                editor.putString("receivedText", messageReceivedTextView.getText().toString());
                break;
            case R.id.getMapMenuItem:
                showToast("Get Map Information selected");
                intent = new Intent(MainActivity.this, MapInformation.class);
                break;

            default:
                showToast("onOptionsItemSelected has reached default");
                return false;
        }
        // pass information to activity
        editor.putString("mapJsonObject", String.valueOf(gridMap.getCreateJsonObject()));
        editor.putString("connStatus", connStatusTextView.getText().toString());
        editor.commit();
        if (startActivityStatus)
            startActivity(intent);
        startActivityStatus = true;
        return super.onOptionsItemSelected(item);
    }

    // Direction dropdown listener for changing direction
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        editor.putString("direction", direction[position]);
        refreshDirection(direction[position]);
        editor.commit();

    }

    // Direction dropdown listener for no action
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    // For setting the direction dropdown box value based on sharedPreferences
    private void setDirectionDropdown(){
        String d = sharedPreferences.getString("direction", "");
        switch (d) {
            case "None":
                directionDropdown.setSelection(0);
                break;
            case "up":
                directionDropdown.setSelection(1);
                break;
            case "down":
                directionDropdown.setSelection(2);
                break;
            case "left":
                directionDropdown.setSelection(3);
                break;
            case "right":
                directionDropdown.setSelection(4);
                break;
        }
    }
    // for refreshing all the label in the screen
    private void refreshLabel() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]-1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]-1));
       // setDirectionDropdown();

    }

    // for refreshing the message sent and received after a certain time
    public void refreshMessage() {
        // get received text from main activity
        messageReceivedTextView.setText(sharedPreferences.getString("receivedText", ""));
        //messageSentTextView.setText(sharedPreferences.getString("sentText", ""));
        messageSentTextView.setText(sharedPreferences.getString("image", ""));
        connStatusTextView.setText(sharedPreferences.getString("connStatus", ""));
      //  setDirectionDropdown();
    }

    // for refreshing the direction of the robot
    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
		if (!(direction.equals("None"))){
		    switch (direction){
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

    // for updating the displaying for robot status
    private void updateStatus(String message) {
        robotStatusTextView.setText(message);
    }

    // Sends algo the coords for SP/WP after setting it on the map
    public static void setSPWP(String type, String x, String y)  {
        showLog("Entering sendMessage");
        sharedPreferences();

        String message;
        message = type + ""  + x + "" + y;

        editor.putString("sentText", sharedPreferences.getString("sentText", "") + "\n " + message);
        editor.commit();
        sendMessage("B2:" + message);
        showLog("Exiting sendMessage");
    }

    public static void sendMessage(String message) {
        showLog("Entering sendMessage");
        sharedPreferences();

        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog(message);
        editor.putString("sentText", sharedPreferences.getString("sentText", "") + "\n " + message);
        editor.commit();
        showLog("Exiting sendMessage");
    }

    // for activating sharedPreferences
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    public static void sharedPreferences() {
        // set TAG and Mode for shared preferences
        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    // show toast message
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // show log message
    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    // for bluetooth
    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if (status.equals("connected")) {
                //When the device reconnects, this broadcast will be called again to enter CONNECTED if statement
                //must dismiss the previous dialog that is waiting for connection if not it will block the execution
                try {
                    myDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "mainReceiver: Device now connected to " + mDevice.getName());
                Toast.makeText(MainActivity.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", mDevice.getName());
                connStatusTextView.setText(mDevice.getName());
            } else if (status.equals("disconnected")) {
                Log.d(TAG, "mainReceiver: Disconnected from " + mDevice.getName());
                Toast.makeText(MainActivity.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();
                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                mBluetoothConnection.connectionLost(mBTDevice);

                // For displaying disconnected for all page
                editor.putString("connStatus", "None");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("None");

                myDialog.show();
            }
            editor.commit();
        }
    };

    // Receiving message from RPI through bluetooth
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String payload = intent.getStringExtra("receivedMessage");
            showLog("messageReceiver: " + payload);

            try {

                // Receiving robot status message
           if (payload.length() > 8 && payload.substring(0, 3).equals("B5:")){
           payload=payload.substring(3);

               // Receiving map information message
           } else if (payload.length() == 158 && payload.substring(0, 3).equals("B4:")) {

                    String robotPositionX = payload.substring(3,5);
                    String robotPositionY = payload.substring(5,7);
                    String robotDirection = payload.substring(7,8);
                     String mapInfo = payload.substring(8);  //mapInfo = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
                    showLog("mapInfo: " + mapInfo);

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

                } else if (payload.length() == 8  && payload.substring(0, 3).equals("D3:")){

               String imageX = payload.substring(3,5);
               String imageY = payload.substring(5,7);
               String imageType = payload.substring(7);

               JSONObject payloadObject = new JSONObject();
               payloadObject.put("imageX", imageX);
               payloadObject.put("imageY", imageY);
               payloadObject.put("imageType", imageType);


               JSONArray payloadArray = new JSONArray();
               payloadArray.put(payloadObject);
               JSONObject payloadBody = new JSONObject();
               payloadBody.put("image", payloadArray);

               payload = String.valueOf(payloadBody);

           }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (gridMap.getAutoUpdate()) {
                try {
                    gridMap.setReceivedJsonObject(new JSONObject(payload));
                    gridMap.updateMapInformation();
                } catch (JSONException e) {
                    showLog("Map information auto update unsuccessful");
                }
            }  else {

                try {
                    gridMap.setReceivedJsonObject(new JSONObject(payload));

                } catch (JSONException e) {
                    showLog("Map information manual update error: " + e);
                }
            }

            sharedPreferences();
            String receivedText = sharedPreferences.getString("receivedText", "") + "\n " + payload;
            editor.putString("receivedText", receivedText);
            editor.commit();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    mBTDevice = data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    // Accelerometer tilt function
    Handler sensorHandler = new Handler();
    boolean sensorFlag = false;

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            //sets flag to true to execute the codes in onSensorChanged.
            sensorFlag = true;
            //calls sensorDelay again to execute 1 seconds later
            sensorHandler.postDelayed(this, 1000); //1 seconds
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
        showLog("SensorChanged X: " + x);
        showLog("SensorChanged Y: " + y);
        showLog("SensorChanged Z: " + z);

        if (sensorFlag) {
            //x,y,z values are based on how easy it is to move the wrist for e.g. tilting device forward is easier so y<-2
            if (y < -2) {
                //move forward
                showLog("Sensor Move Forward Detected");
                gridMap.moveRobot("forward");
                refreshLabel();
                sendMessage("forward");
            } else if (y > 2) {
                //move backward
                showLog("Sensor Move Backward Detected");
                gridMap.moveRobot("back");
                refreshLabel();
                sendMessage("reverse");

            } else if (x > 2) {
                //move left
                showLog("Sensor Move Left Detected");
                gridMap.moveRobot("left");
                refreshLabel();
                sendMessage("left");

            } else if (x < -2) {
                //move right
                showLog("Sensor Move Right Detected");
                gridMap.moveRobot("right");
                refreshLabel();
                sendMessage("right");
            }
        }
        //set flag back to false so that it wont execute the code above until 1-2 seconds later
        sensorFlag = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mainReceiver);
            //unregister sensor in case not turned off.
            mSensorManager.unregisterListener(this);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    //unregister bluetooth connection status broadcast when the activity switches
    @Override
    protected void onPause() {
        super.onPause();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mainReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    //register bluetooth connection status broadcast when the activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        try {
            //Broadcasts when bluetooth state changes (connected, disconnected etc) custom receiver
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mainReceiver, filter2);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    // for saving states when changing activity
    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }

}
