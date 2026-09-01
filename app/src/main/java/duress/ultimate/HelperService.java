package duress.ultimate;

import android.app.Service;
import android.os.Binder;
import android.app.Notification;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import java.util.List;
import java.util.Locale;

public class HelperService extends Service {	

	@Override 
	public void onCreate() {
    super.onCreate();			
	forceBindAndStart();
	scheduleAlarm();	
	MyDeviceAdminReceiver.Start(this);		
	}    
    	
	private void forceBindAndStart() {
	try {
	Intent serviceIntent2 = new Intent(this, RiderService.class);		
    bindService(serviceIntent2, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT); 
    startService(serviceIntent2);
	} catch (Throwable t) {}    
  }
	
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {}
        @Override
        public void onServiceDisconnected(ComponentName name) {
          forceBindAndStart();
        }
    };


	private void scheduleAlarm() {
    try {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        
        Intent intent = new Intent(this, MyDeviceAdminReceiver.class);                                            
        PendingIntent piRepeating = PendingIntent.getBroadcast(this, 888, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 70000, 70000, piRepeating);
    } catch (Throwable t) {} }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {	
	  return START_STICKY;
    }

	@Override
	public IBinder onBind(Intent intent) {
    return new Binder();
	}
	
    @Override
    public void onDestroy() {        
        MyDeviceAdminReceiver.Start(this);
        super.onDestroy();
    }
}
