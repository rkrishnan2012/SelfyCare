package selfycare.com.selfycare;

public class EmotionClassifier {
    EmotionClassifier(String faceConfigFile, String eyeConfigFile, int width, int height,
                      int nWidths, int nLambdas, int nThetas, String[] svmClassifierFiles){
        mNativeObj = nativeInit(faceConfigFile, eyeConfigFile, width, height, nWidths,
                nLambdas, nThetas, svmClassifierFiles);
    }

    public void classify(String bitmapImagePath){
        nativeClassify(mNativeObj, bitmapImagePath);
    }

    public void release() {
        nativeDestroyObject(mNativeObj);
        mNativeObj = 0;
    }

    private long mNativeObj = 0;

    private static native long nativeInit(String faceConfigFile, String eyeConfigFile,
                                          int width, int height, int nWidths, int nLambdas,
                                          int nThetas, String[] svmClassifierFiles);

    private static native long nativeClassify(long nativeObject, String bitmapImagePath);

    private static native void nativeDestroyObject(long thiz);
}
