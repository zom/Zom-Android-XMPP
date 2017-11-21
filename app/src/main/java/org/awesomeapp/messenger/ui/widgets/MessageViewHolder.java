package org.awesomeapp.messenger.ui.widgets;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.awesomeapp.messenger.ui.GalleryMediaViewHolder;
import org.awesomeapp.messenger.ui.MediaViewHolder;
import org.awesomeapp.messenger.ui.MessageListItem;

import im.zom.messenger.R;

/**
 * Created by n8fr8 on 12/11/15.
 */
public class MessageViewHolder extends MediaViewHolder
{
    public interface OnImageClickedListener {
        void onImageClicked(MessageViewHolder viewHolder, Uri image);
    }

    public TextView mTextViewForMessages;
    public TextView mTextViewForTimestamp;
    public ImageView mAvatar;

    public ViewGroup mMediaContainer;
    public ViewGroup mAudioContainer;
   // public VisualizerView mVisualizerView;
   // public ImageView mAudioButton;

    public LayoutInflater mLayoutInflater;
    // save the media uri while the MediaScanner is creating the thumbnail
    // if the holder was reused, the pair is broken

    private OnImageClickedListener onImageClickedListener;
    public AudioWife mAudioWife;

    public MessageViewHolder(View view) {
        super(view);

        mTextViewForMessages = (TextView) view.findViewById(R.id.message);
        mTextViewForTimestamp = (TextView) view.findViewById(R.id.messagets);
        mAvatar = (ImageView) view.findViewById(R.id.avatar);
        mMediaContainer = (ViewGroup)view.findViewById(R.id.media_thumbnail_container);
        mAudioContainer = (ViewGroup)view.findViewById(R.id.audio_container);
       // mVisualizerView = (VisualizerView) view.findViewById(R.id.audio_view);
       // mAudioButton = (ImageView) view.findViewById(R.id.audio_button);

        // disable built-in autoLink so we can add custom ones
        mTextViewForMessages.setAutoLinkMask(0);
        //mContainer.setBackgroundResource(R.drawable.message_view_rounded_light);
    }

    public void setOnImageClickedListener(OnImageClickedListener listener) {
        this.onImageClickedListener = listener;
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
                    if (mimeType.startsWith("image")) {
                     if (onImageClickedListener != null) {
                         onImageClickedListener.onImageClicked(MessageViewHolder.this, mediaUri);
                     }
                    } else {
                        ((MessageListItem) itemView).onClickMediaIcon(mimeType, mediaUri);
                    }
                }
            });


        }

    }

    public void resetOnClickListenerMediaThumbnail() {
        mMediaThumbnail.setOnClickListener( null );
    }

    long mTimeDiff = -1;

    public void setLayoutInflater (LayoutInflater layoutInflater)
    {
        mLayoutInflater = layoutInflater;
    }
}
