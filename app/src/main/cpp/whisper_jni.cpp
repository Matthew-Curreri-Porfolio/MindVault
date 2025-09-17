#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include "whisper.h"
#include "examples/common.h"

static std::mutex g_whisper_mutex;

extern "C" [[maybe_unused]] JNIEXPORT jlong JNICALL
javaComMindvaultWhisperBridgeInitModel(JNIEnv* env, jclass, jstring jmodelPath, jint nThreads) {
    const char* path = env->GetStringUTFChars(jmodelPath, nullptr);
    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    struct whisper_context * ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(jmodelPath, path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_Mindvault_ai_WhisperBridge_transcribe(JNIEnv* env, jclass, jlong handle,
                                                jstring jaudioPath, jint nThreads) {
    auto * ctx = reinterpret_cast<whisper_context*>(handle);
    if (!ctx) return env->NewStringUTF("");

    const char* wavPath = env->GetStringUTFChars(jaudioPath, nullptr);
    std::lock_guard<std::mutex> lock(g_whisper_mutex);

    // load wav
    audio_data ad = {};
    if (!::read_wav(wavPath, ad)) {
        env->ReleaseStringUTFChars(jaudioPath, wavPath);
        return env->NewStringUTF("");
    }

    // convert to float32 mono if needed
    if (ad.nch != 1) {
        fprintf(stderr, "Only mono wav supported\n");
    }

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress   = false;
    wparams.print_realtime   = false;
    wparams.print_timestamps = false;
    wparams.n_threads        = nThreads > 0 ? nThreads : 4;
    wparams.no_context       = true;
    wparams.single_segment   = true;

    if (whisper_full(ctx, wparams, ad.pcmf32.data(), ad.pcmf32.size()) != 0) {
        env->ReleaseStringUTFChars(jaudioPath, wavPath);
        return env->NewStringUTF("");
    }

    // collect text
    std::string out;
    int n = whisper_full_n_segments(ctx);
    for (int i = 0; i < n; i++) {
        out += whisper_full_get_segment_text(ctx, i);
    }

    env->ReleaseStringUTFChars(jaudioPath, wavPath);
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
javaComMindvaultAiWhisperBridgeFree(JNIEnv*, jclass, jlong handle) {
    auto * ctx = reinterpret_cast<whisper_context*>(handle);
    if (ctx) whisper_free(ctx);
}
