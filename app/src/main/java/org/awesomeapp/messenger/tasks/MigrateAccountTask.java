package org.awesomeapp.messenger.tasks;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.otr.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.model.Server;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.service.adapters.ChatSessionAdapter;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.util.AssetUtil;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import im.zom.messenger.R;

/**
 * Created by n8fr8 on 5/1/17.
 */

public class MigrateAccountTask extends AsyncTask<Server, Void, OnboardingAccount> {

    Activity mContext;
    IImConnection mConn;
    String mDomain;
    long mAccountId;
    long mProviderId;
    ImApp mApp;
    IImConnection mNewConn;
    OnboardingAccount mNewAccount;

    MigrateAccountListener mListener;

    Handler mHandler = new Handler();

    ArrayList<String> mContacts;

    public MigrateAccountTask(Activity context, ImApp app, long providerId, long accountId, MigrateAccountListener listener)
    {
        mContext = context;
        mAccountId = accountId;
        mProviderId = providerId;
        mApp = app;

        mListener = listener;

        mConn = app.getConnection(providerId, accountId);

        Cursor cursor = context.getContentResolver().query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return;

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, context.getContentResolver(), mProviderId, false, null);
        mDomain = providerSettings.getDomain();
        providerSettings.close();
        if (!cursor.isClosed())
            cursor.close();

