package org.awesomeapp.messenger.push.model;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.text.ParseException;

/**
 * A {@link org.chatsecure.pushsecure.response.Device} that is persisted to our application's
 * database. Thus it includes an additional {@link #localId} parameter uniquely identifying
 * the record in our app database.
 *
 * Created by dbro on 9/24/15.
 */
public class PersistedDevice extends org.chatsecure.pushsecure.response.Device {

    public final int localId;

    public PersistedDevice(@NonNull Cursor cursor) throws ParseException {

        super(cursor.getString(cursor.getColumnIndex(PushDatabase.Devices.NAME)),
                cursor.getString(cursor.getColumnIndex(PushDatabase.Devices.REGISTRATION_ID)),
                cursor.getString(cursor.getColumnIndex(PushDatabase.Devices.DEVICE_ID)),
                cursor.getString(cursor.getColumnIndex(PushDatabase.Devices.SERVER_ID)),
                cursor.getInt(cursor.getColumnIndex(PushDatabase.Devices.ACTIVE)) == 1,
                PushDatabase.DATE_FORMATTER.parse(cursor.getString(cursor.getColumnIndex(PushDatabase.Devices.DATE_CREATED))));

        this.localId = cursor.getInt(cursor.getColumnIndex(PushDatabase.Devices._ID));
    }
}
