package org.awesomeapp.messenger.push;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.TLV;

import org.awesomeapp.messenger.model.Message;
import org.awesomeapp.messenger.push.gcm.GcmRegistration;
import org.awesomeapp.messenger.push.model.PersistedAccount;
import org.awesomeapp.messenger.push.model.PersistedDevice;
import org.awesomeapp.messenger.push.model.PersistedPushToken;
import org.awesomeapp.messenger.push.model.PushDatabase;
import org.awesomeapp.messenger.util.AbortableCountDownLatch;
import org.chatsecure.pushsecure.PushSecureClient;
import org.chatsecure.pushsecure.response.Account;
import org.chatsecure.pushsecure.response.Device;
import org.chatsecure.pushsecure.response.PushToken;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.NoSuchElementException;

import timber.log.Timber;

/**
 * A top-level class for management of ChatSecure-Push
 * <p>
 * Created by dbro on 9/18/15.
 */
public class PushManager {

    public static final String DEFAULT_PROVIDER = "https://chatsecure-push.herokuapp.com/api/v1/";

    enum State {UNAUTHENTICATED, AUTHENTICATED}

    private State state = State.UNAUTHENTICATED;
    private Context context;
    private PushSecureClient client;
    private String providerUrl;
    private String deviceName;

    // <editor-fold desc="Public API">

    public PushManager(@NonNull Context context) {

        this(context, DEFAULT_PROVIDER);
    }

    public PushManager(@NonNull Context context,
                       @NonNull String chatsecurePushServerUrl) {

        this.context = context;
        this.providerUrl = chatsecurePushServerUrl;

        // Testing: "https://chatsecure-push.herokuapp.com/api/v1/"
        client = new PushSecureClient(providerUrl);

    }

    @NonNull
    public String getProviderUrl() {
        return providerUrl;
    }

