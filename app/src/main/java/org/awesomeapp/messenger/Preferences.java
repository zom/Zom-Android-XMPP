package org.awesomeapp.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.awesomeapp.messenger.util.Languages;

import java.util.Locale;

import im.zom.messenger.R;


/**
 * Handles all global preferences that do not need to be stored encrypted,
 * looking after the names of preferences, default values and caching. Needs
 * to be setup in {@link ImApp} using
 * {@link Preferences#setup(android.content.Context)} before it is used.
 */
public class Preferences {

    public static final String TAG = "Preferences";

    /* start encryption modes for OTR */
    public static final String OTR_MODE_FORCE = "force";
    public static final String OTR_MODE_AUTO = "auto";
    public static final String OTR_MODE_REQUESTED = "requested";
    public static final String OTR_MODE_DISABLED = "disabled";
    /**
     * Has the same order as {@link #getOtrModeNames()}
     */
    public static final String[] OTR_MODE_VALUES = {
            OTR_MODE_FORCE,
            OTR_MODE_AUTO,
            OTR_MODE_REQUESTED,
            OTR_MODE_DISABLED
    };
    public static final String DEFAULT_LANGUAGE = Locale.getDefault().getLanguage();
    public static final String DEFAULT_OTR_MODE = OTR_MODE_AUTO;
    public static final String DEFAULT_NOTIFICATION_RINGTONE_URI = "content://settings/system/notification_sound";
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 1;
    public static final boolean DEFAULT_DEBUG_LOGGING = false;
    public static final boolean DEFAULT_DELETE_INSECURE_MEDIA = false;
    public static final boolean DEFAULT_FOREGROUND_PRIORITY = true;
    public static final boolean DEFAULT_HIDE_OFFLINE_CONTACTS = false;
    public static final boolean DEFAULT_LINKIFY_DO = false;
    public static final boolean DEFAULT_NOTIFICATION = true;
    public static final boolean DEFAULT_NOTIFICATION_SOUND = true;
    public static final boolean DEFAULT_NOTIFICATION_VIBRATE = true;
    public static final boolean DEFAULT_START_ON_BOOT = true;
    public static final boolean DEFAULT_LOCK_APP = true;
    public static final boolean DEFAULT_CLEAR_APP_DATA = false;
    public static final boolean DEFAULT_UNINSTALL_APP = false;

    private static final String DEBUG_LOGGING = "prefDebug";
    private static final String DELETE_INSECURE_MEDIA = "pref_delete_unsecured_media";
    private static final String FOREGROUND_PRIORITY = "pref_foreground_enable";
    private static final String HEARTBEAT_INTERVAL = "pref_heartbeat_interval";
    private static final String HIDE_OFFLINE_CONTACTS = "pref_hide_offline_contacts";
    private static final String LANGUAGE = "pref_language";
    private static final String LINKIFY_DO = "pref_linkify_do";
    private static final String NOTIFICATION = "pref_enable_notification";
    private static final String NOTIFICATION_SOUND = "pref_notification_sound";
    private static final String NOTIFICATION_VIBRATE = "pref_notification_vibrate";
    private static final String NOTIFICATION_RINGTONE_URI = "pref_notification_ringtone";
    private static final String OTR_MODE = "pref_security_otr_mode";
    private static final String START_ON_BOOT = "pref_start_on_boot";
    private static final String LOCK_APP = "lock_app";
    private static final String CLEAR_APP_DATA = "clear_app_data";
    private static final String UNINSTALL_APP = "uninstall_app";
    private static final String USE_TIBETAN_DICTIONARY = "prefEnableTibetanDictionary";
    private static final String ADVANCED_NETWORKING = "prefAdvancedNetworking";

    private final static String PROXY_SERVER_HOST = "pref_proxy_server_host";
    private final static String PROXY_SERVER_PORT = "pref_proxy_server_port";

    private final static String BLOCK_SCREENSHOTS = "prefBlockScreenshots";

    private final static String USE_PROOF_MODE = "prefProofMode";

