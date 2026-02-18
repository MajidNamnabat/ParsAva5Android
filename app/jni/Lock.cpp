#include <android/log.h>
#include "Lock.h"
#include <sys/time.h>
#include <cstdio>
#include <ctime>
#include <string>
#include "Steganography.h"
#include <filesystem>
#include <fstream>
#include <sstream>
#include "GlobalTools.h"
#include <random>
#include <utility>

const char PUBLICSERVER_STEGANOGRAPHY_KEY[] = "234";
//const char PUBLICLIC_STEGANOGRAPHY_KEY[] = "571"; //\\ic_action_light.bmp
const char PRIVATELIC_STEGANOGRAPHY_KEY[] = "826";//ic_action_dark.bmp
const char SIMPLE_ENCRYPTION_KEY = 0x56;
const std::string LICENSE_FILE_NAME = "ParsAva.lic";

static std::string mStrHardwareAppId = "";
static std::string mPackageName = "";
//static std::string mLicenseCheckedDate = "";
//static std::string mLastLicenseKey = "";
static bool mIsFirstLockChecked = false;

bool IsAutherize() {
    struct timeval current_time{};
    time_t t;
    struct tm *info;
    char buffer[128];
    /*
    gettimeofday(&current_time, nullptr);
    printf("seconds : %ld\n micro seconds : %ld",
           current_time.tv_sec, current_time.tv_usec );
    info = localtime(&t);

    printf("%s",asctime (info));
    strftime (buffer, sizeof buffer, "Today is %A, %B %d.\n", info);
    printf("%s",buffer);
    strftime (buffer, sizeof buffer, "The time is %I:%M %p.\n", info);
    printf("%s",buffer);
    */
    struct tm enddate{.tm_min = 1, .tm_hour = 1, .tm_mday = 10, // one based
            .tm_mon  = 07, // zero based
            .tm_year = 123, // year - 1900
    };
    time_t nowUTC = std::time(nullptr);
    std::tm *nowLocal = std::localtime(&nowUTC);
    std::time_t nowLocalConverted = std::mktime(nowLocal);
    //time_t ct = time(0);
    strftime(buffer, sizeof buffer, "Today is %A, %B %d.\nThe time is %I:%M %p.\n", nowLocal);
    std::string strNow = buffer;
    /*
    __android_log_print(ANDROID_LOG_WARN, "com.khanenoor.parsavatts.ttslib",
                        "Lock::IsAutherize Now:%s" , strNow.c_str());
    */
    time_t licenseDateUTC = std::mktime(&enddate); // convert to a time_t
    std::tm *licenseDateLocal = std::localtime(&licenseDateUTC);
    strftime(buffer, sizeof buffer, "License is %A, %B %d.\nThe time is %I:%M %p.\n",
             licenseDateLocal);
    std::string strlicenseDate = buffer;
    std::time_t nowLicenseDateLocalConverted = std::mktime(licenseDateLocal);
    /*
    __android_log_print(ANDROID_LOG_WARN, "com.khanenoor.parsavatts.ttslib",
                        "Lock::IsAutherize LicenseDate: %s",strlicenseDate.c_str());
    */
    if (nowLocalConverted == -1 || nowLicenseDateLocalConverted == -1) {
        CGlobalTools::AndroidLogPrint(ANDROID_LOG_WARN, "com.khanenoor.parsavatts.ttslib",
                            "Lock::IsAutherize At least one of the date couldn't be converted");
    } else {
        //first normally after
        double fsecondsElapsed = std::difftime(nowLicenseDateLocalConverted, nowLocalConverted);
        if (fsecondsElapsed > 0)
            return true;
        /*if (nowLocal < licenseDateLocal) {                // compare the time_t's
            return true;
        }*/
    }

    return false;

}
/*style
// 1 : use public server key
   2 : Simple Encrypt then use public server key
   3 : Use private app key
*/
extern "C" JNIEXPORT  jstring   JNICALL
Java_com_khanenoor_parsavatts_Lock_getEncodeData(JNIEnv *env, jclass jclass, jstring pck_name,
                                                 jstring data, jint style) {
    jstring jstrCipher = nullptr;
    std::string strCipher;
    try {
        char *utf8_pckname_in_str = (char *) env->GetStringUTFChars(pck_name, nullptr);

        // Check if the call to GetStringUTFChars was successful.
        if (utf8_pckname_in_str == nullptr) {
            // Handle the error.
        }
        char *utf8_data_in_str = (char *) env->GetStringUTFChars(data, nullptr);

        // Check if the call to GetStringUTFChars was successful.
        if (utf8_data_in_str == nullptr) {
            // Handle the error.
        }
        std::string strPck = utf8_pckname_in_str;
        std::string strData = utf8_data_in_str;
        strCipher = getEncodeData(strPck, strData, style);
        // Release the native memory that was allocated by GetStringUTFChars.
        env->ReleaseStringUTFChars(pck_name, utf8_pckname_in_str);
        env->ReleaseStringUTFChars(data, utf8_data_in_str);
    } catch (...) {
        CGlobalTools::AndroidLog("Lock::getEncodeData catch unknown exception");
        strCipher = "";
    }
    jstrCipher = env->NewStringUTF(strCipher.c_str());
    return jstrCipher;
}

