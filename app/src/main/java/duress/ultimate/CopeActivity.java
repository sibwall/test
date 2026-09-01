package duress.ultimate;

import android.content.pm.LauncherApps;
import android.os.UserHandle;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class CopeActivity extends Activity {

    private static final String PREFS = "prefs";

    private TextView text;
    private AlertDialog deviceOwnerDialog;
    private AlertDialog usbWarningDialog;
    private LinearLayout buttonBox;

    private boolean isEn() { return !Locale.getDefault().getLanguage().equals("ru"); }

    private SharedPreferences getProtectedPrefs() {
        return getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private boolean isDeviceOwner() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && android.os.Build.VERSION.SDK_INT >= 30 && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(getPackageName());
        boolean isOWNER = dpm != null && dpm.isDeviceOwnerApp(getPackageName());   
        return isCOPE || isOWNER;
    }

    private boolean isCopeOwner() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && android.os.Build.VERSION.SDK_INT >= 30 && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(getPackageName());
        return isCOPE;
    }

     private void showDeviceOwnerInstruction() {
        String msg = isEn()
                ? "These features are available only if you have Device Owner rights. To obtain them, you must not have accounts or third-party users on the device. If they exist, delete them or just perform a factory reset.\nThen install this app again and use the adb command to activate Device Owner:\nadb shell dpm set-device-owner duress.ultimate/.MyDeviceAdminReceiver"
                : "Эти функции доступны только если есть права Device Owner, для того чтобы их получить у вас не должно быть аккунтов и сторонних пользователей на устройстве. Если они есть, удалите их или просто сбросьте настройки.\nЗатем установите снова это приложение и используйте adb комманду для активации Device Owner:\nadb shell dpm set-device-owner duress.ultimate/.MyDeviceAdminReceiver";

        StringBuilder fullMsg = new StringBuilder(msg);

        if (Build.VERSION.SDK_INT >= 30) {
            String profileText = isEn()
                    ? "Or, if you don't want to delete accounts from the main profile, you can create a work profile (if you don't have one yet) which will be able to change device-wide policies, using the ADB command:"
                    : "Или если вы не хотите удалять аккаунты из основного профиля, вы можете создать рабочий профиль если у вас его ещё нет который сможет менять политики всего устройства, используя ADB комманду:";

            String pkg = getPackageName();
            String admin = pkg + "/" + MyDeviceAdminReceiver.class.getName();

            String universalCommand =
                    "adb(){ if [ \"$1\" = \"shell\" ]; then shift; fi; \"$@\"; }; " +
                    "USER_ID=$(adb shell pm create-user --profileOf 0 --user-type android.os.usertype.profile.MANAGED WorkProfile | grep -o '[0-9]*$') && " +
                    "adb shell am start-user $USER_ID && " +
                    "adb shell pm install-existing --user $USER_ID " + pkg + " && " +
                    "adb shell dpm set-profile-owner --user $USER_ID " + admin + " && " +
                    "adb shell dpm mark-profile-owner-on-organization-owned-device --user $USER_ID " + admin +
                    " && adb shell am start --user $USER_ID -n " + pkg + "/.EntryActivity";

            fullMsg.append("\n\n").append(profileText).append("\n\n").append(universalCommand);
        }

        final String message = fullMsg.toString();

        deviceOwnerDialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> deviceOwnerDialog = null)
                .create();

        deviceOwnerDialog.setOnShowListener(d -> {
            TextView messageView = deviceOwnerDialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTextIsSelectable(true);
            }
        });

        deviceOwnerDialog.setOnDismissListener(dialog -> deviceOwnerDialog = null);
        deviceOwnerDialog.show();

        Window window = deviceOwnerDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.x = 0;
            params.y = 0;
            window.setAttributes(params);
        }
    }
    
    private void showUsbWarningAlert() {
        if (usbWarningDialog != null && usbWarningDialog.isShowing()) return;

        String alertTitle = isEn() ? "Warning:" : "Предупреждение:";
        String alertMsg = isEn()
                ? "Disabling USB functions occurs at the operating system level and does not affect the low-level logic of the USB port, meaning it does not provide 100% protection.\nThis is just a step towards security.\nIf you want the ability to completely disable the USB port, it is better to use the GrapheneOS operating system."
                : "Отключение USB функций происходит на уровне операционной системы и не затрагивает низкоуровневую логику USB порта, тоесть не даёт 100-процентной защиты.\nЭто лишь шаг к безопасности.\nЕсли вы хотите возможность полного отключения USB порта, лучше использовать операционную систему GrapheneOS.";

        usbWarningDialog = new AlertDialog.Builder(this)
                .setTitle(alertTitle)
                .setMessage(alertMsg)
                .setPositiveButton("OK", (dialog, which) -> usbWarningDialog = null)
                .create();

        Window window = usbWarningDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.x = 0;
            params.y = 0;
            window.setAttributes(params);
        }

        usbWarningDialog.setOnShowListener(dialog -> {
            TextView titleView = usbWarningDialog.findViewById(getResources().getIdentifier("alertTitle", "id", "android"));
            if (titleView != null) {
                titleView.setTextColor(Color.parseColor("#ff5555"));
            }
            TextView msgView = usbWarningDialog.findViewById(android.R.id.message);
            if (msgView != null) {
                msgView.setTextIsSelectable(true);
            }
        });

        usbWarningDialog.setOnDismissListener(dialog -> usbWarningDialog = null);
        usbWarningDialog.show();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        launchWorkProfileDelayed();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);

        text = new TextView(this);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setTextSize(16f);
        text.setTextColor(Color.WHITE);

        buttonBox = new LinearLayout(this);
        buttonBox.setOrientation(LinearLayout.VERTICAL);
        buttonBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, 64, 0, 0);
        buttonBox.setLayoutParams(boxParams);

        root.addView(text);
        root.addView(buttonBox);
        scrollView.addView(root);
        setContentView(scrollView);

        renderMainSettingsMenu();
    }

    private void render(String textValue) { text.setText(textValue); }


    private void renderMainSettingsMenu() {
    buttonBox.removeAllViews();

    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName adminName = new ComponentName(this, MyDeviceAdminReceiver.class);
    boolean isDO = isDeviceOwner();
    DevicePolicyManager parentDpm = isCopeOwner() ? dpm.getParentProfileInstance(adminName) : null;

    render(isEn() ? "Settings" : "Настройки");
    
    if (Build.VERSION.SDK_INT >= 31) {
        CheckBox cbUsbAndDebug = new CheckBox(this);
        cbUsbAndDebug.setText(isEn() ? "Disallow USB-connetions and debugging features" : "Запретить USB-подключения и функции отладки");
        cbUsbAndDebug.setTextColor(Color.WHITE);
        cbUsbAndDebug.setTextSize(16f);

        if (isDO) {
            boolean usbDataDisabled = !dpm.isUsbDataSignalingEnabled();
            Bundle restrictions = dpm.getUserRestrictions(adminName);
            boolean usbFileTransferDisabled = restrictions.getBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER, false);
            boolean adbDisabled = restrictions.getBoolean(UserManager.DISALLOW_DEBUGGING_FEATURES, false);

            cbUsbAndDebug.setChecked(usbDataDisabled && usbFileTransferDisabled && adbDisabled);
        } else {
            cbUsbAndDebug.setChecked(false);
            cbUsbAndDebug.setAlpha(0.5f);
        }

        cbUsbAndDebug.setOnClickListener(v -> {
            if (!isDO) {
                cbUsbAndDebug.setChecked(false);
                showDeviceOwnerInstruction();
                return;
            }
            if (cbUsbAndDebug.isChecked()) {                 
                    dpm.setUsbDataSignalingEnabled(false);        
                    if (parentDpm != null) {             
                        parentDpm.addUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER);             
                        parentDpm.addUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES);        
                    } else {
                        dpm.addUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER);       
                        dpm.addUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES);                           
                    }
                    showUsbWarningAlert();                                               
            } else {
                dpm.setUsbDataSignalingEnabled(true);
                if (parentDpm != null) {    
                    parentDpm.clearUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER);    
                    parentDpm.clearUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES);
                } else {
                    dpm.clearUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER);
                    dpm.clearUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES);                
                }
            }
        });
        buttonBox.addView(cbUsbAndDebug);
    }

    CheckBox cbRestrictions1 = new CheckBox(this);
    cbRestrictions1.setText(isEn() ? "Disallow autofill, backup, and mount physical media" : "Запретить автозаполнение, бэкап и монтирование физических носителей");
    cbRestrictions1.setTextColor(Color.WHITE);
    cbRestrictions1.setTextSize(16f);

    if (isDO) {
        Bundle restrictions = dpm.getUserRestrictions(adminName);
        boolean autofillDisabled = restrictions.getBoolean(UserManager.DISALLOW_AUTOFILL, false);
        boolean mountMediaDisabled = restrictions.getBoolean(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, false);
        boolean backupEnabled = dpm.isBackupServiceEnabled(adminName);

        cbRestrictions1.setChecked(autofillDisabled && mountMediaDisabled && !backupEnabled);
    } else {
        cbRestrictions1.setChecked(false);
        cbRestrictions1.setAlpha(0.5f);
    }

    cbRestrictions1.setOnClickListener(v -> {
        if (!isDO) {
            cbRestrictions1.setChecked(false);
            showDeviceOwnerInstruction();
            return;
        }
        if (cbRestrictions1.isChecked()) {   
            try {      
                dpm.setBackupServiceEnabled(adminName, false);
                if (parentDpm != null) {             
                    parentDpm.addUserRestriction(adminName, UserManager.DISALLOW_AUTOFILL);         
                    parentDpm.addUserRestriction(adminName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);          
                    parentDpm.setBackupServiceEnabled(adminName, false);     
                } else {
                   //dpm.addUserRestriction(adminName, UserManager.DISALLOW_AUTOFILL);                                          
                   dpm.addUserRestriction(adminName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);                        
                }                               
            } catch (Exception e) {        
                new AlertDialog.Builder(this)
                .setMessage(e.getLocalizedMessage())
                .setPositiveButton("ОК", null)
                .show();   
            }
        } else {
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_AUTOFILL);
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);
            dpm.setBackupServiceEnabled(adminName, true);
            if (parentDpm != null) {    
                parentDpm.clearUserRestriction(adminName, UserManager.DISALLOW_AUTOFILL);    
                parentDpm.clearUserRestriction(adminName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);  
                parentDpm.setBackupServiceEnabled(adminName, true);
            }
        }
    });
    buttonBox.addView(cbRestrictions1);

    boolean isGranted = dpm != null && dpm.hasGrantedPolicy(new ComponentName(this, MyDeviceAdminReceiver.class), DeviceAdminInfo.USES_POLICY_DISABLE_KEYGUARD_FEATURES);

    CheckBox cbRestrictions2 = new CheckBox(this);
    cbRestrictions2.setText(isEn() ? "Disallow trust agents and biometric unlock" : "Запретить агентов доверия и биометрию");
    cbRestrictions2.setTextColor(Color.WHITE);
    cbRestrictions2.setTextSize(16f);

    if (isDO && isGranted) {
        int disabledFeatures = dpm.getKeyguardDisabledFeatures(adminName);
        boolean trustAgentsDisabled = (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS) != 0;
        boolean biometricsDisabled = (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS) != 0;

        cbRestrictions2.setChecked(trustAgentsDisabled && biometricsDisabled);
    } else {
        cbRestrictions2.setChecked(false);
        cbRestrictions2.setAlpha(0.5f);
    }

    cbRestrictions2.setOnClickListener(v -> {
        if (!isDO || !isGranted) {
            cbRestrictions2.setChecked(false);
            showDeviceOwnerInstruction();
            return;
        }
        int currentFeatures = dpm.getKeyguardDisabledFeatures(adminName);
        if (cbRestrictions2.isChecked()) {
            int newFeatures = currentFeatures | DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS
                    | DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS;
            dpm.setKeyguardDisabledFeatures(adminName, newFeatures);
            if (parentDpm != null) {    
                int pcur = parentDpm.getKeyguardDisabledFeatures(adminName);    
                parentDpm.setKeyguardDisabledFeatures(adminName, pcur | DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS | DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS);
            }
        } else {
            int newFeatures = currentFeatures & ~DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS
                    & ~DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS;
            dpm.setKeyguardDisabledFeatures(adminName, newFeatures);
            if (parentDpm != null) {
                int pcur = parentDpm.getKeyguardDisabledFeatures(adminName);
                parentDpm.setKeyguardDisabledFeatures(adminName, pcur & ~DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS & ~DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS);
            }
        }
    });
    buttonBox.addView(cbRestrictions2);
    }

    @Override
    protected void onDestroy() {
        if (deviceOwnerDialog != null && deviceOwnerDialog.isShowing()) {
            deviceOwnerDialog.dismiss();
        }
        deviceOwnerDialog = null;

        if (usbWarningDialog != null && usbWarningDialog.isShowing()) {
            usbWarningDialog.dismiss();
        }
        usbWarningDialog = null;

        super.onDestroy();
    }
   
    private void launchWorkProfileDelayed() {

        try {
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            
            if (launcherApps != null && userManager != null) {
                List<UserHandle> profiles = userManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                   if (userManager.getSerialNumberForUser(profile) != 0) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), MainActivity.class.getName()), 
                            profile, null, null
                        );
                                                
                        break;
                    }
                }
            }
        } catch (Throwable t) {}
    }
}
