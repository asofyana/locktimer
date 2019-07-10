package android.andri.com.countdowntimer;


import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private PrefUtils prefUtils;
    private static EditText minutes;
    private static EditText seconds;
    private TextView noticeText;
    private static TextView countdownTimerText;
    private View btnStart;
    private CountDownTimer countDownTimer;
    private long timeToStart;
    private TimerState timerState;
    //private static int MAX_TIME = 0;

    public static final int RESULT_ENABLE = 11;
    private DevicePolicyManager devicePolicyManager;
    private ActivityManager activityManager;
    private ComponentName compName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countdownTimerText = (TextView) findViewById(R.id.countdownText);
        noticeText = (TextView) findViewById(R.id.notice);
        minutes = (EditText) findViewById(R.id.enterMinutes);
        seconds = (EditText) findViewById(R.id.enterSeconds);
        btnStart = findViewById(R.id.button);
        prefUtils = new PrefUtils(getApplicationContext());

        // enable admin
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        compName = new ComponentName(this, MyAdmin.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional text explaining why we need this permission");
        startActivityForResult(intent, RESULT_ENABLE);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String getMinutes = minutes.getText().toString();
                String getSeconds = seconds.getText().toString();
                int maxTime = 0;

                if (!getSeconds.equals("") && getSeconds.length() > 0) {
                    maxTime = Integer.parseInt(getSeconds);
                }

                if (!getMinutes.equals("") && getMinutes.length() > 0) {
                    int noOfMinutes = Integer.parseInt(getMinutes);
                    maxTime = maxTime + (60 * noOfMinutes);
                }

                if (maxTime == 0) {
                    Toast.makeText(MainActivity.this, "Please enter no. of Minutes or Seconds.", Toast.LENGTH_SHORT).show();//Display toast if edittext is empty
                } else {
                    prefUtils.setMaxTime(maxTime);
                    timeToStart = maxTime;
                    if (timerState == TimerState.STOPPED) {
                        prefUtils.setStartedTime(getNow());
                        startTimer();
                        timerState = TimerState.RUNNING;
                    }
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //initializing a countdown timer
        initTimer();
        updatingUI();
        removeAlarmManager();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerState == TimerState.RUNNING) {
            countDownTimer.cancel();
            setAlarmManager();
        }
    }
    private long getNow() {
        Calendar rightNow = Calendar.getInstance();

        Date date = new Date();
        date.setTime((rightNow.getTimeInMillis() / 1000) * 1000);

        return rightNow.getTimeInMillis() / 1000;
    }

    private void initTimer() {

        long startTime = prefUtils.getStartedTime();

        if (startTime > 0) {
            timeToStart = (prefUtils.getMaxTime() - (getNow() - startTime));
            if (timeToStart <= 0) {
                // TIMER EXPIRED
                timeToStart = prefUtils.getMaxTime();
                onTimerFinish2();
            } else {
                startTimer();
                timerState = TimerState.RUNNING;
            }
        } else {
            timeToStart = prefUtils.getMaxTime();
            timerState = TimerState.STOPPED;
        }

    }

    private void onTimerFinish() {
        Toast.makeText(this, "Countdown timer finished!", Toast.LENGTH_SHORT).show();
        prefUtils.setStartedTime(0);
        timeToStart = prefUtils.getMaxTime();

        // Lock screen
        boolean active = devicePolicyManager.isAdminActive(compName);
        if (active && timerState == TimerState.RUNNING) {
            timerState = TimerState.STOPPED; // make sure it is stopped to prevent locked again
            devicePolicyManager.lockNow();
        } else {
            Toast.makeText(MainActivity.this, "You need to enable the Admin Device Features", Toast.LENGTH_SHORT).show();
        }

        timerState = TimerState.STOPPED;
        updatingUI();

    }

    private void onTimerFinish2() {
        Toast.makeText(this, "Countdown timer finished!", Toast.LENGTH_SHORT).show();
        prefUtils.setStartedTime(0);
        timeToStart = prefUtils.getMaxTime();
        countdownTimerText.setText("00:00:00");
        timerState = TimerState.STOPPED;
        updatingUI();
    }

    private void updatingUI() {
        if (timerState == TimerState.RUNNING) {
            btnStart.setEnabled(false);
            noticeText.setText("Countdown Timer is running...");
        } else {
            btnStart.setEnabled(true);
            noticeText.setText("Countdown Timer stopped!");
        }
        //timerText.setText(String.valueOf(timeToStart));
    }

    private void startTimer() {

        countDownTimer = new CountDownTimer(timeToStart * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                timeToStart -= 1;
                long millis = millisUntilFinished;
                //Convert milliseconds into hour,minute and seconds
                String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                countdownTimerText.setText(hms);//set text
                updatingUI();
            }

            @Override
            public void onFinish() {
                onTimerFinish();
                updatingUI();
            }
        }.start();
    }

    public void setAlarmManager() {

        long wakeUpTime = (prefUtils.getStartedTime() + prefUtils.getMaxTime()) * 1000;
        Date date = new Date();
        date.setTime(wakeUpTime);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TimeReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(wakeUpTime, sender), sender);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, wakeUpTime, sender);
        }
    }

    public void removeAlarmManager() {
        Intent intent = new Intent(this, TimeReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }

    private enum TimerState {
        STOPPED,
        RUNNING
    }
}