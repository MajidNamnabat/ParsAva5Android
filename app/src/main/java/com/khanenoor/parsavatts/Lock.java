package com.khanenoor.parsavatts;

import static android.content.Context.TELEPHONY_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Xml;

import com.khanenoor.parsavatts.impractical.Customer;
import com.khanenoor.parsavatts.impractical.IParsAvaWebService;
import com.khanenoor.parsavatts.util.LogUtils;

import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class Lock {
    public static final boolean IS_CHECKLOCK = false;
    public static final int STYLE_PRIVATE_LICENSE_APP = 3;
    public static final int STYLE_PUBLIC_LICENSE_SERVER_ONLY = 1;
    public static final int STYLE_PUBLIC_LICENSE_SERVER_AND_ENCRYPT = 2;
    public static final int PLATFORM_ANDROID = 2;
    public static final int REQUEST_CODE = 101;
    private static final String BASE_URL = "http://parsava.ir/";
    private static final String LICENSE_FILE_NAME = "ParsAva.lic";
    private static final String TAG = Lock.class.getSimpleName();
    private static Retrofit retrofit;

    /*
    FROM THIS LINK:
    https://stackoverflow.com/questions/55173823/i-am-getting-imei-null-in-android-q
    می گوید که جز پریولیج قرار نگرفته است
    Not sure about IMEI number, but you can get the simSerialNumber and
    other carrier info this way.

    getSimSerialNumber() needs privileged permissions from Android 10 onwards,
    and third party apps can't register this permission.

    See : https://developer.android.com/about/versions/10/privacy/changes#non-resettable-device-ids

    A possible solution is to use the TELEPHONY_SUBSCRIPTION_SERVICE
    from Android 5.1, to retrieve the sim serial number. Steps below:

    Check for READ_PHONE_STATE permission.
    Get Active subscription list.( Returns the list of all active sim cards)

    Retrieve the sim details from Subscription Object.
     *    if ( isPermissionGranted(READ_PHONE_STATE) ) {

        String simSerialNo="";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

            SubscriptionManager subsManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

            List<SubscriptionInfo> subsList = subsManager.getActiveSubscriptionInfoList();

            if (subsList!=null) {
                for (SubscriptionInfo subsInfo : subsList) {
                    if (subsInfo != null) {
                        simSerialNo  = subsInfo.getIccId();
                    }
                }
            }
        } else {
            TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            simSerialNo = tMgr.getSimSerialNumber();
        }
    }
     */
    /*
     * About ANDROID_ID
     * What You need to know is that ANDROID_ID is unique to each combination
     * of app-signing key, user, and device. Values of ANDROID_ID are scoped
     * by signing key and user.
     * The value may change if a factory reset is performed on the device or
     * if an APK signing key changes
     *
     * For apps installed on a device running Android 8.0,
     * the value of ANDROID_ID is now scoped per app-signing key,
     * as well as per user. The value of ANDROID_ID is unique for
     * each combination of the app-signing key, user, and device.
     * As a result, apps with different signing keys running
     * on the same device no longer see the same Android ID
     * (even for the same user).
     */
    /*
     * About imei
     * Furthermore,
     * this solution is limited to smartphones
     * because tablets don’t have telephony services
     */
    /*3. Serial Number

    Devices without telephony services
    like tablets must report a unique device ID
    that is available via android.os.Build.SERIAL
    since Android 2.3 Gingerbread.
    Some phones having telephony services can also define a serial number.
    Like not all Android devices have a Serial Number,
    this solution is not reliable.

    https://stackoverflow.com/questions/4799394/is-secure-android-id-unique-for-each-device
    This link say best way is to use UUID unique random
    */
    /*
     * You cant put this permission for android 10 version.
     * You could change your targetSdkVersion to below 29

    Android 10 changes the permissions
    for device identifiers so that
    all device identifiers are now
    protected by the READ_PRIVILEGED_PHONE_STATE permission.
    The READ_PRIVILEGED_PHONE_STATE permission
    is only granted to apps signed with the platform key
    and privileged system apps.
    اصلا انگار اجازه نمی دهد
     */
    public static String getDeviceID() {
        String hardwareId = Build.BOARD +
                '_' +
                Build.BRAND +
                '_' +
                Build.CPU_ABI +
                '_' +
                Build.DEVICE +
                '_' +
                Build.DISPLAY +
                '_' +
                Build.ID +
                '_' +
                Build.MANUFACTURER +
                '_' +
                Build.MODEL +
                '_' +
                Build.PRODUCT;

        /*
        String devIDSihort = "35" + //we make this look like a valid IMEI
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 digits
        */
        return hardwareId;
    }

    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) hexString.append(Integer.toHexString(0xFF & b));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static native String getEncodeData(String pck_name, String data, int style);

    public static native String getDecodeData(String pck_name, String data, int style);

    //public static native String WriteLicense(String pck_name,String response);
    public static native void SetHardwareAppID(String hardwareAppId, String packageName);

    //public static native void SetLastUpdate(String licenseKey);
    public static native String getLicenseKey();
    //public static native void CreateLicFile(String pck_name, String licenseKey);

    /**
     * Create an instance of Retrofit object
     */
    public static IParsAvaWebService getRetrofitInstance() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            //interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).connectTimeout(60, TimeUnit.SECONDS).addInterceptor(interceptor).build();
            retrofit = new retrofit2.Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(SimpleXmlConverterFactory.createNonStrict(new Persister(new AnnotationStrategy()))).build();

            //SimpleXmlConverterFactory.create()
            //https://medium.com/android-news/working-with-xml-using-retrofit2-21c3af9a0472
        }
        return retrofit.create(IParsAvaWebService.class);
    }

    public static long getChecksumValue(String fname) {
        Checksum checksum = new CRC32();
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(fname));
            byte[] bytes = new byte[1024];
            int len = 0;

            while ((len = is.read(bytes)) >= 0) {
                checksum.update(bytes, 0, len);
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return checksum.getValue();
    }

    public static String getCombineHardAppId(String packageName, Context cnx, String phoneCode, String appLicenseUniqueId) {
        StringBuilder applicationId = new StringBuilder();
        //String phoneCode = getPhoneCode();
        //String phoneCode = getHardwareCode(cnx);
        if (!TextUtils.isEmpty(phoneCode)) {
            applicationId.append("IMEI");
            applicationId.append(phoneCode);
        } else {
            phoneCode = Lock.getDeviceID();
            phoneCode = Lock.md5(phoneCode);
            applicationId.append(phoneCode);
            ///////////Generate Random UUID or get old UUID
            ////////////////////////////////////////////////////////////////////////////

            applicationId.append("_");
            applicationId.append(Lock.md5(appLicenseUniqueId));
            //new AESObfuscator(SALT, getPackageName(), deviceId))
        }

        return applicationId.toString();
    }

    public static String Generate_Or_Get_App_UUID(String packageName, Context cnx, boolean isGenerate) {
        final Preferences prefs = new Preferences(cnx);

        String appEncryptLicationUniqueId = prefs.get(Preferences.APP_UUID_ID, "");
        String appLicenseUniqueId = "";
        if (TextUtils.isEmpty(appEncryptLicationUniqueId)) {
            if (isGenerate) {
                appLicenseUniqueId = UUID.randomUUID().toString();
                //appEncryptLicationUniqueId = Lock.getEncodeData(packageName, appLicenseUniqueId, Lock.STYLE_PRIVATE_LICENSE_APP);
                //prefs.set(Preferences.APP_UUID_ID, appEncryptLicationUniqueId);
            }
        } else {
            appLicenseUniqueId = Lock.getDecodeData(packageName, appEncryptLicationUniqueId, Lock.STYLE_PRIVATE_LICENSE_APP);
        }
        return appLicenseUniqueId;
    }

    @SuppressLint("HardwareIds")
    public static String getHardwareCode(Context cnx) {
        String strImei = "";
        try {
            TelephonyManager telephonyManager = (TelephonyManager) cnx.getSystemService(TELEPHONY_SERVICE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && telephonyManager != null) /* Android 10 Level 29*/ {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) /*API 26*/ {
                    if (telephonyManager.getPhoneCount() == 2) {
                        strImei = telephonyManager.getImei(0);
                    } else {
                        strImei = telephonyManager.getImei();
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && telephonyManager.getPhoneCount() == 2) {
                        strImei = telephonyManager.getDeviceId(0);
                    } else {
                        strImei = telephonyManager.getDeviceId();
                    }
                }
            }
            //imei = telephonyManager.getImei();
        /*
        String uid= Settings.Secure.getString(ctx.applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            imei = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    uid
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    telephonyManager.imei
                }
                else -> {
                    telephonyManager.deviceId
                }
            }
        }
        */
        } catch (Exception ignored) {

        }
        return strImei;

    }

    public static boolean IsLicenseFileExist(String package_name) {
        //v56
        if(!IS_CHECKLOCK) {
            return true;
        }
            long start = System.currentTimeMillis();
        String filePathString = "/data/data/" + package_name + "/" + LICENSE_FILE_NAME;
        try {
            File f = new File(filePathString);
            boolean exists = f.exists() && !f.isDirectory();
            long duration = System.currentTimeMillis() - start;
            if (duration > 200) {
                LogUtils.w(TAG, "License file existence check took " + duration + "ms for " + package_name);
            }
            return exists;
        } catch (SecurityException e) {
            LogUtils.w(TAG, "Unable to access license file due to security restrictions", e);
        } catch (Exception e) {
            LogUtils.w(TAG, "Unexpected error while checking license file", e);
        }
        return false;
    }

    public static void DeleteLicenseFile(String package_name) {
        //For instance check one file is exist or not
        String filePathString = "/data/data/" + package_name + "/" + LICENSE_FILE_NAME;

        File f = new File(filePathString);
        // do something
        if (f.exists() && !f.isDirectory()) {
            boolean result = f.delete();
            //LogUtils.w(TAG,"Lock.DeleteLicenseFile License File is Deleted result:" + result);
        }

    }

    public static boolean IsInternetConnection(Context cnx) {

        ConnectivityManager connectivityManager = (ConnectivityManager) cnx.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean connected = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);
        return connected;
    }

    public static String MakeRequest_GetLicenseKey(String productKey, String encryptHardwareAppId) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "  <s:Body>\n" + "    <GetLicenseKey xmlns=\"http://tempuri.org/\">\n" + "      <productKey>" + productKey + "</productKey>\n" + "      <cipherData>" + encryptHardwareAppId + "</cipherData>\n" + "    </GetLicenseKey>\n" + "  </s:Body>\n" + "</s:Envelope>";

    }

    public static String MakeRequest_GetLicenseKeyV2(String productKey, String encryptHardwareAppId, String cipherVersionId) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "  <s:Body>\n" + "    <GetLicenseKeyV2 xmlns=\"http://tempuri.org/\">\n" + "      <productKey>" + productKey + "</productKey>\n" + "      <cipherData>" + encryptHardwareAppId + "</cipherData>\n" + "      <platformType>" + PLATFORM_ANDROID + "</platformType>\n" + "      <cipherVersionId>" + cipherVersionId + "</cipherVersionId>\n" + "    </GetLicenseKeyV2>\n" + "  </s:Body>\n" + "</s:Envelope>";

    }

    /*
    فرمت ورودی خروجی xml مربوط به وب سرویس parsava را با استفاده از gsoap این فولدر می توان فهید
        E:\Speech\Android_Application\Source\dotnet\WebServiceSite\Webservice\gsoap-win32
     */
    public static String MakeRequest_RegisterV2(Customer cus, String encryptHardwareAppId, String app_uuid, String encryptVersionCode, String architecture, String encryptFile1Sign) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "  <s:Body>\n" + "    <RegisterV2 xmlns=\"http://tempuri.org/\">\n" + "      <customerSpecification xmlns=\"http://tempuri.org/\">\n" + "           <Email xmlns=\"http://schemas.datacontract.org/2004/07/ParsAvaService\">" + cus.Email + "</Email>\n" + "           <FirstName xmlns=\"http://schemas.datacontract.org/2004/07/ParsAvaService\">" + cus.FirstName + "</FirstName>\n" + "           <LastName xmlns=\"http://schemas.datacontract.org/2004/07/ParsAvaService\">" + cus.LastName + "</LastName>\n" + "           <MobileNumber xmlns=\"http://schemas.datacontract.org/2004/07/ParsAvaService\">" + cus.MobileNumber + "</MobileNumber>\n" + "           <ProductKey xmlns=\"http://schemas.datacontract.org/2004/07/ParsAvaService\">" + cus.ProductKey + "</ProductKey>\n" + "       </customerSpecification>\n" + "      <cipherData xmlns=\"http://tempuri.org/\">" + encryptHardwareAppId + "</cipherData>\n" + "      <app_uuid xmlns=\"http://tempuri.org/\">" + app_uuid + "</app_uuid>\n" + "      <platformType xmlns=\"http://tempuri.org/\">" + Lock.PLATFORM_ANDROID + "</platformType>\n" + "      <cipherVersionId xmlns=\"http://tempuri.org/\">" + encryptVersionCode + "</cipherVersionId>\n" + "      <architecture xmlns=\"http://tempuri.org/\">" + architecture + "</architecture>\n" + "      <File1Sign xmlns=\"http://tempuri.org/\">" + encryptFile1Sign + "</File1Sign>\n" +
                //"      <File2Sign xmlns=\"http://tempuri.org/\">" + encryptFile2Sign + "</File2Sign>\n" +
                //"      <File3Sign xmlns=\"http://tempuri.org/\">" + encryptFile3Sign + "</File3Sign>\n" +
                "    </RegisterV2>\n" + "  </s:Body>\n" + "</s:Envelope>";

    }

    public static String Analyse_ResponseXml(String input, String targetTag) {
        XmlPullParser parser = Xml.newPullParser();
        String value = "";
        try {
            String strInput = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + input;
            //LogUtils.w(TAG,"Analyse_ResponseXml Input : " + strInput);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(strInput));
            parser.nextTag();
            String tagName;
            //LogUtils.w(TAG,"tagName : " + tagName);
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                tagName = parser.getName();
                //LogUtils.w(TAG, "Analyse_ResponseXml tagName : " + tagName);
                if (tagName.equalsIgnoreCase(targetTag)) {
                    parser.require(XmlPullParser.START_TAG, "", targetTag);
                    if (parser.next() == XmlPullParser.TEXT) {
                        value = parser.getText();
                        //LogUtils.w(TAG,"Analyse_ResponseXml GetText:"+parser.getText());
                    }
                    //parser.require(XmlPullParser.END_TAG, "", "GetLicenseKeyResult");                                }
                    break;
                }
                parser.nextTag();
                tagName = parser.getName();
                //LogUtils.w(TAG, "Analyse_ResponseXml tagName : " + tagName);
            }
        } catch (XmlPullParserException e) {
            LogUtils.w(TAG, "Analyse_ResponseXml parsing xml exception " + e.getMessage());
        } catch (IOException e) {
            LogUtils.w(TAG, "Analyse_ResponseXml parsing xml exception " + e.getMessage());
        } catch (Exception e) {
            //throw new RuntimeException(e);
            LogUtils.w(TAG, "Analyse_ResponseXml parsing xml exception " + e.getMessage());
        }
        return value;
    }

    public static void CreateLicenseFile(String packageName, String licenseFileContent) {
        final String pathName = "/data/data/" + packageName + "/" + LICENSE_FILE_NAME;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(pathName));
            writer.write(licenseFileContent);

        } catch (IOException e) {
            LogUtils.w(TAG, "CreateLicenseFile Exception " + e.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                LogUtils.w(TAG, "CreateLicenseFile Exception " + e.getMessage());
            }
        }
    }
}
