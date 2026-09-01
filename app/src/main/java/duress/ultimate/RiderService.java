package duress.ultimate;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import java.util.*;
import android.net.Uri;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.media.*;
import android.os.*;
import android.provider.*;
import android.os.storage.*;

public class RiderService extends JobService {  

	private final Handler handler = new Handler(Looper.getMainLooper());

	private final Runnable pollRunnable = new Runnable() {
    @Override
    public void run() {
        serviceMainVoid();
        handler.postDelayed(this, 7000);
    } };

	private void serviceMainVoid() {		
		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if ((km != null && km.isKeyguardLocked()) || (pm != null && !pm.isInteractive())) {			            
			            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);                    					
			            			            
			            UserManager um = (UserManager) getSystemService(USER_SERVICE);					
                        if (um.isUserUnlocked(android.os.Process.myUserHandle())) {                        
                            
							//restart preconfiguration
							MyDeviceAdminReceiver.Start(RiderService.this);
							MyDeviceAdminReceiver.Start(RiderService.this);
							MyDeviceAdminReceiver.Start(RiderService.this);
							//restart preconfiguration

							// Profile protection code
							try {
							dpm.lockNow(1);
							} catch (Throwable t) {}							
							// Profile protection code                        
					}
		}
	}
	
	private static final int PERIODIC_JOB_ID = 1001;
    private static final int DELAYED_JOB_ID = 1002;

    @Override
    public boolean onStartJob(JobParameters params) {        
        scheduleJobs(getApplicationContext());
		return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {                    
		scheduleJobs(getApplicationContext());
		return true;
    }

    public static void scheduleJobs(Context context) {
		try {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) return;

        ComponentName componentName = new ComponentName(context, RiderService.class);

        boolean isPeriodicScheduled = false;
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == PERIODIC_JOB_ID) {
                isPeriodicScheduled = true;
                break;
            }
        }

        if (!isPeriodicScheduled) {
            JobInfo.Builder periodicBuilder = new JobInfo.Builder(PERIODIC_JOB_ID, componentName)
                    .setPeriodic(JobInfo.getMinPeriodMillis())
                    .setPersisted(true)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                periodicBuilder.setRequiresBatteryNotLow(false);
                periodicBuilder.setRequiresStorageNotLow(false);
            }

            jobScheduler.schedule(periodicBuilder.build());
        }

        JobInfo.Builder delayedBuilder = new JobInfo.Builder(DELAYED_JOB_ID, componentName)
                .setMinimumLatency(30 * 1000L)                
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            delayedBuilder.setRequiresBatteryNotLow(false);
            delayedBuilder.setRequiresStorageNotLow(false);
        }

        jobScheduler.schedule(delayedBuilder.build());
		} catch (Throwable t) {}	
    }
				
	@Override
    public final void onCreate() {
        super.onCreate();						
		scheduleJobs(this);
		forceBindAndStart();
		handler.post(pollRunnable);
	}		
		

	private final void forceBindAndStart() {	
		try {	    
			Intent intent = new Intent(this, HelperService.class);   
			bindService(intent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);       
			startService(intent);	
		} catch (Throwable t) {}
	}
    
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public final void onServiceConnected(ComponentName name, IBinder service) {}
        @Override
        public final void onServiceDisconnected(ComponentName name) {
            forceBindAndStart();
        }
    };


    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {    	
    return START_STICKY;
    }

    @Override
    public final void onDestroy() {	
	handler.removeCallbacks(pollRunnable);	
    MyDeviceAdminReceiver.Start(this);		    
	super.onDestroy();
    }
}
