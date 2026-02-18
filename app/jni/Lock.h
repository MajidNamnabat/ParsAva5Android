#pragma once
#include <jni.h>
#include <string>
#include <fstream>
#include "EciesAccess.h"
bool IsAutherize();
char* readImageFile(const std::string& path , std::streamsize & fileSize );
std::string readKey(const std::string& path , const char *stKey);
std::string SimpleEncrypt(std::string strInput);
std::string getDecodeData(std::string pck_name, std::string encryptedData , int style);
void DeleteExpiredLic(std::string licenseData);
bool checkLicense(int style);
//void UpdateLicenseFile(std::string licenseData);
std::string getEncodeData(std::string& pck_name, std::string& data , int style);
std::string readLicenseFile(std::string& path, std::string& licenseData);
//void CreateLicenseFile(std::string packageName,std::string licenseKey, std::string licenseCheckDate);

extern "C" {
JNIEXPORT  jstring   JNICALL Java_com_khanenoor_parsavatts_Lock_getEncodeData(JNIEnv * , jclass , jstring,jstring,jint);

JNIEXPORT  jstring   JNICALL
Java_com_khanenoor_parsavatts_Lock_getDecodeData(JNIEnv *, jclass, jstring, jstring , jint);
/*
JNIEXPORT  jstring   JNICALL
Java_com_khanenoor_parsavatts_Lock_WriteLicense(JNIEnv * , jclass , jstring,jstring);
*/
/*
JNIEXPORT  void   JNICALL
Java_com_khanenoor_parsavatts_Lock_CreateLicFile(JNIEnv * , jclass , jstring , jstring);
*/
JNIEXPORT  void   JNICALL
Java_com_khanenoor_parsavatts_Lock_SetHardwareAppID(JNIEnv * , jclass , jstring , jstring);
/*
JNIEXPORT  void   JNICALL
Java_com_khanenoor_parsavatts_Lock_SetLastUpdate(JNIEnv * , jclass , jstring);
*/
JNIEXPORT  jstring   JNICALL
Java_com_khanenoor_parsavatts_Lock_getLicenseKey(JNIEnv * , jclass );

}