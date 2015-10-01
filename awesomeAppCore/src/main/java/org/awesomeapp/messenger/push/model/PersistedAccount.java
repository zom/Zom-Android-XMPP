package org.awesomeapp.messenger.push.model;

import android.database.Cursor;
import android.support.annotation.NonNull;

/**
 * A {@link org.chatsecure.pushsecure.response.Account} that is persisted to our application's
 * database. Thus it includes an additional {@link #localId} parameter uniquely identifying
 * the record in our app database, a {@link #pasword} used to authenticate with
 * the ChatSecure-Push server, and a {@link #providerUrl} describing the backend instance.
 *
 * Created by dbro on 9/24/15.
 */
public class PersistedAccount extends org.chatsecure.pushsecure.response.Account {

    public final int localId;
    public final String pasword;
    public final String providerUrl;

    public PersistedAccount(@NonNull Cursor cursor) {
        super(cursor.getString(cursor.getColumnIndex(PushDatabase.Accounts.USERNAME)),
                cursor.getString(cursor.getColumnIndex(PushDatabase.Accounts.PASSWORD)),
                cursor.getString(cursor.getColumnIndex(PushDatabase.Accounts.EMAIL)));

        this.localId = cursor.getInt(cursor.getColumnIndex(PushDatabase.Accounts._ID));
        this.pasword = cursor.getString(cursor.getColumnIndex(PushDatabase.Accounts.PASSWORD));
        this.providerUrl = cursor.getString(cursor.getColumnIndex(PushDatabase.Accounts.PROVIDER));
    }
}
