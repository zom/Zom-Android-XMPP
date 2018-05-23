package org.awesomeapp.messenger.crypto.otr;

import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.model.Message;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.adapters.ChatSessionAdapter;
import org.awesomeapp.messenger.service.adapters.ChatSessionManagerAdapter;
import org.awesomeapp.messenger.service.adapters.ImConnectionAdapter;
import org.awesomeapp.messenger.service.RemoteImService;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Hashtable;

import net.java.otr4j.api.InstanceTag;
import net.java.otr4j.api.OtrEngineHost;
import net.java.otr4j.api.OtrException;
import net.java.otr4j.api.OtrPolicy;
import net.java.otr4j.api.SessionID;

import android.content.Context;

import javax.annotation.Nonnull;

/*
 * OtrEngineHostImpl is the connects this app and the OtrEngine
 * http://code.google.com/p/otr4j/wiki/QuickStart
 */
public class OtrEngineHostImpl implements OtrEngineHost {

    private OtrPolicy mPolicy;

    private OtrAndroidKeyManagerImpl mOtrKeyManager;

    private Context mContext;

    private RemoteImService mImService;

    public OtrEngineHostImpl(OtrPolicy policy, Context context, OtrAndroidKeyManagerImpl otrKeyManager, RemoteImService imService) throws IOException {
        mPolicy = policy;
        mContext = context;
        mOtrKeyManager = otrKeyManager;
        mImService = imService;

    }

    public ImConnectionAdapter findConnection(SessionID session) {

        return mImService.getConnection(Address.stripResource(session.getAccountID()));
    }

    public KeyPair getKeyPair(SessionID sessionID) {
        KeyPair kp = null;
        kp = mOtrKeyManager.loadLocalKeyPair(sessionID);

        if (kp == null) {
            mOtrKeyManager.generateLocalKeyPair(sessionID);
            kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
        }
        return kp;
    }

    public OtrPolicy getSessionPolicy(SessionID sessionID) {
        return mPolicy;
    }

    @Override
    public int getMaxFragmentSize(@Nonnull SessionID sessionID) {
        return Integer.MAX_VALUE;
    }

    @Override
    public KeyPair getLocalKeyPair(SessionID sessionID) {
        return getKeyPair(sessionID);
    }

    @Override
    public byte[] getLocalFingerprintRaw(SessionID sessionID) {
        return mOtrKeyManager.getLocalFingerprintRaw(sessionID);
    }

    @Override
    public void askForSecret(SessionID sessionID, InstanceTag instanceTag, String s) {

    }


    @Override
    public void unverify(SessionID sessionID, String s) {

    }

    @Override
    public String getReplyForUnreadableMessage(SessionID sessionID) {
        return "The message could not be read";
    }

    @Override
    public String getFallbackMessage(SessionID sessionID) {
        return "Please try again";
    }

    @Override
    public void messageFromAnotherInstanceReceived(SessionID sessionID) {
        OtrDebugLogger.log(sessionID.toString() + ": messageFromAnotherInstanceReceived");

    }

    @Override
    public void multipleInstancesDetected(SessionID sessionID) {
        OtrDebugLogger.log(sessionID.toString() + ": multipleInstancesDetected");
    }

    @Override
    public void extraSymmetricKeyDiscovered(@Nonnull SessionID sessionID, @Nonnull String s, @Nonnull byte[] bytes, @Nonnull byte[] bytes1) {
        OtrDebugLogger.log(sessionID.toString() + ": extraSymmetricKeyDiscovered");

    }

    public void setSessionPolicy(OtrPolicy policy) {
        mPolicy = policy;
    }

    public void injectMessage(SessionID sessionID, String text) {

        ImConnectionAdapter connection = findConnection(sessionID);
        if (connection != null)
        {
            ChatSessionManagerAdapter chatSessionManagerAdapter = (ChatSessionManagerAdapter) connection
                    .getChatSessionManager();
            ChatSessionAdapter chatSessionAdapter = (ChatSessionAdapter) chatSessionManagerAdapter
                    .getChatSession(sessionID.getUserID());

            if (chatSessionAdapter == null)
                chatSessionAdapter = (ChatSessionAdapter)chatSessionManagerAdapter.createChatSession(sessionID.getUserID(),true);

            String body = text;

            if (body == null)
                body = ""; //don't allow null messages, only empty ones!

            Message msg = new Message(body);
            Address to = new XmppAddress(sessionID.getUserID());
            msg.setTo(to);

            /**
            if (!to.getAddress().contains("/")) {
                //always send OTR messages to a resource
                msg.setTo(appendSessionResource(sessionID, to));
            }**/

            msg.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);

            // msg ID is set by plugin
            chatSessionManagerAdapter.getChatSessionManager().sendMessageAsync(chatSessionAdapter.getAdaptee(), msg);


        }
        else
        {
            OtrDebugLogger.log(sessionID.toString() + ": could not find ImConnection");

        }


    }

    @Override
    public void unreadableMessageReceived(SessionID sessionID)  {
        OtrDebugLogger.log(sessionID.toString() + ": unreadableMessageReceived");

    }

    @Override
    public void unencryptedMessageReceived(SessionID sessionID, String s)  {
        OtrDebugLogger.log(sessionID.toString() + ": unencryptedMessageReceived=" + s);

    }

    public void showError(SessionID sessionID, String error) {
        OtrDebugLogger.log(sessionID.toString() + ": ERROR=" + error);


    }

    @Override
    public void smpError(SessionID sessionID, int i, boolean b)  {

    }

    @Override
    public void smpAborted(SessionID sessionID)  {

    }

    @Override
    public void verify(@Nonnull SessionID sessionID, @Nonnull String s) {

    }

    @Override
    public void finishedSessionMessage(SessionID sessionID, String s)  {
        OtrDebugLogger.log(sessionID.toString() + ": finishedSessionMessage=" + s);

    }

    @Override
    public void requireEncryptedMessage(SessionID sessionID, String s)  {
        OtrDebugLogger.log(sessionID.toString() + ": requireEncryptedMessage=" + s);

    }


}
