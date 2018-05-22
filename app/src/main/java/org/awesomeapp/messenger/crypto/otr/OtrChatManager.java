package org.awesomeapp.messenger.crypto.otr;

// Originally: package com.zadov.beem;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.Message;
import org.awesomeapp.messenger.service.RemoteImService;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/*
 * OtrChatManager keeps track of the status of chats and their OTR stuff
 */
public class OtrChatManager implements OtrEngineListener {

    //the singleton instance
    private static OtrChatManager mInstance;

    private OtrEngineHostImpl mOtrEngineHost;
    private Hashtable<String, Session> mSessions;

    private Context mContext;
    private ImApp mApp;

    private final static String TAG = "OtrChatManager";

    private OtrChatManager(int otrPolicy, RemoteImService imService, OtrAndroidKeyManagerImpl otrKeyManager) throws Exception {

        mContext = imService;

        mOtrEngineHost = new OtrEngineHostImpl(new OtrPolicyImpl(otrPolicy),
                mContext, otrKeyManager, imService);

        mSessions = new Hashtable<>();

        mApp = ((ImApp)mContext.getApplicationContext());


    }



    public static synchronized OtrChatManager getInstance(int otrPolicy, RemoteImService imService, OtrAndroidKeyManagerImpl otrKeyManager)
            throws Exception {
        if (mInstance == null) {
            mInstance = new OtrChatManager(otrPolicy, imService,otrKeyManager);
        }

        return mInstance;
    }

    public static OtrChatManager getInstance()
    {
        return mInstance;
    }


    public void setPolicy(int otrPolicy) {
        mOtrEngineHost.setSessionPolicy(new OtrPolicyImpl(otrPolicy));
    }

    public OtrKeyManager getKeyManager() {
        return mOtrEngineHost.getKeyManager();
    }

    public static String processResource(String userId) {
        String[] splits = userId.split("/", 2);
        if (splits.length > 1)
            return splits[1];
        else
            return "UNKNOWN";
    }

    private final static String SESSION_TYPE_XMPP = "XMPP";

    public SessionID getSessionId(String localUserId, String remoteUserId) {

        //if (!remoteUserId.contains("/"))
        //   Log.w(ImApp.LOG_TAG,"resource is not set: " + remoteUserId);

       // boolean stripResource = !remoteUserId.startsWith("group");
        //SessionID sIdTemp = new SessionID(localUserId, XmppAddress.stripResource(remoteUserId), SESSION_TYPE_XMPP);
        SessionID sessionId = new SessionID(localUserId, remoteUserId, SESSION_TYPE_XMPP);

        Session session = mSessions.get(sessionId.toString());

        if (session != null)
            return session.getSessionID();
        else
            return sessionId;

    }

    /**
     * Tell if the session represented by a local user account and a remote user
     * account is currently encrypted or not.
     *
     * @param localUserId
     * @param remoteUserId
     * @return state
     */
    public SessionStatus getSessionStatus(String localUserId, String remoteUserId) {
        SessionID sessionId = getSessionId(localUserId, remoteUserId);
        if (sessionId == null)
            return null;

        return getSessionStatus(sessionId);

    }

    public SessionStatus getSessionStatus(SessionID sessionId) {

        Session session = getSession(sessionId);

        if (session != null)
            return session.getSessionStatus();

        return SessionStatus.PLAINTEXT;
    }

    /**
    public void refreshSession(String localUserId, String remoteUserId) {
        try {
            mOtrEngine.refreshSession(getSessionId(localUserId, remoteUserId));
        } catch (OtrException e) {
            OtrDebugLogger.log("refreshSession", e);
        }
    }*/

    /**
     * Start a new OTR encryption session for the chat session represented by a
     * local user address and a remote user address.
     *
     * @param localUserId i.e. the account of the user of this phone
     * @param remoteUserId i.e. the account that this user is talking to
     */
    /**
    private SessionID startSession(String localUserId, String remoteUserId) throws Exception {

        if (!remoteUserId.contains("/"))
            throw new Exception("can't start session without JabberID: " + localUserId);

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        try {

            mOtrEngine.startSession(sessionId);

            return sessionId;

        } catch (Exception e) {
            OtrDebugLogger.log("startSession", e);

            showError(sessionId, "Unable to start OTR session: " + e.getLocalizedMessage());

        }

        return null;
    }**/


    /**
     * Start a new OTR encryption session for the chat session represented by a
     * {@link SessionID}.
     *
     * @param sessionId the {@link SessionID} of the OTR session
     */
    public SessionID startSession(SessionID sessionId) {

        try {

            Session session = getSession(sessionId);
            if (session == null)
            {
                session = new SessionImpl(sessionId,mOtrEngineHost);
                session.addOtrEngineListener(this);
                mSessions.put(sessionId.toString(),session);
            }

            session.startSession();

            return sessionId;

        } catch (Exception e) {
            OtrDebugLogger.log("startSession", e);

            showError(sessionId,"Unable to start OTR session: " + e.getLocalizedMessage());

        }

        return null;
    }




