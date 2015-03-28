#include <EmotionClassifier.h>

void rotate_image_90n(cv::Mat &src, cv::Mat &dst, int angle);

void EmotionClassifier::init(JNIEnv* jenv, string faceConfigFile, string eyeConfigFile, int width, int height,
    int nWidths, int nLambdas, int nThetas, jobjectArray jSvmClassifierFiles)
{
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Initializing Classifier.");
        clock_t begin = clock();

        //  Setting the classifier files
        string mode  = "svm";
        int pathCount = jenv->GetArrayLength(jSvmClassifierFiles);

        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Adding %d xml files.", pathCount);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Face config file: %s", faceConfigFile.c_str());

        for (int i=0; i<pathCount; i++)
        {
            jstring jClassifierString =
            (jstring)(jenv->GetObjectArrayElement(jSvmClassifierFiles, i));

            string svmClassifierFile =
            ((jenv->GetStringUTFChars(jClassifierString, NULL)));

            cl_paths.push_back(svmClassifierFile);
        }

        clock_t tmConfig = clock();
              __android_log_print(ANDROID_LOG_DEBUG,
              LOG_TAG,
              "Configured classifier in %f seconds.",
              double(tmConfig - begin) / CLOCKS_PER_SEC);

        //  Setup the pre-processor
        facepreproc
        = new FacePreProcessor(faceConfigFile, "none", width, height, nWidths,
         nLambdas, nThetas);

        clock_t tmFacePreProc = clock();
                  __android_log_print(ANDROID_LOG_DEBUG,
                  LOG_TAG,
                  "Setup pre-processor in %f seconds.",
                  double(tmFacePreProc - tmConfig) / CLOCKS_PER_SEC);


        //  Setup the emotionDetector
        emodetector =
            new SVMEmoDetector(kCfactor, kMaxIteration, kErrorMargin);
        emodetector->init(cl_paths);

        clock_t tmEmoDetect = clock();
              __android_log_print(ANDROID_LOG_DEBUG,
              LOG_TAG,
              "Setup emoDetect in %f seconds.",
              double(tmEmoDetect - tmFacePreProc) / CLOCKS_PER_SEC);
}

void EmotionClassifier::classify(string bitmapPath){
    clock_t tmEmoDetect = clock();

    __android_log_print(ANDROID_LOG_DEBUG,
              LOG_TAG,
              "Image path = %s", bitmapPath.c_str());

    //  Read the bitmap image from file
    Mat image;
    image = imread(bitmapPath, CV_LOAD_IMAGE_COLOR);
    if(!image.data)
        return;

    //  Java side is buggy and the images come rotated 90degrees, so rotate it back
    cv::Mat dst;
    rotate_image_90n(image, dst, 270);

    clock_t tmReadImage = clock();
          __android_log_print(ANDROID_LOG_DEBUG,
          LOG_TAG,
          "Read image in %f seconds.", double(tmReadImage - tmEmoDetect) / CLOCKS_PER_SEC);

    LOGD("Face image has been loaded successfully!");

    cv::imwrite("/sdcard/rotated90.jpg", dst);

    //  Have the preprocessor give us a feature vector
    Mat featvector;
    if (facepreproc->preprocess(dst, featvector)) {
        LOGD("Done pre-processing.");
        pair<Emotion, float> prediction = emodetector->predict(featvector);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Emotion: %s, Score: %f",
        emotionStrings(prediction.first).c_str(), prediction.second);
    }
    else{
        LOGD("Pre-processing failed.");
    }

    LOGD("Done processing.");

    return;
}



void rotate_image_90n(cv::Mat &src, cv::Mat &dst, int angle)
{
   if(src.data != dst.data){
       src.copyTo(dst);
   }

   angle = ((angle / 90) % 4) * 90;

   //0 : flip vertical; 1 flip horizontal
   bool const flip_horizontal_or_vertical = angle > 0 ? 1 : 0;
   int const number = std::abs(angle / 90);

   for(int i = 0; i != number; ++i){
       cv::transpose(dst, dst);
       cv::flip(dst, dst, flip_horizontal_or_vertical);
   }
}
