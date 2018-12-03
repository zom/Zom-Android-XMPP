/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.awesomeapp.messenger.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import net.java.otr4j.api.OtrPolicy;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.Preferences;
import org.awesomeapp.messenger.crypto.IOtrKeyManager;
import org.awesomeapp.messenger.crypto.otr.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.crypto.otr.OtrChatManager;
import org.awesomeapp.messenger.crypto.otr.OtrDebugLogger;
import org.awesomeapp.messenger.model.ConnectionFactory;
import org.awesomeapp.messenger.model.ConnectionListener;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.model.ImException;
import org.awesomeapp.messenger.plugin.ImPluginInfo;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.NetworkConnectivityReceiver.State;
import org.awesomeapp.messenger.service.adapters.ImConnectionAdapter;
import org.awesomeapp.messenger.ui.legacy.DummyActivity;
import org.awesomeapp.messenger.ui.legacy.ImPluginHelper;
import org.awesomeapp.messenger.util.Debug;
import org.awesomeapp.messenger.util.LogCleaner;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import im.zom.messenger.R;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import static org.awesomeapp.messenger.service.AdvancedNetworking.TRANSPORT_SS2;


public class RemoteImService extends Service implements ImService, ICacheWordSubscriber {

    private static final String PREV_CONNECTIONS_TRAIL_TAG = "prev_connections";
    private static final String CONNECTIONS_TRAIL_TAG = "connections";
    private static final String LAST_SWIPE_TRAIL_TAG = "last_swipe";
    private static final String SERVICE_DESTROY_TRAIL_TAG = "service_destroy";
    private static final String PREV_SERVICE_CREATE_TRAIL_TAG = "prev_service_create";
    private static final String SERVICE_CREATE_TRAIL_KEY = "service_create";

    private static final String[] ACCOUNT_PROJECTION = { Imps.Account._ID, Imps.Account.PROVIDER,
                                                        Imps.Account.USERNAME,
                                                        Imps.Account.PASSWORD, Imps.Account.ACTIVE, Imps.Account.KEEP_SIGNED_IN};
    // TODO why aren't these Imps.Account.* values?
    private static final int ACCOUNT_ID_COLUMN = 0;
    private static final int ACCOUNT_PROVIDER_COLUMN = 1;
    private static final int ACCOUNT_USERNAME_COLUMN = 2;
    private static final int ACCOUNT_PASSOWRD_COLUMN = 3;
    private static final int ACCOUNT_ACTIVE = 4;
    private static final int ACCOUNT_KEEP_SIGNED_IN = 5;


    private static final int EVENT_SHOW_TOAST = 100;

    private StatusBarNotifier mStatusBarNotifier;
    private Handler mServiceHandler;
    private int mNetworkType;
    private boolean mNeedCheckAutoLogin = true;

    //private SettingsMonitor mSettingsMonitor;
    private OtrChatManager mOtrChatManager;

    private ImPluginHelper mPluginHelper;
    private Hashtable<String, ImConnectionAdapter> mConnections;
    private Hashtable<String, ImConnectionAdapter> mConnectionsByUser;

    private Handler mHandler;

    final RemoteCallbackList<IConnectionCreationListener> mRemoteListeners = new RemoteCallbackList<IConnectionCreationListener>();
    public long mHeartbeatInterval;
    private WakeLock mWakeLock;
    private  NetworkConnectivityReceiver.State mNetworkState;

    private CacheWordHandler mCacheWord = null;

    private NotificationManager mNotifyManager;
    NotificationCompat.Builder mNotifyBuilder;
    private int mNumNotify = 0;
    private final static int notifyId = 7777;
    
    private static final String TAG = "ZomService";

    public long getHeartbeatInterval() {
        return mHeartbeatInterval;
    }

    public static void debug(String msg) {
       LogCleaner.debug(TAG, msg);
    }

    public static void debug(String msg, Exception e) {
        LogCleaner.error(TAG, msg, e);
    }

    private void updateOtrPolicy ()
    {
        int otrPolicy = convertPolicy();
        mOtrChatManager.setPolicy(otrPolicy);

    }

