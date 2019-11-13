package com.jack.mdpremote.SendReceive;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jack.mdpremote.Bluetooth.BluetoothConnectionService;
import com.jack.mdpremote.R;

import java.nio.charset.Charset;

public class SendReceive extends AppCompatActivity {

    Intent intent;

    private String receivedText = "";

    private String sentText = "";

    private String connStatus;

    SharedPreferences sharedPreferences;

    SharedPreferences.Editor editor;

    ProgressDialog progressDialog;

    BluetoothConnectionService BTConnection;

    TextView receivedTextView;

    TextView SentTextView;

    EditText editTextBox;

    Button sendTextBtn;

    Button clearTextBtn;

    Button f1Btn;

    Button f2Btn;

    Button reconfigureBtn;

    TextView connStatusTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        connStatus = "None";

        setContentView(R.layout.activity_send_receive);

        intent = getIntent();

        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("receivedText"))

            receivedText = sharedPreferences.getString("receivedText", "");

        if (sharedPreferences.contains("sentText"))

            sentText = sharedPreferences.getString("sentText", "");

        receivedTextView = findViewById(R.id.messageBoxReceivedTextView);

        SentTextView = findViewById(R.id.messageBoxSentTextView);

        editTextBox = findViewById(R.id.typeBoxEditText);

        sendTextBtn = findViewById(R.id.sendTextBtn);

        clearTextBtn = findViewById(R.id.clearTextBtn);

        f1Btn = findViewById(R.id.f1Btn);

        f2Btn = findViewById(R.id.f2Btn);

        reconfigureBtn = findViewById(R.id.reconfigureBtn);

        receivedTextView.setMovementMethod(new ScrollingMovementMethod());

        SentTextView.setMovementMethod(new ScrollingMovementMethod());

        receivedTextView.setText(receivedText);

        SentTextView.setText(sentText);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        IntentFilter filter2 = new IntentFilter("ConnectionStatus");

        LocalBroadcastManager.getInstance(this).registerReceiver(BCReceiver, filter2);

        if (sharedPreferences.contains("F1")) {

            f1Btn.setContentDescription(sharedPreferences.getString("F1", ""));

        }

        if (sharedPreferences.contains("F2")) {

            f2Btn.setContentDescription(sharedPreferences.getString("F2", ""));

        }

        final FragmentManager fragmentManager = getFragmentManager();

        final ReconfigureFragment reconfigureFragment = new ReconfigureFragment();

        reconfigureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                reconfigureFragment.show(fragmentManager, "Reconfigure Fragment");

            }
        });

        f1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!f1Btn.getContentDescription().toString().equals("empty"))
                    sentText = (f1Btn.getContentDescription().toString());

                byte[] bytes = sentText.getBytes(Charset.defaultCharset());

                BluetoothConnectionService.write(bytes);

            }
        });

        f2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!f2Btn.getContentDescription().toString().equals("empty"))
                    sentText = (f2Btn.getContentDescription().toString());

                byte[] bytes = sentText.getBytes(Charset.defaultCharset());

                BluetoothConnectionService.write(bytes);

            }
        });

        sendTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sentText = " " + editTextBox.getText().toString();

                sharedPreferences = getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putString("sentText", sentText);

                editor.apply();

                SentTextView.setText(sharedPreferences.getString("sentText", ""));

                editTextBox.setText(" ");

                if (BluetoothConnectionService.BluetoothConnectionStatus) {

                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());

                    BluetoothConnectionService.write(bytes);
                }

            }
        });

        clearTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SentTextView.setText("");

            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        connStatusTextView = findViewById(R.id.connStatusTextView);

        if (sharedPreferences.contains("connStatus"))

            connStatus = sharedPreferences.getString("connStatus", "");

        connStatusTextView.setText(connStatus);

        progressDialog = new ProgressDialog(SendReceive.this);

        progressDialog.setMessage("Waiting for other device to reconnect...");

        progressDialog.setCancelable(false);

        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

            }
        });

    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            sharedPreferences = getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

            receivedTextView.setText(sharedPreferences.getString("receivedText", ""));

        }
    };

    public static void closeKeyboard(Activity activity) {

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);

        View view = activity.getCurrentFocus();

        if (view == null) {

            view = new View(activity);

        }

        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private BroadcastReceiver BCReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BluetoothDevice mDevice = intent.getParcelableExtra("Device");

            String status = intent.getStringExtra("Status");

            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

            editor = sharedPreferences.edit();

            connStatusTextView = findViewById(R.id.connStatusTextView);

            if (status.equals("connected")) {

                try {

                    progressDialog.dismiss();

                } catch (NullPointerException e) {

                    e.printStackTrace();

                }

                Toast.makeText(SendReceive.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();

                editor.putString("connStatus", "Connected to " + mDevice.getName());

                connStatusTextView.setText(mDevice.getName());
            } else if (status.equals("disconnected")) {

                Toast.makeText(SendReceive.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();

                BTConnection = new BluetoothConnectionService(SendReceive.this);

                BTConnection.startAcceptThread();

                editor.putString("connStatus", "None");

                connStatusTextView.setText("None");


                closeKeyboard(SendReceive.this);

                progressDialog.show();
            }
            editor.commit();
        }
    };

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

            LocalBroadcastManager.getInstance(this).unregisterReceiver(BCReceiver);

        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        }
    }


}
