package org.awesomeapp.messenger.tasks;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.omemo.Omemo;
import org.awesomeapp.messenger.crypto.otr.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import im.zom.messenger.R;

/**
 * Created by n8fr8 on 5/1/17.
 */

public class RegenerateKeysTask extends AsyncTask<String, Void, String> {

    Activity mContext;
    IImConnection mConn;
    long mAccountId;
    long mProviderId;
    ImApp mApp;
    IImConnection mNewConn;
    RegenerateKeysListener mListener;

    Handler mHandler = new Handler();

    String mUserAddress;

    public RegenerateKeysTask(Activity context, ImApp app, String userAddress, long providerId, long accountId, RegenerateKeysListener listener)
    {
        mContext = context;
        mAccountId = accountId;
        mProviderId = providerId;
        mUserAddress = userAddress;

        mApp = app;

        mListener = listener;


    }

    @Override
    protected String doInBackground(String... newDomains) {

        try {
            mConn = mApp.getConnection(mProviderId, mAccountId);

            List<String> fps = mConn.getFingerprints(mUserAddress);



        }
        catch (RemoteException re)
        {
            //fail!
        }

        //failed
        return null;
    }

    @Override
    protected void onPostExecute(String newFingerprint) {
        super.onPostExecute(newFingerprint);

        if (newFingerprint == null)
        {
            if (mListener != null)
                mListener.regenFailed(mProviderId,mAccountId);
        }
        else
        {
            if (mListener != null)
                mListener.regenComplete(newFingerprint);
        }

    }


    public interface RegenerateKeysListener {

        public void regenComplete(String newFingerprint);

        public void regenFailed(long providerId, long accountId);
    }

}