    private synchronized OtrChatManager initOtrChatManager() {
        int otrPolicy = convertPolicy();

        if (mOtrChatManager == null) {

            try
            {
                OtrAndroidKeyManagerImpl otrKeyManager = OtrAndroidKeyManagerImpl.getInstance(this);

                if (otrKeyManager != null)
                {
                    mOtrChatManager = new OtrChatManager(this, otrKeyManager);
                    mOtrChatManager.setPolicy(otrPolicy);


                }

            }
            catch (Exception e)
            {
                OtrDebugLogger.log("unable to init OTR manager", e);
            }

        } else {
            mOtrChatManager.setPolicy(otrPolicy);
        }

        return mOtrChatManager;
    }

    private int convertPolicy() {

        int otrPolicy = OtrPolicy.OPPORTUNISTIC;

        String otrModeSelect = Preferences.getOtrMode();

        if (otrModeSelect.equals("auto")) {
            otrPolicy = OtrPolicy.OPPORTUNISTIC;
        } else if (otrModeSelect.equals("disabled")) {
            otrPolicy = OtrPolicy.NEVER;

        } else if (otrModeSelect.equals("force")) {
            otrPolicy = OtrPolicy.OTRL_POLICY_ALWAYS;

        } else if (otrModeSelect.equals("requested")) {
            otrPolicy = OtrPolicy.OTRL_POLICY_MANUAL;
        }



        return otrPolicy;
    }

    @Override
    public void onCreate() {
        debug("ImService started");

        startForeground(notifyId, getForegroundNotification());

        final String prev = Debug.getTrail(this, SERVICE_CREATE_TRAIL_KEY);
        if (prev != null)
            Debug.recordTrail(this, PREV_SERVICE_CREATE_TRAIL_TAG, prev);
        Debug.recordTrail(this, SERVICE_CREATE_TRAIL_KEY, new Date());
        final String prevConnections = Debug.getTrail(this, CONNECTIONS_TRAIL_TAG);
        if (prevConnections != null)
            Debug.recordTrail(this, PREV_CONNECTIONS_TRAIL_TAG, prevConnections);
        Debug.recordTrail(this, CONNECTIONS_TRAIL_TAG, "0");
        
        mConnections = new Hashtable<String, ImConnectionAdapter>();
        mConnectionsByUser = new Hashtable<String, ImConnectionAdapter>();

        mHandler = new Handler();

        Debug.onServiceStart();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ImApp.LOG_TAG + ":IM_WAKELOCK");

        // Clear all account statii to logged-out, since we just got started and we don't want
        // leftovers from any previous crash.
        clearConnectionStatii();

        mStatusBarNotifier = new StatusBarNotifier(this);
        mServiceHandler = new ServiceHandler();

        mPluginHelper = ImPluginHelper.getInstance(this);
        mPluginHelper.loadAvailablePlugins();

        // Have the heartbeat start autoLogin, unless onStart turns this off
        mNeedCheckAutoLogin = true;

        installTransports(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent startServiceIntent = new Intent(this, NetworkSchedulerService.class);
            startService(startServiceIntent);
            scheduleNetworkJob();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scheduleNetworkJob() {

        JobInfo myJob = new JobInfo.Builder(0, new ComponentName(this, NetworkSchedulerService.class))
                .setMinimumLatency(1000)
                .setOverrideDeadline(2000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)
                .build();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(myJob);

        myJob = new JobInfo.Builder(0, new ComponentName(this, NetworkSchedulerService.class))
                .setMinimumLatency(1000)
                .setOverrideDeadline(2000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_METERED)
                .setPersisted(true)
                .build();

        jobScheduler.schedule(myJob);
    }


    private void checkUpgrade ()
    {
        ImApp app = ((ImApp)getApplication());

        if (app.needsAccountUpgrade())
        {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

            getStatusBarNotifier().notify(getString(R.string.upgrade_action),
                    getString(R.string.upgrade_desc),getString(R.string.upgrade_desc),notificationIntent, false, false);
        }


    }
    
    private void connectToCacheWord ()
    {
        if (mCacheWord == null) {
            mCacheWord = new CacheWordHandler(this, (ICacheWordSubscriber) this);
            mCacheWord.connectToService();
        }

    }

    private Notification getForegroundNotification() {
       
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        mNotifyBuilder = new NotificationCompat.Builder(this,ImApp.NOTIFICATION_CHANNEL_ID_SERVICE)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.drawable.notify_zom);

        mNotifyBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotifyBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        mNotifyBuilder.setOngoing(true);
        mNotifyBuilder.setWhen(System.currentTimeMillis());
        mNotifyBuilder.setTicker("Zom service restarted at: " + new Date().toLocaleString());

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        mNotifyBuilder.setContentIntent(launchIntent);
        mNotifyBuilder.setContentText(getString(R.string.app_unlocked));

        return mNotifyBuilder.build();

    }

