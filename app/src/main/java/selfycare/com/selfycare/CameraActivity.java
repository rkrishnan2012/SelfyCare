/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package selfycare.com.selfycare;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import selfycare.com.selfycare.gles.FullFrameRect;
import selfycare.com.selfycare.gles.Texture2dProgram;

@SuppressWarnings("deprecation")
public class CameraActivity extends Activity
        implements SurfaceTexture.OnFrameAvailableListener {
    static {
        System.loadLibrary("detection_based_tracker");
    }

    public static final String TAG = CameraActivity.class.toString();
    private static final boolean VERBOSE = false;

    private static final String SELFYCARE_BASE_PATH = "/sdcard/SelfyCare/";
    private static final String DATASET_FOLDER =
            SELFYCARE_BASE_PATH + "dataset_svm_354_cbcl1_1vsallext/";
    private static final String CLASSIFIERS_FOLDER = DATASET_FOLDER + "/classifiers/svm/";
    private static final String DATASET_URL =
            "https://dl.dropboxusercontent.com/u/7618747/dataset_svm_354_cbcl1_1vsallext.zip";
    private static final int FACE_CONFIG_FILE = R.raw.haarcascade_frontalface_cbcl1;
    private static final String FACE_CONFIG_OUTPUT_FILE =
            SELFYCARE_BASE_PATH + "haarcascade_frontalface_cbcl1.xml";

    private static final String DATASET_ZIP_PATH = SELFYCARE_BASE_PATH +
            "dataset_svm_354_cbcl1_1vsallext.zip";
    private GLSurfaceView mGLView;
    private CameraSurfaceRenderer mRenderer;
    private Camera mCamera;
    private CameraHandler mCameraHandler;
    private int mCameraPreviewWidth, mCameraPreviewHeight;


    String[] mClassifierFiles;

    File mFaceConfigFile = new File(FACE_CONFIG_OUTPUT_FILE);
    EmotionClassifier classifier = null;
    public static final AtomicBoolean classifierReady = new AtomicBoolean(false);

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void findClassifierFiles(){
        File dir = new File(CLASSIFIERS_FOLDER);
        if(!dir.exists()){
            Utils.unpackZip(DATASET_ZIP_PATH, SELFYCARE_BASE_PATH);
        }

        if(dir.mkdirs()){
            Log.d(getClass().toString(), "Made the dataset directory.");
        }
        File[] matches = dir.listFiles();
        ArrayList<String> classifierFilesList = new ArrayList<>(matches.length);
        for(File classifierFile : matches){
            classifierFilesList.add(classifierFile.getAbsolutePath());
        }
        mClassifierFiles = new String[matches.length];
        mClassifierFiles = classifierFilesList.toArray(mClassifierFiles);

        Log.d(TAG, "There are " + mClassifierFiles.length + " total xml files.");

        try {
            copyCascadeFiles();
        } catch (IOException e) {
            Log.e(TAG, "Something died.", e);
        }
    }

    private void downloadDatasetArchive() {
        File svmDataSetFolder = new File(DATASET_FOLDER);
        if (!svmDataSetFolder.exists()) {
            // declare the dialog as a member field of your activity
            final ProgressDialog mProgressDialog;

            // instantiate it within the onCreate method
            mProgressDialog = new ProgressDialog(CameraActivity.this);
            mProgressDialog.setMessage(getString(R.string.downloadTitle));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
            // execute this when the downloader must be fired
            final DownloadTask downloadTask = new DownloadTask(CameraActivity.this){
                @Override
                protected void onPostExecute(String result) {
                    mProgressDialog.dismiss();
                    if (result != null){
                        finish();
                    }
                    else{
                        findClassifierFiles();
                    }
                }

                @Override
                protected void onProgressUpdate(final Integer... progress) {
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            mProgressDialog.setProgress(progress[0]);
                        }
                    });
                }
            };
            downloadTask.execute(DATASET_URL,
                    DATASET_ZIP_PATH);
        }
        else{
            findClassifierFiles();
        }
    }

    private void copyCascadeFiles() throws IOException {
        InputStream in = getResources().openRawResource(FACE_CONFIG_FILE);
        FileOutputStream out = new FileOutputStream(FACE_CONFIG_OUTPUT_FILE);
        byte[] buff = new byte[1024];
        int read;

        //noinspection TryFinallyCanBeTryWithResources
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }

        prepareClassifier();
    }

    private void prepareClassifier(){
        new AsyncTask<Void, Void, EmotionClassifier>() {
            @Override
            protected EmotionClassifier doInBackground(Void... params) {
                return new EmotionClassifier(mFaceConfigFile.getAbsolutePath(),
                        "none", 48, 48, 3, 5, 4, mClassifierFiles);
            }

            @Override
            protected void onPostExecute(EmotionClassifier result) {
                classifier = result;
                classifierReady.set(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Button takePictureButton = (Button) findViewById(R.id.takePicture);
                        takePictureButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        }.execute();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        File selfyCareDir = new File(SELFYCARE_BASE_PATH);
        if(selfyCareDir.mkdirs()){
            Log.d(getClass().toString(), "Made the Selfy Care directory.");
        }

        downloadDatasetArchive();


        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);

        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        mGLView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
        mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
        mRenderer = new CameraSurfaceRenderer(mCameraHandler);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Button takePictureButton = (Button) findViewById(R.id.takePicture);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        File pictureFile = getOutputMediaFile();
                        if (pictureFile == null) {
                            return;
                        }
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Something died.", e);
                        }

                        classifier.classify(pictureFile.getAbsolutePath());

                        mCamera.startPreview();
                    }
                });
            }
        });

        Log.d(TAG, "onCreate complete: " + this);
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "SelfyCare");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("SelfyCare", "failed to create directory");
                return null;
            }
        }

        return new File(mediaStorageDir.getPath() + File.separator
                + "selfyCarePic.jpg");
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume -- acquiring camera");
        super.onResume();

        if (mCamera == null) {
            openCamera(1920, 1080);
        }


        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
                //  Fix issue of stretched screen
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int screenWidth = size.x;
                //int screenHeight = size.y;

                ViewGroup.LayoutParams layoutParams = mGLView.getLayoutParams();

                int preViewWidth = mCameraPreviewWidth;
                int preViewHeight = mCameraPreviewHeight;

                if (needsPreviewRotation()) {
                    int temp = preViewHeight;
                    //noinspection SuspiciousNameCombination
                    preViewHeight = preViewWidth;
                    //noinspection SuspiciousNameCombination
                    preViewWidth = temp;
                }

                layoutParams.width = screenWidth;
                layoutParams.height = (int) (screenWidth * ((float) preViewHeight / preViewWidth));

                mGLView.setLayoutParams(layoutParams);
            }
        });

        mGLView.onResume();


        //

        Log.d(TAG, "onResume complete: " + this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause -- releasing camera");
        super.onPause();

        Log.d(TAG, "onPause complete");
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop started");
        super.onStop();

        releaseCamera();
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        mGLView.onPause();
        Log.d(TAG, "onStop complete");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mCameraHandler.invalidateHandler();     // paranoia
    }

    public boolean needsPreviewRotation() {
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        if (display.getRotation() == Surface.ROTATION_0) {
            return true;
        } else if (display.getRotation() == Surface.ROTATION_270) {
            return true;
        }

        return false;
    }

    private void openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        mCamera = Camera.open(1);    // opens first back-facing camera

        Camera.Parameters parms = mCamera.getParameters();

        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);
        parms.setPictureSize(640, 480);
        parms.setColorEffect(android.hardware.Camera.Parameters.EFFECT_MONO);

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        if (display.getRotation() == Surface.ROTATION_0) {
            mCamera.setDisplayOrientation(90);
        }

        if (display.getRotation() == Surface.ROTATION_270) {
            mCamera.setDisplayOrientation(180);
        }

        // leave the frame rate set to default
        mCamera.setParameters(parms);

        Camera.Size mCameraPreviewSize = parms.getPreviewSize();


        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }


