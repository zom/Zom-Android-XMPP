
package info.guardianproject.cacheword;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

/**
 * This class is designed to accompany any Activity that is interested in the
 * secrets cached by CacheWord. <i>The context provided in the constructor must
 * implement the ICacheWordSubscriber interface.</i> This is so the Activity can
 * be alerted to the state change events.
 */
public class CacheWordHandler {
    private static final String TAG = "CacheWordHandler";

    private Context mContext;
    private static CacheWordManager mCacheWordService;
    private static ArrayList<ICacheWordSubscriber> mSubscribers = new ArrayList<>();
    private Notification mNotification;
    private int mTimeout;

    /**
     * Timeout: How long to wait before automatically locking and wiping the
     * secrets after all your activities are no longer visible This is the
     * default setting, and can be changed at runtime via a preference A value
     * of 0 represents instant timeout A value < 0 represents no timeout (or
     * infinite timeout)
     */
    static final int DEFAULT_TIMEOUT_SECONDS = 300;

    /**
     * Tracking service connection state is a bit of a mess. There are three
     * tricky situations:
     * <ol>
     * <li>We call bindService() which returns successfully, but we are told to
     * disconnect before the connection completes (onServiceConnected() is
     * called). This can occur when the activity opens and closes quickly.</li>
     * <li>2. We shouldn't call unbind() if we didn't bind successfully. Doing
     * so produces Service not registered:
     * info.guardianproject.cacheword.CacheWordHandler</li>
     * <li>3. Conversely, We MUST call unbind() if bindService() was called
     * Failing to do so results in Activity FOOBAR has leaked ServiceConnection
     * We must track the connection state separately from the bound state.</li>
     * </ol>
     * We use this flag to help prevent a race condition described in (1).
     */
    private ServiceConnectionState mConnectionState = ServiceConnectionState.CONNECTION_NULL;

    // We use this flag to determine whether or not unbind() should be called
    // as described in #2 and #3
    private BindState mBoundState = BindState.BIND_NULL;

    enum ServiceConnectionState {
        CONNECTION_NULL,
        CONNECTION_INPROGRESS,
        CONNECTION_CANCELED,
        CONNECTION_ACTIVE
    }

    enum BindState {
        BIND_NULL,
        BIND_REQUESTED,
        BIND_COMPLETED

    }