        mContacts = new ArrayList<>();
    }

    @Override
    protected OnboardingAccount doInBackground(Server... newServers) {

        //get existing account username
        String nickname = Imps.Account.getNickname(mContext.getContentResolver(), mAccountId);
        String username = Imps.Account.getUserName(mContext.getContentResolver(), mAccountId);
        String password = Imps.Account.getPassword(mContext.getContentResolver(), mAccountId);

        OtrAndroidKeyManagerImpl keyMan = OtrAndroidKeyManagerImpl.getInstance(mContext);
        KeyPair keyPair = keyMan.generateLocalKeyPair();
        String fingerprint = keyMan.getFingerprint(keyPair.getPublic());

        //find or use provided new server/domain
        for (Server newServer : newServers) {

            if (mDomain.equals(newServer.domain))
                continue; //don't migrate to the same server... to to =

            //register account on new domain with same password
            mNewAccount = registerNewAccount(nickname, username, password, newServer.domain, newServer.server);

            if (mNewAccount == null)
                continue; //try the next server

            String newJabberId = mNewAccount.username + '@' + mNewAccount.domain;
            keyMan.storeKeyPair(newJabberId, keyPair);

            //send migration message to existing contacts and/or sessions
            try {

                boolean loggedInToOldAccount = mConn.getState() == ImConnection.LOGGED_IN;

                //login and set new default account
                SignInHelper signInHelper = new SignInHelper(mContext, mHandler);
                signInHelper.activateAccount(mNewAccount.providerId, mNewAccount.accountId);
                signInHelper.signIn(mNewAccount.password, mNewAccount.providerId, mNewAccount.accountId, true);

                mNewConn = mApp.getConnection(mNewAccount.providerId, mNewAccount.accountId);

                while (mNewConn.getState() != ImConnection.LOGGED_IN) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                }

                String inviteLink = OnboardingManager.generateInviteLink(mContext, newJabberId, fingerprint, nickname, true);

                String migrateMessage = mContext.getString(R.string.migrate_message) + ' ' + inviteLink;
                IChatSessionManager sessionMgr = mConn.getChatSessionManager();
                IContactListManager clManager = mConn.getContactListManager();
                List<IContactList> listOfLists = clManager.getContactLists();

                if (loggedInToOldAccount) {

                    for (IContactList contactList : listOfLists) {
                        String[] contacts = contactList.getContacts();

                        for (String contact : contacts) {
                            mContacts.add(contact);

                            IChatSession session = sessionMgr.getChatSession(contact);

                            if (session == null) {
                                session = sessionMgr.createChatSession(contact, true);
                            }

                            if (!session.isEncrypted()) {
                                //try to kick off some encryption here
                                session.getDefaultOtrChatSession().startChatEncryption();
                                try {
                                    Thread.sleep(500);
                                } //just wait a half second here?
                                catch (Exception e) {
                                }
                            }

                            session.sendMessage(migrateMessage, false);


                            //archive existing contact
                            clManager.archiveContact(contact, session.isGroupChatSession() ? Imps.Contacts.TYPE_NORMAL : Imps.Contacts.TYPE_GROUP, true);
                        }

                    }
                } else {
                    String[] offlineAddresses = clManager.getOfflineAddresses();

                    for (String address : offlineAddresses) {
                        mContacts.add(address);
                        clManager.archiveContact(address, Imps.Contacts.TYPE_NORMAL, true);
                    }
                }

                for (String contact : mContacts) {
                    addToContactList(mNewConn, contact, keyMan.getRemoteFingerprint(contact), null);
                }

                if (loggedInToOldAccount) {
                    //archive existing conversations and contacts
                    List<IChatSession> listSession = mConn.getChatSessionManager().getActiveChatSessions();
                    for (IChatSession session : listSession) {
                        session.leave();
                    }
                    mConn.broadcastMigrationIdentity(newJabberId);
                }

                migrateAvatars(username, newJabberId);
                mApp.setDefaultAccount(mNewAccount.providerId, mNewAccount.accountId);

                //logout of existing account
                setKeepSignedIn(mAccountId, false);

                if (loggedInToOldAccount)
                    mConn.logout();

                return mNewAccount;

            } catch (Exception e) {
                Log.e(ImApp.LOG_TAG, "error with migration", e);
            }
        }

        //failed
        return null;
    }

    @Override
    protected void onPostExecute(OnboardingAccount account) {
        super.onPostExecute(account);


        if (account == null)
        {
            if (mListener != null)
                mListener.migrateFailed(mProviderId,mAccountId);
        }
        else
        {
            if (mListener != null)
                mListener.migrateComplete(account);
        }

    }

    private OnboardingAccount registerNewAccount (String nickname, String username, String password, String domain, String server)
    {
        try {
            OnboardingAccount result = OnboardingManager.registerAccount(mContext, nickname, username, password, domain, server, 5222);
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

    public interface MigrateAccountListener {

        public void migrateComplete (OnboardingAccount account);

        public void migrateFailed (long providerId, long accountId);
    }

    private void setKeepSignedIn(final long accountId, boolean signin) {
        Uri mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        ContentValues values = new ContentValues();
        values.put(Imps.Account.KEEP_SIGNED_IN, signin);
        mContext.getContentResolver().update(mAccountUri, values, null, null);
    }

    private void migrateAvatars (String oldUsername, String newUsername)
    {

        try {

            //first copy the old avatar over to the new account
            byte[] oldAvatar = DatabaseUtils.getAvatarBytesFromAddress(mContext.getContentResolver(),oldUsername);
            if (oldAvatar != null)
            {
                setAvatar(newUsername, oldAvatar);
            }

            //now change the older avatar, so the vcard gets reloaded
            Bitmap bitmap = BitmapFactory.decodeStream(mContext.getAssets().open("stickers/olo and shimi/4greeting.png"));
            setAvatar(oldUsername, bitmap);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }
    }

    private void setAvatar(String address, Bitmap bmp) {

        try {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream);

            byte[] avatarBytesCompressed = stream.toByteArray();
            String avatarHash = "nohash";
            DatabaseUtils.insertAvatarBlob(mContext.getContentResolver(), Imps.Avatars.CONTENT_URI, mProviderId, mAccountId, avatarBytesCompressed, avatarHash, address);
        } catch (Exception e) {
            Log.w(ImApp.LOG_TAG, "error loading image bytes", e);
        }
    }

    private void setAvatar(String address, byte[] avatarBytesCompressed) {

        try {
            String avatarHash = "nohash";
            DatabaseUtils.insertAvatarBlob(mContext.getContentResolver(), Imps.Avatars.CONTENT_URI, mProviderId, mAccountId, avatarBytesCompressed, avatarHash, address);
        } catch (Exception e) {
            Log.w(ImApp.LOG_TAG, "error loading image bytes", e);
        }
    }
}
