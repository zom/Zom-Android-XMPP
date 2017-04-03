package org.awesomeapp.messenger.crypto.omemo;

import android.content.Context;

import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.signal.SignalFileBasedOmemoStore;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoSession;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.io.File;

/**
 * Created by n8fr8 on 3/30/17.
 */

public class Omemo {


    private OmemoManager mOmemoManager;
    private SignalOmemoService mOmemoService;
    private SignalFileBasedOmemoStore mOmemoStore;

    private static Omemo mInstance;

    public synchronized static Omemo getInstance (XMPPTCPConnection connection, Context context) throws Exception
    {
        if (mInstance == null)
        {
            mInstance = new Omemo();
            mInstance.init(connection, context);
        }

        return mInstance;
    }

    public static Omemo getInstance ()
    {
        return mInstance;
    }

    public OmemoService getService ()
    {
        return mOmemoService;
    }

    public OmemoManager getManager ()
    {
        return mOmemoManager;
    }

    private void init (XMPPTCPConnection connection, Context context) throws Exception
    {
        mOmemoManager = OmemoManager.getInstanceFor(connection);

        if (mOmemoStore == null)
        {
            File fileOmemoStore = new File(context.getFilesDir(),"omemo.store");
            mOmemoStore = new SignalFileBasedOmemoStore(mOmemoManager, fileOmemoStore);

        }

        mOmemoService = new SignalOmemoService(mOmemoManager, mOmemoStore);
    }

    public boolean resourceSupportsOmemo(String jidTo)
    {
        try {
            return resourceSupportsOmemo(JidCreate.entityFullFrom(jidTo));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public boolean resourceSupportsOmemo(Jid jidTo)
    {
       try
       {
           boolean result = mOmemoManager.resourceSupportsOmemo(jidTo);
           return result;
       }
       catch (Exception e)
       {
           e.printStackTrace();
           return false;
       }
    }

    public boolean trustOmemoDevice (BareJid jid, boolean isTrusted)
    {

        CachedDeviceList l = mOmemoStore.loadCachedDeviceList(jid);
        int ourId = mOmemoStore.loadOmemoDeviceId();

        for (Integer deviceId : l.getActiveDevices())
        {

            OmemoDevice d = new OmemoDevice(jid, deviceId);
            SignalOmemoSession s = (SignalOmemoSession) mOmemoStore.getOmemoSessionOf(d);
            if(s.getIdentityKey() == null) {
                try {
                    //debug(TAG,"OMEMO Build session...");
                    mOmemoService.buildSessionFromOmemoBundle(d);
                    s = (SignalOmemoSession) mOmemoStore.getOmemoSessionOf(d);
                    //debug(TAG, "OMEMO Session built: " + jid.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            if (mOmemoStore.isDecidedOmemoIdentity(d, s.getIdentityKey())) {
                if (mOmemoStore.isTrustedOmemoIdentity(d, s.getIdentityKey())) {
                   // debug(TAG, jid.toString() + " Status: Trusted");
                } else {
                   // debug(TAG,jid.toString() +  " Status: Untrusted");
                }
            }

            if (isTrusted) {
                mOmemoStore.trustOmemoIdentity(d, s.getIdentityKey());
            }
            else
            {
                mOmemoStore.distrustOmemoIdentity(d, s.getIdentityKey());
            }
        }


        return true;
    }

}
