package org.awesomeapp.messenger.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.otr.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.service.adapters.ChatSessionAdapter;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.json.JSONException;

import java.security.KeyPair;
import java.util.List;

/**
 * Created by n8fr8 on 5/1/17.
 */

public class MigrateAccountTask extends AsyncTask<String, Void, OnboardingAccount> {

    Activity mContext;
    IImConnection mConn;
    long mAccountId;
    long mProviderId;
    ImApp mApp;

    Handler mHandler = new Handler();

    public MigrateAccountTask(Activity context, ImApp app, long providerId, long accountId)
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

        OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(mContext);
        KeyPair keyPair = keyMan.generateLocalKeyPair();
        String fingerprint = keyMan.getFingerprint(keyPair.getPublic());

        //find or use provided new server/domain
        String domain = newDomains[0];

        //register account on new domain with same password
        OnboardingAccount account = registerNewAccount(nickname, username, password, domain);

        if (account == null) {

            username = username + '.' + fingerprint.substring(fingerprint.length()-8,fingerprint.length()).toLowerCase();
            account = registerNewAccount(nickname, username, password, domain);

            if (account == null)
                return null;
        }

        String jabberId = account.username + '@' + account.domain;
        keyMan.storeKeyPair(jabberId,keyPair);

        //send migration message to existing contacts and/or sessions
        try {

            String inviteLink = OnboardingManager.generateInviteLink(mContext,jabberId,fingerprint,nickname);
            String migrateMessage = "I have moved to a new account at " + jabberId + ". You can add me here: " + inviteLink;

            IChatSessionManager sessionMgr = mConn.getChatSessionManager();
            List<IChatSession> sessions = sessionMgr.getActiveChatSessions();

            for (IChatSession session : sessions)
            {
                session.sendMessage(migrateMessage, false);
            }

            /**
            List<String> listOfLists = mConn.getContactListManager().getContactLists();
            for (String listName : listOfLists)
            {
                IContactList contactList = mConn.getContactListManager().getContactList(listName);

            }**/

            //login and set new default account

            SignInHelper signInHelper = new SignInHelper(mContext, mHandler);
            signInHelper.activateAccount(account.providerId, account.accountId);
            signInHelper.signIn(account.password, account.providerId, account.accountId, true);

            IImConnection newConn = mApp.getConnection(account.providerId, account.accountId);
            newConn.login(account.password, true, true);

            while(newConn.getState() != ImConnection.LOGGED_IN)
            {
                try { Thread.sleep(500);}
                catch (Exception e){}
            }

            //add existing contacts on new server


            //archive existing conversations and contacts
            List<IChatSession> listSession = mConn.getChatSessionManager().getActiveChatSessions();
            for (IChatSession session : listSession)
            {

            }

            //logout of existing account
            mConn.logout();

            return account;


        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error with migration",e);
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
            OnboardingAccount result = OnboardingManager.registerAccount(mContext, nickname, username, password, domain, 5222);
            return result;
        }
        catch (JSONException jse)
        {

        }

        return null;
    }
}