std::string getEncodeData(std::string &pck_name, std::string &data, int style) {
    std::string strData;
    std::string strPublicKey;
    std::string strCipher="";
    std::string strL2Data;

    try {
        if (style == 2) {
            strL2Data = SimpleEncrypt(data.c_str());
        } else {
            strL2Data = data.c_str();
        }

        std::string path;
        char key[4];
        if (style == 1 || style == 2) {
            path = "/data/data/" + pck_name + "/parsava_icon.bmp";
            strcpy(key, PUBLICSERVER_STEGANOGRAPHY_KEY);
        } /*else if (style == 3) {
            path = "/data/data/" + (std::string) pck_name + "/ic_action_light.bmp";
            strcpy(key,PUBLICLIC_STEGANOGRAPHY_KEY);

        }*/
        strPublicKey = readKey(path, key);
        if(strPublicKey==""){
            return "";
        }

        /*
         * CGlobalTools::AndroidLog(
                "Lock::getEncodeData style:%d path:%s KEY:%s strPublicKey:%s strData:%s", style,
                path.c_str(), key, strPublicKey.c_str(),
                strL2Data.c_str());
        */
         if (strPublicKey.length() > 0) {
            // strCipher = EncodeData(strPublicKey, strL2Data);
            strCipher = "";
           // CGlobalTools::AndroidLog("Lock::getEncodeData strCipher:%s ", strCipher.c_str());
        }
    } catch (...) {
        CGlobalTools::AndroidLog("Lock::getEncodeData internal catch unknown exception");
        strCipher = "";
    }
    return strCipher;
}

/*style
// 1 : use public server key
// 2
 // 3 " use public app key
 */
std::string getDecodeData(std::string pck_name, std::string encryptedData, int style) {
    std::string strPrivateKey;
    std::string strData;
    if (style == 3 || style == 2) {
        std::string path = "/data/data/" + (std::string) pck_name + "/ic_action_dark.bmp";
        strPrivateKey = readKey(path, PRIVATELIC_STEGANOGRAPHY_KEY);
        if(strPrivateKey==""){
            return "";
        }

        //CGlobalTools::AndroidLog("Lock::getDecodeData strPrivateKey:%s encryptedData:%s", strPrivateKey.c_str(),encryptedData.c_str());
    }
    // strData = DecodeData(strPrivateKey, encryptedData);
    strData = "";
    //CGlobalTools::AndroidLog("Lock::getDecodeData strData:%s",strData.c_str());
    /*
    if(style==2){
        strData = SimpleDecrypt(strData);
    }*/
    return strData;
}

extern "C" JNIEXPORT  jstring   JNICALL
Java_com_khanenoor_parsavatts_Lock_getDecodeData(JNIEnv *env, jclass jclass, jstring pck_name,
                                                 jstring dataEncrypt, jint style) {
    jstring jstrData = nullptr;
    std::string strDataEncrypt;
    std::string strData;

    try {
        char *utf8_pckname_in_str = (char *) env->GetStringUTFChars(pck_name, nullptr);

        // Check if the call to GetStringUTFChars was successful.
        if (utf8_pckname_in_str == nullptr) {
            // Handle the error.
        }
        char *utf8_data_in_str = (char *) env->GetStringUTFChars(dataEncrypt, nullptr);

        // Check if the call to GetStringUTFChars was successful.
        if (utf8_data_in_str == nullptr) {
            // Handle the error.
        }
        strDataEncrypt = utf8_data_in_str;
        strData = getDecodeData(utf8_pckname_in_str, strDataEncrypt, style);
        // Release the native memory that was allocated by GetStringUTFChars.
        env->ReleaseStringUTFChars(pck_name, utf8_pckname_in_str);
        env->ReleaseStringUTFChars(dataEncrypt, utf8_data_in_str);
    } catch (...) {
        CGlobalTools::AndroidLog("Lock::getDecodeData catch unknown exception");
    }
    jstrData = env->NewStringUTF(strData.c_str());
    return jstrData;

}

