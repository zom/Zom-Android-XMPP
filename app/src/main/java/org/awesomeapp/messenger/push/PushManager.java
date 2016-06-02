package org.awesomeapp.messenger.push;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.java.otr4j.session.TLV;
import net.sqlcipher.database.SQLiteConstraintException;

//import org.awesomeapp.messenger.push.gcm.GcmRegistration;
import org.awesomeapp.messenger.push.model.PersistedAccount;
import org.awesomeapp.messenger.push.model.PersistedDevice;
import org.awesomeapp.messenger.push.model.PersistedPushToken;
import org.awesomeapp.messenger.push.model.PushDatabase;
import org.awesomeapp.messenger.util.AbortableCountDownLatch;
import org.awesomeapp.messenger.util.Debug;
import org.chatsecure.pushsecure.PushSecureClient;
import org.chatsecure.pushsecure.response.Account;
import org.chatsecure.pushsecure.response.Device;
import org.chatsecure.pushsecure.response.PushToken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * A top-level class for management of ChatSecure-Push.
 * <p>
 * Usage:
 * <pre>
 * {@code
 *      PushManager manager = new PushManager(context);
 *      manager.authenticateAccount("username", "password", new PushSecureClient.RequestCallback<Account>() {
 *          @Override
 *          public void onSuccess(@NonNull Account account) {
 *              // account has been persisted to the application database
 *              // you may now perform authenticated ChatSecure-Push actions:
 *
 *              // Create a Whitelist Token Exchange TLV to transmit a token to a peer
 *              // manager.createWhitelistTokenExchangeTlv(...);
 *
 *              // Send a Push Message to a peer whose token you've received via
 *              // the Whitelist Token Exchange TLV mechanism, or otherwise.
 *              // manager.sendPushMessageToPeer("bob@dukgo.com", new PushSecureClient.RequestCallback<Message>(){...});
 *          }
 *
 *          @Override
 *          public void onFailure(@NonNull Throwable throwable) {
 *              // Unable to authenticate ChatSecure-Push account.
 *
 *              // Check throwable for an error message describing the issue:
 *              // throwable.getMessage();
 *          }
 *      });
 *
 * }
 * </pre>
 * <p>
 * Created by dbro on 9/18/15.
 */
public class PushManager {

    public static final String DEFAULT_PROVIDER = "https://push.zom.im/api/v1/";

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

        client = new PushSecureClient(providerUrl);

