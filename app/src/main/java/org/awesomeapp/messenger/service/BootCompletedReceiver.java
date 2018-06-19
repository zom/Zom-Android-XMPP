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
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * Automatically initiate the service and connect when the network comes on,
 * including on boot.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
           if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
               GeneralJobIntentService.enqueueWork(context, intent);
           }
    }



}
