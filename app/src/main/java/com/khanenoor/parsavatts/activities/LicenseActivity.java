package com.khanenoor.parsavatts.activities;

import static com.khanenoor.parsavatts.Preferences.APP_PRODUCT_KEY;
import static java.util.Objects.isNull;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.khanenoor.parsavatts.ExtendedApplication;
import com.khanenoor.parsavatts.Lock;
import com.khanenoor.parsavatts.Preferences;
import com.khanenoor.parsavatts.R;
//import com.khanenoor.parsavatts.impractical.Customer;
//import com.khanenoor.parsavatts.impractical.IParsAvaWebService;
import com.khanenoor.parsavatts.util.LogUtils;

import java.io.IOException;



public class LicenseActivity extends Activity {
    private static final String TAG = LicenseActivity.class.getSimpleName();
    private EditText mEdtFirstName = null;
    private EditText mEdtLastName = null;
    private EditText mEdtMobileNumber = null;
    private EditText mEdtEmail = null;
    private EditText mEdtSoftwareKey = null;
    private TextView mLicenseErrors = null;
    private Preferences mPrefs = null;
  //  private IParsAvaWebService mParsWebService = null;
    private boolean mIsRegisterClicked = false;
    /*
    @SuppressLint("PackageManagerGetSignatures")
    private final View.OnClickListener mOnClickListener = v -> {
        mLicenseErrors.setText("");
        if (v.getId() == R.id.license_register) {
            mIsRegisterClicked = true;
            Register();
                } else if (v.getId() == R.id.license_receivelicensekey) {
            mIsRegisterClicked = false;
            GetLicense();
        }
    };
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_license);
        //findViewById(R.id.license_register).setOnClickListener(mOnClickListener);
        //findViewById(R.id.license_receivelicensekey).setOnClickListener(mOnClickListener);
        mEdtFirstName = (EditText) findViewById(R.id.license_first_name);
        mEdtLastName = (EditText) findViewById(R.id.license_last_name);
        mEdtMobileNumber = (EditText) findViewById(R.id.license_mobile);
        mEdtEmail = (EditText) findViewById(R.id.license_email);
        mEdtSoftwareKey = (EditText) findViewById(R.id.license_softwarekey);
        mLicenseErrors = (TextView) findViewById(R.id.license_errors);
        mPrefs = new Preferences(ExtendedApplication.getStorageContext());
    //    mParsWebService = Lock.getRetrofitInstance();
    }

    @SuppressLint("HardwareIds")
    private String getPhoneCode() {

        // in the below line, we are initializing our variables.
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            /*
             * Android Q has restricted to access for both IMEI and serial no.
             *  It is available only for platform and apps with special carrier permission.
             *  Also the permission READ_PRIVILEGED_PHONE_STATE is not available
             * for non platform apps.
             */
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && telephonyManager != null) /* Android 10 Level 29*/ {
                // in the below line, we are checking for permissions
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // if permissions are not provided we are requesting for permissions.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, Lock.REQUEST_CODE);
                }
            }
            return "";//Lock.getHardwareCode(getApplicationContext());
        } catch (Exception ex) {
            LogUtils.w(TAG, "getPhoneCode generate exception ");
        }
        return "";
    }

    // in the below line, we are calling on request permission result method.
    /*
     *  public static String getIMEINumber(@NonNull finla Context context)
            throws SecurityException, NullPointerException {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assert tm != null;
            imei = tm.getImei();
            //this change is for Android 10 as per security concern it will not provide the imei number.
            if (imei == null) {
                imei = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        } else {
            assert tm != null;
            if (tm.getDeviceId() != null && !tm.getDeviceId().equals("000000000000000")) {
                imei = tm.getDeviceId();
            } else {
                imei = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }

        return imei;
    }
     */
    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Lock.REQUEST_CODE) {
            // in the below line, we are checking if permission is granted.
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // if permissions are granted we are displaying below toast message.
                //Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
                if(mIsRegisterClicked){
                    Register();
                } else {
                    GetLicense();
                }
            } else {
                // in the below line, we are displaying toast message
                // if permissions are not granted.
                //Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                String checkValText;
                String errTextDisplay;
                errTextDisplay = getResources().getString(R.string.permission_call_need);
                mLicenseErrors.setText(errTextDisplay);
                FinishProcess();
            }
        }
    }
    */
    /*
    private Customer GetCustomerIfValid() {
        Customer cus = new Customer();
        String errTextDisplay;
        cus.FirstName = mEdtFirstName.getText().toString().trim();
        if (TextUtils.isEmpty(cus.FirstName)) {
            errTextDisplay = getResources().getString(R.string.license_err_firstname_empty);
            mLicenseErrors.setText(errTextDisplay);
        }
        cus.LastName = mEdtLastName.getText().toString().trim();
        if (TextUtils.isEmpty(cus.LastName)) {
            errTextDisplay = getResources().getString(R.string.license_err_lastname_empty);
            mLicenseErrors.setText(errTextDisplay);
        }
        cus.MobileNumber = mEdtMobileNumber.getText().toString().trim();
        if (TextUtils.isEmpty(cus.MobileNumber)) {
            errTextDisplay = getResources().getString(R.string.license_err_mobile_empty);
            mLicenseErrors.setText(errTextDisplay);
        }
        cus.Email = mEdtEmail.getText().toString().trim();
        if (TextUtils.isEmpty(cus.Email)) {
            errTextDisplay = getResources().getString(R.string.license_err_email_empty);
            mLicenseErrors.setText(errTextDisplay);
        }
        cus.ProductKey = mEdtSoftwareKey.getText().toString().trim();
        if (TextUtils.isEmpty(cus.ProductKey)) {
            errTextDisplay = getResources().getString(R.string.license_err_softwarekey_empty);
            mLicenseErrors.setText(errTextDisplay);
        }
        if (mLicenseErrors.getText().length() > 0) {
            findViewById(R.id.license_register).setEnabled(true);
            findViewById(R.id.license_receivelicensekey).setEnabled(true);
            return null;
        }
        return cus;
    }
    */
    /*
    private void Register(){
        final String packageName = getPackageName();
        BeginProcess();
        if (!Lock.IsInternetConnection(this)) {
            mLicenseErrors.setText(R.string.WebService_Error_InternetConnection);
            FinishProcess();
            return;
        }
        String phoneCode = getPhoneCode();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q )  {
            // in the below line, we are checking for permissions
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                RegisterNextGrantPermission(packageName,phoneCode);
            }
        } else {
            RegisterNextGrantPermission(packageName,phoneCode);
        }
        FinishProcess();
    }
    */
    /*
    private void RegisterNextGrantPermission(String packageName,String phoneCode){
        Customer cus = GetCustomerIfValid();
        if (isNull(cus)) {
            FinishProcess();
            return;
        }

        String appLicenseUniqueId = Lock.Generate_Or_Get_App_UUID(packageName, this, true);
        String hardwareAppId = Lock.getCombineHardAppId(packageName, this, phoneCode, appLicenseUniqueId);
        LogUtils.w(TAG, "LicenseActivity hardwareAppId:" + hardwareAppId);
        String encryptHardwareAppId = Lock.getEncodeData(packageName, hardwareAppId, Lock.STYLE_PUBLIC_LICENSE_SERVER_AND_ENCRYPT);
        ////////////////////////////////////////Read Version of Manifest and Encrypt it
        String encryptVersionCode = "";
        try {
            int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;
            encryptVersionCode = Lock.getEncodeData(packageName, String.valueOf(versionCode), Lock.STYLE_PUBLIC_LICENSE_SERVER_ONLY);
        } catch (PackageManager.NameNotFoundException e) {
            //encryptVersionCode ="";
            //throw new RuntimeException(e);
            LogUtils.w(TAG, "LicenseActivity.Register catch exception PackageManager.NameNotFoundException");
        }
        ///////////////////////////////////////Read CRC of libTTsLib.so
        //Problem : the Address of these files are different in different android versions.
        // In Real checking sign of apk is enough, only me can produce this sign
        ///////////////////////////////////////Read CRC of libcryptopp.so
        ///////////////////////////////////// Read Sign of APK Code
        Signature[] sigs = new Signature[0];
        String encryptFile1Sign = "";
        try {
            sigs = getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.w(TAG, "Signature get exception catch");
        }
        if (sigs != null) {
            for (Signature sig : sigs) {
                // log the sig here
                LogUtils.w(TAG, "Signature get: " + sig);
            }
            encryptFile1Sign = Lock.getEncodeData(packageName, sigs[0].toString(), Lock.STYLE_PUBLIC_LICENSE_SERVER_ONLY);
        }
        String requestBodyText = Lock.MakeRequest_RegisterV2(cus, encryptHardwareAppId, appLicenseUniqueId, encryptVersionCode, ExtendedApplication.determineArchName(), encryptFile1Sign);
        RequestBody requestBody = RequestBody.create( requestBodyText,MediaType.parse("text/xml"));
        //LogUtils.w(TAG, requestBodyText);
        //Lock.SetHardwareAppID("asdasd", packageName);
        Call<ResponseBody> retCode = mParsWebService.RegisterV2(requestBody);
        retCode.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                //int statusCode = response.code();
                String strInput = null;
                try {
                    if (response.body() == null) {
                        FinishProcess();
                        return;
                    }
                    strInput = response.body().string();
                } catch (IOException e) {
                    //throw new RuntimeException(e);
                    LogUtils.w(TAG, "LicenseActivity.RegisterV2 exception " + e.getMessage());
                }
                String result = Lock.Analyse_ResponseXml(strInput, "RegisterV2Result");
                //LogUtils.w(TAG, "onResponse response: " + result);
                String[] arrOfSections = result.split("#", 0);


                if (arrOfSections.length > 0 && !arrOfSections[0].equals("0")) {
                    try {
                        //LogUtils.w(TAG, "resultCode: " + arrOfSections[0]);
                        int resultCode = Integer.parseInt(arrOfSections[0]);
                        if (resultCode > 1000 && resultCode < 2000) {
                            String stringId = "WebService_Error_" + arrOfSections[0];
                            @SuppressLint("DiscouragedApi") int resId = getResources().getIdentifier(stringId, "string", packageName);
                            mLicenseErrors.setText(getString(resId));
                        }
                    } catch (Resources.NotFoundException e) {
                        LogUtils.w(TAG, "resource not found " + e.getMessage());
                    } catch (NumberFormatException e) {
                        LogUtils.w(TAG, "resource not found " + e.getMessage());
                    }
                } else {
                    mPrefs.set(APP_PRODUCT_KEY, cus.ProductKey);
                    if (arrOfSections.length > 2 && arrOfSections[1].length() > 0 && arrOfSections[2].length() > 0) {
                        mPrefs.set(Preferences.APP_UUID_ID, arrOfSections[1]);
                        Lock.CreateLicenseFile(packageName, arrOfSections[2]);
                        Lock.SetHardwareAppID(hardwareAppId,packageName);
                    }

                    String errTextDisplay = getResources().getString(R.string.WebService_Register_Succeed);
                    mLicenseErrors.setText(errTextDisplay);
                }
                FinishProcess();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                //EventBus.getDefault().post(new Error("Error: " + t.getMessage()));
                LogUtils.w(TAG, "Call WebService Register failed");
                String errTextDisplay = getResources().getString(R.string.WebService_Register_CallFailed);

                mLicenseErrors.setText(errTextDisplay);
                FinishProcess();
            }
        });
    }
    */
    /*
    private void GetLicense(){
        final String packageName = getPackageName();
        BeginProcess();
        if (!Lock.IsInternetConnection(this)) {
            mLicenseErrors.setText(R.string.WebService_Error_InternetConnection);
            FinishProcess();
            return;
        }

        String phoneCode = getPhoneCode();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q )  {
            // in the below line, we are checking for permissions
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                GetLicenseNextGrantPermission(packageName,phoneCode);
            }
        } else {
            GetLicenseNextGrantPermission(packageName,phoneCode);
        }
        FinishProcess();
    }
    */
    /*
    private void GetLicenseNextGrantPermission(String packageName,String phoneCode){

        String appLicenseUniqueId = Lock.Generate_Or_Get_App_UUID(packageName, this, false);

        String hardwareAppId = Lock.getCombineHardAppId(packageName, this, phoneCode, appLicenseUniqueId);
        //LogUtils.w(TAG, "LicenseActivity.mOnClickListener hardwareAppId:" + hardwareAppId);
        String encryptHardwareAppId = Lock.getEncodeData(packageName, hardwareAppId, Lock.STYLE_PUBLIC_LICENSE_SERVER_AND_ENCRYPT);
        //LogUtils.w(TAG, "LicenseActivity.mOnClickListener encrypted hardwareAppId : " + encryptHardwareAppId);
        String productKey = mEdtSoftwareKey.getText().toString().trim();
        if (TextUtils.isEmpty(productKey)) {
            productKey = mPrefs.get(APP_PRODUCT_KEY, "");
        }
        ////////////////////////////////////////Read Version of Manifest and Encrypt it
        String encryptVersionCode = "";
        try {
            int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;
            encryptVersionCode = Lock.getEncodeData(packageName, String.valueOf(versionCode), Lock.STYLE_PUBLIC_LICENSE_SERVER_ONLY);
        } catch (PackageManager.NameNotFoundException e) {
            //encryptVersionCode ="";
            //throw new RuntimeException(e);
            LogUtils.w(TAG, "LicenseActivity.Register catch exception PackageManager.NameNotFoundException");
        }

        String requestBodyText = Lock.MakeRequest_GetLicenseKeyV2(productKey, encryptHardwareAppId, encryptVersionCode);
        //I suggest
        // Call<ResponseBody> login(@Path("login") String postfix, @Body RequestBody params);
        // ResponseBody use it.
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/xml"), requestBodyText);
        Call<ResponseBody> retCode = mParsWebService.GetLicenseKeyV2(requestBody);
        retCode.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    //GetLicenseKeyElement fdf = response.body();
                    //int statusCode = response.code();
                    //LogUtils.w(TAG, "status code: " + statusCode);
                    String strInput = null;
                    try {
                        if (response.body() == null) {
                            FinishProcess();
                            return;
                        }
                        strInput = response.body().string();
                    } catch (IOException e) {
                        //throw new RuntimeException(e);
                        LogUtils.w(TAG, "LicenseActivity.GetLicenseKey exception " + e.getMessage());
                    }
                    String result = Lock.Analyse_ResponseXml(strInput, "GetLicenseKeyV2Result");
                    //LogUtils.w(TAG, "onResponse response: " + result);


                    String[] arrOfSections = result.split("#", 0);
                    //result 0 : Succeed
                    //result other 0 : Not Found
                    if (arrOfSections.length > 0 && arrOfSections[0].equals("0")) {
                        if (arrOfSections.length > 2 && arrOfSections[2].length() > 0) {
                            Lock.CreateLicenseFile(packageName, arrOfSections[2]);
                            Lock.SetHardwareAppID(hardwareAppId,packageName);
                            String errTextDisplay = getResources().getString(R.string.WebService_ReceiverLicense_Succeed);
                            mLicenseErrors.setText(errTextDisplay);
                        }
                        //} else if(arrOfSections.length > 0 && arrOfSections[0].equals("1031")){
                    } else {
                        try {
                            //LogUtils.w(TAG, "resultCode: " + arrOfSections[0]);
                            int resultCode = Integer.parseInt(arrOfSections[0]);
                            if (resultCode > 1000 && resultCode < 2000) {
                                String stringId = "WebService_Error_" + arrOfSections[0];
                                @SuppressLint("DiscouragedApi") int resId = getResources().getIdentifier(stringId, "string", packageName);
                                mLicenseErrors.setText(getString(resId));
                            }
                        } catch (Resources.NotFoundException e) {
                            LogUtils.w(TAG, "resource not found " + e.getMessage());
                            String errTextDisplay = getResources().getString(R.string.WebService_ReceiverLicense_Failed);
                            mLicenseErrors.setText(errTextDisplay);
                        } catch (NumberFormatException e) {
                            LogUtils.w(TAG, "resource not found " + e.getMessage());
                            String errTextDisplay = getResources().getString(R.string.WebService_ReceiverLicense_Failed);
                            mLicenseErrors.setText(errTextDisplay);

                        }
                    }
                }
                FinishProcess();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.w(TAG, "Call WebService Receive License failed");
                String errTextDisplay = getResources().getString(R.string.WebService_Register_CallFailed);

                mLicenseErrors.setText(errTextDisplay);
                FinishProcess();
            }
        });

    }
    */
    private void BeginProcess(){
        findViewById(R.id.license_register).setEnabled(false);
        findViewById(R.id.license_receivelicensekey).setEnabled(false);

    }
    private void FinishProcess(){
        findViewById(R.id.license_register).setEnabled(true);
        findViewById(R.id.license_receivelicensekey).setEnabled(true);

    }
}
