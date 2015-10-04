package org.awesomeapp.messenger.push;

import android.support.annotation.NonNull;
import android.util.Log;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrTlvHandler;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.TLV;

import org.chatsecure.pushsecure.PushSecureClient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * Facilitates the ChatSecure-Push Whitelist Token Exchange over OTR TLV.
 * Call {@link #processTlv(TLV)} whenever new TLV messages arrive, and subsequently call
 * {@link #getPendingTlvs()} for outgoing TLVs in response to those received.
 * Created by dbro on 9/28/15.
 */
public class WhitelistTokenTlvHandler implements OtrTlvHandler {

    private static final String TAG = "TokenTlvHandler";

    private final List<TLV> pendingTlvs = new ArrayList<>();
    private boolean processing;
    private SessionID sessionID;
    private PushManager pushManager;

    public WhitelistTokenTlvHandler(@NonNull PushManager pushManager,
                                    @NonNull SessionID sessionID) {

        this.pushManager = pushManager;
        this.sessionID = sessionID;
    }

    /**
     * Processes the given tlv, blocking until complete
     *
     * @throws OtrException
     */
    @Override
    public void processTlv(TLV tlv) throws OtrException {

        if (tlv.getType() == WhitelistTokenTlv.TLV_WHITELIST_TOKEN) {
            try {
                synchronized (pendingTlvs) {
                    processing = true;
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
                                            synchronized (pendingTlvs) {
                                                Log.d(TAG, "Queueing Whitelist Token Exchange TLV response");
                                                pendingTlvs.add(response);
                                                processing = false;
                                                pendingTlvs.notify();
                                            }
                                        }

                                        @Override
                                        public void onFailure(@NonNull Throwable t) {
                                            Log.e(TAG, "Failed to create Whitelist Token Exchange TLV", t);
                                            synchronized (pendingTlvs) {
                                                processing = false;
                                                pendingTlvs.notify();
                                            }
                                        }
                                    }, null);

                    while (processing) pendingTlvs.wait();
                }

            } catch (UnsupportedEncodingException | InterruptedException e) {
                Log.e(TAG, "Failed to save Whitelist token payload", e);
            }
        }
    }

    /**
     * @return a List of TLVs that should be sent to the remote peer
     * indicated by the {@link #sessionID} argument passed during construction.
     * Note: The returned TLVs are no longer referenced by this instance.
     */
    @NonNull
    public List<TLV> getPendingTlvs() {

        synchronized (pendingTlvs) {
            while (processing) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return new ArrayList<>();
                }
            }

            // Shallow copy and clear pendingTlvs : TLVs should be returned (and sent) only once
            ArrayList<TLV> outgoingTlvs = new ArrayList<>(pendingTlvs);
            pendingTlvs.clear();
            return outgoingTlvs;
        }
    }
}
