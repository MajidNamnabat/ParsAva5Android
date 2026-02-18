package com.khanenoor.parsavatts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.khanenoor.parsavatts.databinding.ActivitySupportBinding;
import com.khanenoor.parsavatts.util.LogUtils;

public class SupportActivity extends AppCompatActivity {
    private static final String TAG = SupportActivity.class.getSimpleName();
    final int REQUEST_CODE_ACTION_REQUEST = 786;
    final int REQUEST_CODE_ACTION_IGNORE_SETTINGS = 674;
    private static final String MANUFACTURER_XIAOMI = "Xiaomi";
    private static final String MANUFACTURER_HUAWEI = "Huawei";

    private ActivitySupportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Button btnPermissionBattery = findViewById(R.id.btnSupportBatteryPermission);
        Button btnShowAppSettings = findViewById(R.id.btnShowAppsSettings);

        btnShowAppSettings.setOnClickListener(mOnClickListener);
        btnPermissionBattery.setOnClickListener(mOnClickListener);
        if (isXiaomiOrHuawei()) {
            findViewById(R.id.xiaomi_notice).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.xiaomi_notice)).setText(R.string.power_save_alarm);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isXiaomiOrHuawei()) {
            isPowerSaveModeHuaweiXiaomi();
        }
    }

    private final View.OnClickListener mOnClickListener = v -> {
        if (v.getId() == R.id.btnSupportBatteryPermission) {
            Context context = getApplicationContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {

                    Intent batterySaverIntent=new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName()));
                    startActivityForResult(batterySaverIntent, REQUEST_CODE_ACTION_REQUEST);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent2 = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                //intent2.setData(Uri.parse("package:" + context.getApplicationContext().getPackageName()));
                if (intent2.resolveActivity(context.getPackageManager()) != null) {
                    SupportActivity.this.startActivityForResult(intent2,REQUEST_CODE_ACTION_IGNORE_SETTINGS);
                }
            }
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int result = ActivityCompat.checkSelfPermission(context, "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                if (result !=  PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + context.getApplicationContext().getPackageName()));
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        ParsAvaActivity.this.startActivity(intent);
                    }
                } */
        }
        if (v.getId() == R.id.btnShowAppsSettings) {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    };
    /*
    In 3850 Version , we have problem with Xiaomi and Huawei mobiles
    https://stackoverflow.com/questions/47145722/how-to-deal-with-huaweis-and-xiaomis-battery-optimizations


    Then come the Huawei and Xiaomi with their own modifications of Android to "improve battery life".


    These can mess with background running apps so badly that after a week or two the app must be reinstalled. Even clearing data will not "unfreeze" the app.

    Also, you can always show a popup to user after installation asking him/her to go and white-list your app in battery saver.
     In Xiaomi, the user will need to select 'Do not restrict background activity' and
     also enable 'Auto Start' for your app. As far as i know, there is no other solution.

     موبایل شما شیائومی یا هوآوی می باشد. برای کارکرد صحیح پارس آوا، لازم است که پارس آوا را جز لیست سفید در بخش ذخیره باتری قرار دهید. اگر موبایل شما شیائومی می باشد، لازم ست که گزینه Do not restrict background activity را نیز انتخاب نمایید و همچنین لازم است که Auto Start برای پارس آوا را فعال سازید.
     */
    private void isPowerSaveModeHuaweiXiaomi() {


        if (Build.MANUFACTURER.equalsIgnoreCase(MANUFACTURER_XIAOMI)) {
            try {
                int value = android.provider.Settings.System.getInt(this.getContentResolver(), "POWER_SAVE_MODE_OPEN");
                if (value == 1) { //active must be deactivate
                    ((TextView) findViewById(R.id.xiaomi_notice)).setText(R.string.power_save_alarm_xiaomi);
                }
            } catch (Settings.SettingNotFoundException e) {
                LogUtils.d("Valor modo bateria:", "Error");
            }
        } else if (Build.MANUFACTURER.equalsIgnoreCase(MANUFACTURER_HUAWEI)) {
            try {
                int value = android.provider.Settings.System.getInt(this.getContentResolver(), "SmartModeStatus");

            } catch (Settings.SettingNotFoundException e) {
                LogUtils.d("Valor modo bateria:", "Error");
            }
        }
    }

    private boolean isXiaomiOrHuawei() {
        return Build.MANUFACTURER.equalsIgnoreCase(MANUFACTURER_XIAOMI)
                || Build.MANUFACTURER.equalsIgnoreCase(MANUFACTURER_HUAWEI);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        {
            if (requestCode == REQUEST_CODE_ACTION_REQUEST && resultCode == RESULT_OK) {
                int sdfsdf = 0;
                sdfsdf = sdfsdf + 1;
            } else if (requestCode == REQUEST_CODE_ACTION_IGNORE_SETTINGS && resultCode == RESULT_OK) {
                int sdsddsd = 0;
                sdsddsd++;

            }

        }
    }
}
