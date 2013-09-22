package com.stephenklancher.zeosleepmonitor;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.database.Cursor;
import android.util.Log;

import com.myzeo.android.api.data.ZeoDataContract.SleepRecord;
import com.stephenklancher.zeosleepmonitor.ZeoData.EndReason;
import com.stephenklancher.zeosleepmonitor.ZeoData.SleepPhase;

/**
 * Represents a night of sleep as represented by a Sleep Episode/Event from Zeo
 * @author Stephen Klancher
 *
 */
public class Night {
    private static final String TAG = SleepMonitorService.class.getSimpleName();
    
	private int mEpisodeId;
	public int getEpisodeId(){return mEpisodeId;}
	
	private byte[] mDisplayHypnogram={};
	private byte[] mBaseHypnogram={};
	private long mStart;
	private long mEnd;
	private int mZq;
	private EndReason mEndReason;
	private long mLastUpdate;
	private int mTotalZ;
	
	public Night(int episodeId){
		mEpisodeId=episodeId;
		
		this.update();
	}
	
	
	@Override
	public String toString(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		switch(mEndReason){
		case ACTIVE:
			return displayPhaseMax() + "/" + basePhaseMax() + ": " + displayPhase(-1).toString() +
					" (" + basePhase(-1).toString() + ") - " + (mTotalZ / 2) + " minutes";
		case COMPLETE:
			return sdf.format(new Date(mStart)) + " to " + sdf.format(new Date(mEnd)) + ", ZQ: " + mZq;
		case BATTERY_DIED:
		case DISCONNECTED:
		case SERVICE_KILLED:
		case UNKNOWN:
		default:
			return mEndReason.toString() + " at " + sdf.format(new Date(mEnd)) + ": " + displayPhaseMax() 
					+ "/" + basePhaseMax() + ": " + displayPhase(-1).toString() +
					" (" + basePhase(-1).toString() + ") - " + (mTotalZ / 2) + " minutes";
		
		}
	}
	
	
	/**
	 * Update from the sleep episode from zeo when the record has changed
	 */
	public void update(){
		String[] projection = new String[] {
				SleepRecord.DISPLAY_HYPNOGRAM,
				SleepRecord.BASE_HYPNOGRAM,
				SleepRecord.END_OF_NIGHT,
				SleepRecord.END_REASON,
				SleepRecord.ZQ_SCORE,
				SleepRecord.START_OF_NIGHT,
				SleepRecord.TOTAL_Z,
				SleepRecord.UPDATED_ON
        };
    	
    	final Cursor cursor = ZeoData.getContext().getContentResolver().query(
    			SleepRecord.CONTENT_URI, 
                projection, SleepRecord.SLEEP_EPISODE_ID + "=?", 
                new String[] {Integer.toString(mEpisodeId)}, null);
		if (cursor == null) {
			Log.w(TAG, ZeoData.getContext().getString(R.string.null_cursor));
			return;
		}
		
		cursor.moveToFirst();

		mDisplayHypnogram =
                cursor.getBlob(cursor.getColumnIndex(SleepRecord.DISPLAY_HYPNOGRAM));
		mBaseHypnogram =
                cursor.getBlob(cursor.getColumnIndex(SleepRecord.BASE_HYPNOGRAM));

		mZq=cursor.getInt(cursor.getColumnIndex(SleepRecord.ZQ_SCORE));
		mEndReason=EndReason.fromInt(cursor.getInt(cursor.getColumnIndex(SleepRecord.END_REASON)));
		mEnd=cursor.getLong(cursor.getColumnIndex(SleepRecord.END_OF_NIGHT));
		mStart=cursor.getLong(cursor.getColumnIndex(SleepRecord.START_OF_NIGHT));
		mTotalZ=cursor.getInt(cursor.getColumnIndex(SleepRecord.TOTAL_Z));
		mLastUpdate=cursor.getLong(cursor.getColumnIndex(SleepRecord.UPDATED_ON));
		
        cursor.close();
	}
	
	
	/**
	 * Index of the most recent long epoch from the display hypnogram
	 * Takes into account circumstances that result in the last element not being most recent
	 * @return int max index
	 */
	public int displayPhaseMax(){
		//These records appear to be written in groups of twos, 
		//so use the base hypnogram to find out how many we actually expect
		
		//integer division
		int completedLongEpochs = mBaseHypnogram.length / ZeoData.SLEEP_EPOCH_MULTIPLIER;  
		
		if(completedLongEpochs>0 && mDisplayHypnogram.length>0){
			if(completedLongEpochs>mDisplayHypnogram.length){
				//This can happen if the record ended with undefined epochs.
				//Undefined epochs are trimmed from the end of the display hypnogram but not base.
				//Thus if base is longer, just take the last display record as the most recent
				return mDisplayHypnogram.length - 1;
			}else{
				return completedLongEpochs - 1;
			}
		}else{
			return -1;
		}
	}
	
	
	/**
	 * Index of the most recent long epoch from the base hypnogram
	 * 
	 * @return int max index
	 */
	public int basePhaseMax(){
		if(mBaseHypnogram.length>0){
			return mBaseHypnogram.length - 1;
		}else{
			return -1;
		}
	}

	
	/**
	 * Return element of the base hypnogram
	 * @oaran int index, negative numbers count back from most recent
	 * @return SleepPhase
	 */
	public SleepPhase basePhase(int index){

		//normal index
		if(index<mBaseHypnogram.length && index>-1){
			return SleepPhase.fromInt(mBaseHypnogram[index]);
		}

		//negative numbers count back from most recent.  -1 is most recent
		int backwardsIndex=basePhaseMax() + index + 1;
		if(index<0 && backwardsIndex>-1){
			return SleepPhase.fromInt(mBaseHypnogram[backwardsIndex]);
		}
		
		return SleepPhase.UNKNOWN;
	}
	
	/**
	 * Return element of the display hypnogram
	 * @oaran int index, negative numbers count back from most recent
	 * @return SleepPhase
	 */
	public SleepPhase displayPhase(int index){

		//normal index
		if(index<mDisplayHypnogram.length && index>-1){
			return SleepPhase.fromInt(mDisplayHypnogram[index]);
		}

		//negative numbers count back from most recent.  -1 is most recent
		int backwardsIndex=displayPhaseMax() + index + 1;
		if(index<0 && backwardsIndex>-1 && backwardsIndex<mDisplayHypnogram.length){
			return SleepPhase.fromInt(mDisplayHypnogram[backwardsIndex]);
		}
		
		return SleepPhase.UNKNOWN;
	}
}
