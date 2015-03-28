#ifndef EMOTIONCLASSIFIER_H_
#define EMOTIONCLASSIFIER_H_

#include <selfycare_com_selfycare_EmotionClassifier.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <utility>
#include <map>


#include "SVMEmoDetector.h"
#include "matrix_io.h"
#include "FaceDetector.h"
#include "TrainingParameters.h"
#include "FacePreProcessor.h"
#include "FacePreProcessor.h"

/// Width of the faces used during training
const int kWidth = 48;
/// Height of the faces used during training
const int kHeight = 48;
/// N-Widths used during training
const double kNWidths = 2;
/// N-Lambdas used during training
const double kNLambdas = 5;
/// N-Thetas used during training
const double kNThetas = 4;

#define LOG_TAG "SelfyCare/EmotionClassifier"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;
using namespace emotime;


class EmotionClassifier{
    public:
        vector<String> cl_paths;
        EmoDetector* emodetector;
        FacePreProcessor* facepreproc;
        void init(JNIEnv* jenv, string faceConfigFile, string eyeConfigFile, int width, int height,
                 int nWidths, int nLambdas, int nThetas, jobjectArray jSvmClassifierFiles);
        void classify(string bitmapPath);
};

#endif /* EMOTIONCLASSIFIER_H_ */

