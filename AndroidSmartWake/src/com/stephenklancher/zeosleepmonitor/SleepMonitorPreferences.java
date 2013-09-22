package com.stephenklancher.zeosleepmonitor;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SleepMonitorPreferences extends PreferenceActivity {

    public static final String PREF_ALARM_ENABLED = "alarm_enabled";
    public static final String PREF_ALARM_TIME = "alarm_time";
    public static final String PREF_WAKE_AFTER_HOURS = "wake_after_hours";
    
	@SuppressWarnings("deprecation")
    //For compatibility with pre Android 3.0
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
