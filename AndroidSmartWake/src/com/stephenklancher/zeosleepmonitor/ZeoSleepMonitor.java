/**
 * 
 */
package com.stephenklancher.zeosleepmonitor;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;


/**
 * @author Stephen Klancher
 *
 */
@ReportsCrashes(formKey = "dHhjQ3Zad09NbDdIbjhLYTRpVzNmV2c6MQ",
				mode = ReportingInteractionMode.TOAST,
				forceCloseDialogAfterToast = false, // optional, default false
				resToastText = R.string.crash_toast_text) 
public class ZeoSleepMonitor extends Application {

	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }

}
