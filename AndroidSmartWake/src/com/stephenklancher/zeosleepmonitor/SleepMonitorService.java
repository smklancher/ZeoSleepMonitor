package com.stephenklancher.zeosleepmonitor;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.myzeo.android.api.data.ZeoDataContract.Headband;
import com.myzeo.android.api.data.ZeoDataContract.SleepRecord;

public class SleepMonitorService extends Service {
	//http://blog.myzeo.com/smartwake-a-different-way-to-wake-up/
	//http://www.myzeo.com/sleep/faqs/zeo-bedside/what-smartwake

    private static final String TAG = SleepMonitorService.class.getSimpleName();
    
	
    
    //************ Binding *************************
    
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
    	SleepMonitorService getService() {

            Log.i(TAG,"Binder getService");
            
            return SleepMonitorService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		ZeoData.Log("onBind: " + intent);
		return mBinder;
	}
	
	
	//*********** Service **********************

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        //initialize the data functions with a context
        ZeoData.initialize(getApplication());
        
		ZeoData.Log("Received start id " + startId + ": " + intent);
		
		//register to get notified on headband changes
		registerForHeadbandChanges();
		
		registerForSleepRecordChanges();
        
        //schedule the looping posts to do the work
		refreshNow();
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
	

    @Override
    public void onDestroy() {
		unregisterForHeadbandChanges();
		unregisterForSleepRecordChanges();
		
    	ZeoData.Log("Service destroyed.");
    }
    
	
	
	
	//********** Worker *******************
    
    //handles the runable posts
    private Handler mHandler = new Handler();
	
	/**
	 * This will run at intervals while the service is running and do most of the work
	 */
	private Runnable serviceWork = new Runnable() {
 	   public void run() {
 	       if (ZeoData.isHeadbandOnHead()){
 	    	   
 	    	   if(WakeLogic.isTimeToWake()){
 	    		   playAlarm();
 	    	   }else{
 	    	   }
 	    	   
 	    	  WakeLogic.logInteresting();

 	    	   //check every 30 seconds when headband is on
 	    	   //mHandler.postDelayed(serviceWork,30000);
 	       }else{
 	    	   //check every 15 minutes when headband is off
 	    	   //mHandler.postDelayed(serviceWork,15*60*1000);
 	    	  //ZeoData.Log("Headband is off, check again in 15 mins.");
 	    	   
 	    	  registerForHeadbandChanges();
 	    	   ZeoData.Log("Headband is off, check again when headband data changes.");
 	       }
 	   }
 	};
	
 	
 	/**
	 * schedule serviceWork to run now
	 */
	public void refreshNow(){
		mHandler.removeCallbacks(serviceWork);
		mHandler.post(serviceWork);
	}

    
	//********* Observer *******************
	
	/**
	 * Observer to watch for headband changes
	 *
	 */
	class HeadbandObserver extends ContentObserver {
	    public HeadbandObserver(Handler h) {
	        super(h);
	    }

	    @Override
	    public boolean deliverSelfNotifications() {
	        return true;
	    }

	    @Override
	    public void onChange(boolean selfChange) {
	        super.onChange(selfChange);
	        
	    	//we only care about the change if the headband is now on
	    	if(ZeoData.isHeadbandOnHead()){
		    	unregisterForHeadbandChanges();
		    	
	    		ZeoData.Log("Headband is on now.");
	    		//Toast.makeText(this., "Headband is on now.", Toast.LENGTH_LONG).show();
	    		
		        refreshNow();
	    	}
	    	

	    }
	}
	
	private HeadbandObserver mHeadbandObserver=new HeadbandObserver(mHandler);

	private void registerForHeadbandChanges(){
		//to prevent multiple registrations, always unregister first
		unregisterForHeadbandChanges();
		
		getContentResolver().registerContentObserver(
	            Headband.CONTENT_URI, true,
	            mHeadbandObserver); 
	}
	
	private void unregisterForHeadbandChanges(){
		getContentResolver().unregisterContentObserver(
	            mHeadbandObserver); 
	}

	
	/**
	 * Observer to watch for sleep record changes
	 *
	 */
	class SleepRecordObserver extends ContentObserver {
	    public SleepRecordObserver(Handler h) {
	        super(h);
	    }

	    @Override
	    public boolean deliverSelfNotifications() {
	        return true;
	    }

	    @Override
	    public void onChange(boolean selfChange) {
	        super.onChange(selfChange);
	        
	        ZeoData.sleepRecordUpdated();

	        refreshNow();
	    }
	}
	
