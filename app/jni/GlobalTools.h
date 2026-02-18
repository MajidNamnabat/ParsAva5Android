#pragma once
#include <jni.h>
#include <atomic>

class CGlobalTools {
public:
    static void SetLoggingEnabled(bool enabled);
    static bool IsLoggingEnabled();
    static void AndroidLog(const char *szFormat, ...);
    static void AndroidLogPrint(int priority, const char *tag, const char *szFormat, ...);

private:
    static std::atomic<bool> sLogEnabled;
};