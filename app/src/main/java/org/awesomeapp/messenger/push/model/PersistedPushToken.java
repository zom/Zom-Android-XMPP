package org.awesomeapp.messenger.push.model;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.awesomeapp.messenger.push.WhitelistTokenTlv;
import org.chatsecure.pushsecure.response.PushToken;

/**
 * Created by davidbrodsky on 6/24/15.
 */
public class PersistedPushToken extends PushToken {

    public final int localId;
    public final String providerUrl;

    public PersistedPushToken(@NonNull Cursor cursor) {
        super(cursor.getString(cursor.getColumnIndex(PushDatabase.Tokens.TOKEN)),
                cursor.getString(cursor.getColumnIndex(PushDatabase.Tokens.NAME)),
                null,
                null);

        this.localId = cursor.getInt(cursor.getColumnIndex(PushDatabase.Tokens._ID));
        this.providerUrl = cursor.getString(cursor.getColumnIndex(PushDatabase.Tokens.PROVIDER));
    }

}
