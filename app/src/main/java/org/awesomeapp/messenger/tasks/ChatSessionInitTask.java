package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.IOtrChatSession;
import org.awesomeapp.messenger.model.ChatSession;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IImConnection;

/**
 * Created by n8fr8 on 10/23/15.
 */
public class ChatSessionInitTask extends AsyncTask<String, Void, Long> {

    ImApp mApp;
    long mProviderId;
    long mAccountId;
    int mContactType;

    public ChatSessionInitTask (ImApp app, long providerId, long accountId, int contactType)
    {
        mApp = app;
        mProviderId = providerId;
        mAccountId = accountId;
        mContactType = contactType;
    }


    @Override
    protected Long doInBackground(String... remoteAddresses) {


        if (mProviderId != -1 && mAccountId != -1) {
            try {
                IImConnection conn = mApp.getConnection(mProviderId, mAccountId);

                if (conn == null || conn.getState() != ImConnection.LOGGED_IN)
                    return -1l;

                for (String address : remoteAddresses) {

                    IChatSession session = conn.getChatSessionManager().getChatSession(address);

                    //always need to recreate the MUC after login
                    if (mContactType == Imps.Contacts.TYPE_GROUP)
                        session = conn.getChatSessionManager().createMultiUserChatSession(address, null, null, false);

                    if (session != null && session.getDefaultOtrChatSession() != null && session.getDefaultOtrChatSession().isChatEncrypted()) {
                            //then do nothing
                        continue;
                    } else {

                        if (session == null)
                            if (mContactType == Imps.Contacts.TYPE_GROUP)
                                session = conn.getChatSessionManager().createMultiUserChatSession(address, null, null, false);
                            else
                                session = conn.getChatSessionManager().createChatSession(address, false);

                        if (session != null) {
                            int sessionCount = session.getOtrChatSessionCount();

                            for (int i = 0; i < sessionCount; i++) {
                                IOtrChatSession otrChatSession = session.getOtrChatSession(i);
                                if (otrChatSession != null)
                                    otrChatSession.startChatEncryption();
                            }
                        }

                    }

                    return session.getId();


                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return -1L;
    }
}
