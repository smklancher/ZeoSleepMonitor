package com.stephenklancher.zeosleepmonitor;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.myzeo.android.api.data.ZeoDataContract.Headband;
import com.myzeo.android.api.data.ZeoDataContract.SleepEpisode;
import com.myzeo.android.api.data.ZeoDataContract.SleepRecord;

/** 
 * Retreives data from the Zeo data store
 * @author Stephen Klancher
 * 
 */
public class ZeoData {
	
    private static final String TAG = SleepMonitorService.class.getSimpleName();

    public static final long SLEEP_EPOCH_MILLIS_SHORT=30000;
    public static final long SLEEP_EPOCH_MILLIS_LONG=300000;
    public static final int SLEEP_EPOCH_MULTIPLIER=(int) (SLEEP_EPOCH_MILLIS_LONG / SLEEP_EPOCH_MILLIS_SHORT);
    
    /**
     * http://www.myzeo.com/sleep/sleep-stage-6
     * Stage 6 is only used in the 30sec data (base hypnogram) and represents a particularly deep 
     * stage of light sleep. Depending on the sleepstages around it, the algorithm may decide that
     *  it is actually deep sleep for the display hypnogram.
     */
    public static final int SLEEP_STAGE_DEEPERLIGHT=6;
    
    //Instead of extending Application or Activity, intialize with a context so we can use ContentResolver, etc
    private static Context sContext;
    
    /**
     * Initialize with a context so we can use ContentResolver, etc
     * @return void
     */
    public static void initialize(Context aContext){
    	sContext=aContext;
    }
    
    /**
     * Make the context available to any other class
     * probably bad practice but oh well
     * @return Context
     */
    public static Context getContext(){
    	return sContext;
    }
    
    
    /**
     * Enum of sleep phases
     *
     */
    public enum SleepPhase{
    	UNDEFINED("Undefined", SleepRecord.SLEEP_STAGE_UNDEFINED),
    	WAKE("Wake", SleepRecord.SLEEP_STAGE_WAKE),
    	REM("REM", SleepRecord.SLEEP_STAGE_REM),
    	LIGHT("Light", SleepRecord.SLEEP_STAGE_LIGHT),
    	DEEP("Deep", SleepRecord.SLEEP_STAGE_DEEP),
    	MAX("Max phases (not real sleep phase)", SleepRecord.SLEEP_STAGE_NMAX), //Just to mirror Zeo which defines 5 as max even though that isn't true
    	LIGHTDEEP("Deep Stage of Light", SLEEP_STAGE_DEEPERLIGHT), //Deeper phase of light sleep: http://www.myzeo.com/sleep/sleep-stage-6
    	UNKNOWN("Unknown", -1);
    	
    	private String name;
    	private int zeoNumber;
    	
    	private SleepPhase(String name, int zeoNumber){
    		this.name=name;
    		this.zeoNumber=zeoNumber;
    	}

    	public String toString(){
    		return name;
    	}

    	public int toInt(){
    		return zeoNumber;
    	}
    	
    	
    	/**
         * Return SleepPhase enum from Zeo sleep phase int
         * @param int phase
         * @return SleepPhase
         */
    	public static SleepPhase fromInt(int phase){
    		switch(phase) {
        	case SleepRecord.SLEEP_STAGE_UNDEFINED:
        		return SleepPhase.UNDEFINED;
    		case SleepRecord.SLEEP_STAGE_WAKE:
        		return SleepPhase.WAKE;
    		case SleepRecord.SLEEP_STAGE_REM:
        		return SleepPhase.REM;
    		case SleepRecord.SLEEP_STAGE_LIGHT:
        		return SleepPhase.LIGHT;
    		case SleepRecord.SLEEP_STAGE_DEEP:
        		return SleepPhase.DEEP;
    		case SLEEP_STAGE_DEEPERLIGHT:
        		return SleepPhase.LIGHTDEEP;
    		default:
        		return SleepPhase.UNKNOWN;
        	}
    	}
    }
    
    
    
