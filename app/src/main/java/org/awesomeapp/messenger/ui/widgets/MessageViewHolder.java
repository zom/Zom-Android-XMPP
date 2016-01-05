package org.awesomeapp.messenger.ui.widgets;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.awesomeapp.messenger.ui.MediaViewHolder;
import org.awesomeapp.messenger.ui.MessageListItem;

import im.zom.messenger.R;

/**
 * Created by n8fr8 on 12/11/15.
 */
public class MessageViewHolder extends MediaViewHolder
{
    public TextView mTextViewForMessages;
    public TextView mTextViewForTimestamp;
    public ImageView mAvatar;

    public View mMediaContainer;
    public View mAudioContainer;
    public VisualizerView mVisualizerView;
    public ImageView mAudioButton;
    // save the media uri while the MediaScanner is creating the thumbnail
    // if the holder was reused, the pair is broken

    public MessageViewHolder(View view) {
        super(view);

        mTextViewForMessages = (TextView) view.findViewById(R.id.message);
        mTextViewForTimestamp = (TextView) view.findViewById(R.id.messagets);
        mAvatar = (ImageView) view.findViewById(R.id.avatar);
        mMediaContainer = view.findViewById(R.id.media_thumbnail_container);
        mAudioContainer = view.findViewById(R.id.audio_container);
        mVisualizerView = (VisualizerView) view.findViewById(R.id.audio_view);
        mAudioButton = (ImageView) view.findViewById(R.id.audio_button);

        // disable built-in autoLink so we can add custom ones
        mTextViewForMessages.setAutoLinkMask(0);

    }


    public void setOnClickListenerMediaThumbnail( final String mimeType, final Uri mediaUri ) {

        if (mimeType.startsWith("audio") && mAudioContainer != null)
        {
            mAudioContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MessageListItem)itemView).onClickMediaIcon(mimeType, mediaUri);
                }
            });

        }
        else {
            mMediaThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MessageListItem)itemView).onClickMediaIcon(mimeType, mediaUri);
                }
            });


        }

    }

    public void resetOnClickListenerMediaThumbnail() {
        mMediaThumbnail.setOnClickListener( null );
    }

    long mTimeDiff = -1;
}
