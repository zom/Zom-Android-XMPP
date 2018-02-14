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

package org.awesomeapp.messenger.service;

import im.zom.messenger.R;


import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.Preferences;
import org.awesomeapp.messenger.RouterActivity;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.util.SoundService;
import org.awesomeapp.messenger.util.SystemServices;

import java.util.ArrayList;
import java.util.HashMap;

import org.awesomeapp.messenger.MainActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.Toast;

import static org.awesomeapp.messenger.ImApp.NOTIFICATION_CHANNEL_ID_MESSAGE;

public class StatusBarNotifier {
    private static final boolean DBG = false;

    private static final long SUPPRESS_SOUND_INTERVAL_MS = 3000L;

    static final long[] VIBRATE_PATTERN = new long[] { 0, 250, 250, 250 };

    private Context mContext;
    private NotificationManager mNotificationManager;

    private Handler mHandler;
    private ArrayList<NotificationInfo> mNotificationInfos;
    private long mLastSoundPlayedMs;

    private Vibrator mVibrator;
    private VibrationEffect mVibeEffect;
    private final static long[] VIBRATION_TIMINGS = {0,500,100,100,1000};
    public StatusBarNotifier(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mHandler = new Handler();
        mNotificationInfos = new ArrayList<>();
    }

    public void notifyChat(long providerId, long accountId, long chatId, String username,
                           String nickname, String msg, boolean lightWeightNotify) {
        if (!Preferences.isNotificationEnabled()) {
            if (DBG)
                log("notification for chat " + username + " is not enabled");
            return;
        }

        //msg = html2text(msg); // strip tags for html client inbound msgs
        Bitmap avatar = null;


        try { byte[] bdata = DatabaseUtils.getAvatarBytesFromAddress(mContext.getContentResolver(), XmppAddress.stripResource(username));
            avatar = BitmapFactory.decodeByteArray(bdata, 0, bdata.length);
        }
        catch (Exception e){}

        String title = nickname;
        String snippet = mContext.getString(R.string.new_messages_notify) + ' ' + nickname;// + ": " + msg;
        Intent intent = getDefaultIntent(accountId, providerId);//new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, chatId),Imps.Chats.CONTENT_ITEM_TYPE);
        intent.addCategory(ImApp.IMPS_CATEGORY);
        notify(username, title, snippet, msg, providerId, accountId, intent, lightWeightNotify, R.drawable.ic_discuss, avatar, true);
    }

    public void notifyGroupChat(long providerId, long accountId, long chatId, String remoteAddress, String groupname,
            String nickname, String msg, boolean lightWeightNotify) {

        Bitmap avatar = null;

        String snippet = mContext.getString(R.string.new_messages_notify) + ' ' + groupname;// + ": " + msg;
        Intent intent = getDefaultIntent(accountId, providerId);//new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, chatId),Imps.Chats.CONTENT_ITEM_TYPE);
        intent.addCategory(ImApp.IMPS_CATEGORY);
        notify(remoteAddress, groupname, snippet, nickname + ": " + msg, providerId, accountId, intent, lightWeightNotify, R.drawable.ic_discuss, avatar, true);
    }

    public void notifyError(String username, String error) {

        Intent intent = getDefaultIntent(-1,-1);
        notify(username, error, error, error, -1, -1, intent, true, R.drawable.alerts_and_states_error, false);
    }

