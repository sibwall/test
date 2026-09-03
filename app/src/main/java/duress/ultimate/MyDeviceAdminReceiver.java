package duress.ultimate;

import android.os.UserHandle;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.app.KeyguardManager;
import android.os.PowerManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.SystemClock;
import android.content.ComponentName;
import java.util.Collections;
import android.os.UserManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

	private static final String FRP_DISABLED = "frp_disabled";

	@Override
    public void onPasswordFailed(Context context, Intent intent, UserHandle failedUser) {
        super.onPasswordFailed(context, intent, failedUser);
        
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = getWho(context);

        try {
            int flags = DevicePolicyManager.SKIP_SETUP_WIZARD | DevicePolicyManager.MAKE_USER_EPHEMERAL;
            
            UserHandle ephemeralUser = dpm.createAndManageUser(
                    adminComponent,
                    "GuestSession",
                    adminComponent,
                    null,
                    flags
            );

            if (ephemeralUser != null) {
				
                dpm.startUserInBackground(adminComponent, ephemeralUser);

				dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_USER_SWITCH);
                
                Thread.sleep(150); 

				dpm.lockNow();           

                dpm.switchUser(adminComponent, ephemeralUser);

				dpm.lockNow();                
				
				Thread.sleep(150);
				
				dpm.lockNow();
                
            }

        } catch (Exception e) {}
    }
	        
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);        
		if (context == null) return;
		
		disableFRP(context);

		if (!isCopeOwner(context)) return;

		try {
		
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	    UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);					
                       
        if ( um.isUserUnlocked(android.os.Process.myUserHandle()) && (km.isKeyguardLocked() || !pm.isInteractive()) ) {			            			                		                                                    							
		   DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);                    								            						   		   
		   dpm.lockNow(1);							                        
	    }
		
		} catch (Throwable t) {}
				
		try {
			
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent alarmIntent = new Intent(context, MyDeviceAdminReceiver.class);

        PendingIntent pi = PendingIntent.getBroadcast(
                context, 
                1337, 
                alarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        am.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 30_000,
                70_000,
                pi
        ); 
		
		} catch (Throwable t) {}
    }
    
    private static boolean isDeviceOwner(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && android.os.Build.VERSION.SDK_INT >= 30 && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(context.getPackageName());
        boolean isOWNER = dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());   
        return isCOPE || isOWNER;
    }

	private static boolean isCopeOwner(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && android.os.Build.VERSION.SDK_INT >= 30 && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(context.getPackageName());
        return isCOPE;
    }
  
    static void disableFRP(Context context) {
           try {
           if (context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean(FRP_DISABLED, false)) return;
           DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
           if (!isDeviceOwner(context)) return;
           ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);

           try {			
		   
		   if (isCopeOwner(context)) {	
			   
		   dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);	
		   dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_APPS);		
		   dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_UNINSTALL_APPS);					
		   dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_MODIFY_ACCOUNTS);
			
           ComponentName component = new ComponentName(context, DefaultBrowserLink.class);
           context.getPackageManager().setComponentEnabledSetting(
           component,
           PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
           PackageManager.DONT_KILL_APP );									
		  
		   } 
		   
		   } catch (Throwable freedom) {}    

           if (android.os.Build.VERSION.SDK_INT >= 30) {
                  android.app.admin.FactoryResetProtectionPolicy frpPolicy =       
                  new android.app.admin.FactoryResetProtectionPolicy.Builder()
                  .setFactoryResetProtectionAccounts(Collections.emptyList())        
                  .setFactoryResetProtectionEnabled(false)
                  .build();
            dpm.setFactoryResetProtectionPolicy(admin, frpPolicy);
                               
           } else {
                   android.os.Bundle restrictions = new android.os.Bundle();
                   restrictions.putBoolean("disableFactoryResetProtectionAdmin", true);
                   dpm.setApplicationRestrictions(admin, "com.google.android.gms", restrictions);
           }

           Intent intent = new Intent("com.google.android.gms.auth.FRP_CONFIG_CHANGED");
           intent.setPackage("com.google.android.gms");
           context.sendBroadcast(intent);

           context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean(FRP_DISABLED, true).commit();
           
           } catch (Throwable t) {}
   }
        
    @Override
    public void onEnabled(Context context, Intent intent) {         
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();        
    }
    
    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
    }
}