    public synchronized void sendHeartbeat() {
        Debug.onHeartbeat();
        try {
            if (mNeedCheckAutoLogin && mNetworkState != NetworkConnectivityReceiver.State.NOT_CONNECTED) {
                debug("autoLogin from heartbeat");
                mNeedCheckAutoLogin = !autoLogin();;
            }

            mHeartbeatInterval = Preferences.getHeartbeatInterval();
            debug("heartbeat interval: " + mHeartbeatInterval);

            for (ImConnectionAdapter conn : mConnections.values())
            {
                conn.sendHeartbeat();
            }

        } finally {
            return;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(notifyId, getForegroundNotification());

        //if the service restarted, then we need to reconnect/reinit to cacheword
        if ((flags & START_FLAG_REDELIVERY)!=0)  // if crash restart..
            connectToCacheWord();

        if (intent != null)
        {
            if (intent.hasExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN))
                mNeedCheckAutoLogin = intent.getBooleanExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN,
                        true);

            if (HeartbeatService.HEARTBEAT_ACTION.equals(intent.getAction())) {
              //  Log.d(TAG, "HEARTBEAT");
                if (!mWakeLock.isHeld())
                {
                    try {
                        mWakeLock.acquire();
                        sendHeartbeat();
                    } finally {
                        mWakeLock.release();
                    }
                }
                return START_REDELIVER_INTENT;
            }


            if (HeartbeatService.NETWORK_STATE_ACTION.equals(intent.getAction())) {

                if (intent.hasExtra(HeartbeatService.NETWORK_INFO_CONNECTED))
                {
                    ConnectivityManager connectivityManager
                            = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    NetworkConnectivityReceiver.State networkState = State.values()[intent.getIntExtra(HeartbeatService.NETWORK_STATE_EXTRA, 0)];
                    networkStateChanged(activeNetworkInfo,networkState);
                }
                else {
                    NetworkInfo networkInfo = (NetworkInfo) intent
                            .getParcelableExtra(HeartbeatService.NETWORK_INFO_EXTRA);
                    NetworkConnectivityReceiver.State networkState = State.values()[intent.getIntExtra(HeartbeatService.NETWORK_STATE_EXTRA, 0)];

                    if (!mWakeLock.isHeld()) {
                        try {
                            mWakeLock.acquire();
                            networkStateChanged(networkInfo, networkState);

                        } finally {
                            mWakeLock.release();
                        }
                    } else {
                        networkStateChanged(networkInfo, networkState);

                    }
                }

            }


            if (ImServiceConstants.EXTRA_CHECK_SHUTDOWN.equals((intent.getAction())))
            {
                shutdown();
                stopSelf();
            }

        }

        debug("ImService.onStart, checkAutoLogin=" + mNeedCheckAutoLogin + " intent =" + intent
                + " startId =" + startId);

        if (mNeedCheckAutoLogin && mNetworkState != NetworkConnectivityReceiver.State.NOT_CONNECTED) {
            debug("autoLogin from heartbeat");
            mNeedCheckAutoLogin = !autoLogin();
        }

        HeartbeatService.startBeating(getApplicationContext());

