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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.Preferences;

import java.util.ArrayList;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.panic.Panic;
import info.guardianproject.panic.PanicReceiver;
import info.guardianproject.util.Languages;

public class SettingActivity extends PreferenceActivity {
    private static final String TAG = "SettingActivity";

    private static final int CHOOSE_RINGTONE = 5;

    private PackageManager pm;
    private String currentLanguage;
    ListPreference mOtrMode;
    ListPreference mPanicTriggerApp;
    ListPreference mLanguage;
    CheckBoxPreference mLinkifyOnTor;
    CheckBoxPreference mHideOfflineContacts;
    CheckBoxPreference mDeleteUnsecuredMedia;
    CheckBoxPreference mEnableNotification;
    CheckBoxPreference mNotificationVibrate;
    CheckBoxPreference mNotificationSound;
    CheckBoxPreference mForegroundService;
    EditTextPreference mHeartbeatInterval;

    Preference mNotificationRingtone;

    private void setInitialValues() {
        mOtrMode.setValue(Preferences.getOtrMode());

        mLinkifyOnTor.setChecked(Preferences.getLinkifyOnTor());
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

        for (ResolveInfo resolveInfo : PanicReceiver.resolveTriggerApps(pm)) {
            if (resolveInfo.activityInfo == null)
                continue;
            entries.add(resolveInfo.activityInfo.loadLabel(pm));
            entryValues.add(resolveInfo.activityInfo.packageName);
        }
        mPanicTriggerApp.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mPanicTriggerApp.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        setPanicTriggerAppDisplay(PanicReceiver.getTriggerPackageName(this));
    }

    private void setPanicTriggerAppDisplay(String triggerPackageName) {
        if (TextUtils.isEmpty(triggerPackageName)
                || triggerPackageName.equals(Panic.PACKAGE_NAME_DEFAULT)) {
            mPanicTriggerApp.setValue(Panic.PACKAGE_NAME_DEFAULT);
            mPanicTriggerApp.setDefaultValue(Panic.PACKAGE_NAME_DEFAULT);
            mPanicTriggerApp.setSummary(R.string.panic_trigger_app_summary);
            mPanicTriggerApp.setIcon(null);
        } else {
            mPanicTriggerApp.setValue(triggerPackageName);
            mPanicTriggerApp.setDefaultValue(triggerPackageName);
            if (triggerPackageName.equals(Panic.PACKAGE_NAME_NONE)) {
                mPanicTriggerApp.setSummary(R.string.panic_app_none_summary);
                mPanicTriggerApp.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                try {
                    mPanicTriggerApp.setSummary(pm.getApplicationLabel(
                            pm.getApplicationInfo(triggerPackageName, 0)));
                    mPanicTriggerApp.setIcon(pm.getApplicationIcon(triggerPackageName));
                } catch (PackageManager.NameNotFoundException e) {
                    mPanicTriggerApp.setSummary(R.string.panic_trigger_app_summary);
                    mPanicTriggerApp.setIcon(null);
                }
            }
        }
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

        mPanicTriggerApp = (ListPreference) findPreference("pref_panic_trigger_app");
        mLanguage = (ListPreference) findPreference("pref_language");
        mLinkifyOnTor = (CheckBoxPreference) findPreference("pref_linkify_on_tor");
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
                return true;
            }
        });

        mPanicTriggerApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String packageName = (String) newValue;
                PanicReceiver.setTriggerPackageName(SettingActivity.this, packageName);
                setPanicTriggerAppDisplay(packageName);
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
        mHeartbeatInterval = (EditTextPreference) findPreference("pref_heartbeat_interval");
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
