/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.awesomeapp.messenger.ui;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ImUrlActivity;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.widgets.ImageViewActivity;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import im.zom.messenger.R;

public class GalleryListItem extends FrameLayout {

    private static int sCacheSize = 10; // 1MiB
    private static LruCache<String,Bitmap> sBitmapCache = new LruCache<String,Bitmap>(sCacheSize);

    public final static int THUMBNAIL_SIZE_DEFAULT = 400;

    public enum DeliveryState {
        NEUTRAL, DELIVERED, UNDELIVERED
    }

    public enum EncryptionState {
        NONE, ENCRYPTED, ENCRYPTED_AND_VERIFIED

    }
    private CharSequence lastMessage = null;

    private Context context;

    public GalleryListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }


    private final static DateFormat MESSAGE_DATETIME_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private final static DateFormat MESSAGE_TIME_FORMAT = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private static final SimpleDateFormat FMT_SAME_DAY = new SimpleDateFormat("yyyyMMdd");

    private final static Date DATE_NOW = new Date();

    private final static char DELIVERED_SUCCESS = '\u2714';
    private final static char DELIVERED_FAIL = '\u2718';
    private final static String LOCK_CHAR = "Secure";



    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();


    }
/**
    public void setMessageBackground (Drawable d) {
        mHolder.mContainer.setBackgroundDrawable(d);
    }
*/

    public String getLastMessage () {
        return lastMessage.toString();
    }



    private boolean isSameDay (Date date1, Date date2)
    {
        return FMT_SAME_DAY.format(date1).equals(FMT_SAME_DAY.format(date2));
    }

    protected String convertMediaUriToPath(Uri uri) {
        String path = null;

        String [] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = getContext().getContentResolver().query(uri, proj,  null, null, null);
        if (cursor != null && (!cursor.isClosed()))
        {
            if (cursor.isBeforeFirst())
            {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                path = cursor.getString(column_index);
            }

            cursor.close();
        }

        return path;
    }

    private MediaPlayer mMediaPlayer = null;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onClickMediaIcon(String mimeType, Uri mediaUri) {

        if (SecureMediaStore.isVfsUri(mediaUri)) {
            if (mimeType.startsWith("image")) {
                Intent intent = new Intent(context, ImageViewActivity.class);
                intent.putExtra( ImageViewActivity.URI, mediaUri.toString());
                intent.putExtra( ImageViewActivity.MIMETYPE, mimeType);

                context.startActivity(intent);
                return;
            }
            return;
        }
        else
        {


            String body = convertMediaUriToPath(mediaUri);

            if (body == null)
                body = new File(mediaUri.getPath()).getAbsolutePath();

            if (mimeType.startsWith("audio") || (body.endsWith("3gp")||body.endsWith("3gpp")||body.endsWith("amr")))
            {

                if (mMediaPlayer != null)
                    mMediaPlayer.release();

                try
                {
                    mMediaPlayer = new  MediaPlayer();
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setDataSource(body);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();

                    return;
                } catch (IOException e) {
                    Log.e(ImApp.LOG_TAG,"error playing audio: " + body,e);
                }


            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 11)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            //set a general mime type not specific
            intent.setDataAndType(Uri.parse( body ), mimeType);

            Context context = getContext().getApplicationContext();

            if (isIntentAvailable(context,intent))
            {
                context.startActivity(intent);
            }
            else
            {
                Toast.makeText(getContext(), R.string.there_is_no_viewer_available_for_this_file_format, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void reshareMediaFile (String mimeType, Uri mediaUri)
    {
        Intent shareIntent = new Intent(context, ImUrlActivity.class);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setDataAndType(mediaUri,mimeType);
        context.startActivity(shareIntent);

    }

    private void exportMediaFile (String mimeType, Uri mediaUri, java.io.File exportPath)
    {
        try {

            SecureMediaStore.exportContent(mimeType, mediaUri, exportPath);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportPath));
            shareIntent.setType(mimeType);
            context.startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.export_media)));
        } catch (IOException e) {
            Toast.makeText(getContext(), "Export Failed " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }




    private String formatMessage (String body)
    {
        if (body != null)
            return android.text.Html.fromHtml(body).toString();
        else
            return null;
    }



    private SpannableString formatTimeStamp(Date date, int messageType, DateFormat format, GalleryListItem.DeliveryState delivery, EncryptionState encryptionState) {


        StringBuilder deliveryText = new StringBuilder();
        deliveryText.append(format.format(date));
        deliveryText.append(' ');

        if (delivery != null)
        {
            //this is for delivery
            if (delivery == DeliveryState.DELIVERED) {

                deliveryText.append(DELIVERED_SUCCESS);

            } else if (delivery == DeliveryState.UNDELIVERED) {

                deliveryText.append(DELIVERED_FAIL);
            }
        }
        
        if (messageType != Imps.MessageType.POSTPONED)
            deliveryText.append(DELIVERED_SUCCESS);//this is for sent, so we know show 2 checks like WhatsApp!

        SpannableString spanText = null;

        if (encryptionState == EncryptionState.ENCRYPTED)
        {
            deliveryText.append('X');
            spanText = new SpannableString(deliveryText.toString());
            int len = spanText.length();

            spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_lock_outline_black_18dp), len-1,len,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        else if (encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            deliveryText.append('X');
            spanText = new SpannableString(deliveryText.toString());
            int len = spanText.length();

            spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_lock_outline_black_18dp), len-1,len,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        else
        {
            spanText = new SpannableString(deliveryText.toString());
            int len = spanText.length();

        }

     //   spanText.setSpan(new StyleSpan(Typeface.SANS_SERIF), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

       // spanText.setSpan(new RelativeSizeSpan(0.8f), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    //    spanText.setSpan(new ForegroundColorSpan(R.color.soft_grey),
      //        0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanText;
    }



}