    private static Context context;
    private static SharedPreferences preferences;
    private static Preferences instance;

    private Preferences(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setup(Context context) {
        if (instance != null) {
            final String error = "Attempted to reinitialize preferences after it " +
                    "has already been initialized in ImApp";
            throw new RuntimeException(error);
        }
        instance = new Preferences(context);
    }

    /**
     * heartbeat interval in minutes
     */
    public static int getHeartbeatInterval() {
        try {
            // when using preferences.xml, numbers will be stored as a String
            String intervalString = preferences.getString(HEARTBEAT_INTERVAL,
                    String.valueOf(DEFAULT_HEARTBEAT_INTERVAL));
            return Integer.valueOf(intervalString);
        } catch (NumberFormatException e1) {
            return DEFAULT_HEARTBEAT_INTERVAL;
        }
    }

    public static void setHeartbeatInterval(int minutes) {
        preferences.edit().putString(HEARTBEAT_INTERVAL, String.valueOf(minutes)).apply();
    }

    public static boolean getDoLinkify() {
        return preferences.getBoolean(LINKIFY_DO, DEFAULT_LINKIFY_DO);
    }

    public static void setDoLinkify(boolean linkify) {
        preferences.edit().putBoolean(LINKIFY_DO, linkify).apply();
    }

    public static boolean getDeleteInsecureMedia() {
        return preferences.getBoolean(DELETE_INSECURE_MEDIA, DEFAULT_DELETE_INSECURE_MEDIA);
    }

    public static void setDeleteInsecureMedia(boolean delete) {
        preferences.edit().putBoolean(DELETE_INSECURE_MEDIA, delete).apply();
    }

    public static boolean getHideOfflineContacts() {
        return preferences.getBoolean(HIDE_OFFLINE_CONTACTS, DEFAULT_HIDE_OFFLINE_CONTACTS);
    }

    public static void setHideOfflineContacts(boolean hide) {
        preferences.edit().putBoolean(HIDE_OFFLINE_CONTACTS, hide).apply();
    }

    public static boolean isNotificationEnabled() {
        return preferences.getBoolean(NOTIFICATION, DEFAULT_NOTIFICATION);
    }

    public static void setNotification(boolean enable) {
        preferences.edit().putBoolean(NOTIFICATION, enable).apply();
    }

    public static boolean getNotificationSound() {
        return preferences.getBoolean(NOTIFICATION_SOUND, DEFAULT_NOTIFICATION_SOUND);
    }

    public static void setNotificationSound(boolean enable) {
        preferences.edit().putBoolean(NOTIFICATION_SOUND, enable).apply();
    }

    public static boolean getNotificationVibrate() {
        return preferences.getBoolean(NOTIFICATION_VIBRATE, DEFAULT_NOTIFICATION_VIBRATE);
    }

    public static void setNotificationVibrate(boolean enable) {
        preferences.edit().putBoolean(NOTIFICATION_VIBRATE, enable).apply();
    }

    public static boolean getUseForegroundPriority() {
        return preferences.getBoolean(FOREGROUND_PRIORITY, DEFAULT_FOREGROUND_PRIORITY);
    }

    public static void setForegroundPriority(boolean enable) {
        preferences.edit().putBoolean(FOREGROUND_PRIORITY, enable).apply();
    }

    public static boolean getDebugLogging() {
        return preferences.getBoolean(DEBUG_LOGGING, DEFAULT_DEBUG_LOGGING);
    }

    public static void setDebugLogging(boolean enable) {
        preferences.edit().putBoolean(DEBUG_LOGGING, enable).apply();
    }

    public static boolean startOnBoot() {
        return preferences.getBoolean(START_ON_BOOT, DEFAULT_START_ON_BOOT);
    }

    public static void setStartOnBoot(boolean enable) {
        preferences.edit().putBoolean(START_ON_BOOT, enable).apply();
    }

    public static boolean lockApp() {
        return preferences.getBoolean(LOCK_APP, DEFAULT_LOCK_APP);
    }

    public static void setLockApp(boolean enable) {
        preferences.edit().putBoolean(LOCK_APP, enable).apply();
    }

    public static boolean clearAppData() {
        return preferences.getBoolean(CLEAR_APP_DATA, DEFAULT_CLEAR_APP_DATA);
    }

    public static void setClearAppData(boolean enable) {
        preferences.edit().putBoolean(CLEAR_APP_DATA, enable).apply();
    }

    public static boolean uninstallApp() {
        return preferences.getBoolean(UNINSTALL_APP, DEFAULT_UNINSTALL_APP);
    }

    public static void setUninstallApp(boolean uninstallApp) {
        preferences.edit().putBoolean(UNINSTALL_APP, uninstallApp).apply();
    }

    public static String getLanguage() {
        return preferences.getString(LANGUAGE, DEFAULT_LANGUAGE);
    }

    public static void setLanguage(String code) {
        preferences.edit().putString(LANGUAGE, code).apply();
    }

    public static boolean isLanguageTibetan() {
        return TextUtils.equals(getLanguage(), Languages.TIBETAN.getLanguage());
    }

    public static Uri getNotificationRingtoneUri() {
        return Uri.parse(preferences.getString(NOTIFICATION_RINGTONE_URI, DEFAULT_NOTIFICATION_RINGTONE_URI));
    }

    public static void setNotificationRingtoneUri(String uriString) {
        if (TextUtils.isEmpty(uriString)) {
            preferences.edit().putString(NOTIFICATION_RINGTONE_URI, DEFAULT_NOTIFICATION_RINGTONE_URI).apply();
        } else {
            preferences.edit().putString(NOTIFICATION_RINGTONE_URI, uriString).apply();
        }
    }

    public static void setNotificationRingtone(Uri uri) {
        preferences.edit().putString(NOTIFICATION_RINGTONE_URI, uri.toString()).apply();
    }

    public static String getOtrMode() {
        return preferences.getString(OTR_MODE, DEFAULT_OTR_MODE);
    }

    public static void setOtrMode(String otrMode) {
        preferences.edit().putString(OTR_MODE, otrMode).commit();
    }

    /**
     * Has the same order as {@link #getOtrModeNames()}
     */
    public static String[] getOtrModeValues() {
        return OTR_MODE_VALUES;
    }

    /**
     * Has the same order as {@link #OTR_MODE_VALUES}
     */
    public static String[] getOtrModeNames() {
        final String names[] = new String[4];
        names[0] = context.getString(R.string.otr_mode_force);
        names[1] = context.getString(R.string.otr_mode_auto);
        names[2] = context.getString(R.string.otr_mode_requested);
        names[3] = context.getString(R.string.otr_mode_disabled);
        return names;
    }

    public static boolean getUseTibetanDictionary() {
        return preferences.getBoolean(USE_TIBETAN_DICTIONARY, true);
    }

    public static boolean useAdvancedNetworking ()
    {
        return preferences.getBoolean(ADVANCED_NETWORKING,false);
    }

    public static void enableAdvancedNetworking (boolean enable)
    {
        preferences.edit().putBoolean(ADVANCED_NETWORKING,enable).apply();
    }

    public static String getProxyServerHost ()
    {
        return preferences.getString(PROXY_SERVER_HOST,null);
    }

    public static int getProxyServerPort ()
    {
        return preferences.getInt(PROXY_SERVER_PORT,-1);
    }

    public static boolean doBlockScreenshots () { return preferences.getBoolean(BLOCK_SCREENSHOTS,true);}

    public static boolean doGroupEncryption () { return preferences.getBoolean("prefGroupEncryption", false);}

    public static boolean doCheckBatteryOptimizations () { return preferences.getBoolean("prefCheckBattery",true); };

    public static void checkedBatteryOptimizations () {
        preferences.edit().putBoolean("prefCheckBattery", false).apply();
    }

    public static boolean useProofMode () { return preferences.getBoolean(USE_PROOF_MODE, false);}

    public static void setUseProofMode (boolean useProofMode) { preferences.edit().putBoolean(USE_PROOF_MODE,useProofMode).apply(); }
}