    public void notifySubscriptionRequest(long providerId, long accountId, long contactId,
            String username, String nickname) {
        if (!Preferences.isNotificationEnabled()) {
            if (DBG)
                log("notification for subscription request " + username + " is not enabled");
            return;
        }
        String title = nickname;
        String message = mContext.getString(R.string.subscription_notify_text, nickname);
        Intent intent = getDefaultIntent(accountId, providerId);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS, username);
        intent.setType(Imps.Contacts.CONTENT_ITEM_TYPE);
        intent.addCategory(ImApp.IMPS_CATEGORY);
        notify(username, title, message, message, providerId, accountId, intent, false, R.drawable.ic_people_white_24dp, true);
    }

    public void notifySubscriptionApproved(Contact contact, long providerId, long accountId) {
        if (!Preferences.isNotificationEnabled()) {
            if (DBG)
                log("notification for subscription approved is not enabled");
            return;
        }
        String title = contact.getName();
        String message = mContext.getString(R.string.invite_accepted);
        Intent intent = getDefaultIntent(accountId, providerId);//new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS, contact.getAddress().getBareAddress());
        intent.setType(Imps.Contacts.CONTENT_ITEM_TYPE);
        intent.addCategory(ImApp.IMPS_CATEGORY);

        notify(contact.getAddress().getAddress(), title, message, message, providerId, accountId, intent, false, R.drawable.ic_people_white_24dp, false);
    }


    public void notifyGroupInvitation(long providerId, long accountId, long invitationId,
            String username) {

        Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(
                Imps.Invitation.CONTENT_URI, invitationId));

        String title = mContext.getString(R.string.notify_groupchat_label);
        String message = mContext.getString(R.string.group_chat_invite_notify_text, username);
        notify(username, title, message, message, providerId, accountId, intent, false, R.drawable.group_chat, true);
    }

    public void notifyLoggedIn(long providerId, long accountId) {

        Intent intent = new Intent(mContext, MainActivity.class);
        ;

        String title = mContext.getString(R.string.app_name);
        String message = mContext.getString(R.string.presence_available);
        notify(message, title, message, message, providerId, accountId, intent, false, R.drawable.ic_discuss, false);
    }

    public void notifyLocked() {

        Intent intent = new Intent(mContext, RouterActivity.class);

        String title = mContext.getString(R.string.app_name);
        String message = mContext.getString(R.string.account_setup_pers_now_title);
        notify(message, title, message, message, -1, -1, intent, true, R.drawable.ic_lock_outline_black_18dp,false);


    }

    public void notifyDisconnected(long providerId, long accountId) {

        Intent intent = new Intent(mContext, MainActivity.class);
        ;

        String title = mContext.getString(R.string.app_name);
        String message = mContext.getString(R.string.presence_offline);
        notify(message, title, message, message, providerId, accountId, intent, false, R.drawable.alerts_and_states_error, false);
    }


    public void notifyFile(long providerId, long accountId, long id, String username,
            String nickname, String path, Uri uri, String type, boolean b) {
     
        String message = mContext.getString(R.string.file_notify_text);
        Intent intent = SystemServices.Viewer.getViewIntent(uri, type);
        notify(message, nickname, message, message, providerId, accountId, intent, false, R.drawable.ic_stat_status, false);
    }

    public void dismissNotifications(long providerId) {

        Object[] infos = mNotificationInfos.toArray();

        for (Object nInfo : infos) {
            NotificationInfo info = (NotificationInfo)nInfo;
            if (info.mProviderId == providerId) {
                mNotificationManager.cancel(info.computeNotificationId());
                mNotificationInfos.remove(info);
            }
        }
    }

    public void dismissChatNotification(long providerId, String username) {
        Object[] infos = mNotificationInfos.toArray();

        for (Object nInfo : infos) {
            NotificationInfo info = (NotificationInfo)nInfo;
            if (info.getSender() != null && info.getSender().equals(username)) {
                mNotificationManager.cancel(info.computeNotificationId());
                mNotificationInfos.remove(info);
            }
        }
    }

    public void notify(String title, String tickerText, String message,
                        Intent intent, boolean lightWeightNotify, boolean doVibrateSound) {

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

        NotificationInfo info;
                info = new NotificationInfo(-1, -1);
            info.setInfo("", title, message, null, intent);

        mNotificationManager.notify(info.computeNotificationId(),
                info.createNotification(tickerText, lightWeightNotify, R.drawable.ic_stat_status, null, intent,doVibrateSound));


    }

    private void notify(String sender, String title, String tickerText, String message,
                        long providerId, long accountId, Intent intent, boolean lightWeightNotify, int iconSmall, boolean doVibrateSound) {

        notify(sender,title,tickerText,message,providerId,accountId,intent,lightWeightNotify,iconSmall,null, doVibrateSound);
    }

    private void notify(String sender, String title, String tickerText, String message,
                        long providerId, long accountId, Intent intent, boolean lightWeightNotify, int iconSmall, Bitmap iconLarge, boolean doVibrateSound) {

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

        NotificationInfo info = null;

        StringBuffer sbMessage = new StringBuffer();

        synchronized (mNotificationInfos) {

            for (NotificationInfo nInfo : mNotificationInfos)
            {
                if (nInfo.getSender() != null && nInfo.getSender().equals(sender))
                {
                    info = nInfo;
                    if (info.getBigMessage() != null)
                        sbMessage.append(info.getBigMessage()+'\n');
                    else
                        sbMessage.append(info.getMessage()+'\n');
                    break;
                }
            }

            if (info == null) {
                info = new NotificationInfo(providerId, accountId);
                mNotificationInfos.add(info);
                info.setInfo(sender, title, message, null, intent);

            }
            else {
                sbMessage.append(message);
                info.setInfo(sender, title, message, sbMessage.toString(), intent);
            }
        }

        mNotificationManager.notify(info.computeNotificationId(),
                info.createNotification(tickerText, lightWeightNotify, iconSmall, iconLarge, intent, doVibrateSound));





    }

    private Intent getDefaultIntent(long accountId, long providerId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType(Imps.Contacts.CONTENT_TYPE);
        intent.setClass(mContext, MainActivity.class);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, accountId);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, providerId);

        return intent;
    }



    private void setRinger(long providerId, NotificationCompat.Builder builder) {
        Uri ringtoneUri = Preferences.getNotificationRingtoneUri();
        builder.setSound(ringtoneUri);
        mLastSoundPlayedMs = SystemClock.elapsedRealtime();

        if (DBG)
            log("setRinger: notification.sound = " + ringtoneUri);

        if (Preferences.getNotificationVibrate()) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            if (DBG)
                log("setRinger: defaults |= vibrate");
        }
        else if (Preferences.getNotificationSound()||Preferences.getNotificationVibrate())
        {
            builder.setDefaults(Notification.DEFAULT_ALL);

            if (DBG)
                log("setRinger: defaults |= vibrate + sound");
        }
    }

    private static int UNIQUE_INT_PER_CALL = 10000;

    class NotificationInfo {

        private long mProviderId;
        private long mAccountId;
        private String mTitle;
        private String mMessage;
        private String mBigMessage;
        private Intent mIntent;
        private String mSender;

        public NotificationInfo(long providerId, long accountId) {
            mProviderId = providerId;
            mAccountId = accountId;
        }

        public int computeNotificationId() {
            if (mTitle == null)
                return (int)mProviderId;
            return (mSender).hashCode();
        }

        public void setInfo(String sender, String title, String message, String bigMessage, Intent intent) {
            mTitle = title;
            mMessage = message;
            mIntent = intent;
            mSender = sender;
            mBigMessage = bigMessage;
        }


        public Notification createNotification(String tickerText, boolean lightWeightNotify, int icon, Bitmap largeIcon, Intent intent, boolean doNotifyVibrateSound) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,ImApp.NOTIFICATION_CHANNEL_ID_MESSAGE);

            builder
                .setSmallIcon(icon)
                .setTicker(lightWeightNotify ? null : tickerText)
                .setWhen(System.currentTimeMillis())
                .setLights(0xff990000, 300, 1000)
                .setContentTitle(getTitle())
                .setContentText(getMessage())
                .setContentIntent(PendingIntent.getActivity(mContext, UNIQUE_INT_PER_CALL++, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true);

            if (doNotifyVibrateSound) {
                if (Preferences.getNotificationSound()) {

                    try {
                        // play sound
                        Intent serviceIntent = new Intent(mContext, SoundService.class);
                        serviceIntent.setAction("ACTION_START_PLAYBACK");
                        serviceIntent.putExtra("SOUND_URI", Preferences.getNotificationRingtoneUri().toString());
                        mContext.startService(serviceIntent);
                    } catch (java.lang.IllegalStateException e) {
                        //error can't start services in the background
                    }
                }

                if (Preferences.getNotificationVibrate() && isVibrateOn()) {

                    // play vibration
                    if (mVibrator == null) {
                        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mVibeEffect = VibrationEffect.createWaveform(VIBRATION_TIMINGS, -1);
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        mVibrator.vibrate(mVibeEffect);
                    else
                        mVibrator.vibrate(VIBRATE_PATTERN, -1);

                }
            }

            if (!TextUtils.isEmpty(mBigMessage))
            {
                /*
         * Sets the big view "big text" style and supplies the
         * text (the user's reminder message) that will be displayed
         * in the detail area of the expanded notification
         * These calls are ignored by the support library for
         * pre-4.1 devices.
         */
                builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(mBigMessage));
            }

            if (largeIcon != null)
                builder.setLargeIcon(largeIcon);

            /**
            if (!(lightWeightNotify || shouldSuppressSoundNotification())) {
                setRinger(mProviderId, builder);
            }**/

            return builder.build();
        }


        private Intent getMultipleNotificationIntent() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(mContext, MainActivity.class);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_SHOW_MULTIPLE, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            return intent;
        }

        public String getSender () {
            return mSender;
        }

        public String getTitle() {
                        return mTitle;
        }

        public String getBigMessage ()
        {
            return mBigMessage;
        }

        public String getMessage() {

            return mMessage;
        }

        public Intent getIntent() {

            return mIntent;
        }
    }

    private static void log(String msg) {
        RemoteImService.debug("[StatusBarNotify] " + msg);
    }

    private boolean shouldSuppressSoundNotification() {
        return (SystemClock.elapsedRealtime() - mLastSoundPlayedMs < SUPPRESS_SOUND_INTERVAL_MS);
    }

    /**
    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }**/
    AudioManager mAudioManager;

    private boolean isVibrateOn ()
    {
        if (mAudioManager == null)
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        int mode = mAudioManager.getRingerMode();

        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                if ((1 == Settings.System.getInt(mContext.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, 0))) {
                    return true;
                } else {
                    return false;
                }

            case AudioManager.RINGER_MODE_SILENT:
                return false;

            case AudioManager.RINGER_MODE_VIBRATE:
                return true;
        }

        return false;
    }
}
