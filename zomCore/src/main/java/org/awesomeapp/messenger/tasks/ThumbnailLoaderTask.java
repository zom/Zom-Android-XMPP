package org.awesomeapp.messenger.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.GalleryListFragment;
import org.awesomeapp.messenger.ui.GalleryListItem;
import org.awesomeapp.messenger.ui.MediaViewHolder;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by n8fr8 on 8/10/15.
 */

public class ThumbnailLoaderTask extends AsyncTask<ThumbnailLoaderRequest, Void, ThumbnailLoaderTask.ThumbnailLoaderResult>
{
    private static LruCache<String,Bitmap> sBitmapCache;
    public final static int THUMBNAIL_SIZE_DEFAULT = 400;

    public ThumbnailLoaderTask (LruCache<String,Bitmap> bitmapCache)
    {
        sBitmapCache = bitmapCache;
       }

    @Override
    public ThumbnailLoaderTask.ThumbnailLoaderResult doInBackground(ThumbnailLoaderRequest...request)
    {

        Bitmap result=getThumbnail(request[0].mContext,request[0].mResolver,request[0].mUri,THUMBNAIL_SIZE_DEFAULT);

        ThumbnailLoaderTask.ThumbnailLoaderResult tlr=new ThumbnailLoaderTask.ThumbnailLoaderResult();
        tlr.mBitmap=result;
        tlr.mUri=request[0].mUri;
        tlr.mHolder=request[0].mHolder;

        return tlr;

       }

    @Override
    public void onPostExecute(ThumbnailLoaderResult result){

        if(result.mBitmap!=null)
        {
            sBitmapCache.put(result.mUri.toString(),result.mBitmap);

            // confirm the holder is still paired to this uri
            if(!result.mUri.equals(result.mHolder.mMediaUri))
                return;

          // set the thumbnail
            result.mHolder.mMediaThumbnail.setImageBitmap(result.mBitmap);
        }

    }

    public static Bitmap getThumbnail(Context context, ContentResolver cr,Uri uri, int thumbnailSize){

        Bitmap result = sBitmapCache.get(uri.toString());

        if (result != null) {
            return result;
        }
        else if(SecureMediaStore.isVfsUri(uri)){
            return SecureMediaStore.getThumbnailVfs(uri,thumbnailSize);
        }
        else if (uri.getScheme().equals("asset"))
        {
            AssetManager assetManager = context.getAssets();

            try {
                InputStream is = assetManager.open(uri.getPath().substring(1));
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                return bitmap;
            } catch (IOException e) {
                Log.d(ImApp.LOG_TAG,"could not load thumbnail: " + uri.getPath());
                return null;
            }

        }

        return getThumbnailFile(cr,uri,thumbnailSize);

        }

    public static Bitmap getThumbnailFile(ContentResolver cr,Uri uri,int thumbnailSize){

        try
        {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inJustDecodeBounds=true;
            options.inInputShareable=true;
            options.inPurgeable=true;

            InputStream is=cr.openInputStream(uri);
            BitmapFactory.decodeStream(is,null,options);
            if((options.outWidth==-1)||(options.outHeight==-1))
            return null;

            int originalSize=(options.outHeight>options.outWidth)?options.outHeight
            :options.outWidth;

            BitmapFactory.Options opts=new BitmapFactory.Options();
            opts.inSampleSize=originalSize/thumbnailSize;

            is=cr.openInputStream(uri);

            Bitmap scaledBitmap=BitmapFactory.decodeStream(is,null,options);

            return scaledBitmap;
        }
        catch(Exception e)
        {
            Log.d(ImApp.LOG_TAG,"could not getThumbnailFile",e);
            return null;
        }
     }

    public class ThumbnailLoaderResult {
        public Bitmap mBitmap;
        public Uri mUri;
        public MediaViewHolder mHolder;
    }

}