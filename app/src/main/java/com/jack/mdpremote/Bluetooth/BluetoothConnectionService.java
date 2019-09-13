package com.jack.mdpremote.Bluetooth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;


public class BluetoothConnectionService {

    private static final String TAG = "DebuggingTag";

    private static final String appName = "MDP Remote";

    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;
    private ReconnectThread mReconnectThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    Intent connectionStatus;
    private boolean reconnecting = false;
    public static boolean BluetoothConnectionStatus = false; //for other activities to check the status before invoking write()
    private static ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = context;
        startAcceptThread();
    }


    /**
     * This thread runs while listening for incoming connections.
     * It behaves like a server-side client. It runs until a connection is accepted(or cancelled)
     */
    private class AcceptThread extends Thread {
        //local server socket
        private final BluetoothServerSocket ServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            //Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);
                Log.d(TAG, "Accept Thread: Setting up Server using: " + myUUID);
            } catch (IOException e) {
                Log.e(TAG, "Accept Thread: IOException: " + e.getMessage());
            }
            ServerSocket = tmp;
        }

        //run methods automatically executes in any thread
        public void run() {
            Log.d(TAG, "run: AcceptThread Running. ");
            BluetoothSocket socket = null;
            try {
                //This is a blocking call to wait for a connection.
                //Accept thread will hang here until successful connection or exception
                Log.d(TAG, "run: RFCOM server socket start here...");

                socket = ServerSocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");
            } catch (IOException e) {
                Log.e(TAG, "run: IOException: " + e.getMessage());
            }
            //This is for cases where the other device initiates the request. If the other party initiates, we can skip the ConnectThread
            //and go straight to connectedthread
            if (socket != null) {
                connected(socket, socket.getRemoteDevice());
            }
            Log.i(TAG, "END AcceptThread");
        }

        //close server socket
        public void cancel() {
            Log.d(TAG, "cancel: Cancelling AcceptThread");
            try {
                ServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Failed to close AcceptThread ServerSocket " + e.getMessage());
            }
        }
    }

    public void connectionLost(BluetoothDevice device) {

        reconnecting = true;
        if (mReconnectThread == null && mBluetoothAdapter.isEnabled()) {
            mReconnectThread = new ReconnectThread(device);
            mReconnectThread.start();
            Log.e(TAG, "connectionLost, starting reconnect thread.");
        } else if (!mBluetoothAdapter.isEnabled()){
            if (mReconnectThread != null) {
                mReconnectThread.cancel();
            }
            mReconnectThread = null;
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
                    Log.e(TAG, "reconnect thread, attempting to connect to previous device.");
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

    public synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
      //  if (mState == ConnectionConstants.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
       // }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        Log.e(TAG, "Starting new connect thread with previous device.");
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device,myUUID);
        mConnectThread.start();

    }


    /**
     * While both devices are waiting in the accept thread(run() method's ServerSocket.accept()), the ConnectThread
     * will start by grabbing that socket and connecting to it
     * The connection either succeeds or fails
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID u) {
            Log.d(TAG, "ConnectThread: started.");
            mDevice = device;
            deviceUUID = u;
            BluetoothSocket tmp = null;

            //Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using Default UUID: " + deviceUUID);
                tmp = device.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }
            mSocket = tmp;
        }

        public void run() {

            Log.d(TAG, "RUN: mConnectThread");
            //Always cancel Discovery because it will slow down a connection
            //mBluetoothAdapter.cancelDiscovery();

            try {
                //Make a connection to the BluetoothSocket
                //This is a blocking call and will only return upon successful connection or exception
                mSocket.connect();
            } catch (IOException e) {
                //close the socket if there is an exception
                try {
                    mSocket.close();
                    Log.d(TAG, "RUN: ConnectThread socket closed.");
                } catch (IOException e1) {

                    Log.e(TAG, "RUN: ConnectThread: Unable to close connection in socket." + e1.getMessage());
                }
                Log.d(TAG, "RUN: ConnectThread: could not connect to UUID." + myUUID);



                connectionLost(mDevice);
                return;
            }

            Log.d(TAG, "RUN: ConnectThread connected.");
            connected(mSocket, mDevice);


        }

        //close client socket
        public void cancel() {
            Log.d(TAG, "cancel: Closing Client Socket");
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }

    /**
     * Start the chat service. Specifically starts AcceptThread to begin a session
     * in listening (server) mode.
     */
    public synchronized void startAcceptThread() {
        Log.d(TAG, "start");

        //if a connectthread exists, cancel and create a new one.
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        //if accept thread doesn't exist, start a new one.
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start(); //this start is a method found in the Thread class.
            // Thread class run() is executed on the calling thread whereas start() creates a new thread
            // so that the runnable's run() method is executed in parallel
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * Then ConnectThread starts and attempts to make a connection with the other devices(Sitting in AcceptThread)
     */
    public void startClientThread(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        //process dialog box pops up when the connection is establishing
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    /**
     * Manages the connection
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            //custom, broadcast connected status so that other activities' broadcast receivers will be notified
            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", mDevice);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
            BluetoothConnectionStatus = true;

            this.mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024]; //byte array object that holds the input from the input stream
            int bytes; //will use this to read from the input stream

            //dismiss the process dialog box when connection is established
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            //keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // read from input stream
                    bytes = inStream.read(buffer); //blocking call
                    String incomingmessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingmessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage", incomingmessage);

                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input stream. " + e.getMessage());

                    //custom, broadcast disconnected status so that other activities' broadcast receivers will be notified
                    connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", mDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
                    BluetoothConnectionStatus = false;

                    break; //break the while loop if there is a problem with the input stream
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: " + text);
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. " + e.getMessage());
            }
        }

        void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to close socket. " + e.getMessage());
            }
        }

    }

    //this method is called from ConnectThread after establishing a connection.
    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {
        Log.d(TAG, "connected: Starting.");
        //If the android app initiates the connection, the device information will be retrieved when we press on the connect button
        //If the other device initiates, we will need to get device information from the socket and assign to our global variable
        mDevice = device;
        //stop the accept thread from listening
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mReconnectThread != null) {
            mReconnectThread = null;
        }

        //Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    /**
     * write to the ConnectedThread in an unsynchronized manner
     */
    public static void write(byte[] out) {
        //create temporary object
        ConnectedThread tmp;

        //synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write is called.");
        //perform write operation
        if (!(mConnectedThread == null)) {
            mConnectedThread.write(out);
        } else {
            Log.d(TAG, "Bluetooth is not connected");
        }
    }

    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mReconnectThread != null) {
            mReconnectThread = null;
        }

  Log.e(TAG,"Stop all threads");
    }
}