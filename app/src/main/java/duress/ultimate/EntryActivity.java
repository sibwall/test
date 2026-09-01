package duress.ultimate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.app.KeyguardManager;

public class EntryActivity extends Activity {

    static boolean isLogged=true;

	private boolean isCopeOwner() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && android.os.Build.VERSION.SDK_INT >= 30 && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(getPackageName());
        return isCOPE;
    }
	
    @Override
    protected void onCreate(Bundle b) {		
        super.onCreate(b);      
		isLogged=false;
		KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()) {		
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);         
        startActivityForResult(intent, 1337);        
        } else {
		navigateToMainActivity();
        }
    }
	
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 1337) {
			if (resultCode == RESULT_OK) {			
				navigateToMainActivity();
			} else {
                isLogged=false;	
				finishAndRemoveTask();
			}
		}
	}

	private void navigateToMainActivity() {        
        isLogged=true;
		if (!isCopeOwner()) {
		    startActivity(new Intent(this, MainActivity.class));
		} else {
			startActivity(new Intent(this, CopeActivity.class));		
		}
        finish();
    }

}