    /**
     * Enum of reasons a sleep record ended
     *
     */
    public enum EndReason{
    	COMPLETE("Complete", SleepRecord.END_REASON_COMPLETE),
    	ACTIVE("In Progress", SleepRecord.END_REASON_ACTIVE),
    	BATTERY_DIED("Battery Died", SleepRecord.END_REASON_BATTERY_DIED),
    	DISCONNECTED("Headband Disconnected", SleepRecord.END_REASON_DISCONNECTED),
    	SERVICE_KILLED("Service Killed", SleepRecord.END_REASON_SERVICE_KILLED),
    	UNKNOWN("Unknown", -1);
    	
    	private String mName;
    	private int mReason;
    	
    	private EndReason(String name, int reason){
    		this.mName=name;
    		this.mReason=reason;
    	}

    	public String toString(){
    		return mName;
    	}

    	public int toInt(){
    		return mReason;
    	}
    	
    	
    	/**
         * Return EndReason enum 
         * @param int reason
         * @return EndReason
         */
    	public static EndReason fromInt(int reason){
    		switch(reason) {
        	case SleepRecord.END_REASON_COMPLETE:
        		return EndReason.COMPLETE;
    		case SleepRecord.END_REASON_ACTIVE:
        		return EndReason.ACTIVE;
    		case SleepRecord.END_REASON_BATTERY_DIED:
        		return EndReason.BATTERY_DIED;
    		case SleepRecord.END_REASON_DISCONNECTED:
        		return EndReason.DISCONNECTED;
    		case SleepRecord.END_REASON_SERVICE_KILLED:
        		return EndReason.SERVICE_KILLED;
    		default:
        		return EndReason.UNKNOWN;
        	}
    	}
    }
    
    
    
    /** 
     * Detects whether the headband is on or not.
     * @return boolean whether the head band is on or not.
     */
	public static boolean isHeadbandOnHead(){
    	String[] projection = new String[] {
                Headband.ON_HEAD
            };
    	
    	final Cursor cursor = sContext.getContentResolver().query(Headband.CONTENT_URI, 
                projection, null, null, null);
		if (cursor == null) {
			Log.w(TAG, sContext.getString(R.string.null_cursor));
			return false;
		}
		
		cursor.moveToFirst();
		int headband=cursor.getInt(cursor.getColumnIndex(Headband.ON_HEAD));
        cursor.close();
		
		if (headband==1){
			return true;
		}else{
			return false;
		}
    }
	
	
    private static Night sCurrentNight;
    
    /**
     * Singlton for a SleepEpisode of the current night
     * TODO: Enabling a test mode should allow currentNight to return a Night from a different Event ID
     * @return SleepEpisode
     */
    public static Night currentNight(){
    	int currentNightId=mostRecentSleepEventID(0);
    	if(sCurrentNight==null){
    		sCurrentNight=new Night(currentNightId);
    	}else{
    		if(currentNightId != sCurrentNight.getEpisodeId()){
    			sCurrentNight=new Night(currentNightId);
    		}
    	}
    	return sCurrentNight;
    }
    
    

	private static byte[] sDisplayHypnogram={};
	private static byte[] sBaseHypnogram={};
	//private static long sLastHypnogramUpdate;
	private static int sLastSleepEventId;
	
	/**
	 * Refresh data when SleepRecord is updated
	 * TODO: check if we need to handle possible differences in SleepEventID
	 */
	public static void sleepRecordUpdated(){
		ZeoData.currentNight().update();
		Log("SleepRecord Change: " + currentNight().toString());
	}
	
	
	
