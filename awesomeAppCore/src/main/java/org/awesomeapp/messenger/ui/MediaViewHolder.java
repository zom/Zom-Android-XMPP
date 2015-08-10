package org.awesomeapp.messenger.ui;

import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import org.awesomeapp.messenger.util.SecureMediaStore;

import info.guardianproject.otr.app.im.R;

/**
 * Created by n8fr8 on 8/10/15.
 */
public class MediaViewHolder {

    public ImageView mMediaThumbnail;
    public View mContainer;

    // save the media uri while the MediaScanner is creating the thumbnail
    // if the holder was reused, the pair is broken
    public Uri mMediaUri = null;
    ImageView mActionFav;
    ImageView mActionSend;
    ImageView mActionShare;

    public MediaViewHolder (View view)
    {
        mMediaThumbnail = (ImageView) view.findViewById(R.id.media_thumbnail);
        mContainer = view.findViewById(R.id.message_container);

        mActionFav = (ImageView)view.findViewById(R.id.media_thumbnail_fav);
        mActionSend = (ImageView)view.findViewById(R.id.media_thumbnail_send);
        mActionShare = (ImageView)view.findViewById(R.id.media_thumbnail_share);
    }
}

