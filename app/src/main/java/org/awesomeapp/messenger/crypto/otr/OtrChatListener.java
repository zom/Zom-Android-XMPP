package org.awesomeapp.messenger.crypto.otr;

import android.text.TextUtils;
import android.util.Log;

import net.java.otr4j.api.SessionID;
import net.java.otr4j.api.SessionStatus;
import net.java.otr4j.api.TLV;

import org.awesomeapp.messenger.model.ChatSession;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.model.Message;
import org.awesomeapp.messenger.model.MessageListener;
import org.awesomeapp.messenger.provider.Imps;

import java.util.ArrayList;
import java.util.List;


public class OtrChatListener implements MessageListener {

    public static final int TLV_DATA_REQUEST = 0x100;
    public static final int TLV_DATA_RESPONSE = 0x101;
    private OtrChatManager mOtrChatManager;
    private MessageListener mMessageListener;

    public OtrChatListener(OtrChatManager otrChatManager, MessageListener listener) {
        this.mOtrChatManager = otrChatManager;
        this.mMessageListener = listener;
    }

    @Override
    public boolean onIncomingMessage(ChatSession session, Message msg, boolean notifyUser) {

        OtrDebugLogger.log("processing incoming message: " + msg.getID());

        boolean result = false;

        if (mOtrChatManager == null || msg.getType() != Imps.MessageType.INCOMING) {
            if (!TextUtils.isEmpty(msg.getBody())) {
                result = mMessageListener.onIncomingMessage(session, msg, notifyUser);
            } else {
                OtrDebugLogger.log("incoming body was empty");
            }

            return result;
        }
        else {

            //Do the OTR decryption thing

            String body = msg.getBody();
            String remoteAddress = msg.getFrom().getAddress();
            String localAddress = msg.getTo().getAddress();

            SessionID sessionID = mOtrChatManager.getSessionId(localAddress, remoteAddress);
            SessionStatus otrStatus = mOtrChatManager.getSessionStatus(sessionID);

            if (otrStatus == SessionStatus.ENCRYPTED) {
                boolean verified = mOtrChatManager.getKeyManager().isVerified(sessionID);

                if (verified) {
                    msg.setType(Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED);
                } else {
                    msg.setType(Imps.MessageType.INCOMING_ENCRYPTED);
                }

            }

            List<TLV> tlvs = new ArrayList<TLV>();

            try {

                body = mOtrChatManager.decryptMessage(localAddress, remoteAddress, body, tlvs);

                if (!TextUtils.isEmpty(body)) {
                    msg.setBody(body);
                    result = mMessageListener.onIncomingMessage(session, msg, notifyUser);
                } else {

                    OtrDebugLogger.log("Decrypted incoming body was null");

                }

                for (TLV tlv : tlvs) {
                    if (tlv.getType() == TLV_DATA_REQUEST) {
                        OtrDebugLogger.log("Got a TLV Data Request: " + new String(tlv.getValue()));

                        mMessageListener.onIncomingDataRequest(session, msg, tlv.getValue());
                        result = true;

                    } else if (tlv.getType() == TLV_DATA_RESPONSE) {

                        OtrDebugLogger.log("Got a TLV Data Response: " + new String(tlv.getValue()));

                        mMessageListener.onIncomingDataResponse(session, msg, tlv.getValue());
                        result = true;

                    }
                }

            } catch (Exception oe) {

                OtrDebugLogger.log("error decrypting message: " + msg.getID(),oe);
                //  mOtrChatManager.refreshSession(sessionID.getLocalUserId(),sessionID.getRemoteUserId());
                // msg.setBody("[" + "You received an unreadable encrypted message" + "]");
                // mMessageListener.onIncomingMessage(session, msg);
                //mOtrChatManager.injectMessage(sessionID, "");

            }


            SessionStatus newStatus = mOtrChatManager.getSessionStatus(sessionID.getAccountID(), sessionID.getUserID());
            if (newStatus != otrStatus) {

                OtrDebugLogger.log("OTR status changed from: " + otrStatus + " to " + newStatus);
                mMessageListener.onStatusChanged(session, newStatus);
            }

            return result;
        }
    }

    @Override
    public void onIncomingDataRequest(ChatSession session, Message msg, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onIncomingDataResponse(ChatSession session, Message msg, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSendMessageError(ChatSession session, Message msg, ImErrorInfo error) {

        mMessageListener.onSendMessageError(session, msg, error);
        OtrDebugLogger.log("onSendMessageError: " + msg.toString());
    }

    @Override
    public void onIncomingReceipt(ChatSession ses, String id) {
        mMessageListener.onIncomingReceipt(ses, id);
    }

    @Override
    public void onMessagePostponed(ChatSession ses, String id) {
        mMessageListener.onMessagePostponed(ses, id);
    }

    @Override
    public void onReceiptsExpected(ChatSession ses, boolean isExpected) {
        mMessageListener.onReceiptsExpected(ses, isExpected);
    }

    @Override
    public void onStatusChanged(ChatSession session, SessionStatus status) {
        mMessageListener.onStatusChanged(session, status);
    }

    @Override
    public void onIncomingTransferRequest(OtrDataHandler.Transfer transfer) {
        mMessageListener.onIncomingTransferRequest(transfer);
    }
}