    public void endSession(SessionID sessionId) {

        try {

            Session session = getSession(sessionId);
            if (session == null) {
                session.endSession();
            }

        } catch (Exception e) {
            OtrDebugLogger.log("endSession", e);
        }
    }

    public void endSession(String localUserId, String remoteUserId) {

        SessionID sessionId = getSessionId(localUserId, remoteUserId);
        endSession(sessionId);

    }

    public void status(String localUserId, String remoteUserId) {
        getSessionStatus(getSessionId(localUserId,remoteUserId));
    }

    public String decryptMessage(String localUserId, String remoteUserId, String msg, List<TLV> tlvs) throws OtrException {

        String plain = null;

        Session session = getSession(getSessionId(localUserId, remoteUserId));

        if (session != null) {

            mOtrEngineHost.putSessionResource(session.getSessionID(), processResource(remoteUserId));

            //plain = mOtrEngine.transformReceiving(sessionId, msg, tlvs);

            plain = session.transformReceiving(msg);
            /**
            OtrSm otrSm = mOtrSms.get(sessionId.toString());

            if (otrSm != null) {
                List<TLV> smTlvs = otrSm.getPendingTlvs();
                if (smTlvs != null) {
                    String encrypted = mOtrEngine.transformSending(sessionId, "", smTlvs);
                    mOtrEngineHost.injectMessage(sessionId, encrypted);
                }
            }**/

            //not null, but empty so make it null!
            if (TextUtils.isEmpty(plain))
                return null;
        }

        return plain;
    }


    public boolean transformSending(Message message) {
        String localUserId = message.getFrom().getAddress();
        String remoteUserId = message.getTo().getAddress();
        String plainText = message.getBody();

        Session session = getSession(getSessionId(localUserId, remoteUserId));

        String body = null;

        if (session != null) {
            SessionStatus sessionStatus = getSessionStatus(getSessionId(localUserId,remoteUserId));
            if (sessionStatus != SessionStatus.ENCRYPTED) {
                // Cannot send data without OTR, so start a session and drop message.
                // Message will be resent by caller when session is encrypted.
                try {
                    session.startSession();
                } catch (OtrException e) {
                    e.printStackTrace();
                }
                OtrDebugLogger.log("auto-start OTR on data send request");
                return false;
            }

            OtrDebugLogger.log("session status: " + sessionStatus);

            try {
                OtrPolicy sessionPolicy = session.getSessionPolicy();

                if (sessionStatus == SessionStatus.PLAINTEXT && sessionPolicy.getRequireEncryption())
                {
                    session.startSession();
                    return false;
                }
                if (sessionStatus != SessionStatus.PLAINTEXT || sessionPolicy.getRequireEncryption()) {
                    body = session.transformSending(plainText)[0];

                    if (!message.getTo().getAddress().contains("/"))
                        message.setTo(mOtrEngineHost.appendSessionResource(session.getSessionID(), message.getTo()));

                } else if (sessionStatus == SessionStatus.PLAINTEXT && sessionPolicy.getAllowV2()
                           && sessionPolicy.getSendWhitespaceTag()) {
                    // Work around asmack not sending whitespace tag for auto discovery
                    body += " \t  \t\t\t\t \t \t \t   \t \t  \t   \t\t  \t ";

                }
            } catch (Exception e) {
                OtrDebugLogger.log("error encrypting", e);
                return false;
            }
        }

        message.setBody(body);
        
        return true;
    }

    @Override
    public void sessionStatusChanged(final SessionID sessionID) {
        SessionStatus sStatus = getSessionStatus(sessionID);

        OtrDebugLogger.log("session status changed: " + sStatus);

        final Session session = mSessions.get(sessionID.toString());

        if (session != null) {


            if (sStatus == SessionStatus.ENCRYPTED) {

                PublicKey remoteKey = session.getRemotePublicKey();
                mOtrEngineHost.storeRemoteKey(sessionID, remoteKey);


            } else if (sStatus == SessionStatus.PLAINTEXT) {

                mOtrEngineHost.removeSessionResource(sessionID);


            } else if (sStatus == SessionStatus.FINISHED) {
                // Do nothing.  The user must take affirmative action to
                // restart or end the session, so that they don't send
                // plaintext by mistake.
            }
        }

    }

    @Override
    public void multipleInstancesDetected(SessionID sessionID) {

    }

    @Override
    public void outgoingSessionChanged(SessionID sessionID) {

    }

    public boolean isRemoteKeyVerified (String userId, String fingerprint)
    {
        return mOtrEngineHost.isRemoteKeyVerified(userId, fingerprint);
    }

    public String getRemoteKeyFingerprint (String userId)
    {
        return mOtrEngineHost.getRemoteKeyFingerprint(userId);
    }

