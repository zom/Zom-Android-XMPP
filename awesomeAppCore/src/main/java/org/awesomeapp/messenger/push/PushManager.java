package org.awesomeapp.messenger.push;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.awesomeapp.messenger.push.gcm.GcmRegistration;
import org.awesomeapp.messenger.util.AbortableCountDownLatch;
import org.chatsecure.pushsecure.PushSecureClient;
import org.chatsecure.pushsecure.response.Account;
import org.chatsecure.pushsecure.response.Device;
import org.chatsecure.pushsecure.response.PushToken;

import timber.log.Timber;

/**
 * A top-level class for management of ChatSecure-Push
 * <p>
 * Created by dbro on 9/18/15.
 */
public class PushManager {

    enum State {UNAUTHENTICATED, AUTHENTICATED}

    private State state = State.UNAUTHENTICATED;
    private Context context;
    private PushSecureClient client;
    private String deviceName;

    // <editor-fold desc="Public API">

    public PushManager(@NonNull Context context,
                       @NonNull String chatsecurePushServerUrl) {

        this.context = context;

        // Testing: "https://chatsecure-push.herokuapp.com/api/v1/"
        PushSecureClient client = new PushSecureClient(chatsecurePushServerUrl);

    }

    /**
     * Authenticate a ChatSecure-Push Account, which includes registering the host device.
     * When a successful result is delivered to {@param callback} this client may request
     * Whitelist tokens and immediately begin receiving Push messages addressed to them.
     *
     * @param username the ChatSecure-Push Account username
     * @param password the ChatSecure-Push Account password
     * @param callback callback to be notified of result on main thread
     */
    public void authenticateAccount(@NonNull final String username,
                                    @NonNull final String password,
                                    @NonNull final PushSecureClient.RequestCallback<Account> callback) {

        // If we were previously authenticated, clear that state until this authentication completes
        state = State.UNAUTHENTICATED;

        // TODO : query db for account, creating if not available

        final AbortableCountDownLatch preRequisiteLatch = new AbortableCountDownLatch(2);
        final String[] gcmToken = new String[1];
        final Account[] account = new Account[1];


        // This task handles the following flow:
        // 1a. Create ChatSecure-Push Account
        // 1b. Obtain a GCM Registration Id
        // (1a / 1b performed in parallel)
        // 2. Create or update ChatSecure-Push Device record
        // 3. Notify callback of success
        // Note: If any failure occurs in flow, it is also reported to callback
        // This situation is much more gracefully handled by RxJava, but I don't want
        // to introduce such a dependency into this project just for me :)
        new AsyncTask<Void, Void, Account>() {

            private Throwable taskThrowable;

            @Override
            protected Account doInBackground(Void... params) {


                // Create ChatSecure-Push account
                client.authenticateAccount(username, password, null /* (optional) email */,
                        new PushSecureClient.RequestCallback<Account>() {
                            @Override
                            public void onSuccess(@NonNull Account response) {
                                account[0] = response;
                                client.setAccount(response);
                                preRequisiteLatch.countDown();
                            }

                            @Override
                            public void onFailure(@NonNull Throwable throwable) {
                                taskThrowable = throwable;
                                preRequisiteLatch.abort();
                            }
                        });

                // Fetch GCM Registration Id
                GcmRegistration.getRegistrationIdAsync(context, new GcmRegistration.RegistrationCallback() {
                    @Override
                    public void onRegistration(String gcmRegistrationId) {

                        gcmToken[0] = gcmRegistrationId;
                        preRequisiteLatch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        taskThrowable = throwable;
                        preRequisiteLatch.abort();
                    }
                });

                try {
                    // Await the parallel completion of:
                    // (1) ChatSecure-Push Account registration
                    // (2) GCM registration.
                    preRequisiteLatch.await();

                    final AbortableCountDownLatch deviceRegistrationLatch = new AbortableCountDownLatch(1);

                    PushSecureClient.RequestCallback<Device> deviceCreatedOrUpdatedCallback =
                            new PushSecureClient.RequestCallback<Device>() {
                                @Override
                                public void onSuccess(@NonNull Device response) {
                                    deviceRegistrationLatch.countDown();
                                }

                                @Override
                                public void onFailure(@NonNull Throwable t) {
                                    taskThrowable = t;
                                    deviceRegistrationLatch.abort();
                                }
                            };

                    Device persistedDevice = getPersistedDevice();

                    // Create or Update ChatSecure-Push Device
                    if (persistedDevice == null) {

                        createDeviceWithGcmRegistrationId(gcmToken[0], deviceCreatedOrUpdatedCallback);

                    } else if (!persistedDevice.registrationId.equals(gcmToken[0])) {

                        updateDeviceWithGcmRegistrationId(persistedDevice, gcmToken[0], deviceCreatedOrUpdatedCallback);
                    }

                    deviceRegistrationLatch.await();
                    return account[0];

                } catch (InterruptedException e) {
                    // This occurs if abort() is called on either of our CountdownLatches
                    // The root cause should be available in taskThrowable
                    Timber.e(e, "Failed to authenticate ChatSecure-Push Account");
                }

                return null;
            }

            @Override
            protected void onPostExecute(Account result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else if (taskThrowable != null) {
                    callback.onFailure(taskThrowable);
                } else {
                    Timber.e("AuthenticateAccount task failed, but no error was reported");
                }
            }

        }.execute();
    }

