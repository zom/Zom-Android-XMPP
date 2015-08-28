package org.awesomeapp.messenger.service;

import java.security.GeneralSecurityException;
import java.util.Date;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.ImServiceConstants;
import org.awesomeapp.messenger.service.StatusBarNotifier;
import org.awesomeapp.messenger.util.Debug;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * Automatically initiate the service and connect when the network comes on,
 * including on boot.
 */
public class BootCompletedListener extends BroadcastReceiver {

    private static final String LAST_BOOT_TRAIL_TAG = "last_boot";
    public final static String BOOTFLAG = "BOOTFLAG";

    @Override
    public synchronized void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Debug.recordTrail(context, LAST_BOOT_TRAIL_TAG, new Date());
        boolean prefStartOnBoot = prefs.getBoolean("pref_start_on_boot", true);

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            Debug.onServiceStart();
            if (prefStartOnBoot)
            {
                if (Imps.isUnencrypted(context) || prefs.contains(ImApp.PREFERENCE_KEY_TEMP_PASS))
                {
                    Log.d(ImApp.LOG_TAG, "autostart");

                    Intent serviceIntent = new Intent(context,RemoteImService.class);
                 //   serviceIntent.setComponent(ImServiceConstants.IM_SERVICE_COMPONENT);
                    serviceIntent.putExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN, true);
                    context.startService(serviceIntent);

                    Log.d(ImApp.LOG_TAG, "autostart done");
                }
                else
                {
                    //show unlock notification
                    StatusBarNotifier sbn = new StatusBarNotifier(context);
                    sbn.notifyLocked();
                }
            }
        }


    }



}
