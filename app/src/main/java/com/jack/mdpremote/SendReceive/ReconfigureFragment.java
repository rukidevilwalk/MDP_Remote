package com.jack.mdpremote.SendReceive;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.jack.mdpremote.R;

public class ReconfigureFragment extends DialogFragment {

    EditText f1Edit;

    EditText f2Edit;

    String f1;

    String f2;

    View rootView;

    SharedPreferences sharedPreferences;

    SharedPreferences.Editor editor;

    Button saveBtn;

    Button cancelReconfigBtn;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.activity_reconfigure, container, false);

        super.onCreate(savedInstanceState);


        f1Edit = rootView.findViewById(R.id.f1ValueEditText);

        f2Edit = rootView.findViewById(R.id.f2ValueEditText);

        saveBtn = rootView.findViewById(R.id.saveBtn);

        getDialog().setTitle("Reconfiguration");


        cancelReconfigBtn = rootView.findViewById(R.id.cancelReconfigureBtn);

        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);


        if (sharedPreferences.contains("F2")) {

            f2Edit.setText(sharedPreferences.getString("F2", ""));

            f2 = sharedPreferences.getString("F2", "");
        }

        if (sharedPreferences.contains("F1")) {

            f1Edit.setText(sharedPreferences.getString("F1", ""));

            f1 = sharedPreferences.getString("F1", "");

            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        }

        if (savedInstanceState != null) {

            f1 = savedInstanceState.getStringArray("F1F2 value")[0];

            f2 = savedInstanceState.getStringArray("F1F2 value")[1];
        }

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                editor = sharedPreferences.edit();

                editor.putString("F1", f1Edit.getText().toString());

                editor.putString("F2", f2Edit.getText().toString());

                editor.apply();

                if (!sharedPreferences.getString("F1", "").equals(""))
                    f1 = f1Edit.getText().toString();

                if (!sharedPreferences.getString("F2", "").equals(""))
                    f2 = f2Edit.getText().toString();


                getDialog().dismiss();
            }
        });

        cancelReconfigBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (sharedPreferences.contains("F1"))
                    f1Edit.setText(sharedPreferences.getString("F1", ""));

                if (sharedPreferences.contains("F2"))
                    f2Edit.setText(sharedPreferences.getString("F2", ""));

                getDialog().dismiss();
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

    }

    @Override
    public void onDismiss(DialogInterface dialog) {

        super.onDismiss(dialog);

        if (f1 != null && !f1.equals(""))
            ((SendReceive) getActivity()).f1Btn.setContentDescription(f1);

        if (f2 != null && !f2.equals(""))
            ((SendReceive) getActivity()).f2Btn.setContentDescription(f2);

        f1Edit.clearFocus();

    }

}