	/**
	 * Returns most recent sleep phase
	 * @see com.myzeo.android.api.data.ZeoDataContract.SleepRecord
	 * @param int SleepEventID
	 * @param int EpochsBeforeMostRecent 0 being the most recent, 1 being the second most recent...
	 * @return int com.myzeo.android.api.data.ZeoDataContract.SleepRecord
	 */
	public static SleepStageDetail mostRecentSleepPhase(int SleepEventID, int EpochsBeforeMostRecent){
		byte[] display;
		byte[] base;
		
		//only query if we want a different event than is already cached
		if(sLastSleepEventId != SleepEventID){
			String[] projection = new String[] {
					SleepRecord.DISPLAY_HYPNOGRAM,
					SleepRecord.BASE_HYPNOGRAM,
	        };
	    	
	    	final Cursor cursor = sContext.getContentResolver().query(SleepRecord.CONTENT_URI, 
	                projection, SleepRecord.SLEEP_EPISODE_ID + "=?", 
	                new String[] {Integer.toString(SleepEventID)}, null);
			if (cursor == null) {
				Log(sContext.getString(R.string.null_cursor));
				return new SleepStageDetail(0, new byte[0],0);
			}
			
			cursor.moveToFirst();

			display =
	                cursor.getBlob(cursor.getColumnIndex(SleepRecord.DISPLAY_HYPNOGRAM));
			base =
	                cursor.getBlob(cursor.getColumnIndex(SleepRecord.BASE_HYPNOGRAM));
			
			
	        cursor.close();
		}else{
			display=sDisplayHypnogram;
			base=sBaseHypnogram;
		}
		
		if(epochCountDisplay(true) != epochCountDisplay(false)){
    		// need to make this fix more clear, more elegant
    		//display hypno seems to be written in sets of two
			EpochsBeforeMostRecent++;
    	}
		
        //As long as there is enough data...
        if(display.length>EpochsBeforeMostRecent){
        	
        	int phase;
        	//...return the desired element
        	phase=display[display.length - EpochsBeforeMostRecent - 1];
        	
        	int detailEnd=base.length - (EpochsBeforeMostRecent * SLEEP_EPOCH_MULTIPLIER);
        	
        	//Log("Getting detail (" + Integer.toString(detailEnd -SLEEP_EPOCH_MULTIPLIER) + " - " 
        	//		+ Integer.toString(detailEnd) + ") from base (" + base.length + ")");
        	
        	if(base.length<detailEnd){
        		Log("Not enough data: BaseHypnogram=" + Integer.toString(base.length) + 
            			", EpochsBeforeMostRecent: " + Integer.toString(EpochsBeforeMostRecent));
    			return new SleepStageDetail(0, new byte[0],0);
        	}

        	byte[] detail={};
        	if(base.length>detailEnd){
        		//Log(base.length + " : " + detailEnd);
        		detail=Arrays.copyOfRange(base, detailEnd -SLEEP_EPOCH_MULTIPLIER, detailEnd);
        	}
        	
        	SleepStageDetail currentPhase=new SleepStageDetail(phase, detail, epochCountDisplay(true)-1);
        	
        	//checkUndefined(currentPhase);
        	
    		return currentPhase;
        }else{
        	Log("Not enough data: DisplayHypnogram=" + Integer.toString(display.length) + 
        			", EpochsBeforeMostRecent: " + Integer.toString(EpochsBeforeMostRecent));
			return new SleepStageDetail(0, new byte[0],0);
        }
	}
	
	

	//Temp debugging
	//private static ArrayList<Integer> undefinedStages=new ArrayList<Integer>();
	
