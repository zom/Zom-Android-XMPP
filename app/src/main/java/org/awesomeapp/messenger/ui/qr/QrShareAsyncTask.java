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

import im.zom.messenger.R;

public class QrShareAsyncTask extends AsyncTask<String, Void, Bitmap> {
    private static final String TAG = "QrGenAsyncTask";

    private final Activity activity;
    private Bitmap qrBitmap;

    private String inviteLink;
    private StringBuffer message;

    private boolean mShareQRCode = false;

    public QrShareAsyncTask(Activity activity) {
        this.activity = activity;

    }

    public void setShareQRCode (boolean shareQRCode)
    {
        mShareQRCode = shareQRCode;
    }

    /*
     * The method for getting screen dimens changed, so this uses both the
     * deprecated one and the 13+ one, and supports all Android versions.
     */
    @SuppressWarnings("deprecation")
    @TargetApi(13)
    @Override
    protected Bitmap doInBackground(String... s) {
        inviteLink = s[0];

        message = new StringBuffer();

        if (s.length > 0)
        {
            message.append(s[1]).append(": ");
        }

        message.append(inviteLink);
        message.append("\n\n");
        message.append(activity.getString(R.string.action_tap_invite));

        if (mShareQRCode) {
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
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(inviteLink, null,
                    Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

            try {
                qrBitmap = qrCodeEncoder.encodeAsBitmap();
                return qrBitmap;
            } catch (WriterException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {

        if (mShareQRCode && bmp != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            File file = new File(activity.getCacheDir(), "qr.png");

            try {
                FileOutputStream fOut = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();
                file.setReadable(true, false);

                Intent intent = new Intent(     android.content.Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                intent.putExtra(Intent.EXTRA_TEXT,message.toString());
                intent.setType("image/png");

                activity.startActivity(intent);
            }
            catch (Exception e)
            {

            }
        }
        else
        {

            String subject = activity.getString(R.string.app_name_zom) + ' ' + activity.getString(R.string.header_got_invited);

            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT,message.toString());
            intent.putExtra(Intent.EXTRA_SUBJECT,subject);
            intent.setType("text/plain");
            activity.startActivity(intent);
        }

    }
}