char *readImageFile(const std::string &path, std::streamsize &fileSize) {
    std::ifstream sizeFile(path.c_str(), ios::binary);
    if(!sizeFile.good()){
        sizeFile.close();
        return nullptr;
    }
    const auto begin = sizeFile.tellg();
    sizeFile.seekg(0, ios::end);
    const auto end = sizeFile.tellg();
    const auto fsize = (std::streamsize) (end - begin);
    fileSize = fsize;
    char *image = (char *) malloc(fsize * sizeof(char));
    sizeFile.seekg(0, ios::beg);
    if (sizeFile.read(image, fsize)) {
        /* worked! */
    }
    //CGlobalTools::AndroidLog("readImageFile fileSize:%d path:%s", fileSize, path.c_str());
    sizeFile.close();
    return image;
}

std::string readKey(const std::string &path, const char *stKey) {
    std::string strKey="";
    std::streamsize fileSize = 0;
    char *image = readImageFile(path, fileSize);
    if(image== nullptr){
        return strKey;
    }
    CSteganography m_ImageSteg;
    strKey = m_ImageSteg.Decode(image, fileSize, (char *) stKey);

    free(image);
    image = nullptr;
    return strKey;

}
/*
extern "C" JNIEXPORT  jstring   JNICALL
Java_com_khanenoor_parsavatts_Lock_WriteLicense(JNIEnv *env, jclass jclassobj,
                                                                      jstring pck_name,
                                                                      jstring response) {
    jstring jstrLicenseKey;
    try {
        if (response == nullptr) {
            jstrLicenseKey = env->NewStringUTF("1050");
            return jstrLicenseKey;
        }
        //1 Public Server License

        jstring decryptResponse = Java_com_khanenoor_parsavatts_Lock_getDecodeData(env, jclassobj,
                                                                                   pck_name,
                                                                                   response, 1);
        char *utf8_dec_res_in_str = (char *) env->GetStringUTFChars(decryptResponse, nullptr);
        // Check if the call to GetStringUTFChars was successful.
        if (utf8_dec_res_in_str == nullptr) {
            // Handle the error.
        }

        istringstream f(utf8_dec_res_in_str);
        string returnCode = "";
        getline(f, returnCode, '#');
        if (returnCode != "0") {
            env->ReleaseStringUTFChars(decryptResponse, utf8_dec_res_in_str);
            jstrLicenseKey = env->NewStringUTF(returnCode.c_str());
            return jstrLicenseKey;
        } else {
            char *utf8_pckname_in_str = (char *) env->GetStringUTFChars(pck_name, nullptr);

            // Check if the call to GetStringUTFChars was successful.
            if (utf8_pckname_in_str == nullptr) {
                // Handle the error.
            }
            string hardwareAppId;
            getline(f, hardwareAppId, '#');
            string platformType;
            getline(f, platformType, '#');
            string licenseData;
            getline(f, licenseData, '#');
            string licenseKey;
            getline(f, licenseKey, '#');

            std::string strPck = utf8_pckname_in_str;

            CreateLicenseFile(strPck, licenseKey, licenseData);

            CGlobalTools::AndroidLog("parsava.lic created successfully");
            std::string strReturnFunc = "0#" + licenseKey;
            jstrLicenseKey = env->NewStringUTF(strReturnFunc.c_str());

            env->ReleaseStringUTFChars(decryptResponse, utf8_dec_res_in_str);
            // Release the native memory that was allocated by GetStringUTFChars.
            env->ReleaseStringUTFChars(pck_name, utf8_pckname_in_str);

        }
    } catch (...) {
        CGlobalTools::AndroidLog("Lock::getSignNativeCode catch unknown exception");
    }
    return jstrLicenseKey;
}
*/
/*
extern "C" JNIEXPORT  void   JNICALL
Java_com_khanenoor_parsavatts_Lock_CreateLicFile(JNIEnv *env, jclass jclassobj, jstring pck_name,
                                                 jstring licenseKey) {
    try {
        char *utf8_license_key_in_str = (char *) env->GetStringUTFChars(licenseKey, nullptr);
        // Check if the call to GetStringUTFChars was successful.
        if (utf8_license_key_in_str == nullptr) {
            // Handle the error.
        }
        char *utf8_pckname_in_str = (char *) env->GetStringUTFChars(pck_name, nullptr);

        // Check if the call to GetStringUTFChars was successful.
        if (utf8_pckname_in_str == nullptr) {
            // Handle the error.
        }

        time_t nowUTC = std::time(nullptr);
        std::tm *nowLocal = std::localtime(&nowUTC);
        std::time_t nowLocalConverted = std::mktime(nowLocal);
        //time_t ct = time(0);
        char buffer[128];
        strftime(buffer, sizeof buffer, "Today is %A, %B %d.\nThe time is %I:%M %p.\n", nowLocal);

        CreateLicenseFile((std::string) utf8_pckname_in_str, (std::string) utf8_license_key_in_str,
                          (std::string) buffer);
        // Release the native memory that was allocated by GetStringUTFChars.
        env->ReleaseStringUTFChars(licenseKey, utf8_license_key_in_str);
        env->ReleaseStringUTFChars(pck_name, utf8_pckname_in_str);
    } catch (...) {
        CGlobalTools::AndroidLog("Lock::CreateLicFile catch unknown exception");
    }

}
void
CreateLicenseFile(std::string packageName, std::string licenseKey, std::string licenseCheckDate) {
    std::string strFileLicenseContent = licenseKey + "#" + licenseCheckDate;
    std::string encryptLicense = getEncodeData(packageName, strFileLicenseContent, 3);

    std::string path = "/data/data/" + packageName + "/parsava.lic";
    std::remove(path.c_str());
    std::ofstream datafile(path, std::ios_base::binary | std::ios_base::out);
    datafile.write(encryptLicense.c_str(), (int) encryptLicense.length());
    datafile.close();

}
*/

