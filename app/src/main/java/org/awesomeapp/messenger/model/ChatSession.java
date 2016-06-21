/*
 * Copyright (C) 2007 Esmertec AG. Copyright (C) 2007 The Android Open Source
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

package org.awesomeapp.messenger.model;

import org.awesomeapp.messenger.crypto.OtrChatManager;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * A ChatSession represents a conversation between two users. A ChatSession has
 * a unique participant which is either another user or a group.
 */
public class ChatSession {

    private ImEntity mParticipant;
    private ChatSessionManager mManager;

    private MessageListener mListener = null;

    private boolean mIsSubscribed = true;

    private boolean mPushSent = false;
    /**
     * Creates a new ChatSession with a particular participant.
     *
     * @param participant the participant with who the user communicates.
     * @param manager the underlying network connection.
     */
    ChatSession(ImEntity participant, ChatSessionManager manager) {
        mParticipant = participant;
        mManager = manager;

   //     mHistoryMessages = new Vector<Message>();
    }

    public ImEntity getParticipant() {
        return mParticipant;
    }

    /**
    public void setParticipant(ImEntity participant) {
        mParticipant = participant;
    }*/

    /**
     * Adds a MessageListener so that it can be notified of any new message in
     * this session.
     *
     * @param listener
     */
    public void setMessageListener(MessageListener listener) {
        mListener = listener;
    }
    
    public MessageListener getMessageListener ()
    {
        return mListener;
    }

    /**
     * Sends a text message to other participant(s) in this session
     * asynchronously and adds the message to the history. TODO: more docs on
     * async callbacks.
     *
     * @param text the text to send.
     */
    // TODO these sendMessageAsync() should probably be renamed to sendMessageAsyncAndLog()/
    /*
    public void sendMessageAsync(String text) {
        Message message = new Message(text);
        sendMessageAsync(message);
    }*/

    /**
     * Sends a message to other participant(s) in this session asynchronously
     * and adds the message to the history. TODO: more docs on async callbacks.
     *
     * @param message the message to send.
     */
    public int sendMessageAsync(Message message) {

        if (mParticipant instanceof Contact) {
            OtrChatManager cm = OtrChatManager.getInstance();
            SessionID sId = cm.getSessionId(message.getFrom().getAddress(), mParticipant.getAddress().getAddress());
            SessionStatus otrStatus = cm.getSessionStatus(sId);

            message.setTo(new XmppAddress(sId.getRemoteUserId()));
            message.setType(Imps.MessageType.OUTGOING);

            boolean isOffline = !((Contact) mParticipant).getPresence().isOnline();

            if (isOffline || otrStatus != SessionStatus.ENCRYPTED) {

                if (OtrChatManager.getInstance().canDoKnockPushMessage(sId)) {

                    // ChatSecure-Push : If no session is available when sending peer message,
                    // attempt to send a "Knock" push message to the peer asking them to come online
                    //cm.sendKnockPushMessage(sId);
                    if (!mPushSent) {
                        if (otrStatus == SessionStatus.ENCRYPTED) {
                            cm.endSession(sId);
                        }

                        // ChatSecure-Push: If the remote peer is offline, send them a push
                        OtrChatManager.getInstance().sendKnockPushMessage(sId);
                        mPushSent = true;

                    }

                     message.setType(Imps.MessageType.POSTPONED);
                     return message.getType();
                }
            }

            mPushSent = false;

            if (otrStatus == SessionStatus.ENCRYPTED) {

                // ChatSecure-Push : If OTR session is available when sending peer message,
                // ensure we have exchanged Push Whitelist tokens with that peer
                cm.maybeBeginPushWhitelistTokenExchange(sId);

                boolean verified = cm.getKeyManager().isVerified(sId);

                if (verified) {
                    message.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);
                } else {
                    message.setType(Imps.MessageType.OUTGOING_ENCRYPTED);
                }

            } else if (otrStatus == SessionStatus.FINISHED) {

                /**
                if (OtrChatManager.getInstance().canDoKnockPushMessage(sId)) {
                    // ChatSecure-Push: If the remote peer is offline, send them a push
                    OtrChatManager.getInstance().sendKnockPushMessage(sId);
                }*/

            }

           // mHistoryMessages.add(message);
            boolean canSend = cm.transformSending(message);

            if (canSend) {
                mManager.sendMessageAsync(this, message);
            } else {
                //can't be sent due to OTR state
                message.setType(Imps.MessageType.POSTPONED);

            }

        }
        else if (mParticipant instanceof ChatGroup)
        {

            message.setTo(mParticipant.getAddress());
            message.setType(Imps.MessageType.OUTGOING);
          //  mHistoryMessages.add(message);
            mManager.sendMessageAsync(this, message);



        }

