
package info.guardianproject.cacheword;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CacheWordManager {

    private final static String TAG = "CacheWordService";

    private ICachedSecrets mSecrets = null;

    private Notification mNotification;
    private PendingIntent mTimeoutIntent;
    private int mTimeout = CacheWordHandler.DEFAULT_TIMEOUT_SECONDS;
    private Intent mBroadcastIntent = new Intent(Constants.INTENT_NEW_SECRETS);

    private int mSubscriberCount = 0;
    private boolean mIsForegrounded = false;

    private Context mContext = null;

    public CacheWordManager (Context context)
    {
        mContext = context;
    }

    public void onDestroy() {
        if (mSecrets != null) {
            Log.d(TAG, "onDestroy() killed secrets");
            mSecrets.destroy();
            mSecrets = null;
        } else {
            Log.d(TAG, "onDestroy() secrets already null");
        }
    }

    // API for Clients
    // //////////////////////////////////////

    public synchronized ICachedSecrets getCachedSecrets() {
        return mSecrets;
    }

    public synchronized void setCachedSecrets(ICachedSecrets secrets) {
        Log.d(TAG, "setCachedSecrets()");
        mSecrets = secrets;

        handleNewSecrets(true);
    }

    public int getTimeout() {
        return mTimeout;
    }

    public void setTimeout(int timeout) {
        mTimeout = timeout;
        resetTimeout();
    }

    public synchronized boolean isLocked() {
        return mSecrets == null;
    }

    public void lock() {
        Log.d(TAG, "lock");

        synchronized (this) {
            if (mSecrets != null) {
                mSecrets.destroy();
                mSecrets = null;
            }
        }

   //     LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);

        if (mIsForegrounded) {
            mIsForegrounded = false;
        }
    }

    public synchronized void attachSubscriber() {
        mSubscriberCount++;
        Log.d(TAG, "attachSubscriber(): " + mSubscriberCount);
        resetTimeout();
    }

    public synchronized void detachSubscriber() {
        mSubscriberCount--;
        Log.d(TAG, "detachSubscriber(): " + mSubscriberCount);
        resetTimeout();
    }

    // / private methods
    // ////////////////////////////////////

    private void handleNewSecrets(boolean notify) {
        if (!SecretsManager.isInitialized(mContext)) {
            return;
        }
        if (mNotification != null) {
            mIsForegrounded = true;
        } else {
            if (mIsForegrounded) {
                mIsForegrounded = false;
            }
        }
        resetTimeout();

    }

    private void resetTimeout() {
        if (mTimeout < 0)
            mTimeout = CacheWordHandler.DEFAULT_TIMEOUT_SECONDS;
        boolean timeoutEnabled = (mTimeout > 0);

        Log.d(TAG, "timeout enabled: " + timeoutEnabled + ", seconds=" + mTimeout);
        Log.d(TAG, "mSubscriberCount: " + mSubscriberCount);

        if (timeoutEnabled && mSubscriberCount == 0) {
            startTimeout(mTimeout);
        } else {

        }
    }

    /**
     * @param seconds timeout interval in seconds
     */
    private void startTimeout(long seconds) {
        if (seconds <= 0) {
            Log.d(TAG, "immediate timeout");
            lock();
            return;
        }

    }

    public void setNotification(Notification notification) {
        mNotification = notification;
    }

}
