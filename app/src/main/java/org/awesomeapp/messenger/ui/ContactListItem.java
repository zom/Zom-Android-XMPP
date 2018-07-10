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


import im.zom.messenger.R;

import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;

import org.awesomeapp.messenger.ui.widgets.LetterAvatar;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.LogCleaner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

public class ContactListItem extends FrameLayout {

    public static final String[] CONTACT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
                                                Imps.Contacts.ACCOUNT, Imps.Contacts.USERNAME,
                                                Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                                Imps.Contacts.SUBSCRIPTION_TYPE,
                                                Imps.Contacts.SUBSCRIPTION_STATUS,
                                                Imps.Presence.PRESENCE_STATUS,
                                                Imps.Presence.PRESENCE_CUSTOM_STATUS,
                                                Imps.Chats.LAST_MESSAGE_DATE,
                                                Imps.Chats.LAST_UNREAD_MESSAGE,
                                                Imps.Chats.LAST_READ_DATE

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

    static Drawable AVATAR_DEFAULT_GROUP = null;

    private ContactViewHolder mHolder;

    private boolean mShowPresence = false;

    public ContactListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactViewHolder getViewHolder() {
        return mHolder;
    }

    public void setViewHolder(ContactViewHolder viewHolder) {
        mHolder = viewHolder;
    }

