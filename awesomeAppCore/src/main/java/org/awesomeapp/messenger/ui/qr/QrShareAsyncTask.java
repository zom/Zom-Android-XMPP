package org.awesomeapp.messenger.ui.qr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.encode.Contents;
import com.google.zxing.encode.QRCodeEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class QrShareAsyncTask extends AsyncTask<String, Void, Bitmap> {
    private static final String TAG = "QrGenAsyncTask";

    private final Activity activity;
    private Bitmap qrBitmap;

    private String qrData;

    public QrShareAsyncTask(Activity activity) {
        this.activity = activity;

    }

    /*
     * The method for getting screen dimens changed, so this uses both the
     * deprecated one and the 13+ one, and supports all Android versions.
     */
    @SuppressWarnings("deprecation")
    @TargetApi(13)
    @Override
    protected Bitmap doInBackground(String... s) {
        qrData = s[0];
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point outSize = new Point();
        int x, y, qrCodeDimension;
        /* lame, got to use both the new and old APIs here */
        if (Build.VERSION.SDK_INT >= 13) {
            display.getSize(outSize);
            x = outSize.x;
            y = outSize.y;
        } else {
            x = display.getWidth();
            y = display.getHeight();
        }
        if (x < y)
            qrCodeDimension = x;
        else
            qrCodeDimension = y;
        Log.i(TAG, "generating QRCode Bitmap of " + qrCodeDimension + "x" + qrCodeDimension);
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

        try {
            qrBitmap = qrCodeEncoder.encodeAsBitmap();
            return qrBitmap;
        } catch (WriterException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {

        if (bmp != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            File file = new File(activity.getCacheDir(), "qr.png");

            try {
                FileOutputStream fOut = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();
                file.setReadable(true, false);

                final Intent intent = new Intent(     android.content.Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                intent.putExtra(Intent.EXTRA_TEXT,qrData);
                intent.setType("image/png");

                activity.startActivity(intent);
            }
            catch (Exception e)
            {

            }
        }

    }
}