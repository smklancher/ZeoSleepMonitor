package com.stephenklancher.zeosleepmonitor;

import java.util.ArrayList;

import com.stephenklancher.zeosleepmonitor.ZeoData.SleepPhase;

/**
 * Represents one 5-minute sleep stage, including the ten 30-second stages contained within
 * @author Stephen Klancher
 *
 */
public class SleepStageDetail {
	
	private SleepPhase mPhase;
	
	private ArrayList<SleepPhase> mDetail=new ArrayList<SleepPhase>();
	
	private int mIndexInSleepEvent;
	
	/**
	 * ZeoData will provide the phase from the 5-minute data, and the byte array from the 30-second data
	 */
	public SleepStageDetail(int phase, byte[] detail, int indexInSleepEvent){
		this.mPhase=SleepPhase.fromInt(phase);
		this.mIndexInSleepEvent=indexInSleepEvent;
		
		for (int detailPhase : detail){
			SleepPhase sleepPhase=SleepPhase.fromInt(detailPhase);
			this.mDetail.add(sleepPhase);
		}
	}
	
	/**
	 * The 5 minute sleep stage
	 * @return SleepPhase
	 */
	public SleepPhase getStage(){
		return this.mPhase;
	}
	
	/**
	 * Index of this stage in the display hypnogram of the sleep event
	 * @return int index
	 */
	public int indexInSleepEvent(){
		return mIndexInSleepEvent;
	}
	
	
	/**
	 * The set of ten 30 second stages as a string of comma separated ints
	 * @return String csv ints
	 */
	public String getDetailString(){
		return this.mDetail.toString();
	}
	
	
	/**
	 * Sleep phase and all detail phases
	 * @return String
	 */
	@Override public String toString(){
		return getStage().toString() + " " + getDetailString();
	}
	
	
	/**
	 * If detail contains the more rare phase of light deep sleep
	 * @return Boolean
	 */
	public boolean containsLightDeep(){
		for (SleepPhase detailPhase : mDetail){
			if(detailPhase==SleepPhase.LIGHTDEEP){
				return true;
			}
		}
		
		return false;
	}

}
