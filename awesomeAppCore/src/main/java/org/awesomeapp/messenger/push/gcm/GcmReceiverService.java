/*
Copyright 2015 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.awesomeapp.messenger.push.gcm;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.push.model.PushDatabase;
import org.awesomeapp.messenger.service.StatusBarNotifier;
import org.chatsecure.pushsecure.gcm.PushMessage;
import org.chatsecure.pushsecure.gcm.PushParser;


/**
 * Service used for receiving GCM messages. When a message is received this service will log it.
 */
public class GcmReceiverService extends GcmListenerService {

    private static final String TAG = "GcmReceiverService";

    /**
     * Intent Action
     */
    public static final String REVOKE_TOKEN_ACTION = "org.chatsecure.blocktoken";

    /**
     * Token Intents
     */
    public static final String TOKEN_EXTRA = "token";
    public static final String NOTIFICATION_ID_EXTRA = "notId";

    /**
     * PendingIntent Request Codes
     */
    public static final int BLOCK_SENDER_REQUEST = 100;

    private PushParser parser = new PushParser();

    public GcmReceiverService() {
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {

        PushMessage pushSecureMessage = parser.parseBundle(from, data);

        Log.d(TAG, "Got GCM Message!");
        if (pushSecureMessage != null) {
            Log.d(TAG, String.format("Got ChatSecure-Push GCM Message with payload: %s from token %s", pushSecureMessage.payload, pushSecureMessage.token));
            postNotification(pushSecureMessage.payload, pushSecureMessage.token);
        }
    }

    @Override
    public void onDeletedMessages() {
        postNotification("Deleted messages on server");
    }

    @Override
    public void onMessageSent(String msgId) {
        postNotification("Upstream message sent. Id=" + msgId);
    }

    @Override
    public void onSendError(String msgId, String error) {
        postNotification("Upstream message send error. Id=" + msgId + ", error" + error);
    }

    private void postNotification(String msg) {
        postNotification(msg, null);
    }

    private void postNotification(String msg, String fromToken) {

        final String notificationMessage = "Bob wants to chat!";
        final boolean lightWeightNotify = false;

        // First, fetch the local user's Account
        // Zom only has one active Imps Account, so we can use this shortcut.
        Cursor accountCursor = getContentResolver().query(Imps.Account.CONTENT_URI, null, null, null, null);
        if (accountCursor == null || !accountCursor.moveToFirst()) {
            Log.e(TAG, "Failed to query active account");
            return;
        }
        final long accountId = accountCursor.getLong(accountCursor.getColumnIndex(Imps.Account._ID));
        final long providerId = accountCursor.getLong(accountCursor.getColumnIndex(Imps.Account.PROVIDER));

        accountCursor.close();

        // Use the push token to lookup the remote contact's username
        Cursor tokenCursor = getContentResolver().query(PushDatabase.Tokens.CONTENT_URI, null, PushDatabase.Tokens.TOKEN + " = ?", new String[] {fromToken}, null);
        if (tokenCursor == null || !tokenCursor.moveToFirst()) {
            Log.e(TAG, "Failed to query Whitelist Token matching push message");
            return;
        }

        final String expectedSenderJid = tokenCursor.getString(tokenCursor.getColumnIndex(PushDatabase.Tokens.RECIPIENT));

        tokenCursor.close();

        // Fetch the local Contact representing the push message's issuer
        Cursor contactCursor = getContentResolver().query(Imps.Contacts.CONTENT_URI, null, Imps.Contacts.USERNAME + " = ?", new String[]{expectedSenderJid}, null);
        if (contactCursor == null || !contactCursor.moveToFirst()) {
            Log.d(TAG, "Failed to query contact matching Whitelist Token");
            return;
        }

        final long expectedContactId = contactCursor.getInt(contactCursor.getColumnIndex(Imps.Contacts._ID));
        final String expctedContactNickName = contactCursor.getString(contactCursor.getColumnIndex(Imps.Contacts.NICKNAME));

        contactCursor.close();

        // Fetch chat with contact.
        Cursor chatCursor = getContentResolver().query(Imps.Chats.CONTENT_URI, null, Imps.Chats.CONTACT_ID + " = ?", new String[]{String.valueOf(expectedContactId)}, null);
        if (chatCursor == null || !chatCursor.moveToFirst()) {
            Log.d(TAG, "Failed to query chat matching contact");
            // TODO : We can create a Chat Session for the remote contact.
            return;
        }

        final long chatId = chatCursor.getLong(chatCursor.getColumnIndex(Imps.Chats._ID));

        chatCursor.close();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, chatId), Imps.Chats.CONTENT_ITEM_TYPE);
        intent.addCategory(ImApp.IMPS_CATEGORY);

        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                StatusBarNotifier notifier = new StatusBarNotifier(GcmReceiverService.this);
                notifier.notifyChat(providerId, accountId, expectedContactId, expectedSenderJid, expctedContactNickName, notificationMessage, lightWeightNotify);
            }
        });
    }

    private void logChats() {
        Cursor chatCursor = getContentResolver().query(Imps.Chats.CONTENT_URI, null, null, null, null);

        StringBuilder builder = new StringBuilder();
        builder.append("All Chats:\nId\tJid\tContactId\n");
        if (chatCursor != null && chatCursor.moveToFirst()) {
            do {
                builder.append(chatCursor.getInt(chatCursor.getColumnIndex(Imps.Chats._ID)));
                builder.append('\t');
                builder.append(chatCursor.getString(chatCursor.getColumnIndex(Imps.Chats.JID_RESOURCE)));
                builder.append('\t');
                builder.append(chatCursor.getInt(chatCursor.getColumnIndex(Imps.Chats.CONTACT_ID)));
                builder.append('\n');
            } while (chatCursor.moveToNext());
        }

        Log.d(TAG, builder.toString());

        if (chatCursor != null) chatCursor.close();
    }
}