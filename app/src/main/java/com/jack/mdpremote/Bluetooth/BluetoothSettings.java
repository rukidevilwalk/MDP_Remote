package com.jack.mdpremote.Bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

    private String connStatus;

    BluetoothAdapter btAdapter;

    TextView connStatusTextView;

    ListView otherDevicesListView;

    ListView pairedDevicesListView;

    Button connectBtn;

    ProgressDialog progressDialog;

    public ArrayList<BluetoothDevice> newBTDevices;

    public ArrayList<BluetoothDevice> pairedBTDevices;

    public DeviceListAdapter newDeviceListAdapter;

    public DeviceListAdapter pairedDeviceListAdapter;

    SharedPreferences sharedPreferences;

    SharedPreferences.Editor editor;


    BluetoothConnectionService BTConnection;

    private static final UUID deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice mBTDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bluetooth_settings);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Switch btSwitch = findViewById(R.id.bluetoothSwitch);

        if (btAdapter.isEnabled()) {
            btSwitch.setChecked(true);
            btSwitch.setText("ON");
        }

        otherDevicesListView = findViewById(R.id.otherDevicesListView);

        pairedDevicesListView = findViewById(R.id.pairedDevicesListView);

        newBTDevices = new ArrayList<>();

        pairedBTDevices = new ArrayList<>();

        connectBtn = findViewById(R.id.connectBtn);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(bondReceiver, filter);

        IntentFilter filter2 = new IntentFilter("ConnectionStatus");

        LocalBroadcastManager.getInstance(this).registerReceiver(mainReceiver, filter2);

        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                btAdapter.cancelDiscovery();

                otherDevicesListView.setAdapter(newDeviceListAdapter);

                BTConnection = new BluetoothConnectionService(BluetoothSettings.this);

                mBTDevice = pairedBTDevices.get(i);
            }
        });

        otherDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                btAdapter.cancelDiscovery();

                pairedDevicesListView.setAdapter(pairedDeviceListAdapter);

            }
        });


        btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) {
                    compoundButton.setText("ON");
                } else {
                    compoundButton.setText("OFF");
                }

                if (btAdapter == null) {
                    compoundButton.setChecked(false);
                } else {
                    if (!btAdapter.isEnabled()) {

                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 400);

                        startActivity(discoverableIntent);

                        compoundButton.setChecked(true);

                    }
                    if (btAdapter.isEnabled()) {

                        btAdapter.disable();

                    }

                }
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBTDevice != null) {
                    startConnection();
                }
            }
        });


        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        connStatusTextView = findViewById(R.id.connStatusTextView);

        connStatus = "None";

        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("connStatus"))

            connStatus = sharedPreferences.getString("connStatus", "");

        connStatusTextView.setText(connStatus);


        progressDialog = new ProgressDialog(BluetoothSettings.this);

        progressDialog.setMessage("Trying to reconnect..");

        progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

            }
        });
    }

    public void toggleButtonScan(View view) {

        newBTDevices.clear();

        if (btAdapter != null) {

            if (!btAdapter.isEnabled()) {

                Toast.makeText(BluetoothSettings.this, "Turn on Bluetooth!", Toast.LENGTH_SHORT).show();

            }

            if (btAdapter.isDiscovering()) {

                btAdapter.cancelDiscovery();

                btAdapter.startDiscovery();

                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);

                registerReceiver(unpairedReceiver, discoverDevicesIntent);

            } else if (!btAdapter.isDiscovering()) {

                btAdapter.startDiscovery();

                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);

                registerReceiver(unpairedReceiver, discoverDevicesIntent);
            }

            pairedBTDevices.clear();

            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            for (BluetoothDevice d : pairedDevices) {

                pairedBTDevices.add(d);

                pairedDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, pairedBTDevices);

                pairedDevicesListView.setAdapter(pairedDeviceListAdapter);
            }

        }
    }


    private BroadcastReceiver unpairedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                newBTDevices.add(device);

                newDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, newBTDevices);

                otherDevicesListView.setAdapter(newDeviceListAdapter);

            }
        }
    };


    private BroadcastReceiver bondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {


                    Toast.makeText(BluetoothSettings.this, "Paired with " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    mBTDevice = mDevice;
                }

            }
        }
    };


    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BluetoothDevice mDevice = intent.getParcelableExtra("Device");

            String status = intent.getStringExtra("Status");

            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

            editor = sharedPreferences.edit();

            if (status.equals("connected")) {

                try {

                    progressDialog.dismiss();

                } catch (NullPointerException e) {

                    e.printStackTrace();

                }

                Toast.makeText(BluetoothSettings.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();

                editor.putString("connStatus", mDevice.getName());

                connStatusTextView.setText(mDevice.getName());
            } else if (status.equals("disconnected")) {

                Toast.makeText(BluetoothSettings.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();

                BTConnection = new BluetoothConnectionService(BluetoothSettings.this);

                BTConnection.connectionLost(mBTDevice);

                sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

                editor = sharedPreferences.edit();

                editor.putString("connStatus", "None");

                TextView connStatusTextView = findViewById(R.id.connStatusTextView);

                connStatusTextView.setText("None");

                editor.apply();


                try {
                    progressDialog.show();
                } catch (Exception e) {

                }


            }
            editor.commit();
        }
    };


    public void startConnection() {
        startBTConnection(mBTDevice, deviceUUID);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {

        BTConnection.startClientThread(device, uuid);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            LocalBroadcastManager.getInstance(this).unregisterReceiver(mainReceiver);

            LocalBroadcastManager.getInstance(this).unregisterReceiver(unpairedReceiver);

            LocalBroadcastManager.getInstance(this).unregisterReceiver(bondReceiver);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();

        }
    }

    @Override
    public void finish() {

        Intent data = new Intent();

        data.putExtra("mBTDevice", mBTDevice);

        data.putExtra("deviceUUID", deviceUUID);

        setResult(RESULT_OK, data);

        super.finish();
    }
}
