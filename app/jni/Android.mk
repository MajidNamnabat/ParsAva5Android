LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


#Comment Line
include $(CLEAR_VARS)
LOCAL_SRC_FILES := ../src/main/jniLibs/$(TARGET_ARCH_ABI)/libSampleRate.so

LOCAL_MODULE := add_prebuilt3
LOCAL_CFLAGS += -std=gnu++11
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_SRC_FILES  := libSampleRate.cpp  Lock.cpp GlobalTools.cpp # $(FILE_LIST:$(LOCAL_PATH)/%=%)
LOCAL_MODULE     :=  Ttslib
#APP_PLATFORM := android-21
LOCAL_CPP_FEATURES += exceptions
LOCAL_CPPFLAGS :=  -fexceptions -frtti
LOCAL_SHARED_LIBRARIES :=  add_prebuilt3 # add_prebuilt4
LOCAL_LDLIBS := -llog
# LOCAL_STATIC_LIBRARIES := libcryptopp
# LOCAL_C_INCLUDES := $(LOCAL_PATH)/cryptopp
# LOCAL_CPP_INCLUDES := $(LOCAL_PATH)/cryptopp

include $(BUILD_SHARED_LIBRARY)