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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.widgets.ConversationViewHolder;
import org.awesomeapp.messenger.ui.widgets.GroupAvatar;
import org.awesomeapp.messenger.ui.widgets.LetterAvatar;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.GlideUtils;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;
import org.awesomeapp.messenger.util.SystemServices.FileInfo;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.Locale;

import im.zom.messenger.R;

public class ConversationListItem extends FrameLayout {

    public final String[] CHAT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
            Imps.Contacts.ACCOUNT, Imps.Contacts.USERNAME,
            Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
            Imps.Contacts.SUBSCRIPTION_TYPE,
            Imps.Contacts.SUBSCRIPTION_STATUS,
            Imps.Presence.PRESENCE_STATUS,
            Imps.Presence.PRESENCE_CUSTOM_STATUS,
            Imps.Chats.LAST_MESSAGE_DATE,
            Imps.Chats.LAST_UNREAD_MESSAGE,
            Imps.Chats.LAST_READ_DATE,
            Imps.Chats.CHAT_TYPE
            //          Imps.Contacts.AVATAR_HASH,
            //        Imps.Contacts.AVATAR_DATA

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
    public static final int COLUMN_LAST_READ_DATE = 12;
    public static final int COLUMN_CHAT_TYPE = 13;


    static Drawable AVATAR_DEFAULT_GROUP = null;
    private PrettyTime sPrettyTime = null;

