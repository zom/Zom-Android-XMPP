package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import org.awesomeapp.messenger.crypto.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.ImErrorInfo;

/**
 * Created by n8fr8 on 6/9/15.
 */
public class AddContactAsyncTask extends AsyncTask<String, Void, Integer> {

    long mProviderId;
    long mAccountId;
    ImApp mApp;

    public AddContactAsyncTask(long providerId, long accountId, ImApp app)
    {
        mProviderId = providerId;
        mAccountId = accountId;

        mApp = app;
    }

    @Override
    public Integer doInBackground(String... strings) {

        String address = strings[0];
        String fingerprint = strings[1];
        String nickname = null;

        if (strings.length > 2)
            nickname = strings[2];

        return addToContactList(address, fingerprint, nickname);
    }

    @Override
    protected void onPostExecute(Integer response) {
        super.onPostExecute(response);

    }

    private int addToContactList (String address, String otrFingperint, String nickname)
    {
        int res = -1;

        try {
            IImConnection conn = mApp.getConnection(mProviderId,-1);
            if (conn == null)
               conn = mApp.createConnection(mProviderId,mAccountId);

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
