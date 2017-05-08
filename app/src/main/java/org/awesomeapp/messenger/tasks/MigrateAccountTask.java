package org.awesomeapp.messenger.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.otr.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.service.adapters.ChatSessionAdapter;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.json.JSONException;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
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
    IImConnection mNewConn;
    OnboardingAccount mNewAccount;

    Handler mHandler = new Handler();

    ArrayList<String> mContacts;

    public MigrateAccountTask(Activity context, ImApp app, long providerId, long accountId)
    {
        mContext = context;
        mAccountId = accountId;
        mProviderId = providerId;
        mApp = app;

        mConn = app.getConnection(providerId, accountId);

        mContacts = new ArrayList<>();
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
        mNewAccount = registerNewAccount(nickname, username, password, domain);

        if (mNewAccount == null) {

            username = username + '.' + fingerprint.substring(fingerprint.length()-8,fingerprint.length()).toLowerCase();
            mNewAccount = registerNewAccount(nickname, username, password, domain);

            if (mNewAccount == null)
                return null;
        }

        String jabberId = mNewAccount.username + '@' + mNewAccount.domain;
        keyMan.storeKeyPair(jabberId,keyPair);

        //send migration message to existing contacts and/or sessions
        try {

            String inviteLink = OnboardingManager.generateInviteLink(mContext,jabberId,fingerprint,nickname);
            String migrateMessage = "I have moved to a new account at " + jabberId + ". You can add me here: " + inviteLink;
            IChatSessionManager sessionMgr = mConn.getChatSessionManager();

            //login and set new default account
            SignInHelper signInHelper = new SignInHelper(mContext, mHandler);
            signInHelper.activateAccount(mNewAccount.providerId, mNewAccount.accountId);
            signInHelper.signIn(mNewAccount.password, mNewAccount.providerId, mNewAccount.accountId, true);

            mNewConn = mApp.getConnection(mNewAccount.providerId, mNewAccount.accountId);
        //    mNewConn.login(mNewAccount.password, true, true);

            while(mNewConn.getState() != ImConnection.LOGGED_IN)
            {
                try { Thread.sleep(500);}
                catch (Exception e){}
            }

            List<IContactList> listOfLists = mConn.getContactListManager().getContactLists();
            for (IContactList contactList : listOfLists)
            {
                //IContactList contactList = mConn.getContactListManager().getContactList(listName);
                String[] contacts = contactList.getContacts();

                for (String contact : contacts)
                {
                    mContacts.add(contact);

                    IChatSession session = sessionMgr.getChatSession(contact);

                    if (session == null) {
                        session = sessionMgr.createChatSession(contact, true);
                    }

                    session.sendMessage(migrateMessage, false);

                }
            }

            for (String contact : mContacts)
            {
                addToContactList(mNewConn, contact, keyMan.getRemoteFingerprint(contact), null);
            }

            //archive existing conversations and contacts
            List<IChatSession> listSession = mConn.getChatSessionManager().getActiveChatSessions();
            for (IChatSession session : listSession)
            {
                session.leave();
            }

            //logout of existing account
            mConn.logout();

            return mNewAccount;


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

    private int addToContactList (IImConnection conn, String address, String otrFingperint, String nickname)
    {
        int res = -1;

        try {

            IContactList list = getContactList(conn);

            if (list != null) {

                res = list.addContact(address, nickname);
                if (res != ImErrorInfo.NO_ERROR) {

                    //what to do here?
                }

                if (!TextUtils.isEmpty(otrFingperint)) {
                    OtrAndroidKeyManagerImpl.getInstance(mApp).verifyUser(address, otrFingperint);
                }

                //Contact contact = new Contact(new XmppAddress(address),address);
                //IContactListManager contactListMgr = conn.getContactListManager();
                //contactListMgr.approveSubscription(contact);
            }

        } catch (RemoteException re) {
            Log.e(ImApp.LOG_TAG, "error adding contact", re);
        }

        return res;
    }

    private IContactList getContactList(IImConnection conn) {
        if (conn == null) {
            return null;
        }

        try {
            IContactListManager contactListMgr = conn.getContactListManager();

            // Use the default list
            List<IBinder> lists = contactListMgr.getContactLists();
            for (IBinder binder : lists) {
                IContactList list = IContactList.Stub.asInterface(binder);
                if (list.isDefault()) {
                    return list;
                }
            }

            // No default list, use the first one as default list
            if (!lists.isEmpty()) {
                return IContactList.Stub.asInterface(lists.get(0));
            }

            return null;

        } catch (RemoteException e) {
            // If the service has died, there is no list for now.
            return null;
        }
    }
}
