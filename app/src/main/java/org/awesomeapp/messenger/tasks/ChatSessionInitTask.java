package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IImConnection;

/**
 * Created by n8fr8 on 10/23/15.
 */
public class ChatSessionInitTask extends AsyncTask<Contact, Long, Long> {

    ImApp mApp;
    long mProviderId;
    long mAccountId;
    int mContactType;
    boolean mIsNewSession;

    public ChatSessionInitTask (ImApp app, long providerId, long accountId, int contactType, boolean isNewSession)
    {
        mApp = app;
        mProviderId = providerId;
        mAccountId = accountId;
        mContactType = contactType;
        mIsNewSession = isNewSession;
    }

    public Long doInBackground (Contact... contacts)
    {
        if (mProviderId != -1 && mAccountId != -1 && contacts != null) {
            try {
                IImConnection conn = mApp.getConnection(mProviderId, mAccountId);

                if (conn == null)
                    return -1L;

                for (Contact contact : contacts) {

                    IChatSession session = conn.getChatSessionManager().getChatSession(contact.getAddress().getAddress());

                    if (session == null)
                    {
                        if ((mContactType & Imps.Contacts.TYPE_MASK) == Imps.Contacts.TYPE_GROUP)
                            session = conn.getChatSessionManager().createMultiUserChatSession(contact.getAddress().getAddress(), contact.getName(), null, mIsNewSession);
                        else {
                            session = conn.getChatSessionManager().createChatSession(contact.getAddress().getAddress(), mIsNewSession);
                        }

                    }
                    else if (session.isGroupChatSession())
                    {

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