    /**
     * Create a new Whitelist token representing the host device.
     * Must be called after {@link #authenticateAccount(String, String, PushSecureClient.RequestCallback)}.
     *
     * @param recipientIdentifier a String uniquely identifying the remote peer to your application
     * @param callback
     */
    public void createWhitelistTokenForPeer(@NonNull String recipientIdentifier,
                                            @NonNull PushSecureClient.RequestCallback<PushToken> callback) {

        if (!assertAuthenticated()) return;

        Device thisDevice = getPersistedDevice();
        String tokenIdentifier = createWhitelistTokenName(recipientIdentifier, thisDevice.name);
        // TODO : Persist token alongside peer identifier
        client.createToken(thisDevice, createWhitelistTokenName(recipientIdentifier, thisDevice.name), callback);
    }

    /**
     * Revoke a Whitelist token representing the host device.
     * (e.g: Created via {@link #createWhitelistTokenForPeer(String, PushSecureClient.RequestCallback)})
     * Must be called after {@link #authenticateAccount(String, String, PushSecureClient.RequestCallback)}.
     *
     * @param recipientIdentifier
     * @param callback
     */
    public void revokeWhitelistTokenForPeer(@NonNull String recipientIdentifier,
                                            @NonNull PushSecureClient.RequestCallback<Void> callback) {

        if (!assertAuthenticated()) return;

        // TODO : Retrieve persisted tokens related to recipientIdentifier.
        // TODO : Delete persisted tokens for recipientIdentifier.
        //client.deleteToken(...);
    }

    /**
     * Send a push message to a Whitelist token.
     */
    public void sendPushMessageToToken(@NonNull String recipientWhitelistToken,
                                       @NonNull PushSecureClient.RequestCallback<org.chatsecure.pushsecure.response.Message> callback) {

        if (!assertAuthenticated()) return;

        client.sendMessage(recipientWhitelistToken, "" /* push payload */, callback);
    }

    // </editor-fold desc="Public API">

    // <editor-fold desc="Private API">

    @Nullable
    private Device getPersistedDevice() {
        // TODO : Get persisted device from database, or return null
        return null;
    }

    private void setPersistedDevice(@NonNull Device device) {
        // TODO
    }

    private void createDeviceWithGcmRegistrationId(@NonNull String gcmRegistrationId,
                                                   @NonNull final PushSecureClient.RequestCallback<Device> callback) {

        client.createDevice(gcmRegistrationId, getDeviceName(), null /* device identifier */, new PushSecureClient.RequestCallback<Device>() {
            @Override
            public void onSuccess(@NonNull Device response) {
                setPersistedDevice(response);
                state = State.AUTHENTICATED;
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    private void updateDeviceWithGcmRegistrationId(@NonNull Device deviceToUpdate,
                                                   @NonNull String gcmRegistrationId,
                                                   @NonNull final PushSecureClient.RequestCallback<Device> callback) {

        Device updatedDevice = Device.withUpdatedRegistrationId(deviceToUpdate, gcmRegistrationId);
        client.updateDevice(updatedDevice, new PushSecureClient.RequestCallback<Device>() {
            @Override
            public void onSuccess(@NonNull Device response) {
                setPersistedDevice(response);
                state = State.AUTHENTICATED;
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    private boolean assertAuthenticated() {
        boolean authenticated = state != State.AUTHENTICATED;
        if (!authenticated) {
            Timber.e("Not authenticated. Cannot request whitelist token. Did you await the result of #authenticateAccount()");
        }
        return authenticated;
    }

    // <editor-fold desc="Utility">

    /**
     * @return a String describing a Whitelist token
     */
    private String createWhitelistTokenName(@NonNull String intendedRecipient,
                                            @NonNull String thisDeviceName) {

        return intendedRecipient + "->" + thisDeviceName;
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    // </editor-fold desc="Utility">

    // </editor-fold desc="Private API">
}
