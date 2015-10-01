package org.awesomeapp.messenger.push;

import android.support.annotation.NonNull;
import android.util.Log;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrTlvHandler;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.TLV;

import org.awesomeapp.messenger.crypto.OtrChatManager;
import org.chatsecure.pushsecure.PushSecureClient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dbro on 9/28/15.
 */
public class WhitelistTokenTlvHandler implements OtrTlvHandler {

    private static final String TAG = "TokenTlvHandler";

    private final List<TLV> pendingTlvs = new ArrayList<>();
    private SessionID sessionID;
    private PushManager pushManager;

    public WhitelistTokenTlvHandler(@NonNull PushManager pushManager,
                                    @NonNull SessionID sessionID) {

        this.pushManager = pushManager;
        this.sessionID = sessionID;
    }

    @Override
    public void processTlv(TLV tlv) throws OtrException {

        if (tlv.getType() == WhitelistTokenTlv.TLV_WHITELIST_TOKEN) {
            try {
                WhitelistTokenTlv tokenTlv = WhitelistTokenTlv.parseTlv(tlv);
                pushManager.insertReceivedWhitelistTokensTlv(tokenTlv, sessionID);

                pushManager.createWhitelistTokenExchangeTlv(
                        sessionID.getLocalUserId(),
                        sessionID.getRemoteUserId(),
                        new PushSecureClient.RequestCallback<TLV>() {
                            @Override
                            public void onSuccess(@NonNull TLV response) {
                                pendingTlvs.add(response);
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                Log.e(TAG, "Failed to create Whitelist Token Exchange TLV", t);
                            }
                        }, null);

            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Failed to parse Whitelist token payload", e);
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

        // Copy and clear pendingTlvs : These should only be sent once
        ArrayList<TLV> outgoingTlvs = new ArrayList<>();
        Collections.copy(outgoingTlvs, pendingTlvs);
        pendingTlvs.clear();

        return outgoingTlvs;
    }
}