        return message.getType();
    }

    /**
     * Sends message + data to other participant(s) in this session asynchronously.
     *
     * @param message the message to send.
     * @param data the data to send.
     */
    public void sendDataAsync(Message message, boolean isResponse, byte[] data) {

        OtrChatManager cm = OtrChatManager.getInstance();
        sendDataAsync(cm, message, isResponse, data);


    }

    private void sendDataAsync (OtrChatManager cm, Message message, boolean isResponse, byte[] data)
    {
        SessionID sId = cm.getSessionId(message.getFrom().getAddress(), message.getTo().getAddress());
        SessionStatus otrStatus = cm.getSessionStatus(sId);

        //can't send if not encrypted session
        if (otrStatus == SessionStatus.ENCRYPTED) {

            boolean verified = cm.getKeyManager().isVerified(sId);

            if (verified) {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);
            } else {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED);
            }

            boolean canSend = cm.transformSending(message, isResponse, data);

            if (canSend)
                mManager.sendMessageAsync(this, message);
        }


    }

    /**
     * Called by ChatSessionManager when received a message of the ChatSession.
     * All the listeners registered in this session will be notified.
     *
     * @param message the received message.
     *
     * @return true if the message was processed correctly, or false
     *   otherwise (e.g. decryption error)
     */
    public boolean onReceiveMessage(Message message) {
//        mHistoryMessages.add(message);

        OtrChatManager cm = OtrChatManager.getInstance();

        if (cm != null)
        {
            SessionStatus otrStatus = cm.getSessionStatus(message.getTo().getAddress(), message.getFrom().getAddress());

            SessionID sId = cm.getSessionId(message.getTo().getAddress(),message.getFrom().getAddress());

            if (otrStatus == SessionStatus.ENCRYPTED)
            {
                boolean verified = cm.getKeyManager().isVerified(sId);

                if (verified)
                {
                    message.setType(Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED);
                }
                else
                {
                    message.setType(Imps.MessageType.INCOMING_ENCRYPTED);
                }

            }

        }

        if (mListener != null)
            return mListener.onIncomingMessage(this, message);
        else
            return false;
    }

    public void onMessageReceipt(String id) {
        if (mListener != null)
            mListener.onIncomingReceipt(this, id);

    }

    public void onMessagePostponed(String id) {
        if (mListener != null)
            mListener.onMessagePostponed(this, id);
    }

    public void onReceiptsExpected(boolean isExpected) {
        if (mListener != null)
            mListener.onReceiptsExpected(this, isExpected);
    }

    /**
     * Called by ChatSessionManager when an error occurs to send a message.
     *
     * @param message
     *
     * @param error the error information.
     */
    public void onSendMessageError(Message message, ImErrorInfo error) {
        if (mListener != null)
            mListener.onSendMessageError(this, message, error);

    }

    public void onSendMessageError(String messageId, ImErrorInfo error) {
        /**
        for (Message message : mHistoryMessages) {
            if (messageId.equals(message.getID())) {
                onSendMessageError(message, error);
                return;
            }
        }**/
     //   Log.i("ChatSession", "Message has been removed when we get delivery error:" + error);
    }

    /**
     * Returns a unmodifiable list of the history messages in this session.
     *
     * @return a unmodifiable list of the history messages in this session.
     */
    /**
    public List<Message> getHistoryMessages() {
        return Collections.unmodifiableList(mHistoryMessages);
    }*/

    public void sendPushWhitelistTokenAsync(@NonNull Message message,
                                            @NonNull String[] whitelistTokens) {

        OtrChatManager cm = OtrChatManager.getInstance();
        SessionID sId = cm.getSessionId(message.getFrom().getAddress(), mParticipant.getAddress().getAddress());
        SessionStatus otrStatus = cm.getSessionStatus(sId);

        message.setTo(new XmppAddress(sId.getRemoteUserId()));

        if (otrStatus == SessionStatus.ENCRYPTED) {
            boolean verified = cm.getKeyManager().isVerified(sId);

            if (verified) {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);
            } else {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED);
            }

            boolean canSend = cm.transformPushWhitelistTokenSending(message, whitelistTokens);

            if (canSend)
                mManager.sendMessageAsync(this, message);

        }
    }

    public boolean isSubscribed() {
        return mIsSubscribed;
    }

    public void setSubscribed(boolean isSubscribed) {
        mIsSubscribed = isSubscribed;
    }


}
