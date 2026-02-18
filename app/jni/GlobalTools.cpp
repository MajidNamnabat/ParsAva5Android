#include "GlobalTools.h"

#include <cstdarg>
#include <android/log.h>

std::atomic<bool> CGlobalTools::sLogEnabled{false};

void CGlobalTools::SetLoggingEnabled(bool enabled) {
    sLogEnabled.store(enabled, std::memory_order_relaxed);
}

bool CGlobalTools::IsLoggingEnabled() {
    return sLogEnabled.load(std::memory_order_relaxed);
}

void CGlobalTools::AndroidLog(const char *szFormat, ...) {
    if (!IsLoggingEnabled()) {
        return;
    }

    va_list vl;
    va_start(vl, szFormat);
    __android_log_vprint(ANDROID_LOG_WARN, "com.khanenoor.parsavatts.ttslib", szFormat, vl);
    va_end(vl);
}

void CGlobalTools::AndroidLogPrint(int priority, const char *tag, const char *szFormat, ...) {
    if (!IsLoggingEnabled()) {
        return;
    }

    va_list vl;
    va_start(vl, szFormat);
    __android_log_vprint(priority, tag, szFormat, vl);
    va_end(vl);
}