        return START_REDELIVER_INTENT;
    }



    @Override
    public void onLowMemory() {
        debug ("onLowMemory()!");
    }


    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that was raised.
     */
    public void onTrimMemory(int level) {

        // Determine which lifecycle or system event was raised.
        switch (level) {

            case TRIM_MEMORY_UI_HIDDEN:

                /*
                   Release any UI objects that currently hold memory.

                   "release your UI resources" is actually about things like caches.
                   You usually don't have to worry about managing views or UI components because the OS
                   already does that, and that's why there are all those callbacks for creating, starting,
                   pausing, stopping and destroying an activity.
                   The user interface has moved to the background.
                */

                break;

            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_LOW:
            case TRIM_MEMORY_RUNNING_CRITICAL:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
             //   clearMemoryMedium();

                break;

            case TRIM_MEMORY_BACKGROUND:
            case TRIM_MEMORY_MODERATE:
            case TRIM_MEMORY_COMPLETE:

                /*
                   Release as much memory as the process can.
                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
              //  clearMemoryComplete();


                break;

            default:
                /*
                  Release any non-critical data structures.
                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }

    /**
    private void clearMemoryMedium ()
    {
        mOtrChatManager = null;

        for (ImConnectionAdapter conn : mConnections.values())
        {
            conn.clearMemory();
        }

        for (ImConnectionAdapter conn : mConnectionsByUser.values())
        {
            conn.clearMemory();
        }

        System.gc();
    }

    private void clearMemoryComplete ()
    {
        clearMemoryMedium ();

        for (ImConnectionAdapter conn : mConnections.values())
        {
            conn.suspend();
        }

        for (ImConnectionAdapter conn : mConnectionsByUser.values())
        {
            conn.suspend();
        }

        SecureMediaStore.unmount();
        System.gc();
        openEncryptedStores(tempKey,true);

    }**/

    private void clearConnectionStatii() {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues(2);

        values.put(Imps.AccountStatus.PRESENCE_STATUS, Imps.Presence.OFFLINE);
        values.put(Imps.AccountStatus.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);

        try
        {
            //insert on the "account_status" uri actually replaces the existing value
            cr.update(Imps.AccountStatus.CONTENT_URI, values, null, null);
        }
        catch (Exception e)
        {
            //this can throw NPE on restart sometimes if database has not been unlocked
            debug("database is not unlocked yet. caught NPE from mDbHelper in ImpsProvider");
        }
    }


    private boolean autoLogin() {

        debug("Scanning accounts and login automatically");

        ContentResolver resolver = getContentResolver();

        String where = "";//Imps.Account.KEEP_SIGNED_IN + "=1 AND " + Imps.Account.ACTIVE + "=1";
        Cursor cursor = resolver.query(Imps.Account.CONTENT_URI, ACCOUNT_PROJECTION, where, null,
                null);
        if (cursor == null) {
            Log.w(TAG, "Can't query account!");
            return false;
        }

        boolean didAutoLogin = false;

        while (cursor.moveToNext()) {
            long accountId = cursor.getLong(ACCOUNT_ID_COLUMN);
            long providerId = cursor.getLong(ACCOUNT_PROVIDER_COLUMN);
            int isActive = cursor.getInt(ACCOUNT_ACTIVE);
            int isKeepSignedIn = cursor.getInt(ACCOUNT_KEEP_SIGNED_IN);

            if (isActive == 1 && isKeepSignedIn == 1) {
                IImConnection conn = mConnections.get(providerId + "." + accountId);

                if (conn == null)
                    conn = do_createConnection(providerId, accountId);

                try {
                    if (conn.getState() != ImConnection.LOGGED_IN) {
                        try {
                            conn.login(null, true, true);
                        } catch (RemoteException e) {
                            debug("Logging error while automatically login: " + accountId);
                        }
                    }
                } catch (Exception e) {
                    debug("error auto logging into ImConnection: " + accountId);
                }

                didAutoLogin = true;
            }
        }
        cursor.close();

        return didAutoLogin;
    }

    private Map<String, String> loadProviderSettings(long providerId) {
        ContentResolver cr = getContentResolver();
        Map<String, String> settings = Imps.ProviderSettings.queryProviderSettings(cr, providerId);

        return settings;
    }

    @Override
    public void onDestroy() {
        shutdown();
    }

    private void shutdown ()
    {
        Debug.recordTrail(this, SERVICE_DESTROY_TRAIL_TAG, new Date());

        HeartbeatService.stopBeating(getApplicationContext());
        stopService(new Intent(this, NetworkSchedulerService.class));

        debug("ImService stopped.");
        for (ImConnectionAdapter conn : mConnections.values()) {

            if (conn.getState() == ImConnection.LOGGED_IN)
                conn.logout();

        }

        stopForeground(true);

         /* ignore unmount errors and quit ASAP. Threads actively using the VFS will
             * cause IOCipher's VirtualFileSystem.unmount() to throw an IllegalStateException */
        try {
            if (SecureMediaStore.isMounted())
                SecureMediaStore.unmount();
        } catch (IllegalStateException e) {
            debug("there was a problem unmount secure media store: " + e.getMessage());
        }

        if (mCacheWord != null && (!mCacheWord.isLocked())) {
            mCacheWord.lock();
            mCacheWord.disconnectFromService();
        }


    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void showToast(CharSequence text, int duration) {
        Message msg = Message.obtain(mServiceHandler, EVENT_SHOW_TOAST, duration, 0, text);
        msg.sendToTarget();
    }

    public StatusBarNotifier getStatusBarNotifier() {
        return mStatusBarNotifier;
    }

    public OtrChatManager getOtrChatManager() {
        return initOtrChatManager();
    }

    public void scheduleReconnect(long delay) {
        if (!isNetworkAvailable()) {
            // Don't schedule reconnect if no network available. We will try to
            // reconnect when network state become CONNECTED.
            return;
        }
        mServiceHandler.postDelayed(new Runnable() {
            public void run() {
                reestablishConnections();
            }
        }, delay);
    }

    public static void installTransports (Context context)
    {
        //restart transports if needed
        if (Preferences.useAdvancedNetworking())
        {
            if (aNetworking != null)
            {
                aNetworking.stopTransport();
            }

            aNetworking = new AdvancedNetworking();
            aNetworking.installTransport(context,AdvancedNetworking.TRANSPORT_SS2);
            aNetworking.startTransport();

        }
        else
        {
            if (aNetworking != null)
                aNetworking.stopTransport();

            aNetworking = null;
        }
    }

    private IImConnection do_createConnection(long providerId, long accountId) {

        if (providerId == -1)
            return null;

        Map<String, String> settings = loadProviderSettings(providerId);


        //make sure OTR is init'd before you create your first connection
        initOtrChatManager();

        ConnectionFactory factory = ConnectionFactory.getInstance();
        try {

            ImConnection conn = factory.createConnection(settings, this);
            conn.initUser(providerId, accountId);
            ImConnectionAdapter imConnectionAdapter =
                    new ImConnectionAdapter(providerId, accountId, conn, this);


            conn.addConnectionListener(new ConnectionListener() {
                @Override
                public void onStateChanged(int state, ImErrorInfo error) {

                }

                @Override
                public void onUserPresenceUpdated() {

                }

                @Override
                public void onUpdatePresenceError(ImErrorInfo error) {

                }
            });
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(providerId)},null);

            if (cursor == null)
                throw new ImException ("unable to query the provider settings");

            Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                    cursor, contentResolver, providerId, false, null);
            String userName = Imps.Account.getUserName(contentResolver, accountId);
            String domain = providerSettings.getDomain();

            providerSettings.close();

            mConnections.put(providerId + "." + accountId, imConnectionAdapter);
            mConnectionsByUser.put(imConnectionAdapter.getLoginUser().getAddress().getBareAddress(),imConnectionAdapter);

            Debug.recordTrail(this, CONNECTIONS_TRAIL_TAG, "" + mConnections.size());

            synchronized (mRemoteListeners)
            {
                try
                {
                    final int N = mRemoteListeners.beginBroadcast();
                    for (int i = 0; i < N; i++) {
                        IConnectionCreationListener listener = mRemoteListeners.getBroadcastItem(i);
                        try {
                            listener.onConnectionCreated(imConnectionAdapter);
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing the
                            // dead listeners.
                        }
                    }
                }
                finally
                {
                    mRemoteListeners.finishBroadcast();
                }
            }

            return imConnectionAdapter;
        } catch (ImException e) {
            debug("Error creating connection", e);
            return null;
        }
    }

    public void removeConnection(ImConnectionAdapter connection) {

        if (connection != null) {
            mConnections.remove(connection);
            mConnectionsByUser.remove(connection.getLoginUser());

            if (mConnections.size() == 0)
                if (Preferences.getUseForegroundPriority())
                    stopForeground(true);
        }
    }

    boolean isNetworkAvailable ()
    {
        return mNetworkState == State.CONNECTED;
    }

    void networkStateChanged(NetworkInfo networkInfo, NetworkConnectivityReceiver.State networkState) {

        int networkType = networkInfo != null ? networkInfo.getType() : -1;

        debug("networkStateChanged: type=" + networkInfo + " state=" + networkState);

        boolean networkChanged = mNetworkType != networkType
                || mNetworkState != networkState;

        boolean isNetworkAvailable = isNetworkAvailable();

        if (networkChanged) {

            mNetworkState = networkState;
            mNetworkType = networkType;

            isNetworkAvailable = isNetworkAvailable();

            if (isNetworkAvailable) {

                if (mNeedCheckAutoLogin) {
                    mNeedCheckAutoLogin = !autoLogin();
                }

                for (ImConnectionAdapter conn : mConnections.values()) {
                    conn.networkTypeChanged();
                }

            } else {
                suspendConnections();
            }


        }

        //update the notification
        if (mNotifyBuilder != null) {
            String message = "";

            if (!isNetworkAvailable) {
                message = getString(R.string.error_suspended_connection);
                mNotifyBuilder.setSmallIcon(R.drawable.notify_zom);
            } else {
                message = "Zom is online";//getString(R.string.app_unlocked);
                mNotifyBuilder.setSmallIcon(R.drawable.notify_zom);
            }

            mNotifyBuilder.setContentText(message);
            mNotifyBuilder.setTicker(message);
            // Because the ID remains unchanged, the existing notification is
            // updated.
            mNotifyManager.notify(
                    notifyId,
                    mNotifyBuilder.build());

        }


    }

    private static AdvancedNetworking aNetworking;

    public static void activateAdvancedNetworking (Context context)
    {

        if (aNetworking != null)
        {
            aNetworking.stopTransport();
        }
        else
        {
            aNetworking = new AdvancedNetworking();
            aNetworking.installTransport(context,TRANSPORT_SS2);
        }

        aNetworking.startTransport();

    }

    // package private for inner class access
    boolean reestablishConnections() {

        //restart transports if needed
        if (Preferences.useAdvancedNetworking())
            activateAdvancedNetworking(this);

        for (ImConnectionAdapter conn : mConnections.values()) {
            int connState = conn.getState();
            if (connState == ImConnection.SUSPENDED) {
                conn.reestablishSession();
            }
        }

        return mConnections.values().size() > 0;
    }

    private void suspendConnections() {
        for (ImConnectionAdapter conn : mConnections.values()) {
            if (conn.getState() != ImConnection.DISCONNECTED) {
                conn.suspend();
            }
        }

    }


    public ImConnectionAdapter getConnection(String userAddress) {
       return mConnectionsByUser.get(userAddress);
    }


    private final IRemoteImService.Stub mBinder = new IRemoteImService.Stub() {

        @Override
        public List<ImPluginInfo> getAllPlugins() {
            return new ArrayList<ImPluginInfo>(mPluginHelper.getPluginsInfo());
        }

        @Override
        public void addConnectionCreatedListener(IConnectionCreationListener listener) {
            if (listener != null) {
                mRemoteListeners.register(listener);
            }
        }

        @Override
        public void removeConnectionCreatedListener(IConnectionCreationListener listener) {
            if (listener != null) {
                mRemoteListeners.unregister(listener);
            }
        }

        @Override
        public IImConnection createConnection(long providerId, long accountId) {
            return RemoteImService.this.do_createConnection(providerId, accountId);
        }

        @Override
        public boolean removeConnection(long providerId, long accountId) {
            RemoteImService.this.removeConnection(mConnections.get(providerId + "." + accountId));
            return true;
        }

        @Override
        public List getActiveConnections() {
            ArrayList<IBinder> result = new ArrayList<IBinder>(mConnections.size());
            for (IImConnection conn : mConnections.values()) {
                result.add(conn.asBinder());
            }
            return result;
        }

        @Override
        public IImConnection getConnection(long providerId, long accountId) {
            return mConnections.get(providerId + "." + accountId);
        }

        @Override
        public void dismissNotifications(long providerId) {
            mStatusBarNotifier.dismissNotifications(providerId);
        }

        @Override
        public void dismissChatNotification(long providerId, String username) {
            mStatusBarNotifier.dismissChatNotification(providerId, username);
        }

        @Override
        public boolean unlockOtrStore (String password)
        {
         //   OtrAndroidKeyManagerImpl.setKeyStorePassword(password);
            return true;
        }

        @Override
        public IOtrKeyManager getOtrKeyManager()
        {

            OtrAndroidKeyManagerImpl keyMgr = OtrAndroidKeyManagerImpl.getInstance(RemoteImService.this);
            return keyMgr;

        }


        @Override
        public void setKillProcessOnStop (boolean killProcessOnStop)
        {
            mKillProcessOnStop = killProcessOnStop;
        }

        @Override
        public void enableDebugLogging (boolean debug)
        {
            Debug.DEBUG_ENABLED = debug;
        }

        @Override
        public void updateStateFromSettings() throws RemoteException {

            updateOtrPolicy ();

        }

        @Override
        public void shutdownAndLock ()
        {
            shutdown();
        }
    };

    private boolean mKillProcessOnStop = false;

    /*
     //the concept of "background data is deprecated from Android
     // the only thing that matters is checking if Network is available and connected
    private final class SettingsMonitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED.equals(action)) {
                ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                setBackgroundData(manager.getBackgroundDataSetting());
                handleBackgroundDataSettingChange();
            }
        }
    }
    */
    private final class ServiceHandler extends Handler {
        public ServiceHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_SHOW_TOAST:
                Toast.makeText(RemoteImService.this, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
                break;

            default:
            }
        }
    }



    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Debug.recordTrail(this, LAST_SWIPE_TRAIL_TAG, new Date());
        Intent intent = new Intent(this, DummyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 11)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    @Override
    public void onCacheWordLocked() {

        debug("got cacheword locked");

        //do nothing here?
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (settings.contains(ImApp.PREFERENCE_KEY_TEMP_PASS))
        {
            debug("cacheword: setting default pass to unlock");

            try {
                mCacheWord.setPassphrase(settings.getString(ImApp.PREFERENCE_KEY_TEMP_PASS, null).toCharArray());
            } catch (GeneralSecurityException e) {

                Log.d(ImApp.LOG_TAG, "couldn't open cacheword with temp password", e);
            }
        }
        else if (tempKey != null)
        {
            debug("found tempKey in memory, unlocking");

            openEncryptedStores(tempKey, true);

            ((ImApp)getApplication()).initAccountInfo();

            // Check and login accounts if network is ready, otherwise it's checked
            // when the network becomes available.
            if (mNeedCheckAutoLogin && mNetworkState != NetworkConnectivityReceiver.State.NOT_CONNECTED) {
                mNeedCheckAutoLogin = !autoLogin();
            }
        }

    }

    private byte[] tempKey = null;

    @Override
    public void onCacheWordOpened() {

        debug("cacheword is opened");

       tempKey = mCacheWord.getEncryptionKey();
       openEncryptedStores(tempKey, true);

        ((ImApp)getApplication()).initAccountInfo();

        // Check and login accounts if network is ready, otherwise it's checked
        // when the network becomes available.
        if (mNeedCheckAutoLogin && mNetworkState != NetworkConnectivityReceiver.State.NOT_CONNECTED) {
            mNeedCheckAutoLogin = !autoLogin();
        }

        checkUpgrade();

    }

    @Override
    public void onCacheWordUninitialized() {
        // TODO Auto-generated method stub
        
    }

    private boolean openEncryptedStores(byte[] key, boolean allowCreate) {

        SecureMediaStore.init(this, key);

        if (Imps.isUnlocked(this)) {

            return true;
        } else {
            return false;
        }

    }



}