	private SleepRecordObserver mSleepRecordObserver=new SleepRecordObserver(mHandler);

	private void registerForSleepRecordChanges(){
		//to prevent multiple registrations, always unregister first
		unregisterForSleepRecordChanges();
		
		getContentResolver().registerContentObserver(
				SleepRecord.CONTENT_URI, true,
	            mSleepRecordObserver); 
	}
	
	private void unregisterForSleepRecordChanges(){
		getContentResolver().unregisterContentObserver(
	            mSleepRecordObserver); 
	}
	
	
	
	
	
    /**
     * Play alarm sound
     * TODO: Customizable eventually.  Just a tone for now.
     * @return void
     */
    public void playAlarm(){
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ZeoData.getContext());
		boolean alarmEnabled=sharedPref.getBoolean(SleepMonitorPreferences.PREF_ALARM_ENABLED,true);
    	
		if(alarmEnabled){
	    	ZeoData.Log("Playing alarm.");
	    	new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_DTMF_4, 3000);
		}else{
			ZeoData.Log("Alarm would have triggered but is disabled.");
		}
    }
    
    
	
	
    
    
    
    //====Test=====
    public String headbandStatus() {
    	String[] projection = new String[] {
                Headband.ALGORITHM_MODE,
                Headband.BLUETOOTH_ADDRESS,
                Headband.BLUETOOTH_FRIENDLY_NAME,
                Headband.BONDED,
                Headband.CLOCK_OFFSET,
                Headband.CONNECTED,
                Headband.CREATED_ON,
                Headband.DOCKED,
                Headband.ON_HEAD,
                Headband.SW_VERSION,
                Headband.UPDATED_ON
            };
    	
    	final Cursor cursor = getContentResolver().query(Headband.CONTENT_URI,
                projection, null, null, null);
		if (cursor == null) {
			Log.w(TAG, "Cursor was null; something is wrong; perhaps Zeo not installed.");
			Toast.makeText(this, "Unable to access Zeo data provider, is Zeo installed?",
					Toast.LENGTH_LONG).show();
			return null;
		}
		
		java.util.Date tempdate;
		
		StringBuilder builder = new StringBuilder();
        if (cursor.moveToFirst()) {
            builder.append("Headband Status\n");

            do {
                // Begin writing data.
            	builder.append(Headband.ALGORITHM_MODE + ": " + 
                        cursor.getInt(cursor.getColumnIndex(Headband.ALGORITHM_MODE)) + "\n");
            	builder.append(Headband.BLUETOOTH_ADDRESS + ": " + 
                        cursor.getString(cursor.getColumnIndex(Headband.BLUETOOTH_ADDRESS)) + "\n");
            	builder.append(Headband.BLUETOOTH_FRIENDLY_NAME + ": " + 
                        cursor.getString(cursor.getColumnIndex(Headband.BLUETOOTH_FRIENDLY_NAME)) + "\n");
            	builder.append(Headband.BONDED + ": " + 
                        cursor.getInt(cursor.getColumnIndex(Headband.BONDED)) + "\n");;
            	builder.append(Headband.CLOCK_OFFSET + ": " + 
                        cursor.getLong(cursor.getColumnIndex(Headband.CLOCK_OFFSET)) + "\n");
            	

            	tempdate=new java.util.Date(cursor.getLong(cursor.getColumnIndex(Headband.CREATED_ON)));
                builder.append(Headband.CREATED_ON + ": " + tempdate.toString() +"\n");
                
            	builder.append(Headband.CONNECTED + ": " + 
                        cursor.getInt(cursor.getColumnIndex(Headband.CONNECTED)) + "\n");
            	builder.append(Headband.DOCKED + ": " + 
                        cursor.getInt(cursor.getColumnIndex(Headband.DOCKED)) + "\n");
            	builder.append(Headband.ON_HEAD + ": " + 
                        cursor.getInt(cursor.getColumnIndex(Headband.ON_HEAD)) + "\n");
            	builder.append(Headband.SW_VERSION + ": " + 
                        cursor.getString(cursor.getColumnIndex(Headband.SW_VERSION)) + "\n");
            	

            	tempdate=new java.util.Date(cursor.getLong(cursor.getColumnIndex(Headband.UPDATED_ON)));
                builder.append(Headband.UPDATED_ON + ": " + tempdate.toString() +"\n");
                    
                
            } while (cursor.moveToNext());

        } else {
            Log.w(TAG, "No sleep records found.");
            Toast.makeText(this, "No sleep records found in the provider.",
                           Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        return builder.toString();
    }
    
    
    
    
    public String sleepRecordDetailByID(int ID) {
    	String[] projection = new String[] {
                SleepRecord.LOCALIZED_START_OF_NIGHT,
                SleepRecord.START_OF_NIGHT,
                SleepRecord.END_OF_NIGHT,
                SleepRecord.TIMEZONE,
                SleepRecord.ZQ_SCORE,
                SleepRecord.AWAKENINGS,
                SleepRecord.TIME_IN_DEEP,
                SleepRecord.TIME_IN_LIGHT,
                SleepRecord.TIME_IN_REM,
                SleepRecord.TIME_IN_WAKE,
                SleepRecord.TIME_TO_Z,
                SleepRecord.TOTAL_Z,
                SleepRecord.SOURCE,
                SleepRecord.END_REASON,
                SleepRecord.BASE_HYPNOGRAM
            };

            final Cursor cursor = getContentResolver().query(SleepRecord.CONTENT_URI,
                                                             projection, SleepRecord.SLEEP_EPISODE_ID + "=?", new String[] {Integer.toString(ID)}, null);
            if (cursor == null) {
                Log.w(TAG, "Cursor was null; something is wrong; perhaps Zeo not installed.");
                Toast.makeText(this, "Unable to access Zeo data provider, is Zeo installed?",
                               Toast.LENGTH_LONG).show();
                return null;
            }

            java.util.Date tempdate;
            
            StringBuilder builder = new StringBuilder();
            if (cursor.moveToFirst()) {
                builder.append("\n");

	            	tempdate=new java.util.Date(cursor.getLong(cursor.getColumnIndex(SleepRecord.LOCALIZED_START_OF_NIGHT)));
	                builder.append(SleepRecord.LOCALIZED_START_OF_NIGHT + ": " + tempdate.toString() +"\n");
	
	            	tempdate=new java.util.Date(cursor.getLong(cursor.getColumnIndex(SleepRecord.START_OF_NIGHT)));
	                builder.append(SleepRecord.START_OF_NIGHT + ": " + tempdate.toString() +"\n");
	
	            	tempdate=new java.util.Date(cursor.getLong(cursor.getColumnIndex(SleepRecord.END_OF_NIGHT)));
	                builder.append(SleepRecord.END_OF_NIGHT + ": " + tempdate.toString() +"\n");
	                
                    builder.append(SleepRecord.ZQ_SCORE + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.ZQ_SCORE)) + "\n");
                    builder.append(SleepRecord.AWAKENINGS + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.AWAKENINGS)) + "\n");
                    builder.append(SleepRecord.TIME_IN_DEEP + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_DEEP)) +
                                   "\n");
                    builder.append(SleepRecord.TIME_IN_LIGHT + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_LIGHT)) +
                                   "\n");
                    builder.append(SleepRecord.TIME_IN_REM + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_REM)) + "\n");
                    builder.append(SleepRecord.TIME_IN_WAKE + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_WAKE)) +
                                   "\n");
                    builder.append(SleepRecord.TIME_TO_Z + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_TO_Z)) + "\n");
                    builder.append(SleepRecord.TOTAL_Z + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.TOTAL_Z)) + "\n");
                    builder.append(SleepRecord.SOURCE + ": " + cursor.getInt(cursor.getColumnIndex(SleepRecord.SOURCE)) + "\n");
                    int reason=cursor.getInt(cursor.getColumnIndex(SleepRecord.END_REASON));
                    String reasonStr = "Unknown";
                    
                    switch (reason) {
	                    case 0:	reasonStr="Complete record";
	                    	break;
	
	                    case 1:	reasonStr="Record is still active";
	                    	break;
	
	                    case 2:	reasonStr="Headband battery died";
	                    	break;
	
	                    case 3:	reasonStr="Headband disconnected";
	                    	break;
	
	                    case 4:	reasonStr="Service was killed on Android device";
	                    	break;
                
                    }

                    builder.append(SleepRecord.END_REASON + ": " + reasonStr + "\n");
                    
                    builder.append(SleepRecord.BASE_HYPNOGRAM + ": \n");
                    
                    
                    final byte[] baseHypnogram =
                        cursor.getBlob(cursor.getColumnIndex(SleepRecord.BASE_HYPNOGRAM));
                    for (byte stage : baseHypnogram) {
                        builder.append(Byte.toString(stage));
                    }
                
                    builder.append("\n");

            } else {
                Log.w(TAG, "No sleep records found.");
                Toast.makeText(this, "No sleep records found in the provider.",
                               Toast.LENGTH_SHORT).show();
            }
            cursor.close();
            return builder.toString();
    }
    
    
    

    

}
