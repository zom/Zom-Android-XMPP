package org.awesomeapp.messenger.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.awesomeapp.messenger.ImApp;

public class GlideUtils {
    public static RequestOptions noDiskCacheOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE);

    public static boolean loadVideoFromUri(Context context, Uri uri, ImageView imageView) {
        if(SecureMediaStore.isVfsUri(uri))
        {
            try {
                info.guardianproject.iocipher.File fileVideo = new info.guardianproject.iocipher.File(uri.getPath());
                if (fileVideo.exists())
                {
                    Glide.with(context)
                            .load(new info.guardianproject.iocipher.FileInputStream(fileVideo))
                            .apply(noDiskCacheOptions)
                            .into(imageView);
                    return true;
                }
                return false;
            }
            catch (Exception e)
            {
                Log.w(ImApp.LOG_TAG,"unable to load image: " + uri.toString());
            }
        }
        else if (uri.getScheme() != null && uri.getScheme().equals("asset"))
        {
            String assetPath = "file:///android_asset/" + uri.getPath().substring(1);
            Glide.with(context)
                    .load(assetPath)
                    .apply(noDiskCacheOptions)
                    .into(imageView);
            return true;
        }
        else
        {
            Glide.with(context)
                    .load(uri)
                    .into(imageView);
            return true;
        }

        return false;
    }

    public static void loadImageFromUri(Context context, Uri uri, ImageView imageView) {
        if(SecureMediaStore.isVfsUri(uri))
        {
            try {
                info.guardianproject.iocipher.File fileImage = new info.guardianproject.iocipher.File(uri.getPath());
                if (fileImage.exists())
                {
                    Glide.with(context)
                            .load(new info.guardianproject.iocipher.FileInputStream(fileImage))
                            .apply(noDiskCacheOptions)
                            .into(imageView);
                }
            }
            catch (Exception e)
            {
                Log.w(ImApp.LOG_TAG,"unable to load image: " + uri.toString());
            }
        }
        else if (uri.getScheme() != null && uri.getScheme().equals("asset"))
        {
            String assetPath = "file:///android_asset/" + uri.getPath().substring(1);
            Glide.with(context)
                    .load(assetPath)
                    .apply(noDiskCacheOptions)
                    .into(imageView);
        }
        else
        {
            Glide.with(context)
                    .load(uri)
                    .into(imageView);
        }
    }
}