    /**
     * Initializes the CacheWordHandler with the default
     * {@link CacheWordSettings}
     *
     * @see #CacheWordHandler(Context context, CacheWordSettings settings)
     * @param context
     */
    public CacheWordHandler(Context context) {
        this(context, (ICacheWordSubscriber) context, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Initializes the {@code CacheWordHandler} with a distinct {@link Context}
     * and {@link ICacheWordSubscriber} objects. Uses default CacheWordSettings
     */
    public CacheWordHandler(Context context, ICacheWordSubscriber subscriber) {
        this(context, subscriber, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Initializes the CacheWordHandler. Use this form when your {@link Context}
     * (e.g, the {@link Activity}) also implements the
     * {@link ICacheWordSubscriber} interface. The {@code Context} instance
     * passed in MUST implement {@link ICacheWordSubscriber}, or an
     * {@link IllegalArgumentException} will be thrown at runtime.
     * <p>
     * Setting the {@code timeout} to 0 disables the automatic locking timeout,
     * a negative value means use the default value.
     *
     * @param context must implement the {@link ICacheWordSubscriber} interface
     * @param timeout the time in seconds before CacheWord automatically locks
     */
    public CacheWordHandler(Context context, int timeout) {
        try {
            // shame we have to do this at runtime.
            // must ponder a way to enforce this relationship at compile time
            mSubscribers.add((ICacheWordSubscriber) context);
            mContext = context;
            mTimeout = timeout;
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(
                    "CacheWordHandler passed invalid Activity. Expects class that implements ICacheWordSubscriber");
        }
    }

    /**
     * Initializes the {@code CacheWordHandler} with distinct {@code Context}
     * and {@link ICacheWordSubscriber} objects.
     * <p>
     * Setting the {@code timeout} to 0 disables the automatic locking timeout,
     * a negative value means use the default value.
     *
     * @param context your {@link Application}'s or {@link Activity}'s context
     * @param subscriber the object to notify of CacheWord events
     * @param timeout the time in seconds before CacheWord automatically locks
     */
    public CacheWordHandler(Context context, ICacheWordSubscriber subscriber,
            int timeout) {
        mContext = context;
        mSubscribers.add(subscriber);
        mTimeout = timeout;
    }

    /**
     * Connect to {@link CacheWordManager}, starting it if necessary. Once
     * connected, the attached {@code Context} will begin receiving CacheWord
     * events. This should be called in your {@link Activity#onResume} or
     * somewhere else appropriate.
     */
    public synchronized void connectToService() {
        if (isCacheWordConnected())
            return;

        mCacheWordService = new CacheWordManager(mContext);

        mCacheWordService.attachSubscriber();
        mCacheWordService.setTimeout(mTimeout);
        mCacheWordService.setNotification(mNotification);
        mConnectionState = ServiceConnectionState.CONNECTION_ACTIVE;
        mBoundState = BindState.BIND_COMPLETED;
        checkCacheWordState();
    }

    /**
     * Detach but don't disconnect from {@link CacheWordManager}. CacheWord
     * events will continue to be received, but this client will not be
     * considered when performing automatic timeouts.
     */
    public void detach() {
        if (mCacheWordService != null) {
            mCacheWordService.detachSubscriber();
        }
    }

    /**
     * Reattach to the CacheWord service.
     */
    public void reattach() {
        if (mCacheWordService != null) {
            mCacheWordService.attachSubscriber();
            checkCacheWordState();
        }
    }

    /**
     * Disconnect from the CacheWord service. No further CacheWord events will
     * be received.
     */
    public void disconnectFromService() {
        synchronized (this) {
            mConnectionState = ServiceConnectionState.CONNECTION_CANCELED;

            if (mBoundState == BindState.BIND_COMPLETED) {
                if (mCacheWordService != null) {
                    mCacheWordService.detachSubscriber();
                    mCacheWordService = null;
                }
                mBoundState = BindState.BIND_NULL;
                unregisterBroadcastRecevier();
            }
        }
    }

    /**
     * Fetch the secrets from CacheWord
     *
     * @return the secrets or null on failure
     */
    public ICachedSecrets getCachedSecrets() {
        if (!isCacheWordConnected())
            return null;

        return mCacheWordService.getCachedSecrets();
    }

    public byte[] getEncryptionKey() {
        final ICachedSecrets s = getCachedSecrets();
        if (s instanceof PassphraseSecrets) {
            return ((PassphraseSecrets) s).getSecretKey().getEncoded();
        }
        return null;
    }

    /**
     * Write the secrets into CacheWord, initializing the cache if necessary.
     *
     * @param secrets
     */
    public void setCachedSecrets(ICachedSecrets secrets) {
        if (!isCacheWordConnected())
            return;

        mCacheWordService.setCachedSecrets(secrets);
        checkCacheWordState();
    }

    /**
     * Use the basic {@link PassphraseSecrets} implementation to derive
     * encryption keys securely. Initializes cacheword if necessary.
     *
     * @param passphrase
     * @throws GeneralSecurityException on invalid password
     */
    public void setPassphrase(char[] passphrase) throws GeneralSecurityException {
        final PassphraseSecrets ps;
        if (SecretsManager.isInitialized(mContext)) {
            ps = PassphraseSecrets.fetchSecrets(mContext, passphrase);
        } else {
            ps = PassphraseSecrets.initializeSecrets(mContext, passphrase);
            if (ps == null)
                throw new GeneralSecurityException("initializeSecrets could not save the secrets.");
        }
        setCachedSecrets(ps);
    }

    /**
     * Changes the passphrase used to encrypt the derived encryption keys. Since
     * the derived encryption key stays the same, this can safely be called even
     * when the secrets are in use. Only works if you're using the
     * {@link PassphraseSecrets} implementation. (i.e., Are you using
     * {@link #setPassphrase(char[])} or
     * {@link #setCachedSecrets(ICachedSecrets)})?
     *
     * @param current_secrets the current secrets you're using
     * @param new_passphrase the new passphrase to encrypt the old secrets with
     * @return null on error or current_secrets on success
     * @throws {@link IOException}
     */
    public PassphraseSecrets changePassphrase(PassphraseSecrets current_secrets,
            char[] new_passphrase) throws IOException {
        if (!SecretsManager.isInitialized(mContext)) {
            throw new IllegalStateException(
                    "CacheWord is not initialized. Passphrase can't be changed");
        }
        PassphraseSecrets new_secrets = PassphraseSecrets.changePassphrase(mContext,
                current_secrets, new_passphrase);
        if (new_secrets != null)
            return new_secrets;
        else
            throw new IOException("changePassphrase could not save the secrets");
    }

    /**
     * Request {@link CacheWordService} clear the secrets from memory. This is
     * only a request! The cache should not be considered wiped and locked until
     * the {@link ICacheWordSubscriber#onCacheWordLocked()} is received.
     */
    public void lock() {
        if (!isPrepared())
            return;
        mCacheWordService.lock();
        checkCacheWordState();
    }

    /**
     * @return true if the cache is locked or uninitialized, false otherwise
     */
    public boolean isLocked() {
        if (!isPrepared())
            return true;
        return mCacheWordService.isLocked();
    }

    /**
     * Set the automatic lock timeout for a running {@link CacheWordService}
     *
     * @param seconds time in seconds to wait before locking, setting to 0
     *            disables the timeout
     * @throws IllegalStateException
     */
    public void setTimeout(int seconds) throws IllegalStateException {
        if (!isCacheWordConnected())
            throw new IllegalStateException("CacheWord not connected");
        mCacheWordService.setTimeout(seconds);
    }

    /**
     * get the automatic lock timeout from a running {@link CacheWordService}
     *
     * @return seconds time in seconds to wait before locking
     * @throws IllegalStateException
     */
    public int getTimeout() throws IllegalStateException {
        if (!isCacheWordConnected())
            throw new IllegalStateException("CacheWord not connected");
        return mCacheWordService.getTimeout();
    }

    /**
     * Set the {@link Notification} used by {@link CacheWordService} when it
     * runs as a foreground {@link Service}. If this is set to {@code null},
     * then {@link CacheWordService} will run as a background {@link Service}.
     *
     * @param notification
     */
    public void setNotification(Notification notification) {
        mNotification = notification;
    }

    /**
     * Create a blank intent to start an instance of {@link CacheWordService}.
     * It is called "blank" because only the Component field is set.
     *
     * @param context
     * @return an Intent used to send a message to {@link CacheWordService}
     */
    static public Intent getBlankServiceIntent(Context context) {
        Intent i = new Intent();
        i.setClassName(context.getApplicationContext(), Constants.SERVICE_CLASS_NAME);
        return i;
    }


    /**
     * Get a {@link PendingIntent} that will cause {@link CacheWordService} to
     * lock and wipe the passphrase from memory once it is sent.
     *
     * @param context
     * @return
     */
    static public PendingIntent getPasswordLockPendingIntent(Context context) {
        Intent notificationIntent = getBlankServiceIntent(context);
        notificationIntent.setAction(Constants.INTENT_LOCK_CACHEWORD);
        return PendingIntent.getService(context, 0, notificationIntent, 0);
    }

    // / private helpers
    // /////////////////////////////////////////

    private void registerBroadcastReceiver() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mCacheWordReceiver,
                new IntentFilter(Constants.INTENT_NEW_SECRETS));
    }

    private void unregisterBroadcastRecevier() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mCacheWordReceiver);

    }

