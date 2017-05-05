package org.awesomeapp.messenger.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.RemoteException;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.service.adapters.ChatSessionAdapter;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.json.JSONException;

import java.util.List;

/**
 * Created by n8fr8 on 5/1/17.
 */

public class MigrantAccountTask extends AsyncTask<String, Void, OnboardingAccount> {

    Context mContext;
    IImConnection mConn;
    long mAccountId;
    long mProviderId;
    ImApp mApp;

    public MigrantAccountTask (Context context, ImApp app, long providerId, long accountId)
    {
        mContext = context;
        mAccountId = accountId;
        mProviderId = providerId;
        mApp = app;

        mConn = app.getConnection(providerId, accountId);

    }

    @Override
    protected OnboardingAccount doInBackground(String... newDomains) {

        //get existing account username
        String nickname = Imps.Account.getNickname(mContext.getContentResolver(), mAccountId);
        String username = Imps.Account.getUserName(mContext.getContentResolver(), mAccountId);
        String password = Imps.Account.getPassword(mContext.getContentResolver(), mAccountId);

        //find or use provided new server/domain
        String domain = newDomains[0];

        //register account on new domain with same password
        OnboardingAccount account = registerNewAccount(nickname, username, password, domain);

        if (account != null)
        {

            //send migration message to existing contacts and/or sessions
            try {
                List<String> listOfLists = mConn.getContactListManager().getContactLists();
                for (String listName : listOfLists)
                {
                    IContactList contactList = mConn.getContactListManager().getContactList(listName);

                }


                //login and set new default account
                IImConnection newConn = mApp.getConnection(account.providerId, account.accountId);
                newConn.login(account.password, true, true);

                //add existing contacts on new server


                //archive existing conversations and contacts
                List<IChatSession> listSession = mConn.getChatSessionManager().getActiveChatSessions();
                for (IChatSession session : listSession)
                {

                }

                return account;


            }
            catch (RemoteException re)
            {}

        }

        //failed
        return null;
    }

    @Override
    protected void onPostExecute(OnboardingAccount account) {
        super.onPostExecute(account);


    }

    private OnboardingAccount registerNewAccount (String nickname, String username, String password, String domain)
    {
        try {
            OnboardingAccount result = OnboardingManager.registerAccount(mContext, null, nickname, username, password, domain, 5222);
            return result;
        }
        catch (JSONException jse)
        {

        }

        return null;
    }
}
