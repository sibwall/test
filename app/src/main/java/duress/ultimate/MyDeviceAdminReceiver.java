package duress.ultimate;

import java.util.Collections;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    private static final String FRP_DISABLED = "frp_disabled";
        
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        disableFRP(context);
    }

    private static boolean isDeviceOwner(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && android.os.Build.VERSION.SDK_INT >= 30 && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(context.getPackageName());
        boolean isOWNER = dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());   
        return isCOPE || isOWNER;
    }
  
    static void disableFRP(Context context) {
           try {
           if (context.getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean(FRP_DISABLED, false)) return;
           DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
           if (!isDeviceOwner(context)) return;
           ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);

           try {
			Intent browserIntent = Intent.makeMainSelectorActivity(
				Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER
			);
		   dpm.enableSystemApp(adminComponent, browserIntent);   
           dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);	
		   dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_APPS);		
		   dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_UNINSTALL_APPS);					
		   dpm.clearUserRestriction(new ComponentName(context, MyDeviceAdminReceiver.class), UserManager.DISALLOW_MODIFY_ACCOUNTS);	
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
