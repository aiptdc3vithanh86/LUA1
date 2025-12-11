
#include <jni.h>
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_eiapp_MainActivity_runInference(JNIEnv* env,jobject){
    return env->NewStringUTF("Inference placeholder");
}
