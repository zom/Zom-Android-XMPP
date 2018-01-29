/**
 *
 */
package org.awesomeapp.messenger.util;

import im.zom.messenger.R;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.w3c.dom.Text;

/**
 *
 * @author liorsaar
 *
 */

/*
 * Usage:
 * String filePath = writeFile() ;
 * Uri fileUri = SystemService.Scanner.scan( context, filePath ) ; // scan that one file
 * the notification will launch the target activity with the file uri
 * SystemServices.Ntfcation.sent( context, fileUri, NewChatActivity.class ) ;
 * in the target activity call:
 * Uri uri = getIntent().getData() ;
 * SystemServices.Viewer.viewImage( context, uri ) ;
 */
public class SystemServices {
    static class Ntfcation {
        public static void send(Context aContext, Uri aUri, Class<Activity> aTargetActivityClass) {
            NotificationManager mNotificationManager = (NotificationManager)aContext.getSystemService(Context.NOTIFICATION_SERVICE);

            int icon = R.drawable.ic_action_message;
            CharSequence tickerText = "Secured download completed!"; // TODO string
            long when = System.currentTimeMillis();

            Notification notification = new Notification(icon, tickerText, when);
            CharSequence contentTitle = "ChatSecure notification";  // TODO string
            CharSequence contentText = "A secured file was successfuly downloaded.";  // TODO string
            Intent notificationIntent = new Intent(aContext, aTargetActivityClass);
            notificationIntent.setData(aUri); // when the target activity is invoked, extract this uri and call viewImage()
            PendingIntent contentIntent = PendingIntent.getActivity(aContext, 0, notificationIntent, 0);
            //notification.setLatestEventInfo(aContext, contentTitle, contentText, contentIntent);

            mNotificationManager.notify(1, notification);
        }
    }

    public static class Scanner {
        // after writing the file to sd, invoke this to scan a single file without callback
        public static Uri scan(Context aContext, String aPath) {
            File file = new File(aPath);
            Uri uri = Uri.fromFile(file);
            Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            aContext.sendBroadcast(scanFileIntent);
            return uri;
        }
    }

    public static class Viewer {
        public static void viewImage(Context aContext, Uri aUri) {
            view(aContext, aUri, "image/*");
        }

        public static void view(Context aContext, Uri aUri, String aMime) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(aUri, aMime);
            aContext.startActivity(intent);
        }

        public static Intent getViewIntent(Uri uri, String type) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, type);
            return intent;
        }
    }

    public static String sanitize(String path) {
        try {
            return URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class FileInfo {
       // public File file;
        public InputStream stream;
        public String type;
        public String name;
    }
    
    public final static String MIME_TYPE_JPEG = "image/jpeg";
    public final static String MIME_TYPE_PNG = "image/png";
    
    public static String getMimeType(String url)
    {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);            
        }
        
        if (type == null)
            if (url.endsWith("jpg"))
                return MIME_TYPE_JPEG;        
            else if (url.endsWith("png"))
                return MIME_TYPE_PNG;
        
        return type;
    }

    /**
    public static FileInfo getDocumentInfoFromURI(Context aContext, Uri uri) throws IllegalArgumentException {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        Cursor cursor = aContext.getContentResolver()
                .query(uri, null, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
               // Log.i(TAG, "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                String size = null;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }


              //  Log.i(TAG, "Size: " + size);
            }
        } finally {
            cursor.close();
        }
    }

    }
**/

    public static FileInfo getFileInfoFromURI(Context aContext, Uri uri) throws IllegalArgumentException, FileNotFoundException, UnsupportedEncodingException {
        FileInfo info = new FileInfo();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        if (SecureMediaStore.isVfsUri(uri)) {
            info.name = URLEncoder.encode(uri.getLastPathSegment(),"UTF-8");
            info.stream = new info.guardianproject.iocipher.FileInputStream(uri.getPath());
            String type = getMimeType(uri.toString());
            if (!TextUtils.isEmpty(type)) {
                info.type = type;
                return info;
            }
        }
        else if (new File(uri.toString()).exists()) {
            File file = new File(uri.toString());
            info.name =  file.getName();
            info.stream = new FileInputStream(file);
            info.type = aContext.getContentResolver().getType(uri);
            if (!TextUtils.isEmpty(info.type)) {
                info.type = getMimeType(uri.toString());;
                return info;
            }
        }
        else
        {
            info.name = URLEncoder.encode(uri.getLastPathSegment(),"UTF-8");
            info.stream = aContext.getContentResolver().openInputStream(uri);
        }

        if (TextUtils.isEmpty(info.type))
            info.type = aContext.getContentResolver().getType(uri);

        if (!TextUtils.isEmpty(info.type))
            return info;

        if (uri.toString().contains("photos")||uri.toString().contains("images"))
            info.type="image/jpeg"; //assume a jpeg

        Cursor cursor = aContext.getContentResolver().query(uri, null, null, null, null);

        try {
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();

                //need to check columns for different types
                int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                if (dataIdx != -1) {
                    String data = cursor.getString(dataIdx);
                    if (data != null) {
                        info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
                    }
                    else
                    {
                        dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

                        if (dataIdx != -1) {
                            data = cursor.getString(dataIdx);
                            if (data != null) {
                                info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));
                            }
                        }
                    }


                } else {

                    dataIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA);

                    if (dataIdx != -1) {
                        info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
                    } else {
                        dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

                        if (dataIdx != -1) {
                            info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
                        } else {
                            dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

                            if (dataIdx != -1) {
                                info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));

                            }
                            else
                            {

                            }
                        }
                    }


                }
            }
        }
        catch (Exception e)
        {
            Log.e("SystemService","Error retrieving file info: " + uri,e);
        }

        if (cursor != null)
            cursor.close();

        if (info.type == null)
            info.type = getMimeType(uri.getLastPathSegment());

        return info;
    }

    public static FileInfo getContactAsVCardFile(Context context, Uri uri) {
        AssetFileDescriptor fd;
        try {
            fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            java.io.FileInputStream in = fd.createInputStream();
            byte[] buf = new byte[(int) fd.getDeclaredLength()];
            in.read(buf);
            in.close();
            String vCardText = new String(buf);
            Log.d("Vcard", vCardText);
            List<String> pathSegments = uri.getPathSegments();
            String targetPath = "/" + pathSegments.get(pathSegments.size() - 1) + ".vcf";
            SecureMediaStore.copyToVfs(buf, targetPath);
            FileInfo info = new FileInfo();
            info.stream = new info.guardianproject.iocipher.FileInputStream(SecureMediaStore.vfsUri(targetPath).getPath());
            info.type = "text/vcard";
            return info;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