    public ArrayList<String> getRemoteKeyFingerprints (String userId)
    {
        return mOtrEngineHost.getRemoteKeyFingerprints(userId);
    }

    public boolean hasRemoteKeyFingerprint (String userId)
    {
        return mOtrEngineHost.hasRemoteKeyFingerprint(userId);
    }

    public String getLocalKeyFingerprint(String localUserId) {
        return mOtrEngineHost.getLocalKeyFingerprint(localUserId);
    }

    public void injectMessage(SessionID sessionID, String msg) {

        mOtrEngineHost.injectMessage(sessionID, msg);
    }

    public void showWarning(SessionID sessionID, String warning) {

        mOtrEngineHost.showWarning(sessionID, warning);

    }

    public void showError(SessionID sessionID, String error) {
        mOtrEngineHost.showError(sessionID, error);


    }

    public OtrPolicy getSessionPolicy(SessionID sessionID) {

        return mOtrEngineHost.getSessionPolicy(sessionID);
    }

    public KeyPair getKeyPair(SessionID sessionID) {
        return mOtrEngineHost.getKeyPair(sessionID);
    }

    /**
    public void askForSecret(SessionID sessionID, String question) {

        Intent dialog = new Intent(mContext.getApplicationContext(), SmpResponseActivity.class);
        dialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        dialog.putExtra("q", question);
        dialog.putExtra("sid", sessionID.getUserID());//yes "sid" = remoteUserId in this case - see SMPResponseActivity
        ImConnectionAdapter connection = mOtrEngineHost.findConnection(sessionID);
        if (connection == null) {
            OtrDebugLogger.log("Could ask for secret - no connection for " + sessionID.toString());
            return;
        }

        dialog.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, connection.getProviderId());

        mContext.getApplicationContext().startActivity(dialog);

    }

    public void respondSmp(SessionID sessionID, String secret) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        List<TLV> tlvs;

        if (otrSm == null) {
            showError(sessionID, "Could not respond to verification because conversation is not encrypted");
            return;
        }

        tlvs = otrSm.initRespondSmp(null, secret, false);
        Session session = getSession(sessionID);
        String[] encrypted = session.transformSending("",tlvs);

        for (String msg : encrypted)
            mOtrEngineHost.injectMessage(sessionID, msg);

    }**/

    private Session getSession (SessionID sid)
    {
        return mSessions.get(sid.toString());
    }

    /**
    public void initSmp(SessionID sessionID, String question, String secret) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        List<TLV> tlvs;

        if (otrSm == null) {
            showError(sessionID, "Could not perform verification because conversation is not encrypted");
            return;
        }

        tlvs = otrSm.initRespondSmp(question, secret, true);
        Session session = getSession(sessionID);
        String[] encrypted = session.transformSending("", tlvs);

        for (String msg : encrypted)
            mOtrEngineHost.injectMessage(sessionID, msg);

    }

    public void abortSmp(SessionID sessionID) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        if (otrSm == null)
            return;

        List<TLV> tlvs = otrSm.abortSmp();
        Session session = getSession(sessionID);
        String[] encrypted = session.transformSending("", tlvs);
        for (String msg : encrypted)
            mOtrEngineHost.injectMessage(sessionID, msg);

    }
    **/

    /**
     * Create a message body describing the ChatSecure-Push Whitelist Token Exchange.
     * See <a href="https://github.com/ChatSecure/ChatSecure-Push-Server/wiki/Chat-Client-Implementation-Notes#json-whitelist-token-exchange">JSON Whitelist Token Exchange</a>
     *
     * @param message         A {@link Message} providing the 'to' & 'from' addresses, as well as
     *                        any message body text (this is currently unused by the ChatSecure-Push
     *                        spec).
     * @param whitelistTokens An Array of one or more ChatSecure-Push Whitelist tokens
     */
    public boolean transformPushWhitelistTokenSending(@NonNull Message message,
                                                      @NonNull String[] whitelistTokens) {

        String localUserId = message.getFrom().getAddress();
        String remoteUserId = message.getTo().getAddress();
        String body = message.getBody();

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        try {

            List<TLV> tlvs = new ArrayList<>(1);


            if (sessionId != null) {
                Session session = getSession(sessionId);
                if (session.getSessionStatus() != SessionStatus.ENCRYPTED) {
                    // Cannot send Whitelist token without OTR.
                    // TODO: Is it possible to Postpone-send a TLV message?
                    OtrDebugLogger.log("Could not send ChatSecure-Push Whitelist Token TLV. Session not encrypted.");
                    return false;
                }
                OtrDebugLogger.log("session status: " + session.getSessionStatus());

                String msgs[] = session.transformSending(body, tlvs);
                message.setTo(mOtrEngineHost.appendSessionResource(sessionId, message.getTo()));
                message.setBody(msgs[0]);
            }


            return true;

        } catch (Exception e) {
            OtrDebugLogger.log("error encrypting", e);
            return false;
        }
    }


}
