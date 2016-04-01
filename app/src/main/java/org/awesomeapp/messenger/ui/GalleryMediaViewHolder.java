package org.awesomeapp.messenger.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.ImageViewTarget;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.widgets.ImageViewActivity;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.File;
import java.net.URLConnection;
import java.util.Date;

import im.zom.messenger.R;

/**
 * Created by n8fr8 on 2/12/16.
 */
public class GalleryMediaViewHolder extends MediaViewHolder
{
    private Context context;

    private ImageViewTarget<Bitmap> mTarget;

    public GalleryMediaViewHolder (View view, Context context)
    {
        super(view);
        this.context = context;


    }

    public void setOnClickListenerMediaThumbnail( final String mimeType, final Uri mediaUri ) {
        mMediaThumbnail.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                   onClickMediaIcon( mimeType, mediaUri );
            }
        });

    }

    public void resetOnClickListenerMediaThumbnail() {
        mMediaThumbnail.setOnClickListener(null);
    }

    long mTimeDiff = -1;

    public void bind(int id, final String mimeType, final String body, Date date) {


        if( mimeType != null ) {

            if (mContainer.getVisibility() == View.GONE)
                mContainer.setVisibility(View.VISIBLE);

            Uri mediaUri = Uri.parse( body ) ;
            showMediaThumbnail(mimeType, mediaUri, id);

        }
        else {
            mContainer.setVisibility(View.GONE);
        }

    }

    private void showMediaThumbnail (String mimeType, Uri mediaUri, int id)
    {
        /* Guess the MIME type in case we received a file that we can display or play*/
        if (TextUtils.isEmpty(mimeType) || mimeType.startsWith("application")) {
            String guessed = URLConnection.guessContentTypeFromName(mediaUri.toString());
            if (!TextUtils.isEmpty(guessed)) {
                if (TextUtils.equals(guessed, "video/3gpp"))
                    mimeType = "audio/3gpp";
                else
                    mimeType = guessed;
            }
        }
        setOnClickListenerMediaThumbnail(mimeType, mediaUri);

        if (mMediaThumbnail.getVisibility() == View.GONE)
            mMediaThumbnail.setVisibility(View.VISIBLE);

        if( mimeType.startsWith("image/") ) {
            setImageThumbnail(context.getContentResolver(), id, mediaUri);
          //  mMediaThumbnail.setBackgroundColor(Color.TRANSPARENT);
            // holder.mMediaThumbnail.setBackgroundColor(Color.WHITE);

        }
        else if (mimeType.startsWith("audio"))
        {
            mMediaThumbnail.setImageResource(R.drawable.media_audio_play);
            mMediaThumbnail.setBackgroundColor(Color.TRANSPARENT);
        }
        else
        {
            mMediaThumbnail.setImageResource(R.drawable.ic_file); // generic file icon

        }

        //mContainer.setBackgroundColor(mContainer.getResources().getColor(android.R.color.transparent));



    }

    /**
     * @param contentResolver
     * @param id
     * @param mediaUri
     */
    private void setImageThumbnail(ContentResolver contentResolver, int id, Uri mediaUri) {
        // pair this holder to the uri. if the holder is recycled, the pairing is broken
        mMediaUri = mediaUri;
        // if a content uri - already scanned

        setThumbnail(contentResolver, mediaUri);


    }

    /**
     * @param contentResolver
     * @param mediaUri
     */
    private void setThumbnail(ContentResolver contentResolver, Uri mediaUri) {

        if (mTarget != null)
            Glide.clear(mTarget);

        mTarget = new ImageViewTarget<Bitmap>(mMediaThumbnail) {
            @Override
            protected void setResource(Bitmap resource) {

                mMediaThumbnail.setImageBitmap(resource);
            }
        };

        if(SecureMediaStore.isVfsUri(mediaUri))
        {
            try {
                Glide.with(context)
                        .load(new info.guardianproject.iocipher.FileInputStream(new File(mediaUri.getPath()).getPath()))
                        .asBitmap()
                        .placeholder(R.drawable.ic_photo_library_white_36dp)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(mTarget);
            }
            catch (Exception e)
            {
                Log.e(ImApp.LOG_TAG, "unable to load thumbnail: " + e.getMessage());
            }
        }
        else if (mediaUri.getScheme().equals("asset"))
        {
            String assetPath = "file:///android_asset/" + mediaUri.getPath().substring(1);
            Glide.with(context)
                    .load(assetPath)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                   .into(mTarget);
        }
        else
        {
            Glide.with(context)
                    .load(mediaUri)
                    .asBitmap()
                    .placeholder(R.drawable.ic_photo_library_white_36dp)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(mTarget);
        }

    }


    public void onClickMediaIcon(String mimeType, Uri mediaUri) {


        if (mimeType.startsWith("image")) {
            Intent intent = new Intent(context, ImageViewActivity.class);
            intent.putExtra( ImageViewActivity.URI, mediaUri.toString());
            intent.putExtra( ImageViewActivity.MIMETYPE, mimeType);

            context.startActivity(intent);
        }
    }


    /**
     protected void onLongClickMediaIcon(final String mimeType, final Uri mediaUri) {

     final java.io.File exportPath = SecureMediaStore.exportPath(mimeType, mediaUri);

     new AlertDialog.Builder(context)
     .setTitle(context.getString(R.string.export_media))
     .setMessage(context.getString(R.string.export_media_file_to, exportPath.getAbsolutePath()))
     .setNeutralButton("Share on Zom", new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
    reshareMediaFile(mimeType, mediaUri);
    }
    })
     .setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
    exportMediaFile(mimeType, mediaUri, exportPath);
    return;
    }
    })
     .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
    return;
    }
    })
     .create().show();
     }*/
}