extern "C" JNIEXPORT  void   JNICALL
Java_com_khanenoor_parsavatts_Lock_SetHardwareAppID(JNIEnv *env, jclass jclassobj,
                                                    jstring hardwareAppId, jstring pck_name) {
    try {
        char *utf8_hardware_appid_in_str = (char *) env->GetStringUTFChars(hardwareAppId, nullptr);
        // Check if the call to GetStringUTFChars was successful.
        if (utf8_hardware_appid_in_str == nullptr) {
            // Handle the error.
        }
        char *utf8_pckname_in_str = (char *) env->GetStringUTFChars(pck_name, nullptr);
        // Check if the call to GetStringUTFChars was successful.
        if (utf8_pckname_in_str == nullptr) {
            // Handle the error.
        }

        mStrHardwareAppId = utf8_hardware_appid_in_str;
        mPackageName = utf8_pckname_in_str;
        // Release the native memory that was allocated by GetStringUTFChars.
        /*std::string strTest = "Test";
        std::string strTestEncoded = getEncodeData((std::string&) mPackageName, strTest, 4);
        std::string strTestDecoded = getDecodeData((std::string) mPackageName,
                                                   strTestEncoded, 3);
        CGlobalTools::AndroidLog("SetHardwareAppId Decoded %s", strTestDecoded.c_str());
        */
        env->ReleaseStringUTFChars(hardwareAppId, utf8_hardware_appid_in_str);
        env->ReleaseStringUTFChars(pck_name, utf8_pckname_in_str);

    } catch (...) {
        CGlobalTools::AndroidLog("Lock::SetHardwareAppID catch unknown exception");
    }

}

/*
 * style
 * 1 : simple
 * 2 : accurate
 */
