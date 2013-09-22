package com.stephenklancher.zeosleepmonitor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.stephenklancher.zeosleepmonitor.ZeoData.SleepPhase;

/**
 * Uses Zeo data to decide if now is a good time to wake up.
 * @author Stephen Klancher
 *
 */
public class WakeLogic {
    @SuppressWarnings("unused")
	private static final String TAG = SleepMonitorService.class.getSimpleName();
	
	/**
	 * Number of minutes before alarm to look for a good wake time
	 * TODO: Configurability.  For now hardcoded.
	 * @return int minutes
	 */
	private static int wakeWindowMinutes(){
		return 30;
	}
	
	/**
	 * Alarm time (end of alarm window)
	 * TODO: Configurability: Either user chosen time or after certain amount of sleep.  For now hardcoded.
	 * @return Date with hour and minute set
	 */
	private static Date wakeTime(){
		
		//doesn't work?
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ZeoData.getContext());
		int hours =Integer.getInteger(sharedPref.getString(SleepMonitorPreferences.PREF_WAKE_AFTER_HOURS,"0"), 0);
		

		Calendar cal=Calendar.getInstance();
		Calendar today=Calendar.getInstance();
		
		if (hours==0){
			long alarmMillis =sharedPref.getLong(SleepMonitorPreferences.PREF_ALARM_TIME,0);
			
			//set the time (and date) from the preference
			cal.setTimeInMillis(alarmMillis);
			
			//override the date with today
			//inelegant, but the before midnight the alarm will just look like it is long past
			//	then in the morning it will be "today" and work.
			cal.set(Calendar.YEAR, today.get(Calendar.YEAR));
			cal.set(Calendar.MONTH, today.get(Calendar.MONTH));
			cal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
			
			//cal.set(2012, 7, 2, 7, 30);
			//cal.set(Calendar.HOUR_OF_DAY, 7);
			//cal.set(Calendar.MINUTE, 30);
	
		}else{
		
			//wake x hours after beginning of night (not actually same as beginning of sleep)
			int sleepEventID=ZeoData.mostRecentSleepEventID(0);
			//after sleeping for an hour
			if(ZeoData.epochCountBase()>120){
				long start=ZeoData.startOfNight(sleepEventID);
				
				cal.setTimeInMillis(start);
				//add x hours to start of night
				cal.add(Calendar.HOUR, hours);
			}else{
				//before an hour's sleep, return impossible date
				cal.set(9999, 6, 27, 7, 30);
			}
		}
		
		return cal.getTime();
		
		//SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ZeoData.getContext());
		//return new Date(sharedPref.getLong(SleepMonitorPreferences.PREF_ALARM_TIME,0));
		
	}
	
	public static String alarmStatus(){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ZeoData.getContext());
		int hours =Integer.getInteger(sharedPref.getString(SleepMonitorPreferences.PREF_WAKE_AFTER_HOURS,"0"), 0);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		if (hours==0){
			return "Alarm is set for " + sdf.format(wakeTime());
		}else{
			return "Alarm is set for " + Integer.toString(hours) + " hours after start of night (" + sdf.format(wakeTime()) + ")";
		}
	}
	
	
	/**
	 * Decide if it is a good time to wake
	 * @return boolean wake time
	 */
	public static boolean isTimeToWake(){ 
		Date endWake=wakeTime();
		
		boolean pastAlarmLimit=System.currentTimeMillis()>endWake.getTime();
		long alarmWindowMillis=10 * 60 * 1000;
		boolean tooFarPastAlarmTime=System.currentTimeMillis()>(endWake.getTime()+alarmWindowMillis);
		
		//if it past the end of the wake window then wake
		if(pastAlarmLimit && !tooFarPastAlarmTime){
        	ZeoData.Log("End of wake window hit.");
			return true;
		}
		
		Calendar startWake=Calendar.getInstance();
		startWake.setTime(endWake);
		startWake.add(Calendar.MINUTE, wakeWindowMinutes() * -1);
		
		boolean afterAlarmWindowStart=System.currentTimeMillis()>startWake.getTimeInMillis();
		
		//if it is past the start of the wake window, then check criteria
		if(afterAlarmWindowStart && !pastAlarmLimit){
			SleepPhase currentPhase=ZeoData.currentNight().displayPhase(-1);
			SleepPhase previousPhase=ZeoData.currentNight().displayPhase(-2);
			
			//TODO:  probably better to ALWAYS check for interesting conditions then fire events for them
			//		then alarms can hook into events
			boolean enteringRem=(previousPhase==SleepPhase.LIGHT &&
					currentPhase==SleepPhase.REM);
			
			boolean leavingRem=(previousPhase==SleepPhase.REM &&
					currentPhase==SleepPhase.LIGHT);
			
			if(enteringRem){
				ZeoData.Log("Waking as entering REM.");
				return true;
			}
			
			if(leavingRem){
				ZeoData.Log("Waking as leaving REM.");
				return true;
			}
			
			
			ZeoData.Log("Within wake window.  Current Phase: " + currentPhase);
		}else{
			//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			//ZeoData.Log("Outside of wake window.  Window start: " + sdf.format(startWake.getTime()) + ". Window end: " + sdf.format(endWake));
		}
		
		return false;
	}
	
	
	/**
	 * Log anything interesting I can think of
	 * 
	 */
	public static void logInteresting(){
		SleepPhase currentPhase=ZeoData.currentNight().displayPhase(-1);
		SleepPhase previousPhase=ZeoData.currentNight().displayPhase(-2);
		
		//Log what phase we wake from
		if (currentPhase==SleepPhase.WAKE){
			if (previousPhase!=SleepPhase.WAKE && previousPhase!=SleepPhase.UNDEFINED){
				ZeoData.Log("Waking from " + previousPhase);
			}
		}
		
		//Log the more rare light-deep phase
		//if (currentPhase.containsLightDeep()){
		//	ZeoData.Log("Phase detail contaings light-deep: " + currentPhase);
		//}
		
		
		
	}
	
}
