#ifndef LIBSAMPLERATE_H
#define LIBSAMPLERATE_H

#pragma once

#include <jni.h>
#include "libSampleRate.h"
#include "SampleRateCommon.h"

/*
 * High level JNI-facing declarations for working with libsamplerate.
 *
 * The native layer exposes two paths: one that works with Java byte arrays
 * and one that works with direct byte buffers. Both paths share the same
 * helper utilities for buffer allocation, volume correction, and lifecycle
 * management. The goal of these declarations is to make it clear which
 * pieces are responsible for memory ownership so TalkBack users benefit from
 * predictable, low-latency audio when speech is resampled.
 */

/*
**	Standard processing function.
**	Returns non zero on error.
*/

// Drives libsamplerate to transform the buffered frames according to the
// configured SRC_STATE. A non-zero return value indicates a failure that
// should be logged before handing audio back to Java.
JNIEXPORT int JNICALL src_process (SRC_STATE *state, SRC_DATA *data) ;
/*
** This library contains a number of different sample rate converters,
** numbered 0 through N.
**
** Return a string giving either a name or a more full description of each
** sample rate converter or NULL if no sample rate converter exists for
** the given value. The converters are sequentially numbered from 0 to N.
*/

JNIEXPORT const char * JNICALL src_get_name (int converter_type) ;
JNIEXPORT const char * JNICALL src_get_description (int converter_type) ;
JNIEXPORT const char * JNICALL src_get_version (void) ;
/*
** Extra helper functions for converting from short to float and
** back again.
*/

// Conversion helpers keep the JNI layer self-contained and avoid reliance on
// Java-side loops that would increase latency for TalkBack playback.
JNIEXPORT void JNICALL src_short_to_float_array (const short *in, float *out, int len) ;
JNIEXPORT void JNICALL src_float_to_short_array (const float *in, short *out, int len) ;
JNIEXPORT const char* JNICALL src_strerror (int error) ;
JNIEXPORT SRC_STATE* JNICALL src_new (int converter_type, int channels, int *error) ;
JNIEXPORT SRC_STATE* JNICALL src_delete (SRC_STATE *state) ;
JNIEXPORT int JNICALL src_reset (SRC_STATE *state) ;
//Internal Functions
// Manual load/unload hooks allow the hosting app to control when buffers and
// mutexes are available. Keeping ownership explicit prevents dangling
// references when the TTS engine is destroyed.
void JNI_OnLoadManual(JavaVM* vm, void* reserved);
void JNI_OnUnloadManual(JavaVM *vm, void *reserved);

// Release all cached native buffers used during resampling. Centralizing the
// cleanup keeps memory predictable even when callers switch sample rates
// repeatedly.
void releaseBuffers();

// Allocate buffers sized to the incoming and outgoing frame counts. The
// native path using Java byte arrays includes space for an input scratch
// array; the direct-buffer path omits it to respect Java-owned memory.
bool AllocBuffers(int len_input , int len_output,int inSampleFreq,
                  int outSampleFreq,
                  int intChannels,
                  int outChannels);
bool AllocBuffers_native(int len_input, int len_output, int inSampleFreq,
                         int outSampleFreq,
                         int intChannels,
                         int outChannels);

// Manage the lifecycle of the libsamplerate state. These helpers guarantee
// that resample calls do not run unless the converter has been constructed
// for the requested channel count.
void unloadResample();
void loadResample(int inSampleFreq,
                  int outSampleFreq,
                  int intChannels,
                  int outChannels);

// Utility helpers to keep JNI interactions safe and consistent.
bool ExceptionCheck(JNIEnv *env);
void correctVolume(float *buf,int len,float volumeRatio);

extern "C" {

JNIEXPORT jbyteArray JNICALL
        Java_com_khanenoor_parsavatts_engine_EnTts_resample(JNIEnv * env, jobject thiz,jbyteArray
dataIn , jint inSampleFreq,
jint outSampleFreq,
jint intChannels,
jint outChannels ,jfloat volumeRatio) ;

JNIEXPORT jobject
JNICALL Java_com_khanenoor_parsavatts_engine_EnTts_resampleNative(JNIEnv *env, jobject thiz, jobject
dataIn,
                                                                  jint inSampleFreq,
                                                                  jint outSampleFreq,
                                                                  jint inChannels,
                                                                  jint outChannels,
                                                                  jfloat volumeRatio);

JNIEXPORT void JNICALL
Java_com_khanenoor_parsavatts_engine_EnTts_loadResample(JNIEnv * env, jobject thiz, jint inSampleFreq,
                                             jint outSampleFreq,
                                             jint intChannels,
                                             jint outChannels ) ;
JNIEXPORT void JNICALL
Java_com_khanenoor_parsavatts_engine_EnTts_unloadResample(JNIEnv * env, jobject thiz ) ;

}
#endif	/* LIBSAMPLERATE_H */
