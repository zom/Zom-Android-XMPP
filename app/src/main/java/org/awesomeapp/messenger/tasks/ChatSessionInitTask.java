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
public class ChatSessionInitTask extends AsyncTask<String, Long, Long> {

    ImApp mApp;
    long mProviderId;
    long mAccountId;
    int mContactType;
    boolean mStartCrypto = false;

    public ChatSessionInitTask (ImApp app, long providerId, long accountId, int contactType, boolean startCrypto)
    {
        mApp = app;
        mProviderId = providerId;
        mAccountId = accountId;
        mContactType = contactType;
        mStartCrypto = startCrypto;
    }

    public Long doInBackground (String... remoteAddresses)
    {
        if (mProviderId != -1 && mAccountId != -1 && remoteAddresses != null) {
            try {
                IImConnection conn = mApp.getConnection(mProviderId, mAccountId);

                if (conn == null)
                    return -1L;

                for (String address : remoteAddresses) {

                    IChatSession session = conn.getChatSessionManager().getChatSession(address);

                    //always need to recreate the MUC after login
                    if (mContactType == Imps.Contacts.TYPE_GROUP)
                        session = conn.getChatSessionManager().createMultiUserChatSession(address, null, null, false);

                    if (session != null && mContactType == Imps.Contacts.TYPE_NORMAL)
                    {
                        //first check OMEMO
                        /**
                        if (!session.isEncrypted()) {

                            //then you can try OTR
                            session.getDefaultOtrChatSession().startChatEncryption();

                        }**/

                    } else {

                        if (mContactType == Imps.Contacts.TYPE_GROUP)
                            session = conn.getChatSessionManager().createMultiUserChatSession(address, null, null, false);
                        else {
                            session = conn.getChatSessionManager().createChatSession(address, false);
                         //   session.getDefaultOtrChatSession().startChatEncryption();
                        }



                    }

                    if (session != null)
                        return (session.getId());


                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return -1L;
    }

    protected void onPostExecute(Long chatId) {


    }
}
