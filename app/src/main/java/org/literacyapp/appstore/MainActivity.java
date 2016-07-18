package org.literacyapp.appstore;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.log4j.Logger;
import org.literacyapp.appstore.receiver.AlarmReceiver;
import org.literacyapp.appstore.task.DownloadApplicationsAsyncTask;
import org.literacyapp.appstore.util.ConnectivityHelper;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    public static final String PREF_LAST_SYNCHRONIZATION = "pref_last_synchronization";

    private Logger logger = Logger.getLogger(getClass());

    private TextView mTextViewMain;

    private TextView mTextViewLastSynchronization;

    private Button mButtonSynchronization;

    private ProgressBar mProgressBarSynchronization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logger.info("onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTextViewMain = (TextView) findViewById(R.id.textViewMain);
        mTextViewLastSynchronization = (TextView) findViewById(R.id.textViewLastSynchronization);
        mButtonSynchronization = (Button) findViewById(R.id.buttonSynchronization);
        mProgressBarSynchronization = (ProgressBar) findViewById(R.id.progressBarSynchronization);
    }

    @Override
    protected void onStart() {
        logger.info("onStart");
        super.onStart();

        // 1. Write permission is needed for storing Log4J files
        int permissionCheckWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheckWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }

        // 2. Select locale
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String localeAsString = sharedPreferences.getString(LocaleActivity.PREF_LOCALE, null);
        if (TextUtils.isEmpty(localeAsString)) {
            // Ask user to select preferred Locale
            Intent localeIntent = new Intent(this, LocaleActivity.class);
            startActivity(localeIntent);
            return;
        }
        int languageResourceId = getResources().getIdentifier("language_" + localeAsString.toLowerCase(), "string", getApplicationContext().getPackageName());
        mTextViewMain.setText(getString(R.string.locale_selected) + ": " + localeAsString + " (" + getString(languageResourceId) + ")");

        // 3. Start alarm
        Intent alarmReceiverIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar calendarDaily = Calendar.getInstance();
        calendarDaily.set(Calendar.HOUR_OF_DAY, 3);
        calendarDaily.set(Calendar.MINUTE, 0);
        calendarDaily.set(Calendar.SECOND, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendarDaily.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);

        long timeOfLastSynchronizationInMillis = sharedPreferences.getLong(PREF_LAST_SYNCHRONIZATION, 0);
        if (timeOfLastSynchronizationInMillis > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeOfLastSynchronizationInMillis);
            mTextViewLastSynchronization.setText(getString(R.string.last_synchronization) + ": " + calendar.getTime());
        } else {
            mTextViewLastSynchronization.setText(getString(R.string.last_synchronization) + ": " + getString(R.string.never));
        }

        mButtonSynchronization.setVisibility(View.VISIBLE);
        mButtonSynchronization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logger.info("mButtonSynchronization onClick");

                boolean isWifiEnabled = ConnectivityHelper.isWifiEnabled(getApplicationContext());
                logger.info("isWifiEnabled: " + isWifiEnabled);
                if (!isWifiEnabled) {
                    Toast.makeText(getApplicationContext(), getString(R.string.wifi_needs_to_be_enabled), Toast.LENGTH_SHORT).show();
                } else {
                    new DownloadApplicationsAsyncTask(getApplicationContext()).execute();
                }

                mTextViewLastSynchronization.setText(getString(R.string.synchronizing) + "...");
                mButtonSynchronization.setVisibility(View.GONE);
                mProgressBarSynchronization.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted
            } else {
                // Permission denied
            }
        }

        // Close application completely (in order to trigger Log4J configuration in AppstoreApplication on next start)
        finish();
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
