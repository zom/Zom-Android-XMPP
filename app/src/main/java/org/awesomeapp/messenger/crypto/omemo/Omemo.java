package org.awesomeapp.messenger.crypto.omemo;

import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.omemo.OmemoConfiguration;
import org.jivesoftware.smackx.omemo.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.jivesoftware.smackx.omemo.util.OmemoKeyUtil;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.whispersystems.libsignal.IdentityKey;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static org.awesomeapp.messenger.util.LogCleaner.debug;

/**
 * Created by n8fr8 on 3/30/17.
 */

public class Omemo {

    private final static String TAG = "OMEMO";

    private OmemoManager mOmemoManager;
    private OmemoStore mOmemoStore;

    private static boolean mOmemoInit = false;

    public OmemoManager getManager ()
    {
        return mOmemoManager;
    }

    public Omemo (XMPPTCPConnection connection, BareJid user) {

        oneTimeSetup();

        mOmemoManager = this.initOMemoManager(connection, user);

    }

    public void purgeDeviceKeys ()
    {
        mOmemoStore.purgeOwnDeviceKeys(mOmemoManager);
    }

    private OmemoManager initOMemoManager(XMPPTCPConnection conn, BareJid altUser) {
        BareJid user;

        if (conn.getUser() != null) {
            user = conn.getUser().asBareJid();
        } else {
            user = altUser;
        }

        mOmemoStore = OmemoService.getInstance().getOmemoStoreBackend();
        int defaultDeviceId = mOmemoStore.getDefaultDeviceId(user);

        if (defaultDeviceId < 1) {
            defaultDeviceId = OmemoManager.randomDeviceId();
            mOmemoStore.setDefaultDeviceId(user, defaultDeviceId);
        }

        return OmemoManager.getInstanceFor(conn, defaultDeviceId);
    }

    private static synchronized void oneTimeSetup ()
    {
        if (!mOmemoInit) {
            try {
                //init sign libraries
                SignalOmemoService.acknowledgeLicense();
                SignalOmemoService.setup();

                //configure OMEMO global prefs
                OmemoConfiguration.setAddOmemoHintBody(false);

                //init IOCipher-based omemo store
                File fileOmemoStore = new File("omemo-ks","zomomemo.oks");
                OmemoStore store = new SignalIOCipherOmemoStore (fileOmemoStore);
                OmemoService.getInstance().setOmemoStoreBackend(store);

                mOmemoInit = true;

            } catch (Exception e) {
                debug(TAG, "onetime omemo setup error: " + e);
                e.printStackTrace();
            }
        }
    }

    public void close ()
    {
        //do we need to do anything to reinit on reconnect?
    }

    public ArrayList<String> getFingerprints (BareJid jid, boolean autoload) throws CorruptedOmemoKeyException, SmackException, InterruptedException
    {

        try {
            mOmemoManager.buildSessionsWith(jid);
        }
        catch (CannotEstablishOmemoSessionException e)
        {
            debug(TAG,"couldn't establish omemo session: " + e);
        }


        CachedDeviceList list = mOmemoStore.loadCachedDeviceList(mOmemoManager, jid);
        if(list == null) {
            list = new CachedDeviceList();
        }
        ArrayList<String> fps = new ArrayList<>();
        for(int id : list.getActiveDevices()) {
            OmemoDevice d = new OmemoDevice(jid, id);
            IdentityKey idk = (IdentityKey)mOmemoStore.loadOmemoIdentityKey(mOmemoManager, d);
            if(idk == null) {
                System.out.println("No identityKey for "+d);
            } else {
                fps.add(mOmemoStore.keyUtil().getFingerprint(idk).toString());
            }
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

    public boolean resourceSupportsOmemo(final Jid jidContact)
    {
       try
       {
           if (mOmemoManager.contactSupportsOmemo(jidContact.asBareJid()))
               {
                   try {
                       mOmemoManager.buildSessionsWith(jidContact.asBareJid());
                       return true;
                   }
                   catch (CannotEstablishOmemoSessionException e)
                   {
                       debug(TAG,"couldn't establish omemo session: " + e);
                   }
               }
       }
       catch (Exception e) {
           Log.w(TAG, "error checking if resource supports omemo, will check for local fingerprints");

           try {
               mOmemoManager.requestDeviceListUpdateFor(jidContact.asBareJid());

               //let's just check fingerprints instead
               return getFingerprints(jidContact.asBareJid(), false).size() > 0;
           }
           catch (Exception e2)
           {
               debug(TAG, "error checking if resource supports omemo: " + jidContact + ": " + e2.toString());
           }
       }

       return false;
    }

    public void loadDeviceList (BareJid jid)
    {
        try {

            mOmemoManager.requestDeviceListUpdateFor(jid);

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

    public boolean trustOmemoDevice (BareJid jid, String fingerprint, boolean isTrusted)
    {

        HashMap<OmemoDevice,OmemoFingerprint> oFps = mOmemoManager.getActiveFingerprints(jid);
        for (OmemoDevice device: oFps.keySet())
        {
            OmemoFingerprint oFpt = oFps.get(device);

            if (fingerprint == null)
            {
                mOmemoManager.trustOmemoIdentity(device, oFpt);
            }
            else if (OmemoKeyUtil.prettyFingerprint(oFpt.toString()).equals(fingerprint))
            {
                mOmemoManager.trustOmemoIdentity(device, oFpt);
            }
        }


        return true;
    }

}
