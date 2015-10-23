package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import org.awesomeapp.messenger.crypto.OtrAndroidKeyManagerImpl;
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

        return addToContactList(strings[0], strings[1]);
    }

    @Override
    protected void onPostExecute(Integer response) {
        super.onPostExecute(response);
    }

    private int addToContactList (String address, String otrFingperint)
    {
        int res = -1;

        try {
            IImConnection conn = mApp.getConnection(mProviderId,-1);
            if (conn == null)
               conn = mApp.createConnection(mProviderId,mAccountId);

            IContactList list = getContactList(conn);


            if (list != null) {

                    res = list.addContact(address);
                    if (res != ImErrorInfo.NO_ERROR) {

                    } else {
                        if (!TextUtils.isEmpty(otrFingperint)) {
                            OtrAndroidKeyManagerImpl.getInstance(mApp).verifyUser(address, otrFingperint);
                        }
                    }
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
