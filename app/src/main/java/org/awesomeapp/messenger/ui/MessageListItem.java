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

import im.zom.messenger.R;

import org.awesomeapp.messenger.ImUrlActivity;
import org.awesomeapp.messenger.ui.widgets.MessageViewHolder;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.legacy.Markup;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.widgets.ImageViewActivity;
import org.awesomeapp.messenger.ui.widgets.LetterAvatar;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.LinkifyHelper;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Browser;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class MessageListItem extends FrameLayout {

    public enum DeliveryState {
        NEUTRAL, DELIVERED, UNDELIVERED
    }

    public enum EncryptionState {
        NONE, ENCRYPTED, ENCRYPTED_AND_VERIFIED

    }

    private CharSequence lastMessage = null;
    private Uri mediaUri = null;
    private String mimeType = null;

    private Context context;
    private boolean linkify = false;

    public MessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    private MessageViewHolder mHolder = null;

    private final static PrettyTime sPrettyTime = new PrettyTime();
    private final static Date DATE_NOW = new Date();


    /**
     * This trickery is needed in order to have clickable links that open things
     * in a new {@code Task} rather than in ChatSecure's {@code Task.} Thanks to @commonsware
     * https://stackoverflow.com/a/11417498
     *
     */
    class NewTaskUrlSpan extends ClickableSpan {

        private String urlString;

        NewTaskUrlSpan(String urlString) {
            this.urlString = urlString;
        }

        @Override
        public void onClick(View widget) {
            Uri uri = Uri.parse(urlString);
            Context context = widget.getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    class URLSpanConverter implements LinkifyHelper.SpanConverter<URLSpan, ClickableSpan> {
        @Override
        public NewTaskUrlSpan convert(URLSpan span) {
            return (new NewTaskUrlSpan(span.getURL()));
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

    }

    public void setLinkify(boolean linkify) {
        this.linkify = linkify;
    }

    public URLSpan[] getMessageLinks() {
        return mHolder.mTextViewForMessages.getUrls();
    }


    public String getLastMessage () {
        return lastMessage.toString();
    }

    public void bindIncomingMessage(MessageViewHolder holder, int id, int messageType, String address, String nickname, final String mimeType, final String body, Date date, Markup smileyRes,
            boolean scrolling, EncryptionState encryption, boolean showContact, int presenceStatus) {

        mHolder = holder;

        mHolder.mTextViewForMessages.setVisibility(View.VISIBLE);
        mHolder.mAudioContainer.setVisibility(View.GONE);
        mHolder.mMediaContainer.setVisibility(View.GONE);
        mHolder.mContainer.setBackgroundResource(R.drawable.message_view_rounded_light);

        if (nickname == null)
            nickname = address;

        lastMessage = formatMessage(body);
        showAvatar(address, nickname, true, presenceStatus);

        mHolder.resetOnClickListenerMediaThumbnail();

        if( mimeType != null ) {

            Uri mediaUri = Uri.parse(body);
            lastMessage = "";

            if (mimeType.startsWith("audio"))
            {
                mHolder.mAudioButton.setImageResource(R.drawable.media_audio_play);

                try {
                    mHolder.mAudioContainer.setVisibility(View.VISIBLE);
                    showAudioPlayer(mimeType, mediaUri, id, mHolder);
                }
                catch (Exception e)
                {
                    mHolder.mAudioContainer.setVisibility(View.GONE);
                }

            }
            else {
                mHolder.mTextViewForMessages.setVisibility(View.GONE);

                showMediaThumbnail(mimeType, mediaUri, id, mHolder);

                mHolder.mMediaContainer.setVisibility(View.VISIBLE);

            }

        }
        else if (lastMessage.length() > 1 && lastMessage.charAt(0) == '/')
        {
            boolean cmdSuccess = false;

            String cmd = lastMessage.toString().substring(1);
            if (cmd.startsWith("sticker"))
            {
                String[] cmds = cmd.split(":");

                String mimeTypeSticker = "image/png";

                try {

                    String assetPath = cmds[1].split(" ")[0];//just get up to any whitespace;

                    //make sure sticker exists
                    AssetFileDescriptor afd = getContext().getAssets().openFd(assetPath);
                    afd.getLength();
                    afd.close();

                    //now setup the new URI for loading local sticker asset
                    Uri mediaUri = Uri.parse("asset://localhost/" + assetPath);

                    //now load the thumbnail
                    cmdSuccess = showMediaThumbnail(mimeTypeSticker, mediaUri, id, mHolder);
                }
                catch (Exception e)
                {
                    Log.e(ImApp.LOG_TAG, "error loading sticker bitmap: " + cmds[1],e);
                    cmdSuccess = false;
                }

            }

            if (!cmdSuccess)
            {
                SpannableString spannablecontent=new SpannableString(lastMessage);
                mHolder.mTextViewForMessages.setText(spannablecontent);
                mHolder.mContainer.setBackgroundResource(R.drawable.message_view_rounded_light);
            }
            else
            {

                lastMessage = "";
            }

        }
        else {

            mHolder.mContainer.setBackgroundResource(R.drawable.message_view_rounded_light);

        }

        if (isSelected())
            mHolder.mContainer.setBackgroundColor(getResources().getColor(R.color.holo_blue_bright));

        if (lastMessage.length() > 0)
        {
                SpannableString spannablecontent=new SpannableString(lastMessage);

                mHolder.mTextViewForMessages.setText(spannablecontent);

        }
        else
        {
            mHolder.mTextViewForMessages.setText(lastMessage);
        }


        if (date != null)
        {

            String contact = null;
            if (showContact) {
                String[] nickParts = nickname.split("/");
                contact = nickParts[nickParts.length-1];
            }

           CharSequence tsText = formatTimeStamp(date,messageType, null, encryption, contact);


         mHolder.mTextViewForTimestamp.setText(tsText);
         mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);

        }
        else
        {

            mHolder.mTextViewForTimestamp.setText("");
            //mHolder.mTextViewForTimestamp.setVisibility(View.GONE);

        }
        if (linkify)
            LinkifyHelper.addLinks(mHolder.mTextViewForMessages, new URLSpanConverter());
        LinkifyHelper.addTorSafeLinks(mHolder.mTextViewForMessages);
    }

    private boolean showMediaThumbnail (String mimeType, Uri mediaUri, int id, MessageViewHolder holder)
    {
        this.mediaUri = mediaUri;
        this.mimeType = mimeType;

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

        holder.setOnClickListenerMediaThumbnail(mimeType, mediaUri);

        holder.mTextViewForMessages.setText(lastMessage);
        holder.mTextViewForMessages.setVisibility(View.GONE);

        if( mimeType.startsWith("image/") ) {
            setImageThumbnail( getContext().getContentResolver(), id, holder, mediaUri );
            holder.mMediaThumbnail.setBackgroundColor(Color.TRANSPARENT);
        }
        else
        {

            try {
                Glide.with(context)
                        .load(R.drawable.ic_file)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(holder.mMediaThumbnail);
            }
            catch (Exception e)
            {
                Log.e(ImApp.LOG_TAG,"unable to load thumbnail",e);
            }
            holder.mMediaThumbnail.setImageResource(R.drawable.ic_file); // generic file icon
            holder.mTextViewForMessages.setText(mediaUri.getLastPathSegment());
            holder.mTextViewForMessages.setVisibility(View.VISIBLE);

        }

        holder.mMediaContainer.setVisibility(View.VISIBLE);


        holder.mContainer.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        return true;

    }

    private void showAudioPlayer (String mimeType, Uri mediaUri, int id, MessageViewHolder holder) throws Exception
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

        holder.setOnClickListenerMediaThumbnail(mimeType, mediaUri);
        mHolder.mTextViewForMessages.setText("");
        mAudioPlayer = new AudioPlayer(getContext(), mediaUri.getPath(), mimeType, mHolder.mVisualizerView,mHolder.mTextViewForMessages);
        holder.mContainer.setBackgroundColor(getResources().getColor(android.R.color.transparent));

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

    private AudioPlayer mAudioPlayer;

    public void onClickMediaIcon(String mimeType, Uri mediaUri) {


        if (mimeType.startsWith("image")) {
            Intent intent = new Intent(context, ImageViewActivity.class);
            intent.putExtra( ImageViewActivity.URI, mediaUri.toString());
            intent.putExtra( ImageViewActivity.MIMETYPE, mimeType);

            context.startActivity(intent);
        }
        else if (mimeType.startsWith("audio")) {

                if (mAudioPlayer.getDuration() != -1)
                    mHolder.mTextViewForMessages.setText((mAudioPlayer.getDuration()/1000) + "secs");

                if (mAudioPlayer.isPlaying())
                {
                    mHolder.mAudioButton.setImageResource(R.drawable.media_audio_play);
                    mAudioPlayer.pause();
                }
                else
                {
                    mHolder.mAudioButton.setImageResource(R.drawable.media_audio_pause);
                    mAudioPlayer.play();
                }


        }
        else
        {
            exportMediaFile();
            /**
            String body = convertMediaUriToPath(mediaUri);

            if (body == null)
                body = new File(mediaUri.getPath()).getAbsolutePath();


            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 11)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            //set a general mime type not specific
            intent.setDataAndType(Uri.parse(body), mimeType);


            Context context = getContext().getApplicationContext();

            if (isIntentAvailable(context, intent))
            {
                context.startActivity(intent);
            }
            else
            {

                intent = new Intent(Intent.ACTION_SEND);
                intent.setDataAndType(Uri.parse(body), mimeType);

                if (isIntentAvailable(context, intent))
                {
                    context.startActivity(intent);
                }
                else {
                    Toast.makeText(getContext(), R.string.there_is_no_viewer_available_for_this_file_format, Toast.LENGTH_LONG).show();
                }
            }**/
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

    private void forwardMediaFile (String mimeType, Uri mediaUri)
    {

        String resharePath = "vfs:/" + mediaUri.getPath();
        Intent shareIntent = new Intent(context, ImUrlActivity.class);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setDataAndType(Uri.parse(resharePath), mimeType);
        context.startActivity(shareIntent);


    }

    public void forwardMediaFile ()
    {
        if (mimeType != null && mediaUri != null) {
            forwardMediaFile(mimeType, mediaUri);
        }
        else
        {
            Intent shareIntent = new Intent(context, ImUrlActivity.class);
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, lastMessage);
            shareIntent.setType("text/plain");
            context.startActivity(shareIntent);
        }
    }

    public void exportMediaFile ()
    {
        if (mimeType != null && mediaUri != null) {
            java.io.File exportPath = SecureMediaStore.exportPath(mimeType, mediaUri);
            exportMediaFile(mimeType, mediaUri, exportPath);
        }
        else
        {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT,lastMessage);
            shareIntent.setType("text/plain");
            context.startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.export_media)));
        }

    };

    private void exportMediaFile (String mimeType, Uri mediaUri, java.io.File exportPath)
    {
        try {

            SecureMediaStore.exportContent(mimeType, mediaUri, exportPath);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportPath));
            shareIntent.setType(mimeType);
            context.startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.export_media)));
        } catch (Exception e) {
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


    /**
     * @param contentResolver
     * @param id
     * @param aHolder
     * @param mediaUri
     */
    private void setImageThumbnail(final ContentResolver contentResolver, final int id, final MessageViewHolder aHolder, final Uri mediaUri) {

        //if the same URI, we don't need to reload
        if (aHolder.mMediaUri != null && aHolder.mMediaUri.getPath().equals(mediaUri.getPath()))
            return;

        // pair this holder to the uri. if the holder is recycled, the pairing is broken
        aHolder.mMediaUri = mediaUri;
        // if a content uri - already scanned

        if(SecureMediaStore.isVfsUri(mediaUri))
        {
            try {
                Glide.with(context)
                        .load(new info.guardianproject.iocipher.FileInputStream(new File(mediaUri.getPath()).getPath()))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(aHolder.mMediaThumbnail);
            }
            catch (Exception e)
            {
                Log.e(ImApp.LOG_TAG,"unable to load thumbnail",e);
            }
        }
        else if (mediaUri.getScheme().equals("asset"))
        {
            String assetPath = "file:///android_asset/" + mediaUri.getPath().substring(1);
            Glide.with(context)
                    .load(assetPath)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(aHolder.mMediaThumbnail);
        }
        else
        {
            Glide.with(context)
                    .load(mediaUri)
                    .into(aHolder.mMediaThumbnail);
        }


    }


    private String formatMessage (String body)
    {

        if (body != null)
            try {
                return (android.text.Html.fromHtml(body).toString()); //this happens on Xiaomi sometimes
            }
            catch (RuntimeException re){
                return "";
            }
        else
            return "";
    }

    public void bindOutgoingMessage(MessageViewHolder holder, int id, int messageType, String address, final String mimeType, final String body, Date date, Markup smileyRes, boolean scrolling,
            DeliveryState delivery, EncryptionState encryption) {

        mHolder = holder;

        mHolder.mTextViewForMessages.setVisibility(View.VISIBLE);
        mHolder.mAudioContainer.setVisibility(View.GONE);
        mHolder.mMediaContainer.setVisibility(View.GONE);
        mHolder.mContainer.setBackgroundResource(R.drawable.message_view_rounded_light);
        mHolder.mAudioButton.setImageResource(R.drawable.media_audio_play);

        mHolder.resetOnClickListenerMediaThumbnail();

        lastMessage = body;

        if( mimeType != null ) {

            lastMessage = "";

            Uri mediaUri = Uri.parse( body ) ;

            if (mimeType.startsWith("audio"))
            {
                try {
                    mHolder.mAudioContainer.setVisibility(View.VISIBLE);
                    showAudioPlayer(mimeType, mediaUri, id, mHolder);
                }
                catch (Exception e)
                {
                    mHolder.mAudioContainer.setVisibility(View.GONE);
                }

            }
            else {
                mHolder.mTextViewForMessages.setVisibility(View.GONE);

                mHolder.mMediaContainer.setVisibility(View.VISIBLE);
                showMediaThumbnail(mimeType, mediaUri, id, mHolder);

            }

        }
        else if ((!TextUtils.isEmpty(lastMessage)) && lastMessage.charAt(0) == '/' && lastMessage.length()>1)
        {
            String cmd = lastMessage.toString().substring(1);
            boolean cmdSuccess = false;

            if (cmd.startsWith("sticker"))
            {
                String[] cmds = cmd.split(":");

                String mimeTypeSticker = "image/png";
                try {
                    //make sure sticker exists
                    AssetFileDescriptor afd = getContext().getAssets().openFd(cmds[1]);
                    afd.getLength();
                    afd.close();

                    //now setup the new URI for loading local sticker asset
                    Uri mediaUri = Uri.parse("asset://localhost/" + cmds[1]);

                     //now load the thumbnail
                    cmdSuccess = showMediaThumbnail(mimeTypeSticker, mediaUri, id, mHolder);
                }
                catch (Exception e)
                {
                    cmdSuccess = false;
                }

            }

            if (!cmdSuccess)
            {
                SpannableString spannablecontent=new SpannableString(lastMessage);
                mHolder.mTextViewForMessages.setText(spannablecontent);
                mHolder.mContainer.setBackgroundResource(R.drawable.message_view_rounded_light);
            }
            else
            {

                lastMessage = "";
            }

        }
        else {

             SpannableString spannablecontent=new SpannableString(lastMessage);
            mHolder.mTextViewForMessages.setText(spannablecontent);
            mHolder.mContainer.setBackgroundResource(R.drawable.message_view_rounded_light);

        }

        if (isSelected())
            mHolder.mContainer.setBackgroundColor(getResources().getColor(R.color.holo_blue_bright));

        if (date != null)
        {

            CharSequence tsText = formatTimeStamp(date,messageType, delivery, encryption, null);

            mHolder.mTextViewForTimestamp.setText(tsText);

            mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);

        }
        else
        {
            mHolder.mTextViewForTimestamp.setText("");

        }
        if (linkify)
            LinkifyHelper.addLinks(mHolder.mTextViewForMessages, new URLSpanConverter());
        LinkifyHelper.addTorSafeLinks(mHolder.mTextViewForMessages);
    }

    private void showAvatar (String address, String nickname, boolean isLeft, int presenceStatus)
    {
        if (mHolder.mAvatar == null)
            return;

        mHolder.mAvatar.setVisibility(View.GONE);

        if (address != null && isLeft)
        {

            RoundedAvatarDrawable avatar = null;

            try { avatar = (RoundedAvatarDrawable)DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(), XmppAddress.stripResource(address), ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT);}
            catch (Exception e){}

            if (avatar != null)
            {
                mHolder.mAvatar.setVisibility(View.VISIBLE);
                mHolder.mAvatar.setImageDrawable(avatar);

                setAvatarBorder(presenceStatus, avatar);

            }
            else
            {
                int color = getAvatarBorder(presenceStatus);
                int padding = 16;
                LetterAvatar lavatar = new LetterAvatar(getContext(), color, nickname.substring(0,1).toUpperCase(), padding);

                mHolder.mAvatar.setVisibility(View.VISIBLE);
                mHolder.mAvatar.setImageDrawable(lavatar);
            }
        }
    }

    public int getAvatarBorder(int status) {
        switch (status) {
        case Presence.AVAILABLE:
            return (getResources().getColor(R.color.holo_green_light));

        case Presence.IDLE:
            return (getResources().getColor(R.color.holo_green_dark));
        case Presence.AWAY:
            return (getResources().getColor(R.color.holo_orange_light));

        case Presence.DO_NOT_DISTURB:
            return(getResources().getColor(R.color.holo_red_dark));

        case Presence.OFFLINE:
            return(getResources().getColor(R.color.holo_grey_dark));

        default:
        }

        return Color.TRANSPARENT;
    }

    public void bindPresenceMessage(MessageViewHolder holder, String contact, int type, Date date, boolean isGroupChat, boolean scrolling) {

        mHolder = holder;
        mHolder.mContainer.setBackgroundResource(android.R.color.transparent);
        mHolder.mTextViewForMessages.setVisibility(View.GONE);
        mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);

        CharSequence message = formatPresenceUpdates(contact, type, date, isGroupChat, scrolling);
        mHolder.mTextViewForTimestamp.setText(message);


    }

    public void bindErrorMessage(int errCode) {

        mHolder = (MessageViewHolder)getTag();

        mHolder.mTextViewForMessages.setText(R.string.msg_sent_failed);
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.error));

    }

    private SpannableString formatTimeStamp(Date date, int messageType, MessageListItem.DeliveryState delivery, EncryptionState encryptionState, String nickname) {


        StringBuilder deliveryText = new StringBuilder();

        if (nickname != null)
        {
            deliveryText.append(nickname);
            deliveryText.append(' ');
        }

        deliveryText.append(sPrettyTime.format(date));

        SpannableString spanText = null;

        spanText = new SpannableString(deliveryText.toString());

        if (delivery != null)
        {
            deliveryText.append(' ');
            //this is for delivery

            if (messageType == Imps.MessageType.POSTPONED)
            {
                //do nothing
            }
            else if (delivery == DeliveryState.DELIVERED) {

                if (encryptionState == EncryptionState.ENCRYPTED || encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED)
                {
                    deliveryText.append("XX");
                    spanText = new SpannableString(deliveryText.toString());
                    int len = spanText.length();

                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_delivered_grey), len - 2, len - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_encrypted_grey), len - 1, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                } else{
                    deliveryText.append("X");
                    spanText = new SpannableString(deliveryText.toString());
                    int len = spanText.length();

                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_delivered_grey), len-1, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                }



            } else if (delivery == DeliveryState.UNDELIVERED) {


                if (encryptionState == EncryptionState.ENCRYPTED||encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED) {
                    deliveryText.append("X");
                    spanText = new SpannableString(deliveryText.toString());
                    int len = spanText.length();
                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_encrypted_grey), len - 1, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }


            }
            else if (delivery == DeliveryState.NEUTRAL) {

                if (encryptionState == EncryptionState.ENCRYPTED||encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED) {
                    deliveryText.append("XX");
                    spanText = new SpannableString(deliveryText.toString());
                    int len = spanText.length();
                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_sent_grey),len-2,len-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_encrypted_grey), len - 1, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                else
                {
                    deliveryText.append("X");
                    spanText = new SpannableString(deliveryText.toString());
                    int len = spanText.length();
                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_sent_grey),len-1,len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                }

            }


        }
        else
        {
            if (encryptionState == EncryptionState.ENCRYPTED||encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED)
            {
                deliveryText.append('X');
                spanText = new SpannableString(deliveryText.toString());
                int len = spanText.length();

                if (encryptionState == EncryptionState.ENCRYPTED||encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED)
                    spanText.setSpan(new ImageSpan(getContext(), R.drawable.ic_encrypted_grey), len-1, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return spanText;
    }

    private CharSequence formatPresenceUpdates(String contact, int type, Date date, boolean isGroupChat,
            boolean scrolling) {
        String body;

        Resources resources =getResources();

        switch (type) {
        case Imps.MessageType.PRESENCE_AVAILABLE:
            body = resources.getString(isGroupChat ? R.string.contact_joined
                                                   : R.string.contact_online, contact);
            break;

        case Imps.MessageType.PRESENCE_AWAY:
            body = resources.getString(R.string.contact_away, contact);
            break;

        case Imps.MessageType.PRESENCE_DND:
            body = resources.getString(R.string.contact_busy, contact);
            break;

        case Imps.MessageType.PRESENCE_UNAVAILABLE:
            body = resources.getString(isGroupChat ? R.string.contact_left
                                                   : R.string.contact_offline, contact);
            break;

        default:
            return null;
        }

        body += " - ";
        body += formatTimeStamp(date,type, null, EncryptionState.NONE, null);

        if (scrolling) {
            return body;
        } else {
            SpannableString spanText = new SpannableString(body);
            int len = spanText.length();
            spanText.setSpan(new StyleSpan(Typeface.ITALIC), 0, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanText.setSpan(new RelativeSizeSpan((float) 0.8), 0, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spanText;
        }
    }

    public void setAvatarBorder(int status, RoundedAvatarDrawable avatar) {
        switch (status) {
        case Presence.AVAILABLE:
            avatar.setBorderColor(getResources().getColor(R.color.holo_green_light));
            avatar.setAlpha(255);
            break;

        case Presence.IDLE:
            avatar.setBorderColor(getResources().getColor(R.color.holo_green_dark));
            avatar.setAlpha(255);

            break;

        case Presence.AWAY:
            avatar.setBorderColor(getResources().getColor(R.color.holo_orange_light));
            avatar.setAlpha(255);
            break;

        case Presence.DO_NOT_DISTURB:
            avatar.setBorderColor(getResources().getColor(R.color.holo_red_dark));
            avatar.setAlpha(255);

            break;

        case Presence.OFFLINE:
            avatar.setBorderColor(getResources().getColor(R.color.holo_grey_light));
            avatar.setAlpha(150);
            break;


        default:
        }
    }

}
