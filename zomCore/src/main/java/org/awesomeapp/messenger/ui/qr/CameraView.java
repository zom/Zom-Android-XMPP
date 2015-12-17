package org.awesomeapp.messenger.ui.qr;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
import static android.view.SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.awesomeapp.messenger.ImApp;

@SuppressWarnings("deprecation")
public class CameraView extends SurfaceView implements SurfaceHolder.Callback,
AutoFocusCallback {

	private static String TAG = CameraView.class.getPackage().getName();

	private Camera camera = null;
	private PreviewConsumer previewConsumer = null;
	private Size desiredSize = null;
	private int displayOrientation = 0;
	private boolean surfaceExists = false;

	public CameraView(Context context) {
		super(context);
		setKeepScreenOn(true);
	}

	void start(Camera newCamera, PreviewConsumer previewConsumer,
			int rotationDegrees, boolean macro) {

		camera = newCamera;

		this.previewConsumer = previewConsumer;
		setDisplayOrientation(rotationDegrees);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		holder.setType(SURFACE_TYPE_PUSH_BUFFERS); // Required on Android < 3.0
		if(surfaceExists) startPreview(holder);

		new Thread ()
		{
			public void run ()
			{
				while (camera != null) {
					try {
						Thread.sleep(2000);
					}
					catch (Exception e){}
					try {
						camera.autoFocus(CameraView.this);
					}
					catch (Exception e)
					{
						Log.w(ImApp.LOG_TAG,"qr code scanning not working: " + e.toString());
					}

				}
			}
		}.start();
	}

	void stop() {
		stopPreview();
		getHolder().removeCallback(this);
        camera = null;
	}

	private void startPreview(SurfaceHolder holder) {
		try {

			Parameters params = camera.getParameters();

			if (params.getSupportedFocusModes().contains(
					Camera.Parameters.FOCUS_MODE_AUTO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			}

			camera.setParameters(params);

			camera.setPreviewDisplay(holder);
			camera.startPreview();

			previewConsumer.start(camera);
		} catch(IOException e) {
			Log.e(TAG, "Error starting camera preview", e);
		}
	}

	private void stopPreview() {
		try {
			previewConsumer.stop();

            if (camera != null) {
                camera.stopPreview();

            }
		} catch(Exception e) {
			Log.e(TAG, "Error stopping camera preview", e);
		}
	}

	private void setDisplayOrientation(int rotationDegrees) {
		int orientation;
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(0, info);
		if(info.facing == CAMERA_FACING_FRONT) {
			orientation = (info.orientation + rotationDegrees) % 360;
			orientation = (360 - orientation) % 360;
		} else {
			orientation = (info.orientation - rotationDegrees + 360) % 360;
		}
		camera.setDisplayOrientation(orientation);
		displayOrientation = orientation;
	}

	public void surfaceCreated(SurfaceHolder holder) {
		surfaceExists = true;
		startPreview(holder);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		stopPreview();

        if (camera != null) {
            boolean rotatePreview = displayOrientation % 180 == 90;
            Parameters params = camera.getParameters();
            List<Size> sizes = params.getSupportedPreviewSizes();
            Size bestSize = null;
            int bestError = 0;
            for (Size size : sizes) {
                int width = rotatePreview ? size.height : size.width;
                int height = rotatePreview ? size.width : size.height;
                int widthError = Math.abs(width - w);
                int heightError = Math.abs(height - h);
                int error = Math.max(widthError, heightError);
                if (bestSize == null || error < bestError) {
                    bestSize = size;
                    bestError = error;
                }
            }

            params.setPreviewSize(bestSize.width, bestSize.height);


            camera.setParameters(params);
            desiredSize = bestSize;

            startPreview(holder);
        }
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceExists = false;
	}

	public void onAutoFocus(boolean success, Camera camera) {
		Log.d(TAG, "Auto focus: " + success);
	}

	@Override
	protected void onMeasure(int width, int height) {
		if(desiredSize == null) super.onMeasure(width, height);
		else if(displayOrientation % 180 == 90)
			setMeasuredDimension(desiredSize.height, desiredSize.width);
		else setMeasuredDimension(desiredSize.width, desiredSize.height);
	}
}