    public ConversationListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        sPrettyTime = new PrettyTime(getCurrentLocale());
    }

    public void bind(ConversationViewHolder holder, long contactId, long providerId, long accountId, String address, String nickname, int contactType, String message, long messageDate, String messageType, int presence, int subscription, String underLineText, boolean showChatMsg, boolean scrolling, boolean isMuted) {

        //applyStyleColors(holder);

        if (nickname == null)
        {
            nickname = address.split("@")[0].split("\\.")[0];
        }
        else
        {
            nickname = nickname.split("@")[0].split("\\.")[0];
        }

        if (isMuted)
        {
            nickname += " \uD83D\uDD15";
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
            if (Imps.Contacts.TYPE_GROUP == (contactType & Imps.Contacts.TYPE_MASK)) {

                holder.mAvatar.setVisibility(View.VISIBLE);
                try {
                    String groupId = address.split("@")[0];
                    Drawable avatar = new GroupAvatar(groupId);
                    holder.mAvatar.setImageDrawable(avatar);
                } catch (Exception ignored) {
                    if (AVATAR_DEFAULT_GROUP == null)
                        AVATAR_DEFAULT_GROUP = new RoundedAvatarDrawable(BitmapFactory.decodeResource(getResources(),
                                R.drawable.group_chat));
                    holder.mAvatar.setImageDrawable(AVATAR_DEFAULT_GROUP);
                }
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
                        //if (avatar instanceof RoundedAvatarDrawable)
                          //  setAvatarBorder(presence,(RoundedAvatarDrawable)avatar);

                        holder.mAvatar.setImageDrawable(avatar);
                    }
                    else
                    {
                       // int color = getAvatarBorder(presence);
                        int padding = 24;
                        LetterAvatar lavatar = new LetterAvatar(getContext(), nickname, padding);
                        
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

            holder.mMediaThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.mMediaThumb.setVisibility(View.GONE);

            if (holder.mLine2 != null)
            {
                String vPath = message.split(" ")[0];

                if (SecureMediaStore.isVfsUri(vPath)||SecureMediaStore.isContentUri(vPath))
                {

                    if (TextUtils.isEmpty(messageType))
                    {
                        holder.mMediaThumb.setVisibility(View.VISIBLE);
                        holder.mMediaThumb.setImageResource(R.drawable.ic_attach_file_black_36dp);
                        holder.mMediaThumb.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        holder.mLine2.setText("");
                    }
                    else if (messageType.startsWith("image"))
                    {
                        
                        if (holder.mMediaThumb != null)
                        {
                            holder.mMediaThumb.setVisibility(View.VISIBLE);

                            if (messageType != null && messageType.equals("image/png"))
                            {
                                holder.mMediaThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            }
                            else
                            {
                                holder.mMediaThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            }

                            setThumbnail(getContext().getContentResolver(), holder, Uri.parse(vPath), true);

                                    holder.mLine2.setVisibility(View.GONE);
                                    
                        }
                    }
                    else if (messageType.startsWith("audio"))
                    {
                        mLastMediaUri = null;
                        holder.mMediaThumb.setVisibility(View.VISIBLE);
                        holder.mMediaThumb.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        holder.mMediaThumb.setImageResource(R.drawable.ic_volume_up_black_24dp);
                        holder.mLine2.setText("");
                    }
                    else if (messageType.startsWith("video"))
                    {
                        mLastMediaUri = null;
                        holder.mMediaThumb.setVisibility(View.VISIBLE);
                        holder.mMediaThumb.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        holder.mMediaThumb.setImageResource(R.drawable.video256);
                        holder.mLine2.setText("");
                    }
                    else if (messageType.startsWith("application"))
                    {
                        mLastMediaUri = null;
                        holder.mMediaThumb.setVisibility(View.VISIBLE);
                        holder.mMediaThumb.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        holder.mMediaThumb.setImageResource(R.drawable.ic_attach_file_black_36dp);
                        holder.mLine2.setText("");
                    }
                    else
                    {
                        mLastMediaUri = null;
                        holder.mMediaThumb.setVisibility(View.GONE);
                        holder.mLine2.setText(messageType);
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

                        mLastMediaUri = null;
                        setThumbnail(getContext().getContentResolver(), holder, mediaUri, false);
                        holder.mLine2.setVisibility(View.GONE);

                        holder.mMediaThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        holder.mMediaThumb.setVisibility(View.VISIBLE);


                    }

                }
                else if ((!TextUtils.isEmpty(message)) && message.startsWith(":"))
                {
                    String[] cmds = message.split(":");

                    try {
                        String[] stickerParts = cmds[1].split("-");
                        String folder = stickerParts[0];
                        StringBuffer name = new StringBuffer();
                        for (int i = 1; i < stickerParts.length; i++) {
                            name.append(stickerParts[i]);
                            if (i+1<stickerParts.length)
                                name.append('-');
                        }
                        String stickerPath = "stickers/" + folder + "/" + name.toString() + ".png";

                        //make sure sticker exists
                        AssetFileDescriptor afd = getContext().getAssets().openFd(stickerPath);
                        afd.getLength();
                        afd.close();

                        //now setup the new URI for loading local sticker asset
                        Uri mediaUri = Uri.parse("asset://localhost/" + stickerPath);
                        mLastMediaUri = null;
                        setThumbnail(getContext().getContentResolver(), holder, mediaUri, false);
                        holder.mLine2.setVisibility(View.GONE);
                        holder.mMediaThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);

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
                if (chatSession.isEncrypted())
                {
                    holder.mStatusIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_encrypted_grey));
                    holder.mStatusIcon.setVisibility(View.VISIBLE);
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



                //mCurrentChatSession.getOtrChatSession();

    }

    private Uri mLastMediaUri = null;

    /**
     * @param contentResolver
     * @param aHolder
     * @param mediaUri
     */
    private void setThumbnail(final ContentResolver contentResolver, final ConversationViewHolder aHolder, final Uri mediaUri, boolean centerCrop) {

        if (mLastMediaUri != null && mLastMediaUri.getPath().equals(mediaUri.getPath()))
            return;

        mLastMediaUri = mediaUri;
        aHolder.mMediaThumb.setVisibility(View.VISIBLE);

        if (centerCrop)
            aHolder.mMediaThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        else
            aHolder.mMediaThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);


        GlideUtils.loadImageFromUri(getContext(), mediaUri, aHolder.mMediaThumb);
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

    /**
     * Returns darker version of specified <code>color</code>.
     */
    public static int darker (int color, float factor) {
        int a = Color.alpha( color );
        int r = Color.red( color );
        int g = Color.green( color );
        int b = Color.blue( color );

        return Color.argb( a,
                Math.max( (int)(r * factor), 0 ),
                Math.max( (int)(g * factor), 0 ),
                Math.max( (int)(b * factor), 0 ) );
    }


    @TargetApi(Build.VERSION_CODES.N)
    public Locale getCurrentLocale(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            return getResources().getConfiguration().locale;
        }
    }
}