        logAllTokens();

    }

    /**
     * @return the URL describing the ChatSecure-Push provider
     */
    @NonNull
    public String getProviderUrl() {
        return providerUrl;
    }

    /**
     * Create a Whitelist Token Exchange {@link TLV} for transmission to a remote peer over OTR.
     * This method obtains a new receiving Whitelist Token from ChatSecure-Push if necessary
     * before notifying {@param callback}.
     * <p>
     * The token(s) embedded within the resulting TLV will be marked as issued.
     */
    public void createWhitelistTokenExchangeTlv(@NonNull String issuerIdentifier,
                                                @NonNull String recipientIdentifier,
                                                @NonNull final PushSecureClient.RequestCallback<TLV> callback,
                                                @Nullable final String extraData) throws UnsupportedEncodingException {

     //   if (!assertAuthenticated()) return;

        if (Debug.DEBUG_ENABLED)
            Timber.d("createWhitelistTokenExchangeTlv recipient %s issuer %s", recipientIdentifier, issuerIdentifier);

        // Note that an outgoing Whitelist token must have the host identifier as it's "recipient"
        final Cursor persistedTokens = getPersistedTokenCursor(issuerIdentifier, recipientIdentifier, false);

        if (persistedTokens != null && persistedTokens.getCount() > 0) {
            Timber.d("Got token for identifier %s", issuerIdentifier);
            markWhitelistTokenIssued(persistedTokens.getInt(persistedTokens.getColumnIndex(PushDatabase.Tokens._ID)));

            String peerWhitelistToken = persistedTokens.getString(persistedTokens.getColumnIndex(PushDatabase.Tokens.TOKEN));
            TLV tokenTlv = createWhitelistTokenExchangeTlvWithToken(
                    new String[]{peerWhitelistToken},
                    null);
            callback.onSuccess(tokenTlv);
            persistedTokens.close();
            return;
        } else if (persistedTokens != null) persistedTokens.close();

        if (Debug.DEBUG_ENABLED)
            Timber.d("Got no token for recipient %s issuer %s. Creating new", recipientIdentifier, issuerIdentifier);

        createReceivingWhitelistTokenForPeer(issuerIdentifier, recipientIdentifier, new PushSecureClient.RequestCallback<PersistedPushToken>() {
            @Override
            public void onSuccess(@NonNull PersistedPushToken response) {
                try {
                    TLV tlv = new TLV(WhitelistTokenTlv.TLV_WHITELIST_TOKEN,
                            WhitelistTokenTlv.createGson().toJson(
                                    new WhitelistTokenTlv(
                                            response.providerUrl,
                                            new String[]{response.token},
                                            extraData))
                                    .getBytes("UTF-8"));
                    markWhitelistTokenIssued(response.localId);
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
     * This method uses the provided Whitelist tokens. See {@link WhitelistTokenTlv} for details
     * on the TLV data format.
     *
     * @param tokens    an array of ChatSecure-Push Whitelist Tokens to be packaged in the {@link TLV}.
     *                  These tokens are typically issued by a local user for transmission
     *                  to a remote user.
     * @param extraData additional data to be packaged in the {@link TLV}
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
                                if (Debug.DEBUG_ENABLED)
                                    Timber.d("Got Account");
                                account[0] = response;
                                client.setAccount(response);
                                setPersistedAccount(response, password, providerUrl);
                                preRequisiteLatch.countDown();
                            }

                            @Override
                            public void onFailure(@NonNull Throwable throwable) {
                                if (Debug.DEBUG_ENABLED)
                                    Timber.e("Failed to get Account", throwable);
                                taskThrowable = throwable;
                                preRequisiteLatch.abort();
                            }
                        });

                // Fetch GCM Registration Id
                /**
                GcmRegistration.getRegistrationIdAsync(context, new GcmRegistration.RegistrationCallback() {
                    @Override
                    public void onRegistration(String gcmRegistrationId) {
                        Timber.d("Got GCM");
                        gcmToken[0] = gcmRegistrationId;
                        preRequisiteLatch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Timber.e("Failed to get GCM", throwable);
                        taskThrowable = throwable;
                        preRequisiteLatch.abort();
                    }
                });*/

                try {
                    // Await the parallel completion of:
                    // (1) ChatSecure-Push Account registration
                    // (2) GCM registration.
                    preRequisiteLatch.await();
                    if (Debug.DEBUG_ENABLED)
                        Timber.d("Latch - Got GCM and CSP");

                    final AbortableCountDownLatch deviceRegistrationLatch = new AbortableCountDownLatch(1);

                    PushSecureClient.RequestCallback<Device> deviceCreatedOrUpdatedCallback =
                            new PushSecureClient.RequestCallback<Device>() {
                                @Override
                                public void onSuccess(@NonNull Device response) {
                                    Timber.d("Registered Device");
                                    deviceRegistrationLatch.countDown();
                                }

                                @Override
                                public void onFailure(@NonNull Throwable t) {
                                    Timber.e("Failed to register Device", t);

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
                    } else {
                        state = State.AUTHENTICATED;
                        deviceRegistrationLatch.countDown();
                    }

                    Timber.d("Awaiting device registration");
                    deviceRegistrationLatch.await();
                    Timber.d("Latch - Regisered device");
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
                    Timber.d("authenticateAccount finished with success");
                    callback.onSuccess(result);
                } else if (taskThrowable != null) {
                    Timber.e("authenticateAccount failed", taskThrowable);
                    callback.onFailure(taskThrowable);
                } else {
                    Timber.e("AuthenticateAccount task failed, but no error was reported");
                }
            }

        }.executeOnExecutor(Executors.newSingleThreadExecutor());
    }

    /**
     * Create a new Whitelist Token authorizing push access to the local device.
     * Must be called after {@link #authenticateAccount(String, String, PushSecureClient.RequestCallback)}.
     *
     * @param issuerIdentifier    a String uniquely identifying the local account that will receive
     *                            push messages with the produced Whitelist Token.
     * @param recipientIdentifier a String uniquely identifying the remote account who will use the
     *                            produced Whitelist Token to send push messages to your application.
     *                            This is stored internally with the token to enable functionality of
     *                            {@link #revokeWhitelistTokensForPeer(String, String, PushSecureClient.RequestCallback)}
     * @param callback
     */
    public void createReceivingWhitelistTokenForPeer(@NonNull final String issuerIdentifier,
                                                     @NonNull final String recipientIdentifier,
                                                     @NonNull final PushSecureClient.RequestCallback<PersistedPushToken> callback) {

        if (!assertAuthenticated()) return;

        final PersistedDevice thisDevice = getPersistedDevice();
        final String tokenIdentifier = createWhitelistTokenName(recipientIdentifier, thisDevice.name);

        client.createToken(thisDevice, tokenIdentifier, new PushSecureClient.RequestCallback<PushToken>() {
            @Override
            public void onSuccess(@NonNull PushToken response) {
                ContentValues tokenValues = new ContentValues(6);
                tokenValues.put(PushDatabase.Tokens.RECIPIENT, recipientIdentifier);
                tokenValues.put(PushDatabase.Tokens.ISSUER, issuerIdentifier);
                tokenValues.put(PushDatabase.Tokens.NAME, tokenIdentifier);
                tokenValues.put(PushDatabase.Tokens.TOKEN, response.token);
                tokenValues.put(PushDatabase.Tokens.DEVICE, thisDevice.localId);
                tokenValues.put(PushDatabase.Tokens.CREATED_DATE, PushDatabase.DATE_FORMATTER.format(new Date()));

                Uri persistedTokenUri = context.getContentResolver().insert(PushDatabase.Tokens.CONTENT_URI, tokenValues);
                if (Debug.DEBUG_ENABLED)
                    Timber.d("Inserted token %s for recipient %s issuer %s. Uri %s", response.token, recipientIdentifier, issuerIdentifier, persistedTokenUri);
                logAllTokens();

                String persistedTokenId = persistedTokenUri.getLastPathSegment();
                Cursor persistedTokenCursor = context.getContentResolver().query(Uri.withAppendedPath(PushDatabase.Tokens.CONTENT_URI, persistedTokenId), null, null, null, null);
                if (persistedTokenCursor != null && persistedTokenCursor.moveToFirst()) {
                    PersistedPushToken persistedPushToken = new PersistedPushToken(persistedTokenCursor);
                    callback.onSuccess(persistedPushToken);
                } else {
                    callback.onFailure(new IOException("Failed to retrieve persisted push token"));
                }

                if (persistedTokenCursor != null) persistedTokenCursor.close();
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
     * {@link #getPersistedWhitelistToken(String, String, PushSecureClient.RequestCallback)}
     *
     * @param tlv                 The Whitelist Token TLV received from the remote peer
     * @param recipientIdentifier a String uniquely identifying the local account that received {@param tlv}
     * @param issuerIdentifier    a String uniquely identifying the remote account that issued {@param tlv}
     */
    public void insertReceivedWhitelistTokensTlv(@NonNull WhitelistTokenTlv tlv,
                                                 @NonNull String recipientIdentifier,
                                                 @NonNull String issuerIdentifier) {

        for (int idx = 0; idx < tlv.tokens.length; idx++) {
            ContentValues tokenValues = new ContentValues(7);
            tokenValues.put(PushDatabase.Tokens.RECIPIENT, recipientIdentifier);
            tokenValues.put(PushDatabase.Tokens.ISSUER, issuerIdentifier);
            tokenValues.put(PushDatabase.Tokens.ISSUED, 1);//they have been issued to you
            tokenValues.put(PushDatabase.Tokens.PROVIDER, tlv.endpoint);
            tokenValues.put(PushDatabase.Tokens.NAME, createWhitelistTokenName(recipientIdentifier, issuerIdentifier));
            tokenValues.put(PushDatabase.Tokens.TOKEN, tlv.tokens[idx]);
            tokenValues.put(PushDatabase.Tokens.CREATED_DATE, PushDatabase.DATE_FORMATTER.format(new Date()));
            try {
                Uri uri = context.getContentResolver().insert(PushDatabase.Tokens.CONTENT_URI, tokenValues);
                if (Debug.DEBUG_ENABLED)
                    Timber.d("Inserted token %s for recipient %s issuer %s. Uri %s", tlv.tokens[idx], recipientIdentifier, issuerIdentifier, uri);
                logAllTokens();
            } catch (SQLiteConstraintException e) {
                // This token is already stored, ignore.
                Timber.e(e, "Failed to insert token %s.", tlv.tokens[idx], e);
            }
        }
    }

    /**
     * Retrieve a persisted Whitelist token for sending a push to {@param pushRecipientIdentifier} on behalf of
     * {@param pushSenderIdentifier}.
     * <p>
     * TODO: Make fully asynchronous or remove
     *
     * @param pushRecipientIdentifier a String uniquely identifying the remote peer who should receive
     *                                the push message. This identifier will be used to query
     * @param pushSenderIdentifier    a String uniquely identifying the local user which the push message
     *                                should be sent on behalf of.
     * @param callback                a callback which will receive the {@link PersistedPushToken}
     *                                or a {@link NoSuchElementException}
     */
    public void getPersistedWhitelistToken(@NonNull final String pushRecipientIdentifier,
                                           @NonNull final String pushSenderIdentifier,
                                           @NonNull final PushSecureClient.RequestCallback<PushToken> callback) {

        if (Debug.DEBUG_ENABLED)
            Timber.d("Lookup push token issued by %s received by %s", pushRecipientIdentifier, pushSenderIdentifier);
        Cursor persistedTokens = getPersistedTokenCursor(pushRecipientIdentifier, pushSenderIdentifier, true);
        if (persistedTokens != null && persistedTokens.getCount() > 0) {
            callback.onSuccess(new PersistedPushToken(persistedTokens));
            persistedTokens.close();
        } else {
            callback.onFailure(new NoSuchElementException(String.format("No token exists for peer %s", pushRecipientIdentifier)));
        }
        if (persistedTokens != null) persistedTokens.close();
    }

    /**
     * Retrieve a persisted Whitelist token for sending a push to {@param pushRecipientIdentifier} on behalf of
     * {@param pushSenderIdentifier}.
     * <p>
     * TODO: Make fully asynchronous or remove
     *
     * @param pushRecipientIdentifier a String uniquely identifying the remote peer who should receive
     *                                the push message. This identifier will be used to query
     * @param pushSenderIdentifier    a String uniquely identifying the local user which the push message
     *                                should be sent on behalf of.
     */
    public boolean hasPersistedWhitelistToken(@NonNull final String pushRecipientIdentifier,
                                           @NonNull final String pushSenderIdentifier) {

        boolean response = false;

        if (Debug.DEBUG_ENABLED)
            Timber.d("Lookup push token issued by %s received by %s", pushRecipientIdentifier, pushSenderIdentifier);
        Cursor persistedTokens = getPersistedTokenCursor(pushRecipientIdentifier, pushSenderIdentifier, true);
        response = (persistedTokens != null && persistedTokens.getCount() > 0);

        if (persistedTokens != null) persistedTokens.close();

        return response;
    }

    /**
     * Mark a Whitelist token as issued. This means we should consider it successfully transmitted
     * to its {@link PushDatabase.Tokens#RECIPIENT}, and it should not be transmitted to any other peers.
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
        if (result != 1) Timber.e("Failed to mark token %d as issued", tokenLocalId);
        else {
            if (Debug.DEBUG_ENABLED)
                Timber.d("Marked token %d issued", tokenLocalId);
            logAllTokens();
        }
    }

    /**
     * Mark a Whitelist token as issued. This means we should consider it successfully transmitted
     * to its {@link PushDatabase.Tokens#RECIPIENT}, and it should not be transmitted to any other peers.
     *
     * @param token the token value to make as issued/used
     */
    public void markWhitelistTokenIssued(String token) {
        ContentValues tokenValues = new ContentValues(1);
        tokenValues.put(PushDatabase.Tokens.ISSUED, 1);
        int result = context.getContentResolver().update(
                PushDatabase.Tokens.CONTENT_URI,
                tokenValues,
                PushDatabase.Tokens.TOKEN + " = ?",
                new String[]{token});
        if (result != 1) Timber.e("Failed to mark token %d as issued", token);
        else {
            Timber.d("Marked token as issued: " + token);
            logAllTokens();
        }
    }

    /**
     * Revoke Whitelist tokens created by this application install for the given recipient. This method
     * will only succeed if the matching Whitelist Token(s) was/were created by the ChatSecure-Push account
     * currently active as a result of {@link #authenticateAccount(String, String, PushSecureClient.RequestCallback)}.
     * NOTE: This does not currently delete tokens that may have been issued by another application install.
     * Currently, the only way to do that is to adopt a common naming convention for tokens that incorporates
     * the recipient OR to delete all tokens the server reports.
     * (e.g: Created via {@link #createReceivingWhitelistTokenForPeer(String, String, PushSecureClient.RequestCallback)}
     * Must be called after {@link #authenticateAccount(String, String, PushSecureClient.RequestCallback)}.
     *
     * @param issuerIdentifier    a String uniquely identifying the local user who issued the tokens to be revoked.
     * @param recipientIdentifier a String uniquely identifying the remote user who was issued the tokens to be revoked.
     * @param callback            a callback indicating success or failure.
     */
    public void revokeWhitelistTokensForPeer(@NonNull final String issuerIdentifier,
                                             @NonNull final String recipientIdentifier,
                                             @NonNull final PushSecureClient.RequestCallback<Void> callback) {

   //     if (!assertAuthenticated()) return;

        final Cursor recipientTokens = getPersistedTokenCursor(issuerIdentifier, recipientIdentifier, false);
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

    /**
     * Send a ChatSecure-Push push message from {@param issuerIdentifier} to {@param recipientIdentifier}
     * if a Whitelist Token matching the pair is available.
     *
     * @param issuerIdentifier    a String uniquely identifying the local user who is issuing the push message.
     * @param recipientIdentifier a String uniquely identifying the remote user who will receive the push message.
     * @param callback            a callback indicating success or failure
     */
    public void sendPushMessageToPeer(@NonNull final String issuerIdentifier,
                                      @NonNull final String recipientIdentifier,
                                      @NonNull final PushSecureClient.RequestCallback<org.chatsecure.pushsecure.response.Message> callback) {

      //  if (!assertAuthenticated()) return;

        if (Debug.DEBUG_ENABLED)
            Timber.d("Send push to %s from %s", recipientIdentifier, issuerIdentifier);
        getPersistedWhitelistToken(recipientIdentifier, issuerIdentifier, new PushSecureClient.RequestCallback<PushToken>() {
            @Override
            public void onSuccess(@NonNull PushToken response) {
                PersistedPushToken ppt = (PersistedPushToken)response;
                sendPushMessageToToken(response.token, ppt.providerUrl, callback);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }


    // </editor-fold desc="Public API">

    // <editor-fold desc="Private API">

    /**
     * @param recipientIdentifier the recipient of the token. To receive a token for
     *                            transmission to a remote peer, this should be the local host's
     *                            identifier. To receive a token for sending a push to a remote
     *                            peer, this should be that remote peer's identifier.
     * @param issuedFilter        Filter the returned tokens by those that have
     *                            been marked 'issued' or those that have not. When retrieving a
     *                            list of tokens to be revoked this should be true. When retrieving
     *                            a list of tokens for transmission to remote peers, this should be false.
     */
    @Nullable
    private Cursor getPersistedTokenCursor(@NonNull String issuerIdentifier,
                                           @NonNull String recipientIdentifier,
                                           boolean issuedFilter) {

        String where = PushDatabase.Tokens.RECIPIENT + " = ? "
                + " AND " + PushDatabase.Tokens.ISSUER + " = ? "
                + " AND " + PushDatabase.Tokens.ISSUED + " = ?";

        String[] whereArgs = new String[]{recipientIdentifier, issuerIdentifier, issuedFilter ? "1" : "0"};

        Cursor result = context.getContentResolver().query(
                PushDatabase.Tokens.CONTENT_URI,
                null,
                where,
                whereArgs,
                PushDatabase.Tokens.CREATED_DATE + " DESC");  // Most recent tokens first
        if (Debug.DEBUG_ENABLED)
            Timber.d("Query token for recipient %s issuer %s isued %b. result: %d", recipientIdentifier, issuerIdentifier, issuedFilter, (result != null ? result.getCount() : 0));
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

        if (accountCursor == null) {
            Timber.w("Persisted Account cursor was null. Maybe CacheWord was not available?");
            return null;
        } else if (!accountCursor.moveToFirst()) {
            accountCursor.close();
            Timber.d("Persisted Account cursor had no rows");
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
        deviceValues.put(PushDatabase.Devices.SERVER_ID, device.id);
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

        Timber.d("Updating device");
        Device updatedDevice = Device.withUpdatedRegistrationId(deviceToUpdate, gcmRegistrationId);
        client.updateDevice(updatedDevice, new PushSecureClient.RequestCallback<Device>() {
            @Override
            public void onSuccess(@NonNull Device response) {
                Timber.d("Updated device");
                setPersistedDevice(response);
                state = State.AUTHENTICATED;
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Timber.e("Failed to update device", t);
                callback.onFailure(t);
            }
        });
    }

    private boolean assertAuthenticated() {
        boolean authenticated = state == State.AUTHENTICATED;
        if (!authenticated) {
            if (Debug.DEBUG_ENABLED)
                Timber.w("Not authenticated. Cannot request whitelist token. Did you await the result of #authenticateAccount()");
        }
        return authenticated;
    }

    /**
     * Send a push message to a Whitelist token.
     *
     * @param recipientWhitelistToken a raw ChatSecure-Push Whitelist Token string.
     * @param callback                a callback indicating success or failure
     */
    private void sendPushMessageToToken(@NonNull String recipientWhitelistToken,@NonNull String recipientProviderUrl,
                                        @NonNull PushSecureClient.RequestCallback<org.chatsecure.pushsecure.response.Message> callback) {

    //    if (!assertAuthenticated()) return;
   //     client = new PushSecureClient(providerUrl);
        client.sendMessage(recipientWhitelistToken, "" /* push payload */, recipientProviderUrl, callback);
    }

    // </editor-fold desc="Private API">

    // <editor-fold desc="Utility">

    private void logTokens(@NonNull String recipientIdentifier,
                           @NonNull String issuerIdentifier) {

        StringBuilder log = new StringBuilder(String.format("Tokens issued by %s, received by %s:", issuerIdentifier, recipientIdentifier));

        Cursor issuedTokens = getPersistedTokenCursor(issuerIdentifier, recipientIdentifier, true);

        if (issuedTokens != null && issuedTokens.moveToFirst()) {
            do {
                log.append(issuedTokens.getString(issuedTokens.getColumnIndex(PushDatabase.Tokens.TOKEN)));
            } while (issuedTokens.moveToNext());
        }

        if (issuedTokens != null) issuedTokens.close();


        Cursor nonIssuedTokens = getPersistedTokenCursor(issuerIdentifier, recipientIdentifier, false);

        if (nonIssuedTokens != null && nonIssuedTokens.moveToFirst()) {
            do {
                log.append(nonIssuedTokens.getString(nonIssuedTokens.getColumnIndex(PushDatabase.Tokens.TOKEN)));
            } while (nonIssuedTokens.moveToNext());
        }

        if (nonIssuedTokens != null) nonIssuedTokens.close();

        if (Debug.DEBUG_ENABLED)
            Timber.d(log.toString());
    }

    private void logAllTokens() {

        if (Debug.DEBUG_ENABLED) {
            StringBuilder log = new StringBuilder("All Whitelist Tokens:\nId\tRecipient\tIssuer\tIssued\tToken\n");

            Cursor allTokens = context.getContentResolver().query(PushDatabase.Tokens.CONTENT_URI, null, null, null, PushDatabase.Tokens.CREATED_DATE + " DESC");

            if (allTokens != null && allTokens.moveToFirst()) {
                do {
                    log.append(allTokens.getInt(allTokens.getColumnIndex(PushDatabase.Tokens._ID)));
                    log.append('\t');
                    log.append(allTokens.getString(allTokens.getColumnIndex(PushDatabase.Tokens.RECIPIENT)));
                    log.append('\t');
                    log.append(allTokens.getString(allTokens.getColumnIndex(PushDatabase.Tokens.ISSUER)));
                    log.append('\t');
                    log.append(allTokens.getInt(allTokens.getColumnIndex(PushDatabase.Tokens.ISSUED)));
                    log.append('\t');
                    log.append(allTokens.getString(allTokens.getColumnIndex(PushDatabase.Tokens.TOKEN)));
                    log.append('\t');
                    log.append(allTokens.getString(allTokens.getColumnIndex(PushDatabase.Tokens.PROVIDER)));
                    log.append('\t');
                    log.append('\n');

                } while (allTokens.moveToNext());
            }
            if (allTokens != null) allTokens.close();
            Timber.d(log.toString());
        }
    }

    /**
     * Strip the resource off a JabberID. e.g: a@b.com/foo -> a@b.com
     * When using JIDs as the identifiers in methods like {@link #getPersistedTokenCursor(String, String, boolean)},
     * you should strip the resource off the JID, because if the contact is offline we'll just have the bare JID available.
     */
    public static String stripJabberIdResource(@NonNull String fullJabberId) {
        int trailingSlashIdx = fullJabberId.lastIndexOf('/');
        if (trailingSlashIdx != -1) {
            return fullJabberId.substring(0, trailingSlashIdx);
        }
        return fullJabberId;
    }

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
}
