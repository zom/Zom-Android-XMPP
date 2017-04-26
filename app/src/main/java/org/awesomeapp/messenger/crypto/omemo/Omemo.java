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
import org.jxmpp.jid.FullJid;
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

    {
        new OmemoInitializer().initialize();
    }

    public OmemoService getService ()
    {
        return mOmemoService;
    }

    public OmemoManager getManager ()
    {
        return mOmemoManager;
    }

    public Omemo (XMPPTCPConnection connection, Context context) throws Exception
    {

        mOmemoManager = OmemoManager.getInstanceFor(connection);

        if (mOmemoStore == null)
        {
            File fileOmemoStore = new File(context.getFilesDir(),"omemo.store");
            mOmemoStore = new SignalFileBasedOmemoStore(mOmemoManager, fileOmemoStore);

        }

        mOmemoService = new SignalOmemoService(mOmemoManager, mOmemoStore);

        mOmemoManager.setAddOmemoHintBody(false);
        mOmemoManager.initialize();

    }

    public void close ()
    {
        //do we need to do anything to reinit on reconnect?
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

           String fingerprint = mOmemoStore.getFingerprint(new OmemoDevice(jid, id));
            fps.add(KeyUtil.prettyFingerprint(fingerprint));

        }

        return fps;
    }

    /**
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
    }**/

    public boolean resourceSupportsOmemo(final Jid jid)
    {
       try
       {
           if (jid.hasResource())
           {
               return mOmemoManager.resourceSupportsOmemo(jid.asFullJidIfPossible());
           }
           else
           {
               return getFingerprints(jid.asBareJid(),false).size() > 0;
           }
       }
       catch (Exception e) {
           Log.e(TAG, "error checking if resource supports omemo: " + jid, e);
           ;
           try {
               //let's just check fingerprints instead
               return getFingerprints(jid.asBareJid(), false).size() > 0;
           }
           catch (Exception e2)
           {
               Log.e(TAG, "error checking if resource supports omemo: " + jid, e2);
               
           }
       }

       return false;
    }

    public void loadDeviceList (BareJid jid)
    {
        try {

            mOmemoManager.requestDeviceListUpdateFor(jid);

         //   OmemoDeviceListElement deviceList = mOmemoService.getPubSubHelper().fetchDeviceList(jid);

        }
        catch (Exception e)
        {
            Log.w(TAG, "error fetching device list",e);
        }
    }

    /**
    public boolean allDevicesTrustDecided (BareJid jid)
    {

        CachedDeviceList l = mOmemoStore.loadCachedDeviceList(jid);
        int ourId = mOmemoStore.loadOmemoDeviceId();

        for (Integer deviceId : l.getActiveDevices())
        {

            OmemoDevice d = new OmemoDevice(jid, deviceId);
            try {
                mOmemoService.buildSessionFromOmemoBundle(d);
                SignalOmemoSession s = (SignalOmemoSession) mOmemoStore.getOmemoSessionOf(d);
                if(s.getIdentityKey() == null) {
                        //debug(TAG,"OMEMO Build session...");
                        mOmemoService.buildSessionFromOmemoBundle(d);
                        s = (SignalOmemoSession) mOmemoStore.getOmemoSessionOf(d);
                        //debug(TAG, "OMEMO Session built: " + jid.toString());

                }


                //if we have a device that is untrusted
                if (!mOmemoStore.isDecidedOmemoIdentity(d, s.getIdentityKey())) {
                    return false;
                }

            } catch (Exception e) {
                Log.w(TAG, "error getting device session",e);

            }



        }


        return true;
    }**/

    public boolean trustOmemoDevice (BareJid jid, boolean isTrusted)
    {

        CachedDeviceList l = mOmemoStore.loadCachedDeviceList(jid);
//        int ourId = mOmemoStore.loadOmemoDeviceId();

        for (Integer deviceId : l.getAllDevices())
        {

            try {
                OmemoDevice d = new OmemoDevice(jid, deviceId);
                mOmemoService.buildSessionFromOmemoBundle(d);
                SignalOmemoSession s = (SignalOmemoSession) mOmemoStore.getOmemoSessionOf(d);
                if(s.getIdentityKey() == null) {
                    Log.d(TAG,"unable to find identity key for: " + jid + " deviceid:" + deviceId);
                     continue;
                }

                if (mOmemoStore.isDecidedOmemoIdentity(d, s.getIdentityKey())) {
                    if (mOmemoStore.isTrustedOmemoIdentity(d, s.getIdentityKey())) {
                        Log.d(TAG, jid.toString() + " Status: Trusted");
                    } else {
                        Log.d(TAG,jid.toString() +  " Status: Untrusted");
                    }
                }
                else {
                    if (s.getIdentityKey() == null)
                    {
                        Log.w(TAG, jid.toString() + " can't trust, identity key is null");

                    }
                    else {
                        if (isTrusted) {
                            mOmemoStore.trustOmemoIdentity(d, s.getIdentityKey());
                            Log.d(TAG, jid.toString() + " New Status: Trusted");

                        } else {
                            mOmemoStore.distrustOmemoIdentity(d, s.getIdentityKey());
                            Log.d(TAG, jid.toString() + " New Status: Untrusted");

                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "error getting device session",e);

            }

        }


        return true;
    }

}
