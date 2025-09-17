#include <jni.h>
#include <string>
#include <thread>
#include <vector>
#include <mutex>
#include "ggml.h"
#include "llama.h"

static std::mutex g_ctx_mutex;

extern "C" JNIEXPORT jlong JNICALL
Java_com_journalapp_ai_LlamaBridge_initModel(JNIEnv* env, jclass, jstring jpath, jint nCtx, jint nThreads) {
    const char* modelPath = env->GetStringUTFChars(jpath, nullptr);

    llama_backend_init();

    llama_model_params mparams = llama_model_default_params();
    llama_model* model = llama_model_load_from_file(modelPath, mparams);
    env->ReleaseStringUTFChars(jpath, modelPath);
    if (!model) return 0;

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = nCtx;
    cparams.n_threads = nThreads > 0 ? nThreads : (int)std::thread::hardware_concurrency();

    llama_context* ctx = llama_init_from_model(model, cparams);
    return reinterpret_cast<jlong>(ctx);
}

static std::string run_generate(llama_context* ctx, const std::string &prompt, int max_tokens, float temp, float top_p) {
    std::lock_guard<std::mutex> lock(g_ctx_mutex);
    if (!ctx) return "";

    const llama_model* model = llama_get_model(ctx);
    const llama_vocab* vocab = llama_model_get_vocab(model);

    // 1) Tokenize prompt
    int32_t needed = llama_tokenize(vocab, prompt.c_str(), (int32_t)prompt.size(),
                                    nullptr, 0, true, true);
    if (needed <= 0) return "";

    std::vector<llama_token> tokens(needed);
    llama_tokenize(vocab, prompt.c_str(), (int32_t)prompt.size(),
                   tokens.data(), (int32_t)tokens.size(), true, true);

    // 2) Run initial eval using llama_batch + llama_decode
    llama_batch batch = llama_batch_init((int32_t)tokens.size(), 0, 1);
    for (int i = 0; i < (int)tokens.size(); i++) {
        batch.token[i]  = tokens[i];
        batch.pos[i]    = i;
        batch.seq_id[i] = 0;
        batch.logits[i] = (i == (int)tokens.size() - 1);
    }
    batch.n_tokens = (int32_t)tokens.size();

    if (llama_decode(ctx, batch) != 0) {
        llama_batch_free(batch);
        return "";
    }
    llama_batch_free(batch);

    // 3) Generate tokens
    std::string out;
    int n_remaining = max_tokens;
    while (n_remaining-- > 0) {
        // Instead of llama_sample_token_greedy (gone), use logits directly
        const float* logits = llama_get_logits(ctx);
        if (!logits) break;

        // Greedy pick: argmax
        int vocab_size = llama_vocab_n_tokens(vocab);
        int best_token = 0;
        float best_score = logits[0];
        for (int i = 1; i < vocab_size; i++) {
            if (logits[i] > best_score) {
                best_score = logits[i];
                best_token = i;
            }
        }

        if (best_token == llama_vocab_eos(vocab)) break;

        const char* piece = llama_token_get_text(vocab, best_token);
        if (!piece) break;
        out += std::string(piece);

        // Feed back token
        llama_batch next = llama_batch_init(1, 0, 1);
        next.token[0]  = best_token;
        next.pos[0]    = (int)tokens.size();
        next.seq_id[0] = 0;
        next.logits[0] = 1;
        next.n_tokens  = 1;

        if (llama_decode(ctx, next) != 0) {
            llama_batch_free(next);
            break;
        }
        llama_batch_free(next);

        tokens.push_back(best_token);
    }

    return out;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_journalapp_ai_LlamaBridge_generate(JNIEnv* env, jclass, jlong ctxHandle, jstring jprompt, jint maxTokens, jfloat temp, jfloat topP) {
    auto* ctx = reinterpret_cast<llama_context*>(ctxHandle);
    if (!ctx) return env->NewStringUTF("");

    const char* prompt = env->GetStringUTFChars(jprompt, nullptr);
    std::string result = run_generate(ctx, std::string(prompt), maxTokens, temp, topP);
    env->ReleaseStringUTFChars(jprompt, prompt);

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_journalapp_ai_LlamaBridge_free(JNIEnv* env, jclass, jlong ctxHandle) {
    auto* ctx = reinterpret_cast<llama_context*>(ctxHandle);
    if (!ctx) return;

    const llama_model* model = llama_get_model(ctx);
    llama_free(ctx);
    if (model) llama_model_free(const_cast<llama_model*>(model));
    llama_backend_free();
}
