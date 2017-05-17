/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.awesomeapp.messenger.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;

import java.util.ArrayList;
import java.util.List;

import im.zom.messenger.R;

public class ContactListActivity extends AppCompatActivity {

    private ImApp mApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.awesome_activity_contact_list);

        Intent intent = getIntent();
        mApp = (ImApp)getApplication();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(R.string.contacts);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_conversation_detail, menu);
        return true;
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == OnboardingManager.REQUEST_SCAN) {

                ArrayList<String> resultScans = data.getStringArrayListExtra("result");
                for (String resultScan : resultScans)
                {

                    try {
                        //parse each string and if they are for a new user then add the user
                        OnboardingManager.DecodedInviteLink diLink = OnboardingManager.decodeInviteLink(resultScan);

                        new AddContactAsyncTask(mApp.getDefaultProviderId(),mApp.getDefaultAccountId(), mApp).execute(diLink.username, diLink.fingerprint, diLink.nickname);

                        //if they are for a group chat, then add the group
                    }
                    catch (Exception e)
                    {
                        Log.w(ImApp.LOG_TAG, "error parsing QR invite link", e);
                    }
                }
            }

        }
    }

    public void startChat (long providerId, long accountId, String username)
    {

        Intent data = new Intent();
        data.putExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER,providerId);
        data.putExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT,accountId);
        data.putExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME,username);
        setResult(RESULT_OK,data);
        finish();
    }


}
