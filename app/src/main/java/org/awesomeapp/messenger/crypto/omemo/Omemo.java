package org.awesomeapp.messenger.crypto.omemo;

import android.content.Context;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.omemo.OmemoInitializer;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.elements.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.elements.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.signal.SignalFileBasedOmemoStore;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoSession;
import org.jivesoftware.smackx.omemo.util.KeyUtil;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.whispersystems.libsignal.IdentityKey;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by n8fr8 on 3/30/17.
 */

public class Omemo {

    private final static String TAG = "OMEMO";

    private OmemoManager mOmemoManager;
    private SignalOmemoService mOmemoService;
    private SignalFileBasedOmemoStore mOmemoStore;

    public OmemoService getService ()
    {
        return mOmemoService;
    }

    public OmemoManager getManager ()
    {
        return mOmemoManager;
    }

    {
        new OmemoInitializer().initialize();
    }

    public Omemo (XMPPTCPConnection connection, Context context) throws Exception
    {

        OmemoConstants.ADD_OMEMO_HINT_BODY = false;

        mOmemoManager = OmemoManager.getInstanceFor(connection);

        if (mOmemoStore == null)
        {
            File fileOmemoStore = new File(context.getFilesDir(),"omemo.store");
            mOmemoStore = new SignalFileBasedOmemoStore(mOmemoManager, fileOmemoStore);
        }

        mOmemoService = new SignalOmemoService(mOmemoManager, mOmemoStore);
    }

    public ArrayList<String> getFingerprints (BareJid jid, boolean autoload) throws CorruptedOmemoKeyException, SmackException, XMPPException.XMPPErrorException, InterruptedException
    {
        if (autoload)
            loadDeviceList(jid);

        CachedDeviceList list = mOmemoStore.loadCachedDeviceList(jid);
        if(list == null) {
            list = new CachedDeviceList();
        }
        ArrayList<String> fps = new ArrayList<>();
        for(int id : list.getActiveDevices()) {
            IdentityKey idk = mOmemoStore.loadOmemoIdentityKey(new OmemoDevice(jid, id));
            if(idk != null) {
                fps.add(KeyUtil.prettyFingerprint(mOmemoStore.keyUtil().getFingerprint(idk)));
            } else {
                OmemoBundleElement b = mOmemoService.getPubSubHelper().fetchBundle(new OmemoDevice(jid, id));
                idk = mOmemoStore.keyUtil().identityKeyFromBytes(b.getIdentityKey());
                if(idk != null) {
                    fps.add(KeyUtil.prettyFingerprint(mOmemoStore.keyUtil().getFingerprint(idk)));
                }
            }
        }

        return fps;
    }

    public ArrayList<String> getJidWithFingerprint (BareJid jid) throws CorruptedOmemoKeyException, SmackException, XMPPException.XMPPErrorException, InterruptedException
    {
        CachedDeviceList list = mOmemoStore.loadCachedDeviceList(jid);
        if(list == null) {
            list = new CachedDeviceList();
        }

        ArrayList<String> fps = new ArrayList<>();
        for(int id : list.getActiveDevices()) {

            IdentityKey idk = mOmemoStore.loadOmemoIdentityKey(new OmemoDevice(jid, id));
            if(idk != null) {
                fps.add(KeyUtil.prettyFingerprint(mOmemoStore.keyUtil().getFingerprint(idk)));

            } else {
                OmemoBundleElement b = mOmemoService.getPubSubHelper().fetchBundle(new OmemoDevice(jid, id));
                idk = mOmemoStore.keyUtil().identityKeyFromBytes(b.getIdentityKey());
                if(idk != null) {
                    fps.add(KeyUtil.prettyFingerprint(mOmemoStore.keyUtil().getFingerprint(idk)));
                }
            }
        }

        return fps;
    }


    public boolean resourceSupportsOmemo(final Jid jid)
    {
       try
       {
           if (jid.hasResource())
           {
               return mOmemoManager.resourceSupportsOmemo(jid);
           }
           else
           {
               return getFingerprints(jid.asBareJid(),false).size() > 0;
           }
       }
       catch (Exception e)
       {
           Log.w(TAG, "error checking if resource supports omemo: " + jid,e);
           ;
           return false;
       }
    }

    public void loadDeviceList (BareJid jid)
    {
        try {
            OmemoDeviceListElement deviceList = mOmemoService.getPubSubHelper().fetchDeviceList(jid);
        }
        catch (Exception e)
        {
            Log.w(TAG, "error fetching device list",e);
        }
    }

    public boolean allDevicesTrustDecided (BareJid jid)
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
                    Log.w(TAG, "error getting device session",e);

                }
            }

            //if we have a device that is untrusted
            if (!mOmemoStore.isDecidedOmemoIdentity(d, s.getIdentityKey())) {
                return false;
            }

        }


        return true;
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
                    Log.w(TAG, "error getting device session",e);

                }
            }

            if (s.getIdentityKey() == null)
                continue; //can't trust a key that is null!

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
