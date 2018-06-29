package org.awesomeapp.messenger.ui.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraViewImpl;

import org.apache.commons.io.IOUtils;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.Preferences;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import im.zom.messenger.R;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileOutputStream;


public class CameraActivity extends AppCompatActivity {

    CameraView mCameraView;
    OrientationEventListener mOrientationEventListener;
    int mLastOrientation = -1;

    boolean mOneAndDone = true;
    public final static String SETTING_ONE_AND_DONE = "oad";

    public static final int ORIENTATION_PORTRAIT = 0;
    public static final int ORIENTATION_LANDSCAPE = 1;
    public static final int ORIENTATION_PORTRAIT_REVERSE = 2;
    public static final int ORIENTATION_LANDSCAPE_REVERSE = 3;

    private Handler mHandler = new Handler ()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 1)
            {
              //  Toast.makeText(CameraActivity.this,"\uD83D\uDCF7",Toast.LENGTH_SHORT).show();
            }
            else if (msg.what == 2)
            {
                mCameraView.stop();
            }
        }
    };

    private Executor mExec = new ThreadPoolExecutor(1,3,60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mOneAndDone = getIntent().getBooleanExtra(SETTING_ONE_AND_DONE,true);

        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCameraView = findViewById(R.id.camera_view);

        findViewById(R.id.btnCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraView.takePicture();
            }
        });

        findViewById(R.id.toggle_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraView.switchCamera();
            }
        });

        findViewById(R.id.toggle_flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraView.setFlash(CameraView.FLASH_AUTO);
            }
        });

        mOrientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {

                if (orientation < 0) {
                    return; // Flip screen, Not take account
                }

                int curOrientation;

                if (orientation <= 45) {
                    curOrientation = ORIENTATION_PORTRAIT;
                } else if (orientation <= 135) {
                    curOrientation = ORIENTATION_LANDSCAPE_REVERSE;
                } else if (orientation <= 225) {
                    curOrientation = ORIENTATION_PORTRAIT_REVERSE;
                } else if (orientation <= 315) {
                    curOrientation = ORIENTATION_LANDSCAPE;
                } else {
                    curOrientation = ORIENTATION_PORTRAIT;
                }
                if (curOrientation != mLastOrientation) {

                    mLastOrientation = curOrientation;

                }
            }
        };

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        } else {
            mOrientationEventListener.disable();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOrientationEventListener.disable();
        System.gc();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap rotate (Bitmap bitmap, int rotationDegrees)
    {

        Matrix matrix = new Matrix();
        matrix.postRotate(-rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.start();
        mCameraView.setOnPictureTakenListener(new CameraViewImpl.OnPictureTakenListener() {
            @Override
            public void onPictureTaken(final Bitmap bitmap, final int rotationDegrees) {

                mExec.execute(new Runnable()
                {
                    public void run ()
                    {

                        if (mOneAndDone)
                            mHandler.sendEmptyMessage(2);

                        storeBitmap(rotate(bitmap,rotationDegrees));

                    }
                });

                mHandler.sendEmptyMessage(1);
            }
        });

    }

    @Override
    protected void onPause() {

        if (mCameraView.isCameraOpened())
            mCameraView.stop();

        super.onPause();
    }

    private void storeBitmap (Bitmap bitmap)
    {
        // import
        String sessionId = "self";
        String offerId = UUID.randomUUID().toString();

        try {

            final Uri vfsUri = SecureMediaStore.createContentPath(sessionId,"cam" + new Date().getTime() + ".jpg");

            OutputStream out = new FileOutputStream(new File(vfsUri.getPath()));
            bitmap = getResizedBitmap(bitmap,SecureMediaStore.DEFAULT_IMAGE_WIDTH,SecureMediaStore.DEFAULT_IMAGE_WIDTH);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for JPG*/, out);

            bitmap.recycle();
            System.gc();

            String mimeType = "image/jpeg";

            //adds in an empty message, so it can exist in the gallery and be forwarded
            Imps.insertMessageInDb(
                    getContentResolver(), false, new Date().getTime(), true, null, vfsUri.toString(),
                    System.currentTimeMillis(), Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED,
                    0, offerId, mimeType);

            if (mOneAndDone) {
                Intent data = new Intent();
                data.setData(vfsUri);
                setResult(RESULT_OK, data);
                finish();
            }

            if (Preferences.useProofMode()) {

                try {
                    ProofMode.generateProof(CameraActivity.this, vfsUri);
                } catch (FileNotFoundException e) {
                    Log.e(ImApp.LOG_TAG,"error generating proof for photo",e);
                }
            }


        }
        catch (IOException ioe)
        {
            Log.e(ImApp.LOG_TAG,"error importing photo",ioe);
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int maxWidth, int maxHeight) {

        float scale = Math.min(((float)maxHeight / bm.getWidth()), ((float)maxWidth / bm.getHeight()));

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scale, scale);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}
