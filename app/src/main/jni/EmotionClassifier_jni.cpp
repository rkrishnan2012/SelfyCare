#include <selfycare_com_selfycare_EmotionClassifier.h>
#include <EmotionClassifier.h>
#include <android/log.h>
#include <utility>
#include <map>


#define LOG_TAG "SelfyCare/EmotionClassifier"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;


JNIEXPORT jlong JNICALL Java_selfycare_com_selfycare_EmotionClassifier_nativeInit
(JNIEnv* jenv, jclass, jstring jFaceConfigFile, jstring jEyeConfigFile, jint jWidth,
    jint jHeight, jint jnWidths, jint jnLambdas, jint jnThetas, jobjectArray jSvmClassifierFiles)
{
    EmotionClassifier* nativeObj = new EmotionClassifier();

    string faceConfigFile(jenv->GetStringUTFChars(jFaceConfigFile, NULL));
    string eyeConfigFile(jenv->GetStringUTFChars(jEyeConfigFile, NULL));

    nativeObj->init(jenv, faceConfigFile, eyeConfigFile, (int)jWidth, (int)jHeight,
                  (int)jnWidths, (int)jnLambdas, (int)jnThetas, jSvmClassifierFiles);

    return (jlong)nativeObj;
}


JNIEXPORT jlong JNICALL Java_selfycare_com_selfycare_EmotionClassifier_nativeClassify
  (JNIEnv* jenv, jclass, jlong thiz, jstring jbitmapPath)
{
    EmotionClassifier* nativeObj = ((EmotionClassifier*)thiz);
    string bitmapPath(jenv->GetStringUTFChars(jbitmapPath, NULL));
    nativeObj->classify(bitmapPath);
}

JNIEXPORT void JNICALL Java_selfycare_com_selfycare_EmotionClassifier_nativeDestroyObject
  (JNIEnv *, jclass, jlong){

}