    public void bind (ContactViewHolder holder, Contact contact, OnClickListener confirmListener, OnClickListener denyListener) {
        String address = contact.getAddress().getBareAddress();
        String nickname = contact.getName();

        int typeUnmasked = Imps.ContactsColumns.TYPE_NORMAL;
        final int type = typeUnmasked & Imps.Contacts.TYPE_MASK;

        //    final String lastMsg = cursor.getString(COLUMN_LAST_MESSAGE);

        //   long lastMsgDate = cursor.getLong(COLUMN_LAST_MESSAGE_DATE);
        final int presence = contact.getPresence().getStatus();

        final int subType = contact.getSubscriptionType();
        final int subStatus = contact.getSubscriptionStatus();

        String statusText = "";

        //   nickname += " " + holder.mContactId + " " + holder.mAccountId + " " + holder.mProviderId;


        if (TextUtils.isEmpty(nickname))
        {
            nickname = address.split("@")[0].split("\\.")[0];
        }
        else
        {
            nickname = nickname.split("@")[0].split("\\.")[0];
        }


        //    nickname += " " + subType;// + " " + subStatus;
         holder.mLine1.setText(nickname);

        //holder.mStatusIcon.setVisibility(View.GONE);

        if (holder.mAvatar != null)
        {
            if (Imps.Contacts.TYPE_GROUP == (type & Imps.Contacts.TYPE_MASK)) {

                holder.mAvatar.setVisibility(View.VISIBLE);

                if (AVATAR_DEFAULT_GROUP == null)
                    AVATAR_DEFAULT_GROUP = new RoundedAvatarDrawable(BitmapFactory.decodeResource(getResources(),
                            R.drawable.group_chat));


                holder.mAvatar.setImageDrawable(AVATAR_DEFAULT_GROUP);

            }
            else
            {

                Drawable avatar = null;

                try
                {
                    avatar = DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(), address, ImApp.SMALL_AVATAR_WIDTH, ImApp.SMALL_AVATAR_HEIGHT);

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
                        //int color = getAvatarBorder(presence);
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

        statusText = address;

        if ((typeUnmasked & Imps.Contacts.TYPE_FLAG_HIDDEN) != 0)
        {
            statusText += " | " + getContext().getString(R.string.action_archive);
        }

        if (holder.mLine2 != null)
            holder.mLine2.setText(statusText);

        if (Imps.Contacts.TYPE_NORMAL == type)
        {

            if (subStatus == Imps.ContactsColumns.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING) {

                if (subType == Imps.ContactsColumns.SUBSCRIPTION_TYPE_FROM) {

                    if (holder.mSubBox != null) {
                        holder.mSubBox.setVisibility(View.VISIBLE);
                        holder.mButtonSubApprove.setOnClickListener(confirmListener);
                        holder.mButtonSubDecline.setOnClickListener(denyListener);
                    }

                }
                else
                {
                    if (holder.mSubBox != null)
                        holder.mSubBox.setVisibility(View.GONE);

                    if (holder.mLine2 != null)
                        holder.mLine2.setText(getContext().getString(R.string.title_pending));
                }

            }
            else
            {
                if (holder.mSubBox != null)
                    holder.mSubBox.setVisibility(View.GONE);
            }

        }

        if (mShowPresence) {
            switch (presence) {
                case Presence.AVAILABLE:
                    if (holder.mLine2 != null)
                        holder.mLine2.setText(getContext().getString(R.string.presence_available));

                case Presence.IDLE:
                    if (holder.mLine2 != null)
                        holder.mLine2.setText(getContext().getString(R.string.presence_available));
                    /**
                     case Presence.AWAY:
                     return (getResources().getColor(R.color.holo_orange_light));

                     case Presence.DO_NOT_DISTURB:
                     return(getResources().getColor(R.color.holo_red_dark));

                     case Presence.OFFLINE:
                     return(getResources().getColor(R.color.holo_grey_dark));
                     **/
                default:
            }
        }

        holder.mLine1.setVisibility(View.VISIBLE);
    }

    public void bind(ContactViewHolder holder, Cursor cursor, String underLineText, boolean scrolling) {
        bind(holder, cursor, underLineText, true, scrolling);
    }

    public void bind(ContactViewHolder holder, Cursor cursor, String underLineText, boolean showChatMsg, boolean scrolling) {

        final String address = cursor.getString(COLUMN_CONTACT_USERNAME);
        String nickname = cursor.getString(COLUMN_CONTACT_NICKNAME);

        int typeUnmasked = cursor.getInt(COLUMN_CONTACT_TYPE);
        final int type = typeUnmasked & Imps.Contacts.TYPE_MASK;

    //    final String lastMsg = cursor.getString(COLUMN_LAST_MESSAGE);

     //   long lastMsgDate = cursor.getLong(COLUMN_LAST_MESSAGE_DATE);
        final int presence = cursor.getInt(COLUMN_CONTACT_PRESENCE_STATUS);

        final int subType = cursor.getInt(COLUMN_SUBSCRIPTION_TYPE);
        final int subStatus = cursor.getInt(COLUMN_SUBSCRIPTION_STATUS);

     //   String statusText = cursor.getString(COLUMN_CONTACT_CUSTOM_STATUS);


        if (TextUtils.isEmpty(nickname))
        {
            nickname = address.split("@")[0].split("\\.")[0];
        }
        else
        {
            nickname = nickname.split("@")[0].split("\\.")[0];
        }

        final Contact contact = new Contact(new XmppAddress(address), nickname, Imps.Contacts.TYPE_NORMAL);

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

        //holder.mStatusIcon.setVisibility(View.GONE);

        if (holder.mAvatar != null)
        {
            if (Imps.Contacts.TYPE_GROUP == (type & Imps.Contacts.TYPE_MASK)) {

                holder.mAvatar.setVisibility(View.VISIBLE);

                if (AVATAR_DEFAULT_GROUP == null)
                    AVATAR_DEFAULT_GROUP = new RoundedAvatarDrawable(BitmapFactory.decodeResource(getResources(),
                            R.drawable.group_chat));


                    holder.mAvatar.setImageDrawable(AVATAR_DEFAULT_GROUP);

            }
            else
            {

                Drawable avatar = null;

                try
                {
                    avatar = DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(), address, ImApp.SMALL_AVATAR_WIDTH, ImApp.SMALL_AVATAR_HEIGHT);

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
                        //int color = getAvatarBorder(presence);
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

        String statusText = address;

        if ((typeUnmasked & Imps.Contacts.TYPE_FLAG_HIDDEN) != 0)
        {
            statusText += " | " + getContext().getString(R.string.action_archive);
        }

        if (holder.mLine2 != null)
            holder.mLine2.setText(statusText);

        if (Imps.Contacts.TYPE_NORMAL == type)
        {

            if (subStatus == Imps.ContactsColumns.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING) {

                if (subType == Imps.ContactsColumns.SUBSCRIPTION_TYPE_FROM) {

                    if (holder.mSubBox != null) {
                        holder.mSubBox.setVisibility(View.VISIBLE);

                        holder.mButtonSubApprove.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                approveSubscription(contact);
                                mHolder.itemView.performClick();
                            }

                        });

                        holder.mButtonSubDecline.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                declineSubscription(contact);
                            }
                        });
                    }

                }
                else
                {
                    if (holder.mSubBox != null)
                        holder.mSubBox.setVisibility(View.GONE);

                    if (holder.mLine2 != null)
                            holder.mLine2.setText(getContext().getString(R.string.title_pending));
                }

            }
            else
            {
                if (holder.mSubBox != null)
                    holder.mSubBox.setVisibility(View.GONE);
            }

        }

        if (mShowPresence) {
            switch (presence) {
                case Presence.AVAILABLE:
                    if (holder.mLine2 != null)
                        holder.mLine2.setText(getContext().getString(R.string.presence_available));

                case Presence.IDLE:
                    if (holder.mLine2 != null)
                        holder.mLine2.setText(getContext().getString(R.string.presence_available));
                    /**
                     case Presence.AWAY:
                     return (getResources().getColor(R.color.holo_orange_light));

                     case Presence.DO_NOT_DISTURB:
                     return(getResources().getColor(R.color.holo_red_dark));

                     case Presence.OFFLINE:
                     return(getResources().getColor(R.color.holo_grey_dark));
                     **/
                default:
            }
        }

        holder.mLine1.setVisibility(View.VISIBLE);
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


    /*
    private String queryGroupMembers(ContentResolver resolver, long groupId) {
        String[] projection = { Imps.GroupMembers.NICKNAME };
        Uri uri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, groupId);
        Cursor c = resolver.query(uri, projection, null, null, null);
        StringBuilder buf = new StringBuilder();
        if (c != null) {
            while (c.moveToNext()) {
                buf.append(c.getString(0));
                                                Imps.Avatars.DATA
                if (!c.isLast()) {
                    buf.append(',');
                }
            }
        }
        c.close();

        return buf.toString();
    }*/

    void approveSubscription(Contact contact) {

        ImApp app = ((ImApp)((Activity)getContext()).getApplication());
        IImConnection mConn = app.getConnection(mHolder.mProviderId, mHolder.mAccountId);

        if (mConn != null)
        {
            try {
                IContactListManager manager = mConn.getContactListManager();
                manager.approveSubscription(contact);
            } catch (RemoteException e) {

                // mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "approve sub error",e);
            }
        }
    }

    void declineSubscription(Contact contact) {

        ImApp app = ((ImApp)((Activity)getContext()).getApplication());
        IImConnection mConn = app.getConnection(mHolder.mProviderId, mHolder.mAccountId);

        if (mConn != null)
        {
            try {
                IContactListManager manager = mConn.getContactListManager();
                manager.declineSubscription(contact);
                app.dismissChatNotification(mHolder.mProviderId,contact.getAddress().getBareAddress());
                manager.removeContact(contact.getAddress().getBareAddress());
            } catch (RemoteException e) {
                // mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "decline sub error",e);
            }
        }
    }

    void deleteContact (Contact contact)
    {
        try {

            IImConnection mConn;
            ImApp app = ((ImApp)((Activity)getContext()).getApplication());
            mConn = app.getConnection(mHolder.mProviderId, mHolder.mAccountId);

            IContactListManager manager = mConn.getContactListManager();

            int res = manager.removeContact(contact.getAddress().getBareAddress());
            if (res != ImErrorInfo.NO_ERROR) {
                //mHandler.showAlert(R.string.error,
                //      ErrorResUtils.getErrorRes(getResources(), res, address));
            }

        }
        catch (RemoteException re)
        {

        }
    }

    public void applyStyleColors (ContactViewHolder holder)
    {
        //not set color
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        int themeColorHeader = settings.getInt("themeColor",-1);
        int themeColorText = settings.getInt("themeColorText",-1);
        int themeColorBg = settings.getInt("themeColorBg",-1);


        if (themeColorText != -1)
        {
            if (holder.mLine1 != null)
                holder.mLine1.setTextColor(themeColorText);

            if (holder.mLine2 != null)
                holder.mLine2.setTextColor(themeColorText);

            //holder.mLine2.setTextColor(darker(themeColorText,2.0f));

        }

    }

    public void setShowPresence (boolean showPresence)
    {
        mShowPresence = showPresence;
    }



}
