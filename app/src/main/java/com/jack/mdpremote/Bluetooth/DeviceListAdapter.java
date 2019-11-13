package com.jack.mdpremote.Bluetooth;

import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;


import com.jack.mdpremote.R;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    private LayoutInflater mLI;
    private ArrayList<BluetoothDevice> devices;
    private int vRI;

    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices) {
        super(context, tvResourceId, devices);
        this.devices = devices;
        mLI = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vRI = tvResourceId;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = mLI.inflate(vRI, null);

        BluetoothDevice device = devices.get(position);

        if (device != null) {
            TextView name = convertView.findViewById(R.id.deviceName);
            TextView address = convertView.findViewById(R.id.deviceAddress);

            if (name != null) {
                name.setText(device.getName());
            }
            if (address != null) {
                address.setText(device.getAddress());
            }
        }

        return convertView;
    }
}