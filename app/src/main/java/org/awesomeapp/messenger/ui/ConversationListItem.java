/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import net.java.otr4j.session.SessionStatus;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.IOtrChatSession;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.widgets.ConversationViewHolder;
import org.awesomeapp.messenger.ui.widgets.LetterAvatar;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;
import org.awesomeapp.messenger.util.SystemServices.FileInfo;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import im.zom.messenger.R;

public class ConversationListItem extends FrameLayout {
    public static final String[] CONTACT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
                                                Imps.Contacts.ACCOUNT, Imps.Contacts.USERNAME,
                                                Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                                Imps.Contacts.SUBSCRIPTION_TYPE,
                                                Imps.Contacts.SUBSCRIPTION_STATUS,
                                                Imps.Presence.PRESENCE_STATUS,
                                                Imps.Presence.PRESENCE_CUSTOM_STATUS,
                                                Imps.Chats.LAST_MESSAGE_DATE,
                                                Imps.Chats.LAST_UNREAD_MESSAGE,

    };


    public static final int COLUMN_CONTACT_ID = 0;
    public static final int COLUMN_CONTACT_PROVIDER = 1;
    public static final int COLUMN_CONTACT_ACCOUNT = 2;
    public static final int COLUMN_CONTACT_USERNAME = 3;
    public static final int COLUMN_CONTACT_NICKNAME = 4;
    public static final int COLUMN_CONTACT_TYPE = 5;
    public static final int COLUMN_SUBSCRIPTION_TYPE = 6;
    public static final int COLUMN_SUBSCRIPTION_STATUS = 7;
    public static final int COLUMN_CONTACT_PRESENCE_STATUS = 8;
    public static final int COLUMN_CONTACT_CUSTOM_STATUS = 9;
    public static final int COLUMN_LAST_MESSAGE_DATE = 10;
    public static final int COLUMN_LAST_MESSAGE = 11;

    static Drawable AVATAR_DEFAULT_GROUP = null;
    private final static PrettyTime sPrettyTime = new PrettyTime();

    public ConversationListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