	/**
	 * Debugging - check undefined stages
	 */
	/*
	private static void checkUndefined(SleepStageDetail currentPhase){
		//iterate over previously undefined stages and check them
		Iterator<Integer> i = undefinedStages.iterator();
		while (i.hasNext()){
			Integer index=i.next();
			
			//+++ This will error if undefinedStages isn't cleared
			SleepPhase stage=zeoSleepPhase(sDisplayHypnogram[index]);
			
			if(stage==SleepPhase.UNDEFINED){
				//Log("Undefined " + index.toString() + ": Still undefined.");
			}else{
				Log("Undefined " + index.toString() + ": Now " + stage.toString());
				i.remove();
			}
			
		}


		//add the current stage if undefined.
		if(currentPhase.getStage()==SleepPhase.UNDEFINED){
			Integer index=currentPhase.indexInSleepEvent();
			//and if it hasn't already been added
			if(undefinedStages.contains(index) != true){
				undefinedStages.add(currentPhase.indexInSleepEvent());
			}
			
		}
		
	}
	*/
	
	
	/**
	 * Start of night
	 * @param int SleepEventID
	 * @return long unix timestamp of start of night
	 */
	public static long startOfNight(int sleepEventID){
		String[] projection = new String[] {
				SleepRecord.START_OF_NIGHT
        };
		

    	final Cursor cursor = sContext.getContentResolver().query(SleepRecord.CONTENT_URI, 
                projection, SleepRecord.SLEEP_EPISODE_ID + "=?", 
                new String[] {Integer.toString(sleepEventID)}, null);
		if (cursor == null) {
			Log(sContext.getString(R.string.null_cursor));
			return 0;
		}
		
		cursor.moveToFirst();
		
		long start =
                cursor.getLong(cursor.getColumnIndex(SleepRecord.START_OF_NIGHT));
        cursor.close();
        
        return start;
	}

	
	/**
	 * How many epochs are in this sleep event (Display)
	 * @param boolean corrected Compare against the number of base epochs and subtract to match
	 * @return int number of epochs
	 */
	public static int epochCountDisplay(boolean corrected){
		if(corrected){
			return (int) (epochCountBase() / SLEEP_EPOCH_MULTIPLIER);
		}else{
			return sDisplayHypnogram.length;
		}
	}
	
	/**
	 * How many epochs are in this sleep event (Base)
	 * @param int SleepEventID
	 * @return int number of epochs
	 */
	public static int epochCountBase(){
		return sBaseHypnogram.length;
	}
	
	/**
	 * Gets the most recent SleepEvent ID
	 * @return int Sleep Event ID, -1 if no ID available
	 */
	public static int mostRecentSleepEventID(int nightsBeforeMostRecent) {
    	String[] projection = new String[] {
                SleepEpisode._ID,
                SleepEpisode.START_TIMESTAMP
            };
    	
    	int id=-1;
    	
    	final Cursor cursor = sContext.getContentResolver().query(SleepEpisode.CONTENT_URI,
                projection, null, null, SleepEpisode.START_TIMESTAMP + " DESC");
    	
		if (cursor == null) {
			Log(sContext.getString(R.string.null_cursor));
		}else{
			//as long as there is data as far back as we want
			if(cursor.getCount()>nightsBeforeMostRecent){
				cursor.moveToPosition(nightsBeforeMostRecent);
				id=cursor.getInt(cursor.getColumnIndex(SleepEpisode._ID));
			} else {
	            Log("Sleep record not found (" + nightsBeforeMostRecent + "before most recent)");
	        }
	        
            cursor.close();
		}
        
        return id;
    }
	
	
	
	
	/**
	 * Log to file so app can check what the service did later
	 * @param String msg
	 */
	public static void Log(String msg){
		Log.i(TAG,msg);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		msg = sdf.format(date) + ": " + msg + "\n";
		
		FileOutputStream fos;
		try {
			fos = sContext.openFileOutput("SleepMonitorLog", Context.MODE_APPEND);
			fos.write(msg.getBytes());
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Get text of log
	 * @return String text of log
	 */
	public static String getLogText(){
		try {
		    BufferedReader inputReader = new BufferedReader(new InputStreamReader(
		    		sContext.openFileInput("SleepMonitorLog")));
		    String inputString;
		    StringBuffer stringBuffer = new StringBuffer();                
		    while ((inputString = inputReader.readLine()) != null) {
		        stringBuffer.append(inputString + "\n");
		    }
		    return stringBuffer.toString();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		return "";
	}
	
}
