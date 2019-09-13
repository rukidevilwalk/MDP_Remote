package com.jack.mdpremote.Bluetooth;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jack.mdpremote.R;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothSettings extends AppCompatActivity {
    private static final String TAG = "BluetoothSettings";
    private String connStatus;
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mNewBTDevices;
    public ArrayList<BluetoothDevice> mPairedBTDevices;
    public DeviceListAdapter mNewDevlceListAdapter;
    public DeviceListAdapter mPairedDevlceListAdapter;
    TextView connStatusTextView;
    ListView otherDevicesListView;
    ListView pairedDevicesListView;
    Button connectBtn;
    ProgressDialog myDialog;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    //These 3 for connection establishment
    BluetoothConnectionService mBluetoothConnection;
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice mBTDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Switch bluetoothSwitch = findViewById(R.id.bluetoothSwitch);
        //If bluetooth is already on, Set the toggle to true when pop up opens
        if (mBluetoothAdapter.isEnabled()) {
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setText("ON");
        }

        otherDevicesListView = findViewById(R.id.otherDevicesListView);
        pairedDevicesListView = findViewById(R.id.pairedDevicesListView);
        mNewBTDevices = new ArrayList<>();
        mPairedBTDevices = new ArrayList<>();

        connectBtn = findViewById(R.id.connectBtn);

        //Broadcasts when bond state changes (Pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bondReceiver, filter);

        //Broadcasts when bluetooth state changes (connected, disconnected etc) custom receiver
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(mainReceiver, filter2);

        //New Devices List View event handler
        otherDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //cancel discovery since it is memory intensive
                mBluetoothAdapter.cancelDiscovery();
                //this is to remove the highlight bar(if any) when a device is selected at the paired devices list view
                pairedDevicesListView.setAdapter(mPairedDevlceListAdapter);

                String deviceName = mNewBTDevices.get(i).getName();
                String deviceAddress = mNewBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);


                //create the bond for first time pairing devices
                //NOTE: createBond() method requires API 17+
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Log.d(TAG, "onItemClick: Initiating pairing with " + deviceName);
                    mNewBTDevices.get(i).createBond();

                    //start connection service AFTER bonding (acceptthread will start first and the device will sit and wait for a connection,
                    //which is the connectthread that is initiated with the connect button)
                    mBluetoothConnection = new BluetoothConnectionService(BluetoothSettings.this);
                    mBTDevice = mNewBTDevices.get(i);
                }
            }
        });
        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //cancel discovery since it is memory intensive
                mBluetoothAdapter.cancelDiscovery();
                //this is to remove the highlight bar(if any) when a device is selected at the new devices list view
                otherDevicesListView.setAdapter(mNewDevlceListAdapter);

                String deviceName = mPairedBTDevices.get(i).getName();
                String deviceAddress = mPairedBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                //start connection service (acceptthread will start first and the device will sit and wait for a connection,
                // which is the connectthread that is initiated with the connect button)
                mBluetoothConnection = new BluetoothConnectionService(BluetoothSettings.this);
                mBTDevice = mPairedBTDevices.get(i);
            }
        });

        //On off Switch button event handler
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.d(TAG, "onChecked: Switch button toggled. Enabling/Disabling Bluetooth");
                //this is the most convenient way of changing the text for switch
                if (isChecked) {
                    compoundButton.setText("ON");
                } else {
                    compoundButton.setText("OFF");
                }

                if (mBluetoothAdapter == null) {
                    Log.d(TAG, "enableDisableBT: Device does not support Bluetooth capabilities!");
                    Toast.makeText(BluetoothSettings.this, "Device Does Not Support Bluetooth capabilities!", Toast.LENGTH_LONG).show();
                    compoundButton.setChecked(false);
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: enabling Bluetooth");
                        Log.d(TAG, "enableDisableBT: Making device discoverable for 600 seconds.");
                        //Enable Bluetooth - removed because ACTION_REQUEST_DISCOVERABLE can turn on bluetooth so this is redundant
                        /*Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(enableBTIntent);*/
                        //Enable discoverability
                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
                        startActivity(discoverableIntent);

                        compoundButton.setChecked(true);//need this for cases where the user tapped outside of the pop up box during allow/deny prompt

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(stateReceiver, BTIntent); //intercepts changes to bluetooth status and logs them

                        IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                        registerReceiver(discoverabilityReceiver, discoverIntent); //intercepts changes to discoverability status and logs them
                    }
                    if (mBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: disabling Bluetooth");
                        mBluetoothAdapter.disable();

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(stateReceiver, BTIntent); //intercepts changes to bluetooth status and logs them
                    }

                }
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBTDevice == null) {
                    Toast.makeText(BluetoothSettings.this, "Please Select a Device before connecting.", Toast.LENGTH_LONG).show();
                } else {
                    startConnection();
                }
            }
        });


        // for toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        connStatusTextView = findViewById(R.id.connStatusTextView);
        connStatus = "None";
        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("connStatus"))
            connStatus = sharedPreferences.getString("connStatus", "");

        connStatusTextView.setText(connStatus);

        //Progress dialog to show when the bluetooth is disconnected
        myDialog = new ProgressDialog(BluetoothSettings.this);
        myDialog.setMessage("Trying to reconnect..");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    public void toggleButtonScan(View view) {
        Log.d(TAG, "toggleButton: Scanning for unpaired devices.");
        mNewBTDevices.clear(); //clear list of bt devices whenever scan button is pressed so that list view doesnt display previously found devices
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(BluetoothSettings.this, "Please turn on Bluetooth first!", Toast.LENGTH_SHORT).show();
            }
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "toggleButton: Cancelling Discovery.");

                //Check bluetooth permissions in manifest
                checkBTPermissions();

                mBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND); //ACTION_FOUND: remote device discovered
                registerReceiver(unpairedReceiver, discoverDevicesIntent);
            } else if (!mBluetoothAdapter.isDiscovering()) {
                //Check bluetooth permissions in manifest
                checkBTPermissions();

                mBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND); //ACTION_FOUND: remote device discovered
                registerReceiver(unpairedReceiver, discoverDevicesIntent);
            }
            //get a list of bonded devices. This does not require any discovery
            mPairedBTDevices.clear();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            Log.d(TAG, "toggleButton: Number of paired devices found: " + pairedDevices.size());
            for (BluetoothDevice d : pairedDevices) {
                Log.d(TAG, "Paired Devices: " + d.getName() + " : " + d.getAddress());
                mPairedBTDevices.add(d);
                mPairedDevlceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, mPairedBTDevices);
                pairedDevicesListView.setAdapter(mPairedDevlceListAdapter); //set adapter to the list
            }

        }
    }

    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");

        }
    }

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "stateReceiver: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "stateReceiver: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "stateReceiver: STATE ON");

                        break;
                    //BLUETOOTH TURNING ON STATE
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "stateReceiver: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for Bluetooth Discoverability mode on/off or expiry
     */
    private final BroadcastReceiver discoverabilityReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "discoverabilityReceiver: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "discoverabilityReceiver: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "discoverabilityReceiver: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "discoverabilityReceiver: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "discoverabilityReceiver: Connected.");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast receiver for listing devices that are not yet paired
     * Executed by toggleButtonScan
     */
    private BroadcastReceiver unpairedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {//ACTION_FOUND: remote device discovered
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); //get device object(parcelable)
                mNewBTDevices.add(device); //add remote device found to the arraylist
                Log.d(TAG, "onReceive: " + device.getName() + " : " + device.getAddress());
                mNewDevlceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mNewBTDevices);
                otherDevicesListView.setAdapter(mNewDevlceListAdapter); //set adapter to the list

            }
        }
    };

    /**
     * Broadcast receiver for bluetooth bonding changes
     * Only logging here to track the events, not other action needed
     */
    private BroadcastReceiver bondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BOND_BONDED.");
                    Toast.makeText(BluetoothSettings.this, "Successfully paired with " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    mBTDevice = mDevice; //assign paired device to global variable
                }

                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BOND_BONDING.");
                }

                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BOND_NONE.");
                }
            }
        }
    };

    /**
     * Broadcast receiver for bluetooth connection status
     * NOTE: failed to connect to device will display a toast message which is found in BluetoothConnectionService ConnectThread
     */
    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if (status.equals("connected")) {
                //When the device reconnects, this broadcast will be called again to enter CONNECTED if statement
                //must dismiss the previous dialog that is waiting for connection if not it will block the execution
                try {
                    myDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "mainReceiver: Device now connected to " + mDevice.getName());
                Toast.makeText(BluetoothSettings.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", mDevice.getName());
                connStatusTextView.setText(mDevice.getName());
            } else if (status.equals("disconnected")) {
                Log.d(TAG, "mainReceiver: Disconnected from " + mDevice.getName());
                Toast.makeText(BluetoothSettings.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();
                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(BluetoothSettings.this);
                mBluetoothConnection.connectionLost(mBTDevice);

                // For displaying disconnected for all page
                sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "None");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("None");
                editor.commit();

                //show disconnected dialog
                try {
                    myDialog.show();
                } catch (Exception e) {
                    Log.d(TAG, "BluetoothSettings: mainReceiver Dialog show failure");
                }


            }
            editor.commit();
        }
    };

    /**
     * method for starting connection(connectthread) after device is in acceptthread
     * NOTE: the connection will fail and app will crash if the devices did not pair first
     */
    public void startConnection() {
        startBTConnection(mBTDevice, myUUID);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection");

        mBluetoothConnection.startClientThread(device, uuid);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        super.onDestroy();
        //close broadcast receivers when activity is finishing/destroyed
        try {
//            unregisterReceiver(stateReceiver); //try catch for cases where bluetooth adapter =null on devices that dont support bluetooth
//            unregisterReceiver(discoverabilityReceiver);
//            unregisterReceiver(unpairedReceiver);
//            unregisterReceiver(bondReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(discoverabilityReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(unpairedReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bondReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mainReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("mBTDevice", mBTDevice);
        data.putExtra("myUUID", myUUID);
        setResult(RESULT_OK, data);
        super.finish();
    }
}
