package com.stephenklancher.zeosleepmonitor;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    	Log.v(TAG,"onCreate");
    	

        //initialize the data functions with a context
        ZeoData.initialize(getApplication());
        
    	
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        
    	Log.v(TAG,"onResume");
        
      //start the service if needed
        if(isMyServiceRunning()==false){
        	Log.v(TAG,"Starting service.");
        	startService(new Intent(MainActivity.this,SleepMonitorService.class));
        }
        
        doBindService();
        
        
        if(mIsBound && mBoundService!=null){
        	Log.v(TAG,"Trying to access service.");
        	refreshLog();
        }else{
        	Log.v(TAG,"Service not bound.");
        }
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.refreshLog:
            	refreshLog();
                return true;
                
            case R.id.clearLog:
            	FileOutputStream fos;
        		try {
        			fos = openFileOutput("SleepMonitorLog", Context.MODE_PRIVATE);
        			String msg="Log Deleted.";
        			fos.write(msg.getBytes());
        			fos.close();
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
            	refreshLog();
                return true;
                
            case R.id.send_logs:
            	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        		Date date = new Date();
            	String[] myEmail={"smknight@gmail.com"};
        		
        		
            	Intent sendIntent = new Intent();
            	sendIntent.setAction(Intent.ACTION_SEND);
            	sendIntent.putExtra(Intent.EXTRA_TEXT, ZeoData.getLogText());
            	sendIntent.putExtra(Intent.EXTRA_SUBJECT, "SleepMonitor Logs: " + sdf.format(date));
            	sendIntent.putExtra(Intent.EXTRA_EMAIL, myEmail);
            	sendIntent.setType("text/plain");
            	startActivity(Intent.createChooser(sendIntent, "Send Logs..."));
            	return true;
            	
            case R.id.playAlarm:
            	mBoundService.playAlarm();
            	return true;
            	
            case R.id.menu_settings:       		
            	Intent intent = new Intent(this, SleepMonitorPreferences.class);
            	startActivity(intent);
            	return true;
            	
            default:
                return super.onOptionsItemSelected(item); 
        }
    }
    
    
    private void refreshLog(){
    	mBoundService.refreshNow();
    	
    	TextView mainText = (TextView) findViewById(R.id.mainText);
        String status="";
        status=WakeLogic.alarmStatus() + "\n\n" + ZeoData.currentNight().toString() + "\n\n" + ZeoData.getLogText();
        mainText.setText(status);
    }

    
    //binding
    private boolean mIsBound;
    
    private SleepMonitorService mBoundService;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((SleepMonitorService.LocalBinder)service).getService();
            
            Log.i(TAG,"onServiceConnected");
            
            refreshLog();

            // Tell the user about this for our demo.
            Toast.makeText(MainActivity.this, R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.

            Log.i(TAG,"onServiceDisconnected");
            
            mBoundService = null;
            Toast.makeText(MainActivity.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };
    
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        
    	Log.i(TAG,"doBindService");
        
        bindService(new Intent(MainActivity.this, 
        		SleepMonitorService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    void doUnbindService() {
    	
    	Log.i(TAG,"doUnbindService");
    	
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
    
    
    
    
    
    //http://stackoverflow.com/a/5921190/221018
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SleepMonitorService.class.getName().equals(service.service.getClassName())) {

            	Log.v(TAG,SleepMonitorService.class.getName() + " is running.");
                return true;
            }else{
            	//Log.v(TAG,"Not interested in " + service.service.getClassName());
            }
        }
    	Log.v(TAG,SleepMonitorService.class.getName() + " is not running.");
        return false;
    }
}