    /**
     * Create a Whitelist Token Exchange {@link TLV} for transmission to a remote peer over OTR.
     * This method obtains a new Whitelist Token from ChatSecure-Push if necessary before notifying {@param callback}
     */
    public void createWhitelistTokenExchangeTlv(@NonNull String hostIdentifier,
                                                @NonNull String recipientIdentifier,
                                                @NonNull final PushSecureClient.RequestCallback<TLV> callback,
                                                @Nullable final String extraData) throws UnsupportedEncodingException {

        if (!assertAuthenticated()) return;

        Cursor persistedTokens = getPersistedTokenCursorForRecipientIdentifier(hostIdentifier, false);

        if (persistedTokens != null && persistedTokens.getCount() > 0) {
            String peerWhitelistToken = persistedTokens.getString(persistedTokens.getColumnIndex(PushDatabase.Tokens.TOKEN));
            TLV tokenTlv = createWhitelistTokenExchangeTlvWithToken(
                    new String[]{peerWhitelistToken},
                    null);
            callback.onSuccess(tokenTlv);
            persistedTokens.close();
            return;
        }

        createReceivingWhitelistTokenForPeer(recipientIdentifier, new PushSecureClient.RequestCallback<PushToken>() {
            @Override
            public void onSuccess(@NonNull PushToken response) {
                try {
                    TLV tlv = new TLV(WhitelistTokenTlv.TLV_WHITELIST_TOKEN,
                            WhitelistTokenTlv.createGson().toJson(
                                    new WhitelistTokenTlv(
                                            providerUrl,
                                            new String[]{response.token},
                                            extraData))
                                    .getBytes("UTF-8"));
                    callback.onSuccess(tlv);
                } catch (UnsupportedEncodingException e) {
                    this.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    /**
     * Create a Whitelist Token Exchange {@link TLV} for transmission to a remote peer over OTR.
     * This method uses the provided Whitelist token.
     */
    public TLV createWhitelistTokenExchangeTlvWithToken(@NonNull String[] tokens,
                                                        @Nullable String extraData) throws UnsupportedEncodingException {

        return new TLV(WhitelistTokenTlv.TLV_WHITELIST_TOKEN,
                WhitelistTokenTlv.createGson().toJson(
                        new WhitelistTokenTlv(
                                providerUrl,
                                tokens,
                                extraData))
                        .getBytes("UTF-8"));

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
                                setPersistedAccount(response, password, providerUrl);
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
     * Create a new Whitelist token authorizing push access to the local device.
     * Must be called after {@link #authenticateAccount(String, String, PushSecureClient.RequestCallback)}.
     *
     * @param recipientIdentifier a String uniquely identifying the remote peer to your application.
     *                            This is stored internally with the token to enable functionality of
     *                            {@link #revokeWhitelistTokensForPeer(String, PushSecureClient.RequestCallback)}
     * @param callback
     */
    public void createReceivingWhitelistTokenForPeer(@NonNull final String recipientIdentifier,
                                                     @NonNull final PushSecureClient.RequestCallback<PushToken> callback) {

        if (!assertAuthenticated()) return;

        final PersistedDevice thisDevice = getPersistedDevice();
        final String tokenIdentifier = createWhitelistTokenName(recipientIdentifier, thisDevice.name);

        client.createToken(thisDevice, createWhitelistTokenName(recipientIdentifier, thisDevice.name), new PushSecureClient.RequestCallback<PushToken>() {
            @Override
            public void onSuccess(@NonNull PushToken response) {
                ContentValues tokenValues = new ContentValues(4);
                tokenValues.put(PushDatabase.Tokens.RECIPIENT, recipientIdentifier);
                tokenValues.put(PushDatabase.Tokens.NAME, tokenIdentifier);
                tokenValues.put(PushDatabase.Tokens.TOKEN, response.token);
                tokenValues.put(PushDatabase.Tokens.DEVICE, thisDevice.localId);
                context.getContentResolver().insert(PushDatabase.Tokens.CONTENT_URI, tokenValues);

                callback.onSuccess(response);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    /**
     * Persist ChatSecure-Push Whitelist tokens received from a remote peer via the
     * OTR TLV Token Exchange scheme. These tokens can be later retrieved via
     * {@link #getSendingWhitelistTokenForPeer(String, PushSecureClient.RequestCallback)} (String)}
     *
     * @param tlv       A representation of the Whitelist Token TLV received from the remote peer
     * @param sessionID a description of the current Session. Used to determine the local and remote
     *                  user ids.
     */
    public void insertReceivedWhitelistTokensTlv(@NonNull WhitelistTokenTlv tlv,
                                                 @NonNull SessionID sessionID) {

        // TODO: Does sessionId.getLocalUserId give us what we want? e.g: The same identifier used elsewhere
        for (int idx = 0; idx < tlv.tokens.length; idx++) {
            ContentValues tokenValues = new ContentValues(3);
            tokenValues.put(PushDatabase.Tokens.RECIPIENT, sessionID.getLocalUserId());
            tokenValues.put(PushDatabase.Tokens.NAME, createWhitelistTokenName(sessionID.getLocalUserId(), sessionID.getRemoteUserId()));
            tokenValues.put(PushDatabase.Tokens.TOKEN, tlv.tokens[idx]);
            context.getContentResolver().insert(PushDatabase.Tokens.CONTENT_URI, tokenValues);
        }
    }

    /**
     * Retrieve a Whitelist token for sending a push to {@param recipientIdentifier}
     * <p>
     * TODO: Make fully asynchronous or remove
     *
     * @param recipientIdentifier a String uniquely identifying the remote peer to your application.
     * @param callback
     */
    public void getSendingWhitelistTokenForPeer(@NonNull final String recipientIdentifier,
                                                @NonNull final PushSecureClient.RequestCallback<PushToken> callback) {

        // TODO: Technically this should *only* return tokens marked 'issued'
        Cursor persistedTokens = getPersistedTokenCursorForRecipientIdentifier(recipientIdentifier, true);

        if (persistedTokens != null) {
            callback.onSuccess(new PersistedPushToken(persistedTokens));
            persistedTokens.close();
        } else {
            callback.onFailure(new NoSuchElementException(String.format("No token exists for peer %s", recipientIdentifier)));
        }
    }

    /**
     * Mark a Whitelist token as issued. This means we should consider it successfully transmitted
     * to its {@link PushDatabase.Tokens#RECIPIENT}, and it should not
     * be transmitted to any other peers.
     *
     * @param tokenLocalId the local database id of the Whitelist token
     */
    public void markWhitelistTokenIssued(final int tokenLocalId) {
        ContentValues tokenValues = new ContentValues(1);
        tokenValues.put(PushDatabase.Tokens.ISSUED, 1);
        int result = context.getContentResolver().update(
                PushDatabase.Tokens.CONTENT_URI,
                tokenValues,
                PushDatabase.Tokens._ID + " = ?",
                new String[]{String.valueOf(tokenLocalId)});
        if (result != 1) Timber.e("Failed to mark token %d as issued");
    }

    /**
     * Revoke Whitelist tokens created by this application install for the given recipient.
     * NOTE: This does not currently delete tokens that may have been issued by another application install.
     * Currently, the only way to do that is to adopt a common naming convention for tokens that incorporates
     * the recipient.
     * (e.g: Created via {@link #createReceivingWhitelistTokenForPeer(String, PushSecureClient.RequestCallback)}
     * Must be called after {@link #authenticateAccount(String, String, PushSecureClient.RequestCallback)}.
     *
     * @param recipientIdentifier
     * @param callback
     */
    public void revokeWhitelistTokensForPeer(@NonNull final String recipientIdentifier,
                                             @NonNull final PushSecureClient.RequestCallback<Void> callback) {

        if (!assertAuthenticated()) return;

        final Cursor recipientTokens = getPersistedTokenCursorForRecipientIdentifier(recipientIdentifier, true);
        if (recipientTokens != null && recipientTokens.getCount() > 0) {

            new AsyncTask<Void, Void, Throwable>() {

                @Override
                protected Throwable doInBackground(Void... params) {

                    final AbortableCountDownLatch latch = new AbortableCountDownLatch(recipientTokens.getCount());

                    do {
                        client.deleteToken(recipientTokens.getString(recipientTokens.getColumnIndex(PushDatabase.Tokens.TOKEN)), new PushSecureClient.RequestCallback<Void>() {
                            @Override
                            public void onSuccess(@NonNull Void response) {
                                Timber.d("Deleted token!");
                                latch.countDown();
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                Timber.e(t, "Failed to delete token");
                                latch.abort();
                            }
                        });
                    } while (recipientTokens.moveToNext());

                    try {
                        latch.await();
                        return null;
                    } catch (InterruptedException e) {
                        Timber.e(e, "Latch interrupted");
                        return e;
                    }
                }

                @Override
                protected void onPostExecute(Throwable throwable) {
                    if (throwable != null) {
                        callback.onFailure(throwable);
                    } else {
                        callback.onSuccess((Void) new Object());
                    }
                }

            }.execute();

        }
    }

    public void sendPushMessageToPeer(@NonNull final String recipientIdentifier,
                                      @NonNull final PushSecureClient.RequestCallback<org.chatsecure.pushsecure.response.Message> callback) {

        if (!assertAuthenticated()) return;

        getSendingWhitelistTokenForPeer(recipientIdentifier, new PushSecureClient.RequestCallback<PushToken>() {
            @Override
            public void onSuccess(@NonNull PushToken response) {
                sendPushMessageToToken(response.token, callback);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
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

    /**
     * @param recipientIdentifier the recipient of the token. To receive a token for
     *                            transmission to a remote peer, this should be the local host's
     *                            identifier. To receive a token for sending a push to a remote
     *                            peer, this should be that remote peer's identifier.
     * @param showIssuedTokens    whether to filter the returned tokens by those that have not
     *                            been marked 'issued'. When retrieving a list of tokens to be
     *                            revoked this should be true. When retrieving a list of tokens
     *                            for transmission to remote peers, this should be false.
     */
    @Nullable
    private Cursor getPersistedTokenCursorForRecipientIdentifier(@NonNull String recipientIdentifier,
                                                                 boolean showIssuedTokens) {

        String where = PushDatabase.Tokens.RECIPIENT + " = ? " +
                (showIssuedTokens ?
                        " AND " + PushDatabase.Tokens.ISSUED + " = ?" : "");

        String[] whereArgs = showIssuedTokens ?
                new String[]{recipientIdentifier, "1"} :
                new String[]{recipientIdentifier};

        // TODO: Perhaps we should sort by created date
        Cursor result = context.getContentResolver().query(
                PushDatabase.Tokens.CONTENT_URI,
                null,
                where,
                whereArgs,
                null);
        if (result != null) result.moveToFirst();
        return result;
    }

    @Nullable
    public PersistedAccount getPersistedAccount() {
        Cursor accountCursor = context.getContentResolver().query(
                PushDatabase.Accounts.CONTENT_URI,
                null,
                PushDatabase.Accounts._ID + " = ?",
                new String[]{"0"},
                null);

        if (accountCursor == null)
            return null;
        else if (!accountCursor.moveToFirst()) {
            accountCursor.close();
            return null;
        }

        PersistedAccount account = new PersistedAccount(accountCursor);
        accountCursor.close();
        return account;
    }

    private void setPersistedAccount(@NonNull Account account,
                                     @NonNull String password,
                                     @NonNull String provider) {

        ContentValues accountValues = new ContentValues(4);
        accountValues.put(PushDatabase.Accounts._ID, 0); // We should only have one ChatSecure-Push Account
        accountValues.put(PushDatabase.Accounts.USERNAME, account.username);
        accountValues.put(PushDatabase.Accounts.PASSWORD, password);
        accountValues.put(PushDatabase.Accounts.PROVIDER, provider);

        // Update or Insert
        if (context.getContentResolver().update(PushDatabase.Accounts.CONTENT_URI, accountValues, PushDatabase.Accounts._ID + " = ?", new String[]{"0"}) == 1) {
            Timber.d("Updated persisted account");
        } else {
            context.getContentResolver().insert(PushDatabase.Accounts.CONTENT_URI, accountValues);
            Timber.d("Inserted persisted account");
        }
    }

    @Nullable
    private PersistedDevice getPersistedDevice() {

        // We should only have one persisted device record
        Cursor deviceCursor = context.getContentResolver().query(
                PushDatabase.Devices.CONTENT_URI,
                null,
                PushDatabase.Devices._ID + " = ?",
                new String[]{"0"},
                null);

        if (deviceCursor == null)
            return null;
        else if (!deviceCursor.moveToFirst()) {
            deviceCursor.close();
            return null;
        }


        try {
            PersistedDevice device = new PersistedDevice(deviceCursor);
            deviceCursor.close();
            return device;
        } catch (ParseException e) {
            Timber.e(e, "Failed to create PersistedDevice from Cursor");
            return null;
        }
    }

    private void setPersistedDevice(@NonNull Device device) {
        ContentValues deviceValues = new ContentValues(6);
        deviceValues.put(PushDatabase.Devices._ID, 0); // We only want to store one record for the host device
        deviceValues.put(PushDatabase.Devices.NAME, device.name);
        deviceValues.put(PushDatabase.Devices.DATE_CREATED, PushDatabase.DATE_FORMATTER.format(device.dateCreated));
        deviceValues.put(PushDatabase.Devices.REGISTRATION_ID, device.registrationId);
        deviceValues.put(PushDatabase.Devices.DEVICE_ID, device.deviceId);
        deviceValues.put(PushDatabase.Devices.ACTIVE, device.active);

        // Update or Insert
        if (context.getContentResolver().update(PushDatabase.Devices.CONTENT_URI, deviceValues, PushDatabase.Devices._ID + " = ?", new String[]{"0"}) == 1) {
            Timber.d("Updated persisted device");
        } else {
            context.getContentResolver().insert(PushDatabase.Devices.CONTENT_URI, deviceValues);
            Timber.d("Inserted persisted device");
        }
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
    private String createWhitelistTokenName(@NonNull String toIdentifier,
                                            @NonNull String fromIdentifier) {

        return toIdentifier + "->" + fromIdentifier;
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