    private void checkCacheWordState() {
        // this is ugly as all hell

        int newState = Constants.STATE_UNKNOWN;

        if (!isCacheWordConnected()) {
            newState = Constants.STATE_UNKNOWN;
            Log.d(TAG, "checkCacheWordState: not connected");
        } else if (!isCacheWordInitialized()) {
            newState = Constants.STATE_UNINITIALIZED;
            Log.d(TAG, "checkCacheWordState: STATE_UNINITIALIZED");
        } else if (isCacheWordConnected() && mCacheWordService.isLocked()) {
            newState = Constants.STATE_LOCKED;
            Log.d(TAG, "checkCacheWordState: STATE_LOCKED, but isCacheWordConnected()=="
                    + isCacheWordConnected());
        } else {
            newState = Constants.STATE_UNLOCKED;
            Log.d(TAG, "checkCacheWordState: STATE_UNLOCKED");
        }

        if (newState == Constants.STATE_UNINITIALIZED) {
            for (ICacheWordSubscriber subscriber : mSubscribers)
                subscriber.onCacheWordUninitialized();

        } else if (newState == Constants.STATE_LOCKED) {
            for (ICacheWordSubscriber subscriber : mSubscribers)
                subscriber.onCacheWordLocked();
        } else if (newState == Constants.STATE_UNLOCKED) {
            for (ICacheWordSubscriber subscriber : mSubscribers)
                subscriber.onCacheWordOpened();
        } else {
            Log.e(TAG, "Unknown CacheWord state entered!");

        }
    }

    /**
     * @return true if cacheword is connected and available for calling
     */
    private boolean isCacheWordConnected() {
        return mCacheWordService != null;
    }

    private boolean isCacheWordInitialized() {
        return SecretsManager.isInitialized(mContext);
    }

    public void deinitialize() {
        SecretsManager.setInitialized(mContext, false);
    }

    private boolean isPrepared() {
        return isCacheWordConnected() && isCacheWordInitialized();
    }

    private BroadcastReceiver mCacheWordReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_NEW_SECRETS)) {
                if (isCacheWordConnected()) {
                    checkCacheWordState();
                }
            }
        }
    };

    /**
    private ServiceConnection mCacheWordServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ICacheWordBinder cwBinder = (ICacheWordBinder) binder;
            if (cwBinder != null) {
                Log.d(TAG, "onServiceConnected");
                synchronized (CacheWordHandler.this) {
                    if (mConnectionState == ServiceConnectionState.CONNECTION_INPROGRESS) {
                        mCacheWordService = cwBinder.getService();
                        registerBroadcastReceiver();
                        mCacheWordService.attachSubscriber();
                        mCacheWordService.setTimeout(mTimeout);
                        mCacheWordService.setNotification(mNotification);
                        mConnectionState = ServiceConnectionState.CONNECTION_ACTIVE;
                        mBoundState = BindState.BIND_COMPLETED;
                        checkCacheWordState();
                    } else if (mConnectionState == ServiceConnectionState.CONNECTION_CANCELED) {
                        // race condition hit
                        if (mBoundState != BindState.BIND_NULL) {
                            mContext.unbindService(mCacheWordServiceConnection);
                            mBoundState = BindState.BIND_NULL;
                        }
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisonnected");
            synchronized (CacheWordHandler.this) {
                if (mBoundState != BindState.BIND_NULL) {
                    mContext.unbindService(mCacheWordServiceConnection);
                    mBoundState = BindState.BIND_NULL;
                    unregisterBroadcastRecevier();
                }
                mCacheWordService = null;
            }

        }

    };**/

}
