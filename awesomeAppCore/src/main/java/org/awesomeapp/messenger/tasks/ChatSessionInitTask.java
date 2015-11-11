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
public class ChatSessionInitTask extends AsyncTask<String, Void, Boolean> {

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
    protected Boolean doInBackground(String... remoteAddresses) {


        if (mProviderId != -1 && mAccountId != -1) {
            try {
                IImConnection conn = mApp.getConnection(mProviderId, mAccountId);

                for (String address : remoteAddresses) {

                    IChatSession session = conn.getChatSessionManager().getChatSession(address);

                 //   if (session == null) {

                        if (mContactType == Imps.Contacts.TYPE_GROUP) {
                            session = conn.getChatSessionManager().createMultiUserChatSession(address, null, null, false);

                        } else {
                            if (session != null && session.getOtrChatSession() != null && session.getOtrChatSession().isChatEncrypted())
                            {
                                //then do nothing
                                continue;
                            }
                            else {
                                if (session == null)
                                    session = conn.getChatSessionManager().createChatSession(address, false);

                                IOtrChatSession otrChatSession = session.getOtrChatSession();
                                otrChatSession.startChatEncryption();
                            }
                        }
//                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
