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

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.gcm.GcmListenerService;

import org.chatsecure.pushsecure.gcm.PushMessage;
import org.chatsecure.pushsecure.gcm.PushParser;

import java.util.Random;


/**
 * Service used for receiving GCM messages. When a message is received this service will log it.
 */
public class GcmReceiverService extends GcmListenerService {

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

        if (pushSecureMessage != null)
            postNotification(pushSecureMessage.payload, pushSecureMessage.token);
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
        int notificationId = new Random().nextInt(Integer.MAX_VALUE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentTitle(msg);
        if (fromToken != null) builder.setContentText("From " + fromToken);
        builder.setVibrate(new long[]{250, 250});
//        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

//        Intent blockIntent = new Intent(this, MainActivity.class);
//        blockIntent.setAction(REVOKE_TOKEN_ACTION);
//        blockIntent.putExtra(TOKEN_EXTRA, fromToken);
//        blockIntent.putExtra(NOTIFICATION_ID_EXTRA, notificationId);
//        blockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        PendingIntent blockPendingIntent =
//                PendingIntent.getActivity(
//                        this,
//                        BLOCK_SENDER_REQUEST,
//                        blockIntent,
//                        PendingIntent.FLAG_UPDATE_CURRENT);
//
//        builder.addAction(R.drawable.ic_block, "Block Sender", blockPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }
}