/**
    public void bind(ConversationViewHolder holder, Cursor cursor, String underLineText, boolean scrolling) {
        bind(holder, cursor, underLineText, true, scrolling);
    }
*/

    public void bind(ConversationViewHolder holder, long contactId, long providerId, long accountId, String address, String nickname, int contactType, String message, long messageDate, int presence, String underLineText, boolean showChatMsg, boolean scrolling) {


        if (nickname == null)
        {
            nickname = address.split("@")[0].split("\\.")[0];
        }
        else
        {
            nickname = nickname.split("@")[0].split("\\.")[0];
        }

        if (Imps.Contacts.TYPE_GROUP == contactType) {

            String groupCountString = getGroupCount(getContext().getContentResolver(), contactId);
            nickname += groupCountString;
        }

        if (!TextUtils.isEmpty(underLineText)) {
            // highlight/underline the word being searched 
            String lowercase = nickname.toLowerCase();
            int start = lowercase.indexOf(underLineText.toLowerCase());
            if (start >= 0) {
                int end = start + underLineText.length();
                SpannableString str = new SpannableString(nickname);
                str.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                holder.mLine1.setText(str);

            }
            else
                holder.mLine1.setText(nickname);

        }
        else
            holder.mLine1.setText(nickname);

        holder.mStatusIcon.setVisibility(View.GONE);

        if (holder.mAvatar != null)
        {
            if (Imps.Contacts.TYPE_GROUP == contactType) {

                holder.mAvatar.setVisibility(View.VISIBLE);

                if (AVATAR_DEFAULT_GROUP == null)
                    AVATAR_DEFAULT_GROUP = new RoundedAvatarDrawable(BitmapFactory.decodeResource(getResources(),
                            R.drawable.group_chat));


                    holder.mAvatar.setImageDrawable(AVATAR_DEFAULT_GROUP);


            }
         //   else if (cursor.getColumnIndex(Imps.Contacts.AVATAR_DATA)!=-1)
           else {
//                holder.mAvatar.setVisibility(View.GONE);

                Drawable avatar = null;

                try
                {
                    avatar = DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(), address, ImApp.SMALL_AVATAR_WIDTH, ImApp.SMALL_AVATAR_HEIGHT);
                  // avatar = DatabaseUtils.getAvatarFromCursor(cursor, COLUMN_AVATAR_DATA, ImApp.SMALL_AVATAR_WIDTH, ImApp.SMALL_AVATAR_HEIGHT);
                }
                catch (Exception e)
                {
                    //problem decoding avatar
                    Log.e(ImApp.LOG_TAG,"error decoding avatar",e);
                }

                try
                {
                    if (avatar != null)
                    {
                        if (avatar instanceof RoundedAvatarDrawable)
                            setAvatarBorder(presence,(RoundedAvatarDrawable)avatar);

                        holder.mAvatar.setImageDrawable(avatar);
                    }
                    else
                    {
                        String letterString = null;
                                
                        if (nickname.length() > 0)
                            letterString = nickname.substring(0,1);
                        else
                            letterString = "?"; //the unknown name!
                         
                        int color = getAvatarBorder(presence);
                        int padding = 24;
                        LetterAvatar lavatar = new LetterAvatar(getContext(), color, letterString, padding);
                        
                        holder.mAvatar.setImageDrawable(lavatar);

                    }

                    holder.mAvatar.setVisibility(View.VISIBLE);
                }
                catch (OutOfMemoryError ome)
                {
                    //this seems to happen now and then even on tiny images; let's catch it and just not set an avatar
                }

            }
        }

        if (showChatMsg && message != null) {


            if (holder.mLine2 != null)
            {
                if (SecureMediaStore.isVfsUri(message))
                {
                    FileInfo fInfo = SystemServices.getFileInfoFromURI(getContext(), Uri.parse(message));
                    
                    if (fInfo.type == null || fInfo.type.startsWith("image"))
                    {
                        
                        if (holder.mMediaThumb != null)
                        {
                            holder.mMediaThumb.setVisibility(View.VISIBLE);

                            if (fInfo.type != null && fInfo.type.equals("image/png"))
                            {
                                holder.mMediaThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            }
                            else
                            {
                                holder.mMediaThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            }

                            setThumbnail(getContext().getContentResolver(), holder, Uri.parse(message));

                                    holder.mLine2.setVisibility(View.GONE);
                                    
                        }
                    }
                    else
                    {
                        holder.mLine2.setText("");
                    }

                }
                else if ((!TextUtils.isEmpty(message)) && message.startsWith("/"))
                {
                    String cmd = message.toString().substring(1);

                    if (cmd.startsWith("sticker"))
                    {
                        String[] cmds = cmd.split(":");

                        String mimeTypeSticker = "image/png";
                        Uri mediaUri = Uri.parse("asset://"+cmds[1]);

                        setThumbnail(getContext().getContentResolver(), holder, mediaUri);
                        holder.mLine2.setVisibility(View.GONE);

                    }

                }
                else if ((!TextUtils.isEmpty(message)) && message.startsWith(":"))
                {
                    String[] cmds = message.split(":");

                    try {
                        String[] stickerParts = cmds[1].split("-");
                        String stickerPath = "stickers/" + stickerParts[0].toLowerCase() + "/" + stickerParts[1].toLowerCase() + ".png";

                        //make sure sticker exists
                        AssetFileDescriptor afd = getContext().getAssets().openFd(stickerPath);
                        afd.getLength();
                        afd.close();

                        //now setup the new URI for loading local sticker asset
                        Uri mediaUri = Uri.parse("asset://localhost/" + stickerPath);
                        setThumbnail(getContext().getContentResolver(), holder, mediaUri);
                        holder.mLine2.setVisibility(View.GONE);
                    } catch (Exception e) {

                    }
                }
                else
                {
                    if (holder.mMediaThumb != null)
                        holder.mMediaThumb.setVisibility(View.GONE);
                    
                    holder.mLine2.setVisibility(View.VISIBLE);



                    try {
                        holder.mLine2.setText(android.text.Html.fromHtml(message).toString());
                    }
                    catch (RuntimeException re){}
                }
            }

            if (messageDate != -1)
            {
                Date dateLast = new Date(messageDate);
                holder.mStatusText.setText(sPrettyTime.format(dateLast));

            }
            else
            {
                holder.mStatusText.setText("");
            }

        }
        else if (holder.mLine2 != null)
        {
            holder.mLine2.setText(address);

            if (holder.mMediaThumb != null)
                holder.mMediaThumb.setVisibility(View.GONE);
        }

        holder.mLine1.setVisibility(View.VISIBLE);

        if (providerId != -1)
            getEncryptionState (providerId, accountId, address, holder);
    }

    private void getEncryptionState (long providerId, long accountId, String address, ConversationViewHolder holder)
    {

         try {

             ImApp app = ((ImApp)((Activity) getContext()).getApplication());

             IImConnection conn = app.getConnection(providerId,accountId);
             if (conn == null || conn.getChatSessionManager() == null)
                 return;

            IChatSession chatSession = conn.getChatSessionManager().getChatSession(address);

            if (chatSession != null)
            {
                IOtrChatSession otrChatSession = chatSession.getDefaultOtrChatSession();
                if (otrChatSession != null)
                {
                    SessionStatus chatStatus = SessionStatus.values()[otrChatSession.getChatStatus()];

                    if (chatStatus == SessionStatus.ENCRYPTED)
                    {
                        boolean isVerified = otrChatSession.isKeyVerified(address);
                       // holder.mStatusIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_lock_outline_black_18dp));
                        holder.mStatusIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_encrypted_grey));
                        holder.mStatusIcon.setVisibility(View.VISIBLE);
                    }
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



                //mCurrentChatSession.getOtrChatSession();

    }

    public void setAvatarBorder(int status, RoundedAvatarDrawable avatar) {
        switch (status) {
        case Presence.AVAILABLE:
            avatar.setBorderColor(getResources().getColor(R.color.holo_green_light));
            break;

        case Presence.IDLE:
            avatar.setBorderColor(getResources().getColor(R.color.holo_green_dark));

            break;

        case Presence.AWAY:
            avatar.setBorderColor(getResources().getColor(R.color.holo_orange_light));
            break;

        case Presence.DO_NOT_DISTURB:
            avatar.setBorderColor(getResources().getColor(R.color.holo_red_dark));

            break;

        case Presence.OFFLINE:
            avatar.setBorderColor(getResources().getColor(android.R.color.transparent));
            break;


        default:
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

    private Uri mLastMediaUri = null;

    /**
     * @param contentResolver
     * @param aHolder
     * @param mediaUri
     */
    private void setThumbnail(final ContentResolver contentResolver, final ConversationViewHolder aHolder, final Uri mediaUri) {

        if (mLastMediaUri != null && mLastMediaUri.getPath().equals(mediaUri.getPath()))
            return;

        mLastMediaUri = mediaUri;

        Glide.clear(aHolder.mMediaThumb);

        if(SecureMediaStore.isVfsUri(mediaUri))
        {
            info.guardianproject.iocipher.File fileMedia = new info.guardianproject.iocipher.File(mediaUri.getPath());
            if (fileMedia.exists())
            {
                try {
                    Glide.with(getContext())
                            .load(new info.guardianproject.iocipher.FileInputStream(fileMedia))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(aHolder.mMediaThumb);
                }
                catch (Exception e)
                {
                    Log.e(ImApp.LOG_TAG,"unable to load thumbnail",e);
                }
            }
        }
        else if (mediaUri.getScheme().equals("asset"))
        {
            String assetPath = "file:///android_asset/" + mediaUri.getPath().substring(1);
            Glide.with(getContext())
                    .load(assetPath)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(aHolder.mMediaThumb);
        }
        else
        {
            Glide.with(getContext())
                    .load(mediaUri)
                    .into(aHolder.mMediaThumb);
        }

    }

    private String getGroupCount(ContentResolver resolver, long groupId) {
        String[] projection = { Imps.GroupMembers.NICKNAME };
        Uri uri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, groupId);
        Cursor c = resolver.query(uri, projection, null, null, null);
        StringBuilder buf = new StringBuilder();
        if (c != null) {

            buf.append(" (");
            buf.append(c.getCount());
            buf.append(")");

            c.close();
        }

        return buf.toString();
    }

}