bool checkLicense(int style) {
    if (mIsFirstLockChecked) {
        //CGlobalTools::AndroidLog("mIsFirstLockChecked==true");
        return true;
    }

    std::string path = "/data/data/" + mPackageName + "/" + LICENSE_FILE_NAME;

    if (style == 1) {
        ifstream f(path.c_str());
        bool isGoodLicFile = f.good();
        f.close();
        return isGoodLicFile;
        
    }
    std::string licenseData = "";
    try {
        std::string licenseKey = readLicenseFile(path, licenseData);
        /*
        CGlobalTools::AndroidLog("checkLicense licenseKey:%s,liceseData:%s", licenseKey.c_str(),
                                 licenseData.c_str());
        */
         if(licenseKey=="" || licenseData==""){
            CGlobalTools::AndroidLog("checkLicense ParsAva.lic not exist");
            return false;
        }

        //I have doubt about this code , in license file , license key must stored not hardwareAppId,
        //so verifier of cryptopp must called
        //BUGGY YOU MUST VERIFY ONLY
        //EXTRACT PUBLIC KEY
        path = "/data/data/" + mPackageName + "/parsava_icon.bmp";
        std::string strPublicKey = readKey(path, PUBLICSERVER_STEGANOGRAPHY_KEY);
        if(strPublicKey==""){
            return false;
        }
        // std::string strIsValid = VData(strPublicKey, mStrHardwareAppId, licenseKey);
        std::string strIsValid = "-1";
        /*
        CGlobalTools::AndroidLog("checkLicense mStrHardwareAppId:%s , VData result:%s",
                                 mStrHardwareAppId.c_str(), strIsValid.c_str());
        */
        int nIsValid = std::stoi(strIsValid);
        //std::string hardwareAppId = getDecodeData(mPackageName, licenseKey, 1); //PUBLIC SERVER
        if (nIsValid < 0) {
            //if(isValid){
            //path = "/data/data/" + mPackageName + "/" + LICENSE_FILE_NAME;
            //Must Delete the file? It is not valid ?
            //May be one time become invalid , so It is not need to delete the file
            /*
            int ret_code = std::remove(path.c_str());
            if (ret_code == 0) {
                CGlobalTools::AndroidLog("ParsAva.lic was successfully deleted");
            } else {
                CGlobalTools::AndroidLog("Error during the deletion: ParsAva.lic");
            }*/
            return false;
        } else {
            DeleteExpiredLic(licenseData);
            mIsFirstLockChecked = true;
            return true;
        }
        /*
        if (mLastLicenseKey != "") {
            UpdateLicenseFile(licenseData);
            mLastLicenseKey = "";
            mLicenseCheckedDate = "";
        }
        */
    }
    catch (...) {
        CGlobalTools::AndroidLog("CheckLicense Unknown Exception Occurred");
        return false;
    }
}

/*
void UpdateLicenseFile(std::string licenseData) {


    /////////////////////////////////////////////////////////////
    CGlobalTools::AndroidLog("UpdateLicenseFile : %s", licenseData.c_str());
    struct std::tm tmLic;
    std::istringstream ssLic(licenseData);
    ssLic >> std::get_time(&tmLic, "%Y-%m-%dT%H:%M:%SZ"); // or just %T in this case
    std::time_t timeLic = mktime(&tmLic);

    struct std::tm tmNewLic;
    std::istringstream ssNewLic(mLicenseCheckedDate);
    ssLic >> std::get_time(&tmNewLic, "%Y-%m-%dT%H:%M:%SZ"); // or just %T in this case
    std::time_t timeNewLic = mktime(&tmNewLic);


    //first normally after
    double fsecondsElapsed = std::difftime(timeNewLic, timeLic);
    if (fsecondsElapsed > 0) {
        CreateLicenseFile(mPackageName, mLastLicenseKey, mLicenseCheckedDate);
        CGlobalTools::AndroidLog("parsava.lic created successfully");
    }
}
*/
void DeleteExpiredLic(string licenseData) {
    time_t nowUTC = std::time(nullptr);
    std::tm *nowLocal = std::localtime(&nowUTC);
    std::time_t nowLocalConverted = std::mktime(nowLocal);

    //CGlobalTools::AndroidLog("DeleteExpiredLic : %s", licenseData.c_str());
    struct std::tm tmLic;
    std::istringstream ssLic(licenseData);
    ssLic >> std::get_time(&tmLic, "%m/%d/%Y %I:%M:%S %p"); // or just %T in this case
    std::time_t timeLic = mktime(&tmLic);
    //Doubt HOUR is 12H or 24H
    /*char buffer[128];
    std::strftime(buffer, 128, "licenseData is %m/%d/%Y %H:%M:%S %p", &tmLic);
    CGlobalTools::AndroidLog("DeleteExpiredLic buffer %s" , buffer);
    */
    //first normally after
    double fsecondsElapsed = std::difftime(nowLocalConverted,timeLic);
    int nDaysElapsed = (int) fsecondsElapsed / (24 * 60 * 60);
    //CGlobalTools::AndroidLog("DeleteExpiredLic fsecondsElapsed:%.3f nDaysElapsed:%d" , fsecondsElapsed,nDaysElapsed);
    if (nDaysElapsed > 6 * 30) {

        /* Initialise. Do this once (not for every
           random number). */
        std::random_device rd;
        std::mt19937_64 gen(rd());

        /* This is where you define the number generator for unsigned long long: */
        std::uniform_int_distribution<unsigned long long> dis;

        unsigned long long randomValue = dis(gen);
        if (randomValue % 5 == 0) {
            std::string path = "/data/data/" + mPackageName + "/" + LICENSE_FILE_NAME;
            int ret_code = std::remove(path.c_str());
            /*if (ret_code == 0) {
                CGlobalTools::AndroidLog("ParsAva.lic was successfully deleted");
            } else {
                CGlobalTools::AndroidLog("Error during the deletion: ParsAva.lic");
            }*/

        }
    }

}

