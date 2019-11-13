package com.jack.mdpremote.Bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;


public class BluetoothConnectionService {


    private static final String appName = "MDP Remote";

    private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;

    private AcceptThread AcceptThread;

    private ReconnectThread ReconnectThread;

    private ConnectThread ConnectThread;

    private static ConnectedThread ConnectedThread;

    private Context context;

    private BluetoothDevice mDevice;

    private UUID deviceUUID;

    private boolean reconnecting = false;

    public static boolean BluetoothConnectionStatus = false;

    private ProgressDialog progressDialog;

    private Intent connectionStatus;

    public BluetoothConnectionService(Context context) {

        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.context = context;

        startAcceptThread();

    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket ServerSocket;

        private AcceptThread() {

            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, mUUID);

            } catch (IOException e) {

            }
            ServerSocket = tmp;
        }

        public void run() {

            BluetoothSocket socket = null;

            try {

                socket = ServerSocket.accept();

            } catch (IOException e) {

            }


            if (socket != null) {
                connected(socket, socket.getRemoteDevice());
            }

        }

        private void cancel() {

            try {
                ServerSocket.close();
            } catch (IOException e) {

            }
        }
    }

    public void connectionLost(BluetoothDevice device) {

        reconnecting = true;

        if (ReconnectThread == null && mBluetoothAdapter.isEnabled()) {

            ReconnectThread = new ReconnectThread(device);

            ReconnectThread.start();


        } else if (!mBluetoothAdapter.isEnabled()) {

            if (ReconnectThread != null) {

                ReconnectThread.cancel();

            }

            ReconnectThread = null;
        }

    }

    private class ReconnectThread extends Thread {

        private boolean flag = true;

        ReconnectThread(BluetoothDevice device) {
            mDevice = device;
        }

        public void run() {

            while (mBluetoothAdapter.isEnabled() && flag) {

                if (reconnecting) {

                    connect(mDevice);
                }

                reconnecting = false;

                try {

                    sleep(3000);

                } catch (InterruptedException e) {

                    e.printStackTrace();

                }
            }
        }

        void cancel() {
            this.flag = false;
        }
    }

    private synchronized void connect(BluetoothDevice device) {

        if (ConnectThread != null) {

            ConnectThread.cancel();

            ConnectThread = null;

        }

        if (ConnectedThread != null) {

            ConnectedThread.cancel();

            ConnectedThread = null;

        }

        if (AcceptThread != null) {

            AcceptThread.cancel();

            AcceptThread = null;

        }

        ConnectThread = new ConnectThread(device, mUUID);

        ConnectThread.start();
    }


    private class ConnectThread extends Thread {

        private BluetoothSocket mSocket;

        private ConnectThread(BluetoothDevice device, UUID u) {

            mDevice = device;

            deviceUUID = u;

            BluetoothSocket tmp = null;

            try {

                tmp = device.createRfcommSocketToServiceRecord(deviceUUID);

            } catch (IOException e) {

            }

            mSocket = tmp;
        }

        public void run() {

            mBluetoothAdapter.cancelDiscovery();

            try {

                mSocket.connect();

            } catch (IOException e) {

                try {

                    mSocket.close();

                } catch (IOException e1) {

                }

                connectionLost(mDevice);

                return;
            }

            connected(mSocket, mDevice);


        }


        private void cancel() {

            try {

                mSocket.close();

            } catch (IOException e) {

            }
        }
    }


    public synchronized void startAcceptThread() {

        if (ConnectThread != null) {

            ConnectThread.cancel();

            ConnectThread = null;
        }

        if (AcceptThread == null) {

            AcceptThread = new AcceptThread();

            AcceptThread.start();

        }
    }


    public void startClientThread(BluetoothDevice device, UUID uuid) {

        progressDialog = ProgressDialog.show(context, "Connecting Bluetooth", "Please Wait...", true);

        ConnectThread = new ConnectThread(device, uuid);

        ConnectThread.start();
    }


    private class ConnectedThread extends Thread {

        private final BluetoothSocket socket;

        private final InputStream inputStream;

        private final OutputStream outputStream;

        private ConnectedThread(BluetoothSocket socket) {

            connectionStatus = new Intent("ConnectionStatus");

            connectionStatus.putExtra("Status", "connected");

            connectionStatus.putExtra("Device", mDevice);

            LocalBroadcastManager.getInstance(context).sendBroadcast(connectionStatus);

            BluetoothConnectionStatus = true;

            this.socket = socket;

            InputStream tmpIn = null;

            OutputStream tmpOut = null;

            try {

                tmpIn = this.socket.getInputStream();

                tmpOut = this.socket.getOutputStream();

            } catch (IOException e) {

                e.printStackTrace();

            }

            inputStream = tmpIn;

            outputStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];

            int bytes;

            try {

                progressDialog.dismiss();

            } catch (NullPointerException e) {

                e.printStackTrace();

            }

            while (true) {

                try {

                    bytes = inputStream.read(buffer);
                    String inMsg = new String(buffer, 0, bytes);

                    Intent incomingMessageIntent = new Intent("incomingMessage");

                    incomingMessageIntent.putExtra("receivedMessage", inMsg);

                    LocalBroadcastManager.getInstance(context).sendBroadcast(incomingMessageIntent);

                } catch (IOException e) {

                    connectionStatus = new Intent("ConnectionStatus");

                    connectionStatus.putExtra("Status", "disconnected");

                    connectionStatus.putExtra("Device", mDevice);

                    LocalBroadcastManager.getInstance(context).sendBroadcast(connectionStatus);

                    BluetoothConnectionStatus = false;

                    break;
                }
            }
        }

        private void write(byte[] bytes) {

            try {

                outputStream.write(bytes);

            } catch (IOException e) {

            }

        }

        void cancel() {

            try {

                socket.close();

            } catch (IOException e) {

            }
        }

    }


    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {

        mDevice = device;

        if (AcceptThread != null) {

            AcceptThread.cancel();

            AcceptThread = null;

        }

        if (ConnectedThread != null) {

            ConnectedThread.cancel();

            ConnectedThread = null;

        }

        if (ReconnectThread != null) {

            ReconnectThread = null;

        }

        ConnectedThread = new ConnectedThread(mSocket);

        ConnectedThread.start();
    }


    public static void write(byte[] out) {

        if (!(ConnectedThread == null)) {

            ConnectedThread.write(out);

        }
    }

}