package android.andri.com.countdowntimer;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Date;

public class PrefUtils {

    private static final String START_TIME = "countdown_timer";
    private static final String MAX_TIME = "max_timer";
    private SharedPreferences mPreferences;

    public PrefUtils(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public long getStartedTime() {
        return mPreferences.getLong(START_TIME, 0);
    }

    public void setStartedTime(long startedTime) {
        Date date = new Date();
        date.setTime(startedTime*1000);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong(START_TIME, startedTime);
        editor.apply();
    }

    public int getMaxTime() { return mPreferences.getInt(MAX_TIME, 60); };

    public void setMaxTime(int maxTime) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(MAX_TIME, maxTime);
        editor.apply();
    }
}