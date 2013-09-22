ZeoSleepMonitor
===============

With Zeo out of business and my headband no longer working (see http://blog.stephenklancher.com/2013/09/goodnight-zeo/), I decided I should release the projects I worked on as-is in case they benefit anyone out there who still has functioning hardware.  

There are two components included here, and a related one forked off of the Zeo Android API.

AndroidSmartWake
----------------
This is basically a reimplementation of the Smart Wake feature that was supposed to already be part of Zeo Mobile for Android. Smart Wake is supposed to wake you up in at the optimum time in your sleep cycle (entering or leaving REM sleep). However after using Zeo for several months I never saw this feature work.  After learning they had an open Android API (https://github.com/zeoeng/zeo-android-api), I made my own version of Smart Wake. It is very limited and very unpolished, but it does work.

ZeoCSVtoGraph
-------------
PHP code to take CSV exported sleep data and display it in the same style of colorful bar graph/timeline used by Zeo.  The Android API included a sample that would export locally stored data to CSV.  I think there may have also been a CSV export option the on the Zeo website.  
* Example output: http://stephenklancher.com/misc/zeo/graph.php.  
* CSV Format Documentation: https://github.com/smklancher/ZeoSleepMonitor/blob/master/Reference/Export%20Data%20Help%20Sheet.pdf?raw=true


ZeoDataToCSV
------------
There was sample code with the API which would export the data to CSV.  I modified the sample code a bit(https://github.com/smklancher/zeo-android-api/compare/patch-1?expand=1#diff-0) to show headband and last sleep event status too.
* APK available here: https://github.com/smklancher/ZeoSleepMonitor/blob/master/Reference/com.myzeo.android.demos.data_to_csv.apk?raw=true