//    /**
//     * onClick handler for "rebind" checkbox.
//     */
//    public void clickRebindCheckbox(View unused) {
//        CheckBox cb = (CheckBox) findViewById(R.id.rebindHack_checkbox);
//        TextureRender.sWorkAroundContextProblem = cb.isChecked();
//    }


    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    private void handleSetSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(this);
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.
        if (VERBOSE) Log.d(TAG, "ST onFrameAvailable");
        mGLView.requestRender();
    }

    /**
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p/>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<CameraActivity> mWeakActivity;

        public CameraHandler(CameraActivity activity) {
            mWeakActivity = new WeakReference<>(activity);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakActivity.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

            CameraActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.w(TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }
}

/**
 * Renderer object for our GLSurfaceView.
 * <p/>
 * Do not call any methods here directly from another thread -- use the
 * GLSurfaceView#queueEvent() call.
 */
class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = CameraActivity.TAG;
    private static final boolean VERBOSE = false;

    private CameraActivity.CameraHandler mCameraHandler;

    private FullFrameRect mFullScreen;

    private final float[] mSTMatrix = new float[16];
    private int mTextureId;

    private SurfaceTexture mSurfaceTexture;

    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;

    /**
     * Constructs CameraSurfaceRenderer.
     * <p/>
     *
     * @param cameraHandler Handler for communicating with UI thread
     */
    public CameraSurfaceRenderer(CameraActivity.CameraHandler cameraHandler) {
        mCameraHandler = cameraHandler;

        mTextureId = -1;

        mIncomingSizeUpdated = false;
        mIncomingWidth = mIncomingHeight = -1;
    }

    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p/>
     * For best results, call this *after* disabling Camera preview.
     */
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);     // assume the GLSurfaceView EGL context is about
            mFullScreen = null;             //  to be destroyed
        }
        mIncomingWidth = mIncomingHeight = -1;
    }


    /**
     * Records the size of the incoming camera preview frames.
     * <p/>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setCameraPreviewSize(int width, int height) {
        Log.d(TAG, "setCameraPreviewSize");
        mIncomingWidth = width;
        mIncomingHeight = height;
        mIncomingSizeUpdated = true;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");


        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

        mTextureId = mFullScreen.createTextureObject();

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // Tell the UI thread to enable the camera preview.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                CameraActivity.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        //this fixes weird sizing and artifact issue after onpause
        gl.glViewport(0, 0, width, height);

        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);

    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (VERBOSE) Log.d(TAG, "onDrawFrame tex=" + mTextureId);

        // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
        // was there before.
        mSurfaceTexture.updateTexImage();

        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.i(TAG, "Drawing before incoming texture size set; skipping");
            return;
        }

        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        // Draw the video frame.
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullScreen.drawFrame(mTextureId, mSTMatrix);


    }

}

