# ggml needs non-finite math; override fast-math subset
cmake_minimum_required(VERSION 3.18)

project(journal_ai LANGUAGES C CXX)

# Global flags
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math -funroll-loops")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -funroll-loops -fexceptions -frtti")
add_compile_options(-fno-finite-math-only)

# Third-party code
add_subdirectory(${CMAKE_SOURCE_DIR}/third_party/llama.cpp ${CMAKE_BINARY_DIR}/llama)
add_subdirectory(${CMAKE_SOURCE_DIR}/third_party/whisper.cpp ${CMAKE_BINARY_DIR}/whisper)

# --- JNI bridge shared lib (define ONCE) ---
add_library(journal_ai SHARED
        llama_jni.cpp
        whisper_jni.cpp
        third_party/whisper.cpp/examples/common.cpp   # include once
)

target_include_directories(journal_ai PRIVATE
        ${CMAKE_SOURCE_DIR}/third_party/llama.cpp
        ${CMAKE_SOURCE_DIR}/third_party/whisper.cpp
)

# Link against the third-party libs + Android log
target_link_libraries(journal_ai PRIVATE
        llama
        whisper
        log
)

# If you want to force-disable OpenMP on Android (optional):
# target_compile_definitions(journal_ai PRIVATE GGML_NO_OPENMP=1)
