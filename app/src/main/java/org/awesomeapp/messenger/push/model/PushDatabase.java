package org.awesomeapp.messenger.push.model;

import android.net.Uri;
import android.provider.BaseColumns;

import org.awesomeapp.messenger.provider.Imps;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * A representation of ChatSecure-Push's database in the existing codebase's style. I've tried to
 * isolate the table definitions from {@link org.awesomeapp.messenger.provider.ImpsProvider} so
 * that it's easy to see the additional data requirements of ChatSecure-Push.
 * <p>
 * For new apps, it's preferable to use something like Schematic to generate this SQL boilerplate.
 * <p>
 * Created by dbro on 9/23/15.
 */
public class PushDatabase {

    /**
     * Database Date-to-String format
     */
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

    private interface ChatSecurePushBaseColumns {

        /**
         * The ChatSecure-Push server's identifier for this entity. Used to identify an entity
         * in API calls.
         */
        String SERVER_ID = "s_id";
    }

    // <editor-fold desc="Accounts">

    /**
     * A ChatSecure-Push account. This generally represents one application-install, though it
     * can be migrated across installations with username/password credentials.
     */
    public static final class Accounts implements AccountColumns, BaseColumns, ChatSecurePushBaseColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.awesomeapp.messenger.provider.Imps/csp-accounts");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of ChatSecure-Push Accounts.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-csp-accounts";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single ChatSecure-Push Account.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-csp-accounts";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = AccountColumns.USERNAME + " ASC";

    }

    private interface AccountColumns {

        /**
         * The ChatSecure-Push account username <p>Type: TEXT</p>
         */
        String USERNAME = "username";

        /**
         * The ChatSecure-Push account email (optional) <p>Type: TEXT</p>
         */
        String EMAIL = "email";

        /**
         * The ChatSecure-Push account password <p>Type: TEXT</p>
         */
        String PASSWORD = "pw";

        /**
         * The ChatSecure-Push provider. This is usually a URL corresponding to the
         * ChatSecure-Push backend. e.g: "https://chatsecure-push.herokuapp.com/api/v1/" <p>Type: TEXT</p>
         */
        String PROVIDER = "provider";
    }

    public static String getAccountsTableSqlWithName(String tablename) {
        return "CREATE TABLE " + tablename + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                + ChatSecurePushBaseColumns.SERVER_ID + " TEXT,"
                + AccountColumns.USERNAME + " TEXT NOT NULL,"
                + AccountColumns.EMAIL + " TEXT,"
                + AccountColumns.PASSWORD + " TEXT NOT NULL,"
                + AccountColumns.PROVIDER + " TEXT NOT NULL,"
                + "UNIQUE (" + Imps.AccountColumns.USERNAME + ")"
                + ");";
    }

    // </editor-fold desc="Accounts">

    // <editor-fold desc="Devices">

    /**
     * A ChatSecure-Push Device. This represents a entity that can receive push messages. On Android,
     * this is a GCM device.
     */
    public static final class Devices implements DeviceColumns, BaseColumns, ChatSecurePushBaseColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.awesomeapp.messenger.provider.Imps/csp-devices");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of ChatSecure-Push Devices.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-csp-devices";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single ChatSecure-Push Devices.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-csp-devices";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = DeviceColumns.NAME + " ASC";

    }

    private interface DeviceColumns {

        /**
         * The ChatSecure-Push device name <p>Type: TEXT</p>
         */
        String NAME = "name";

        /**
         * The ChatSecure-Push device registraion id. On Android, this is the host device's
         * GCM token. <p>Type: TEXT</p>
         */
        String REGISTRATION_ID = "reg_id";

        /**
         * The ChatSecure-Push device id. This is an optional identifier for the hardware device.
         * It's used to match a device to an existing ChatSecure-Push device if the ChatSecure-Push
         * issued device id is not known. e.g: A fresh install of an app onto an already-registered
         * device won't produce duplicate device entries if this field is used.
         * <p>Type: TEXT</p>
         */
        String DEVICE_ID = "d_id";

        /**
         * The date this device was registered. This value is reported by the ChatSecure-Push
         * server on device creation. <p>Type: TEXT</p>
         */
        String DATE_CREATED = "date";

        /**
         * Whether or not this device is "active", or should receive push messages.
         */
        String ACTIVE = "active";
    }

    public static String getDeviceTableSqlWithName(String tablename) {
        return "CREATE TABLE " + tablename + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                + ChatSecurePushBaseColumns.SERVER_ID + " TEXT,"
                + DeviceColumns.NAME + " TEXT,"
                + DeviceColumns.REGISTRATION_ID + " TEXT NOT NULL,"
                + DeviceColumns.DEVICE_ID + " TEXT,"
                + DeviceColumns.DATE_CREATED + " TEXT, "
                + DeviceColumns.ACTIVE + " INTEGER, "
                + "UNIQUE (" + DeviceColumns.REGISTRATION_ID + ", " + DeviceColumns.DEVICE_ID + ")"
                + ");";
    }

    // </editor-fold desc="Devices">

    // <editor-fold desc="Tokens">

    /**
     * A ChatSecure-Push Whitelist Token. A Token grants its recipient push access to a
     * {@link org.awesomeapp.messenger.push.model.PushDatabase.Devices} entity that may be revoked
     * by its issuer.
     */
    public static final class Tokens implements TokenColumns, BaseColumns, ChatSecurePushBaseColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.awesomeapp.messenger.provider.Imps/csp-tokens");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of ChatSecure-Push Whitelist
         * tokens.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-csp-tokens";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single ChatSecure-Push Whitelist
         * tokens.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-csp-tokens";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = TokenColumns.NAME + " ASC";

    }

    private interface TokenColumns {

        /**
         * The ChatSecure-Push token name <p>Type: TEXT</p>
         */
        String NAME = "name";

        /**
         * The ChatSecure-Push server URL that issued this token <p>Type: TEXT</p>
         */
        String PROVIDER = "provider";

        /**
         * An identifier describing the issuer of this token. If this is a token created
         * by this application, this value will correspond to a local account identifier <p>Type: TEXT</p>
         */
        String ISSUER = "issuer";

        /**
         * An identifier describing the intended recipient of this token. If this is a "sending"
         * token received from a remote peer, this value will correspond to a local account identifier <p>Type: TEXT</p>
         */
        String RECIPIENT = "recipient";

        /**
         * The date this token was registered. This value is reported by the ChatSecure-Push
         * server on token creation. <p>Type: TEXT</p>
         */
        String CREATED_DATE = "date";

        /**
         * The actual token value. This can be thought of us as an address for push messages
         * that corresponds to the device identified during the creation of this token <p>Type: TEXT</p>
         */
        String TOKEN = "token";

        /**
         * The id of the corresponding device in the {@link PushDatabase.Devices} collection <p>Type: INTEGER</p>
         */
        String DEVICE = "d_id";

        /**
         * Whether or not this token was transmitted to {@link #RECIPIENT} <p>Type: INTEGER</p>
         */
        String ISSUED = "issued";
    }

    public static String getTokenTableSqlWithName(String tablename) {
        return "CREATE TABLE " + tablename + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                + ChatSecurePushBaseColumns.SERVER_ID + " TEXT,"
                + TokenColumns.NAME + " TEXT,"
                + TokenColumns.PROVIDER + " TEXT,"
                + TokenColumns.RECIPIENT + " TEXT,"
                + TokenColumns.ISSUER + " TEXT,"
                + TokenColumns.CREATED_DATE + " TEXT,"
                + TokenColumns.TOKEN + " TEXT NOT NULL,"
                + TokenColumns.DEVICE + " INTEGER,"
                + TokenColumns.ISSUED + " INTEGER NOT NULL DEFAULT 0,"
                + "UNIQUE (" + TokenColumns.TOKEN + ")"
                + ");";
    }

    // </editor-fold desc="Tokens">
}
