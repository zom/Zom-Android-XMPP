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

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.bots.ZomServicesRecyclerViewAdapter;

import im.zom.messenger.R;

public class ServicesActivity extends BaseActivity implements ZomServicesRecyclerViewAdapter.ServiceItemCallback {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.awesome_activity_services);
        setTitle(R.string.action_services);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        applyStyleForToolbar();

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recyclerServices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(new ZomServicesRecyclerViewAdapter(this, this));
    }


    public void applyStyleForToolbar() {



        //not set color
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int selColor = settings.getInt("themeColor",-1);

        if (selColor != -1) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(selColor);
                getWindow().setStatusBarColor(selColor);
            }

            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(selColor));
        }

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
    public void onBotClicked(final String jid, final String nickname) {
        ImApp app = (ImApp)getApplication();

        final ProgressDialog dialog;
        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.upgrade_progress_action));
        dialog.setCancelable(true);
        dialog.show();

        new AddContactAsyncTask(app.getDefaultProviderId(), app.getDefaultAccountId(), app){
            @Override
            protected void onPostExecute(Integer response) {
                super.onPostExecute(response);

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                Intent intent = new Intent(ServicesActivity.this, MainActivity.class);
                intent.putExtra("username", jid);
                startActivity(intent);
                finish();

            }

        }.execute(jid, null, nickname);



    }

}
