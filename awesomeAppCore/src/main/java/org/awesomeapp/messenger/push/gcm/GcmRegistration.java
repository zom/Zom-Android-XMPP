package org.awesomeapp.messenger.push.gcm;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.concurrent.Executors;

import info.guardianproject.otr.app.im.R;
import timber.log.Timber;

public class GcmRegistration {

    /**
     * Fetch the current GCM token for this device.
     */
    public static void getRegistrationIdAsync(@NonNull final Context packageContext,
                                              @NonNull final RegistrationCallback callback) {

        new AsyncTask<Context, Void, String>() {

            private Throwable throwable;

            @Override
            protected String doInBackground(Context... params) {
                Context context = params[0];
                try {
                    return getRegistrationId(context);
                } catch (IOException e) {
                    Timber.e(e, "Failed to fetch GCM token");
                    throwable = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String token) {
                if (throwable != null)
                    callback.onFailure(throwable);
                else
                    callback.onRegistration(token);
                // TODO: Ensure instanceID.getToken never returns null
            }

        }.executeOnExecutor(Executors.newSingleThreadExecutor(), packageContext);
        // We execute on a separate Executor so that this method can be used within an AsyncTask
    }

    public static String getRegistrationId(@NonNull final Context packageContext) throws IOException {
        InstanceID instanceID = InstanceID.getInstance(packageContext);
        return instanceID.getToken(packageContext.getString(R.string.gcm_defaultSenderId),
                GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
    }

    public interface RegistrationCallback {
        void onRegistration(String token);

        void onFailure(Throwable throwable);
    }
}