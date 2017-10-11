package org.awesomeapp.messenger.ui.qr;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.lang.reflect.Array;
import java.util.ArrayDeque;

@SuppressWarnings("deprecation")
public class QrCodeDecoder implements PreviewConsumer, PreviewCallback {

	private static final String TAG =
			QrCodeDecoder.class.getPackage().getName();

	private final Reader reader = new QRCodeReader();
	private final ResultCallback callback;

	private boolean stopped = false;

	private ArrayDeque<PreviewFrame> mPreviewArray = new ArrayDeque<>();

	public QrCodeDecoder(ResultCallback callback) {
		this.callback = callback;
	}

	private Size previewSize = null;
    private Camera camera = null;

	public void start(Camera camera) {
		Log.d(TAG, "Started");
		stopped = false;
        this.camera = camera;
		askForPreviewFrame(camera);

        previewSize = camera.getParameters().getPreviewSize();

		new Thread(new DecoderTask()).start();
	}

	public void stop() {
		Log.d(TAG, "Stopped");
		stopped = true;
        camera.setPreviewCallback(null);
	}

	private void askForPreviewFrame(Camera camera) {
		if(!stopped) camera.setPreviewCallback(this);
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		if(!stopped) {
			PreviewFrame frame = new PreviewFrame(data, previewSize.width, previewSize.height);
			mPreviewArray.add(frame);
		}
	}

	private class PreviewFrame {
		final byte[] data;
		final int width, height;

		public PreviewFrame(byte[] data, int width, int height)
		{
			this.data = data;
			this.width = width;
			this.height = height;
		}
	}

	private class DecoderTask implements Runnable {

		@Override
		public void run ()
		{
			PreviewFrame frame = null;

			while (!stopped) {
				frame = mPreviewArray.poll();
				if (frame != null) {
					long now = System.currentTimeMillis();
					LuminanceSource src = new PlanarYUVLuminanceSource(frame.data, frame.width,
							frame.height, 0, 0, frame.width, frame.height, false);
					BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(src));
					Result result = null;
					try {
						result = reader.decode(bitmap);
					} catch (ReaderException e) {
						continue;
					} catch (OutOfMemoryError e2) {
						continue;
					}
					catch (NullPointerException e3) {
						continue;
					}

					finally {
						reader.reset();
					}
					System.gc();
					long duration = System.currentTimeMillis() - now;
					Log.d(TAG, "Decoding barcode took " + duration + " ms");
					callback.handleResult(result);
				}
				else
				{
					try { Thread.sleep(1000);}catch(Exception e){}
				}
			}

		}

	}

	public interface ResultCallback {

		void handleResult(Result result);
	}
}
