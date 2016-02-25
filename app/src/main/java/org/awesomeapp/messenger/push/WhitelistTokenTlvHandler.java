package org.awesomeapp.messenger.push;

import android.support.annotation.NonNull;
import android.util.Log;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrTlvHandler;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.TLV;

import org.chatsecure.pushsecure.PushSecureClient;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;

/**
 * Facilitates the ChatSecure-Push Whitelist Token Exchange over OTR TLV.
 * Call {@link #processTlv(TLV)} whenever new TLV messages arrive, and receive notification
 * via {@link #tlvSender} when outgoing TLVs are requested in response to those received.
 * Created by dbro on 9/28/15.
 */
public class WhitelistTokenTlvHandler implements OtrTlvHandler {

    private static final String TAG = "TokenTlvHandler";

    private final TlvSender tlvSender;
    private SessionID sessionID;
    private PushManager pushManager;

    public WhitelistTokenTlvHandler(@NonNull PushManager pushManager,
                                    @NonNull SessionID sessionID,
                                    @NonNull TlvSender tlvSender) {

        this.pushManager = pushManager;
        this.sessionID = sessionID;
        this.tlvSender = tlvSender;
    }

    /**
     * Processes the given tlv, blocking until complete
     *
     * @throws OtrException
     */
    @Override
    public synchronized void processTlv(final TLV tlv) throws OtrException {

        if (tlv.getType() == WhitelistTokenTlv.TLV_WHITELIST_TOKEN) {
            try {
                WhitelistTokenTlv tokenTlv = WhitelistTokenTlv.parseTlv(tlv);
                Timber.d("Got TLV: %s", tokenTlv);

                pushManager.insertReceivedWhitelistTokensTlv(tokenTlv,
                        PushManager.stripJabberIdResource(sessionID.getLocalUserId()),
                        PushManager.stripJabberIdResource(sessionID.getRemoteUserId()));

                pushManager.createWhitelistTokenExchangeTlv(
                        PushManager.stripJabberIdResource(sessionID.getLocalUserId()),
                        PushManager.stripJabberIdResource(sessionID.getRemoteUserId()),
                        new PushSecureClient.RequestCallback<TLV>() {
                            @Override
                            public void onSuccess(@NonNull TLV response) {
                                Log.d(TAG, "Notifying Whitelist Token Exchange TLV response");
                                tlvSender.onSendRequested(response, sessionID);
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                Log.e(TAG, "Failed to create Whitelist Token Exchange TLV", t);
                            }
                        }, null);

            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Failed to save Whitelist token payload", e);
            }
        }
    }

    public interface TlvSender {
        void onSendRequested(@NonNull TLV tlv, @NonNull SessionID sessionID);
    }
}
