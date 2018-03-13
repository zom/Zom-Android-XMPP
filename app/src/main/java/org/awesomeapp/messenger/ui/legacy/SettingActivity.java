/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.awesomeapp.messenger.ui.legacy;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.Preferences;
import org.awesomeapp.messenger.service.RemoteImService;
import org.awesomeapp.messenger.ui.PanicSetupActivity;
import org.awesomeapp.messenger.util.Languages;
import org.jivesoftware.smackx.bookmarks.BookmarkManager;

import java.util.ArrayList;

import im.zom.messenger.R;
import info.guardianproject.panic.Panic;
import info.guardianproject.panic.PanicResponder;

public class SettingActivity extends PreferenceActivity {
    private static final String TAG = "SettingActivity";

    private static final int CHOOSE_RINGTONE = 5;

    private PackageManager pm;
    private String currentLanguage;
    ListPreference mOtrMode;
    ListPreference mPanicTriggerApp;
    Preference mPanicConfig;
    ListPreference mLanguage;
  //  CheckBoxPreference mLinkifyOnTor;
    CheckBoxPreference mHideOfflineContacts;
    CheckBoxPreference mDeleteUnsecuredMedia;
    CheckBoxPreference mEnableNotification;
    CheckBoxPreference mNotificationVibrate;
    CheckBoxPreference mNotificationSound;
    CheckBoxPreference mForegroundService;
    CheckBoxPreference mAllowScreenshot;
    EditTextPreference mHeartbeatInterval;

    Preference mNotificationRingtone;

    private void setInitialValues() {
        mOtrMode.setValue(Preferences.getOtrMode());

//        mLinkifyOnTor.setChecked(Preferences.getDoLinkify());
        mHideOfflineContacts.setChecked(Preferences.getHideOfflineContacts());
        mDeleteUnsecuredMedia.setChecked(Preferences.getDeleteInsecureMedia());
        mEnableNotification.setChecked(Preferences.isNotificationEnabled());
        mNotificationVibrate.setChecked(Preferences.getNotificationVibrate());
        mNotificationSound.setChecked(Preferences.getNotificationSound());
        mForegroundService.setChecked(Preferences.getUseForegroundPriority());

        mHeartbeatInterval.setText(String.valueOf(Preferences.getHeartbeatInterval()));

        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
        entries.add(0, getString(R.string.panic_app_none));
        entries.add(1, getString(R.string.panic_app_default));
        entryValues.add(0, Panic.PACKAGE_NAME_NONE);
        entryValues.add(1, Panic.PACKAGE_NAME_DEFAULT);

        for (ResolveInfo resolveInfo : PanicResponder.resolveTriggerApps(pm)) {
            if (resolveInfo.activityInfo == null)
                continue;
            entries.add(resolveInfo.activityInfo.loadLabel(pm));
            entryValues.add(resolveInfo.activityInfo.packageName);
        }
        mPanicTriggerApp.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mPanicTriggerApp.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        PanicResponder.configTriggerAppListPreference(mPanicTriggerApp,
                R.string.panic_trigger_app_summary, R.string.panic_app_none_summary);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        pm = getPackageManager();

        mOtrMode = (ListPreference) findPreference("pref_security_otr_mode");
        mOtrMode.setEntries(Preferences.getOtrModeNames());
        mOtrMode.setEntryValues(Preferences.getOtrModeValues());
        mOtrMode.setDefaultValue(Preferences.DEFAULT_OTR_MODE);

        mAllowScreenshot = (CheckBoxPreference)findPreference("prefBlockScreenshots");

        mPanicTriggerApp = (ListPreference) findPreference("pref_panic_trigger_app");
        mPanicConfig = (Preference) findPreference("pref_panic_config");
        mLanguage = (ListPreference) findPreference("pref_language");
   //     mLinkifyOnTor = (CheckBoxPreference) findPreference("pref_linkify_on_tor");
        mHideOfflineContacts = (CheckBoxPreference) findPreference("pref_hide_offline_contacts");
        mDeleteUnsecuredMedia = (CheckBoxPreference) findPreference("pref_delete_unsecured_media");
        mEnableNotification = (CheckBoxPreference) findPreference("pref_enable_notification");
        mNotificationVibrate = (CheckBoxPreference) findPreference("pref_notification_vibrate");
        mNotificationSound = (CheckBoxPreference) findPreference("pref_notification_sound");

        mNotificationRingtone = findPreference("pref_notification_ringtone");


        Languages languages = Languages.get(this);
        currentLanguage = getResources().getConfiguration().locale.getLanguage();
        mLanguage.setDefaultValue(currentLanguage);
        mLanguage.setEntries(languages.getAllNames());
        mLanguage.setEntryValues(languages.getSupportedLocales());
        mLanguage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String language = (String) newValue;
                ImApp.resetLanguage(SettingActivity.this, language);
                setResult(RESULT_OK);
                return true;
            }
        });

        mAllowScreenshot.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                setResult(RESULT_OK);
                return true;
            }
        });

        mPanicTriggerApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String packageName = (String) newValue;
                PanicResponder.setTriggerPackageName(SettingActivity.this, packageName);
                PanicResponder.configTriggerAppListPreference(mPanicTriggerApp,
                        R.string.panic_trigger_app_summary, R.string.panic_app_none_summary);
                return true;
            }
        });

        mPanicConfig.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingActivity.this, PanicSetupActivity.class);
                startActivity(intent);
                return true;
            }
        });

        findPreference("pref_color_reset").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(SettingActivity.this);

                SharedPreferences.Editor pEdit = settings.edit();
                pEdit.remove("themeColorBg");
                pEdit.remove("themeColorText");
                pEdit.remove("themeColor");
                pEdit.commit();
                setResult(RESULT_OK);
                finish();
                return true;
            }
        });

        mNotificationRingtone.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {

                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_ringtone_title));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Preferences.getNotificationRingtoneUri());
                startActivityForResult(intent, CHOOSE_RINGTONE);
                return true;
            }

        });

        mForegroundService = (CheckBoxPreference) findPreference("pref_foreground_enable");
        mForegroundService.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                ((ImApp)getApplication()).forceStopImService();

                return true;
            }
        });


        mHeartbeatInterval = (EditTextPreference) findPreference("pref_heartbeat_interval");

        findPreference("prefAdvancedNetworking").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                RemoteImService.installTransports(getApplicationContext());
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == CHOOSE_RINGTONE) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            Preferences.setNotificationRingtone(uri);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setInitialValues();
    }
}
