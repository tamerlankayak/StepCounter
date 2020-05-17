package com.example.stepcounter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
import android.service.autofill.Dataset;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StepActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mButtonViewToday;
    private TextView tvStepCount;

    private FitnessOptions fitnessOptions;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step);

        initViews();

        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);


        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    account,
                    fitnessOptions);
            return;
        } else {
            new ViewTodaysStepCountTask().execute();
        }
    }

    private void displayStepDataForToday() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    account,
                    fitnessOptions);
            return;
        }

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                    @Override
                    public void onSuccess(DataSet dataSet) {
                        showDataSet(dataSet);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Xeta", e.getMessage());
            }
        });
    }

    private void showDataSet(DataSet dataSet) {

        tvStepCount = findViewById(R.id.tvStepCount);
        Log.e("History", "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();

        for (final DataPoint dp : dataSet.getDataPoints()) {

            Log.e("History", "Data point:");
            Log.e("History", "\tType: " + dp.getDataType().getName());
            Log.e("History", "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.e("History", "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            for (final Field field : dp.getDataType().getFields()) {
                Log.e("History", "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStepCount.setText(dp.getValue(field).toString());
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                new ViewTodaysStepCountTask().execute();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new ViewTodaysStepCountTask().execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_view_today: {
                new ViewTodaysStepCountTask().execute();
                break;
            }
        }
    }

    private class ViewTodaysStepCountTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            displayStepDataForToday();
            return null;
        }
    }


    /*
      public void showAlertDialogWithAutoDismiss() {
          AlertDialog.Builder builder = new AlertDialog.Builder(StepActivity.this,android.R.style.Animation_Translucent);
          builder.setTitle("Title")
                  .setMessage("message")
                  .setCancelable(false).setCancelable(false);
          final AlertDialog alertDialog = builder.create();
          alertDialog.show();
          new Handler().postDelayed(new Runnable() {
              @Override
              public void run() {
                  if (alertDialog.isShowing()){
                      alertDialog.dismiss();
                  }
              }
          }, 1000); //change 5000 with a specific time you want
      }

       */
    private void initViews() {
        mButtonViewToday = (Button) findViewById(R.id.btn_view_today);
        mButtonViewToday.setOnClickListener(this);
/*
        mButtonViewWeek = (Button) findViewById(R.id.btn_view_week);
        mButtonAddSteps = (Button) findViewById(R.id.btn_add_steps);
        mButtonUpdateSteps = (Button) findViewById(R.id.btn_update_steps);
        mButtonDeleteSteps = (Button) findViewById(R.id.btn_delete_steps);

        mButtonViewWeek.setOnClickListener(this);
        mButtonAddSteps.setOnClickListener(this);
        mButtonUpdateSteps.setOnClickListener(this);
        mButtonDeleteSteps.setOnClickListener(this);

 */
    }
}
