/*
 * Copyright (C) 2011 Zeo Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 //SMK - added headbandStatus, sleepRecordDetailByID, and mostRecentSleepEventID
		these are used to display status of the headband and most recent sleep event.
 
package com.myzeo.android.demos.data_to_csv;

import static com.myzeo.android.api.data.ZeoDataContract.SleepRecord;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.myzeo.android.api.data.ZeoDataContract.Headband;
import com.myzeo.android.api.data.ZeoDataContract.SleepEpisode;

/**
 * Main entry point for the zeo CSV generator software.
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /** String that will contain the complete CSV gathered from the Zeo data provider. */
    private String mSleepCsv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button shareButton = (Button) findViewById(R.id.share_data);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSleepCsv == null || mSleepCsv.equals("")) {
                    // Warn user that there is no CSV data and abort prematurely.
                    Toast.makeText(MainActivity.this,
                                   "Sorry, there is no CSV data to send.",
                                   Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_SEND);
                final long now = System.currentTimeMillis();
                final String currentDateTime =
                    DateUtils.formatDateTime(MainActivity.this,
                                             now,
                                             DateUtils.FORMAT_SHOW_DATE |
                                             DateUtils.FORMAT_SHOW_TIME |
                                             DateUtils.FORMAT_SHOW_YEAR);
                // Add the data header
                intent.putExtra(Intent.EXTRA_SUBJECT, "My Zeo sleep data as CSV as of " +
                                currentDateTime + ".");
                // Add the body text
                StringBuilder builder = new StringBuilder();
                builder.append("Attached is my Zeo sleep data in CSV form as of: " +
                               currentDateTime + ".\n\n");
                // NOTE, uncomment the following to allow sleepCSV data to be included in the email
                // (inline)
                //builder.append(mSleepCsv);

                intent.putExtra(Intent.EXTRA_TEXT, builder.toString());
                intent.setType("text/plain");

                // Append file with sleep data in it.
                final String filename =
                    "sleep_data_" + DateFormat.format("yyyy-MM-dd'T'kk-mm-ss", now) + ".csv";
                File csvFile = writeCsvFile(mSleepCsv, filename);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(csvFile));

                // pass the csv off to sharing chooser
                startActivity(Intent.createChooser(intent, "Share Sleep CSV"));
            }
        });
    }

    /**
     * Resume the app repopulating the CSV data from scratch.
     */
    @Override
    public void onResume() {
        super.onResume();

        TextView csvText = (TextView) findViewById(R.id.csv_data);
        mSleepCsv = buildCsv();
		//SMK - get status of the headband and most recent sleep event
        String status = headbandStatus() + "\n" + sleepRecordDetailByID(mostRecentSleepEventID());
        if (mSleepCsv != null) {
            csvText.setText(status);
        }
    }
    
    //SMK - return some information about the current status of the headband
    private String headbandStatus() {
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
    
    
    
    //SMK - return information about the sleep event indicated by ID
    private String sleepRecordDetailByID(int ID) {
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
    
    
    

    //SMK - return the ID for the most recent sleep event, to be used by sleepRecordDetailByID.
    private int mostRecentSleepEventID() {
    	String[] projection = new String[] {
                SleepEpisode._ID,
                SleepEpisode.START_TIMESTAMP
            };
    	
    	final Cursor cursor = getContentResolver().query(SleepEpisode.CONTENT_URI,
                projection, null, null, SleepEpisode.START_TIMESTAMP + " DESC");
		if (cursor == null) {
			Log.w(TAG, "Cursor was null; something is wrong; perhaps Zeo not installed.");
			Toast.makeText(this, "Unable to access Zeo data provider, is Zeo installed?",
					Toast.LENGTH_LONG).show();
			return -1;
		}
		
        if (cursor.moveToFirst()) {
        	int ID= cursor.getInt(cursor.getColumnIndex(SleepEpisode._ID));
            cursor.close();
            return ID;

        } else {
            Log.w(TAG, "No sleep records found.");
            Toast.makeText(this, "No sleep records found in the provider.",
                           Toast.LENGTH_SHORT).show();
            cursor.close();
        }
        
        return -1;
    }
    
    

    /**
     * Method queries the Zeo data provider; extracts some notable fields and builds a CSV string
     * containing that data.
     *
     * @return the CSV data taken from the zeo data provider's sleep record table.
     */
    private String buildCsv() {
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
            SleepRecord.DISPLAY_HYPNOGRAM,
            SleepRecord.BASE_HYPNOGRAM
        };

        final Cursor cursor = getContentResolver().query(SleepRecord.CONTENT_URI,
                                                         projection, null, null, null);
        if (cursor == null) {
            Log.w(TAG, "Cursor was null; something is wrong; perhaps Zeo not installed.");
            Toast.makeText(this, "Unable to access Zeo data provider, is Zeo installed?",
                           Toast.LENGTH_LONG).show();
            return null;
        }

        StringBuilder builder = new StringBuilder();
        if (cursor.moveToFirst()) {
            // Write the header
            String delim = "";
            for (String column : projection) {
                builder.append(delim).append(column);
                delim = ",";
            }
            builder.append("\n");

            do {
                // Begin writing data.
                builder.append(
                    cursor.getLong(cursor.getColumnIndex(SleepRecord.LOCALIZED_START_OF_NIGHT)) +
                    ",");
                builder.append(cursor.getLong(cursor.getColumnIndex(SleepRecord.START_OF_NIGHT)) +
                               ",");
                builder.append(cursor.getLong(cursor.getColumnIndex(SleepRecord.END_OF_NIGHT)) +
                               ",");
                builder.append(cursor.getString(cursor.getColumnIndex(SleepRecord.TIMEZONE)) +
                               ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.ZQ_SCORE)) + ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.AWAKENINGS)) + ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_DEEP)) +
                               ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_LIGHT)) +
                               ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_REM)) + ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_IN_WAKE)) +
                               ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.TIME_TO_Z)) + ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.TOTAL_Z)) + ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.SOURCE)) + ",");
                builder.append(cursor.getInt(cursor.getColumnIndex(SleepRecord.END_REASON)) + ",");
                final byte[] displayHypnogram =
                    cursor.getBlob(cursor.getColumnIndex(SleepRecord.DISPLAY_HYPNOGRAM));
                for (byte stage : displayHypnogram) {
                    builder.append(Byte.toString(stage));
                }
                builder.append(",");
                // Output the base hypnogram.
                final byte[] baseHypnogram =
                    cursor.getBlob(cursor.getColumnIndex(SleepRecord.BASE_HYPNOGRAM));
                for (byte stage : baseHypnogram) {
                    builder.append(Byte.toString(stage));
                }
                builder.append("\n");
            } while (cursor.moveToNext());

        } else {
            Log.w(TAG, "No sleep records found.");
            Toast.makeText(this, "No sleep records found in the provider.",
                           Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        return builder.toString();
    }

    /**
     * Write given CSV data to Android external storage using the given filename. Return the file
     * created to the caller if data was successfully written.
     */
    @SuppressWarnings("unused")
	private File writeCsvFile(String csvData, String filename) {
        final String storageState =
            Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(storageState) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)) {
            Toast.makeText(this, "Can not share sleep data as no external storage (SD card?) is available.",
                           Toast.LENGTH_SHORT).show();
            return null;
        }

        File saveDir =
            new File(Environment.getExternalStorageDirectory(),
                     "/Android/data/com.myzeo.android.demos.sleep_to_csv");
        saveDir.mkdirs();
        // Attempt to store CSV data to filesystem.
        File csvFile = new File(saveDir, filename);
        if (csvFile == null) {
            Toast.makeText(this, "Unable to create the csv file needed for transmission of data.",
                           Toast.LENGTH_SHORT).show();
            return null;
        }

        Writer writer;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(csvFile));
        } catch (IOException e) {
            Toast.makeText(this, "Unable to write the csv file needed for transmission of data.",
                           Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            writer.write(csvData);
        } catch (IOException e) {
            Toast.makeText(this, "Failure to write CSV text.", Toast.LENGTH_SHORT).show();
        }

        try {
            writer.close();
        } catch (IOException e) {
            Log.w(TAG, "Failure to close the output stream handle.");
        }

        return csvFile;
    }
}