std::string SimpleEncrypt(std::string strInput) {
    if ((int) strInput.length() > 0) {
        strInput[0] = (char) (strInput[0] ^ SIMPLE_ENCRYPTION_KEY);
    }
    for (int l = 1; l < (int) strInput.length(); l++) {
        strInput[l] = (char) (strInput[l] ^ strInput[l - 1]);
    }
    std::string strHexFormatInput = ""; //GetHexFormat(strInput);
    return strHexFormatInput;
}
/*
extern "C" JNIEXPORT  void   JNICALL
Java_com_khanenoor_parsavatts_Lock_SetLastUpdate(JNIEnv *env, jclass jclassobj,
                                                 jstring licenseKey) {
    try {
        char *utf8_license_key_in_str = (char *) env->GetStringUTFChars(licenseKey, nullptr);
        // Check if the call to GetStringUTFChars was successful.
        if (utf8_license_key_in_str == nullptr) {
            // Handle the error.
        }
        mLastLicenseKey = utf8_license_key_in_str;

        mIsFirstLockChecked = false;
        time_t nowUTC = std::time(nullptr);
        std::tm *nowLocal = std::localtime(&nowUTC);
        std::time_t nowLocalConverted = std::mktime(nowLocal);
        //time_t ct = time(0);
        char buffer[128];
        strftime(buffer, sizeof buffer, "Today is %A, %B %d.\nThe time is %I:%M %p.\n", nowLocal);
        mLicenseCheckedDate = buffer;
    } catch (...) {
        CGlobalTools::AndroidLog("Lock::SetLastUpdate catch unknown exception");
    }
}
*/
extern "C" JNIEXPORT  jstring   JNICALL
Java_com_khanenoor_parsavatts_Lock_getLicenseKey(JNIEnv *env, jclass jclassobj) {
    jstring jstrLicenseKey;
    std::string licenseKey = "";
    try {
        std::string path = "/data/data/" + mPackageName + "/" + LICENSE_FILE_NAME;
        std::string licenseData = "";
        std::string licenseKey = readLicenseFile(path, licenseData);
    } catch (...) {
        CGlobalTools::AndroidLog("Lock::SetLastUpdate catch unknown exception");
    }
    jstrLicenseKey = env->NewStringUTF(licenseKey.c_str());
    return jstrLicenseKey;
}

std::string readLicenseFile(std::string &path, std::string &licenseData) {
    string licenseKey="";
    licenseData="";
    std::string strEncryptLic;
    /*
    std::ios_base::ate position the cursor at the end of the text whereas std::ios_base_app appends text (with a write operation) at the end, though you can still read from the beginning :)
    */
    std::ifstream datafile(path, std::ios_base::binary | std::ios_base::ate);
    try {

        if(!datafile.good()){
            CGlobalTools::AndroidLog("readLicenseFile file is not good and not found!");
            datafile.close();
            return licenseKey;
        }
        std::streamoff size = datafile.tellg();
        datafile.seekg(0, std::ios::beg);
        std::ostringstream buf;
        buf << datafile.rdbuf();
        datafile.close();
        //CGlobalTools::AndroidLog("readLicenseFile buf:%s",buf.str().c_str());

        ////////////////////////////////////////////////Decrypt Content of File
        //Use Public Application Key
        std::string strLicenseData = getDecodeData(mPackageName, buf.str(), 3);
        //CGlobalTools::AndroidLog("readLicenseFile strLicenseData:%s",strLicenseData.c_str());
        //////////////////////////////////////////////Read Sections
        istringstream f(strLicenseData);
        getline(f, licenseKey, '#');

        getline(f, licenseData, '#');

    } catch (...) {
        CGlobalTools::AndroidLog("Lock::SetLastUpdate catch unknown exception");
        datafile.close();
    }
    return licenseKey;
}