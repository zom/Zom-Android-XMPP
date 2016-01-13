package org.awesomeapp.messenger.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

public class AssetUtil {

    /** Read a properties file from /assets.  Returns null if it does not exist. */
    public static Properties getProperties(String name, Context context) {
        Resources resources = context.getResources();
        AssetManager assetManager = resources.getAssets();

        // Read from the /assets directory
        try {
            InputStream inputStream = assetManager.open(name);
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            Log.i("ChatSecure", "no chatsecure.properties available");
            return null;
        }
    }

    // In this method, we need to copy the mp3 file to the sd card location from
    // where android picks up ringtone files
    // After copying, we make the mp3 as current ringtone
    public static boolean installRingtone(final Context context, int resid, final String toneName) {

        String exStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String filename = toneName + ".mp3";
        File fileAlarms = new File(exStoragePath,"/Notifications");
        final File fileTone = new File(fileAlarms,filename);

        if (fileTone.exists())
            return false;

        boolean exists = fileAlarms.exists();
        if (!exists) {
            fileAlarms.mkdirs();
        }

        if (fileTone.exists())
            return false;

        byte[] buffer = null;
        InputStream fIn = context.getResources().openRawResource(
                resid);
        int size = 0;

        try {
            size = fIn.available();
            buffer = new byte[size];
            fIn.read(buffer);
            fIn.close();
        } catch (IOException e) {
            return false;
        }


        FileOutputStream save;
        try {
            save = new FileOutputStream(fileTone);
            save.write(buffer);
            save.flush();
            save.close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        MediaScannerConnection.scanFile(context, new String[]{fileTone.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {

            @Override
            public void onScanCompleted(String path, Uri uriTone) {

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DATA, fileTone.getAbsolutePath() );
                values.put(MediaStore.MediaColumns.TITLE, toneName );
                values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
                values.put(MediaStore.Audio.Media.ARTIST, "zom");

                //new
                values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                values.put(MediaStore.Audio.Media.IS_ALARM, true);
                values.put(MediaStore.Audio.Media.IS_MUSIC, false);

                // Insert it into the database
                Uri newUri = context.getContentResolver()
                        .insert(uriTone, values);

//                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);

                //   Settings.System.putString(context.getContentResolver(),
               //         Settings.System.RINGTONE, uri.toString());

            }
        });



        return true;
    }

}
