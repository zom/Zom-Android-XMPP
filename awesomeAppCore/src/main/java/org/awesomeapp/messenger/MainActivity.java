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

package org.awesomeapp.messenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;


import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.AccountFragment;
import org.awesomeapp.messenger.ui.ContactListActivity;
import org.awesomeapp.messenger.ui.ContactsListFragment;
import org.awesomeapp.messenger.ui.ConversationDetailActivity;
import org.awesomeapp.messenger.ui.ConversationListFragment;
import org.awesomeapp.messenger.ui.GalleryFragment;
import org.awesomeapp.messenger.ui.GalleryListFragment;
import org.awesomeapp.messenger.ui.MoreFragment;
import org.awesomeapp.messenger.ui.legacy.SettingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.guardianproject.iocipher.VirtualFileSystem;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IImConnection;
import info.guardianproject.otr.app.im.R;
import org.awesomeapp.messenger.ui.legacy.AddContactActivity;
import org.awesomeapp.messenger.ui.legacy.ContactsPickerActivity;
import org.awesomeapp.messenger.util.LogCleaner;


/**
 * TODO
 */
public class MainActivity extends AppCompatActivity {

 //   private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private FloatingActionButton mFab;
    private ImApp mApp;

    public final static int REQUEST_ADD_CONTACT = 9999;
    public final static int REQUEST_CHOOSE_CONTACT = REQUEST_ADD_CONTACT+1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.awesome_activity_main);

        mApp = (ImApp)getApplication();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
     //   ab.setHomeAsUpIndicator(R.drawable.ic_menu);
//        ab.setDisplayHomeAsUpEnabled(true);

        /*
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }*/

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
       final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(new ConversationListFragment(), getString(R.string.title_chats), R.drawable.ic_message_white_36dp);
        //adapter.addFragment(new ContactsListFragment(), getString(R.string.contacts), R.drawable.ic_face_white_36dp);
        adapter.addFragment(new GalleryListFragment(), getString(R.string.title_gallery), R.drawable.ic_photo_library_white_36dp);
        adapter.addFragment(new MoreFragment(), getString(R.string.title_more), R.drawable.ic_more_horiz_white_36dp);
        adapter.addFragment(new AccountFragment(), getString(R.string.title_me), R.drawable.ic_face_white_24dp);

        mViewPager.setAdapter(adapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        TabLayout.Tab tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_discuss);
     //   tab.setText(R.string.title_chats);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_photo_library_white_24dp);
      //  tab.setText(R.string.title_gallery);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_toys_white_24dp);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_face_white_24dp);
        tabLayout.addTab(tab);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                mViewPager.setCurrentItem(tab.getPosition());


            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int tabIdx = mViewPager.getCurrentItem();

                if (tabIdx == 0)
                {
                    Intent intent = new Intent(MainActivity.this, ContactListActivity.class);
                    startActivityForResult(intent, REQUEST_CHOOSE_CONTACT);
                }
                else if (tabIdx == 1)
                {
                    //add contact
                }
                else if (tabIdx == 2)
                {

                }


            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();


        //if VFS is not mounted, then send to WelcomeActivity
        if (!VirtualFileSystem.get().isMounted())
        {
            finish();
            startActivity(new Intent(this,RouterActivity.class));

        }
        else
        {
            ImApp app = (ImApp)getApplication();
            app.getTrustManager().bindDisplayActivity(this);
            app.checkForCrashes(this);
            mApp.initAccountInfo();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_CONTACT)
            {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                long providerId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER,-1);
                startChat(providerId, username);
            }
            else if (requestCode == REQUEST_CHOOSE_CONTACT)
            {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                long providerId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER,-1);
                startChat(providerId, username);
            }
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.menu_add_contact:

                Intent i = new Intent(this, AddContactActivity.class);
                startActivityForResult(i,REQUEST_ADD_CONTACT);
                return true;

            case R.id.menu_settings:
                Intent sintent = new Intent(this, SettingActivity.class);
                startActivityForResult(sintent,REQUEST_ADD_CONTACT+3);
                return true;

            case R.id.menu_group_chat:
                showGroupChatDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {

                        if (menuItem.getItemId() == R.id.menu_add_account) {

                            startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
                            return true;

                        } else {

                            menuItem.setChecked(true);
                  //          mDrawerLayout.closeDrawers();
                            return true;
                        }


                    }
                });

    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();
        private final List<Integer> mFragmentIcons = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title, int icon) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
            mFragmentIcons.add(icon);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
        return mFragmentTitles.get(position);
        }



    }

    public void startChat (long providerId, String username)
    {
        long chatId = startChat(providerId, username,Imps.ContactsColumns.TYPE_NORMAL,true, null);

        if (chatId != -1) {
            Intent intent = new Intent(this, ConversationDetailActivity.class);
            intent.putExtra("id", chatId);
            startActivity(intent);
        }
    }

    private long startChat (long providerId, String address,int userType, boolean isNewChat, String message)
    {
        IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);
        long mRequestedChatId = -1;

        if (conn != null)
        {
            try {
                IChatSessionManager manager = conn.getChatSessionManager();
                IChatSession session = manager.getChatSession(address);

                if (session == null && manager != null) {

                    // Create session.  Stash requested contact ID for when we get called back.
                    if (userType == Imps.ContactsColumns.TYPE_GROUP)
                        session = manager.createMultiUserChatSession(address, null, isNewChat);
                    else
                        session = manager.createChatSession(address, isNewChat);

                    if (session != null)
                    {
                        mRequestedChatId = session.getId();
                        if (message != null)
                            session.sendMessage(message);
                    }

                } else {
                    mRequestedChatId = session.getId();

                }

            } catch (RemoteException e) {
                //  mHandler.showServiceErrorAlert(e.getMessage());
                LogCleaner.debug(ImApp.LOG_TAG, "remote exception starting chat");

            }

        }
        else
        {
            LogCleaner.debug(ImApp.LOG_TAG, "could not start chat as connection was null");
        }

        return mRequestedChatId;
    }

    private void showGroupChatDialog ()
    {

        // This example shows how to add a custom layout to an AlertDialog
        LayoutInflater factory = LayoutInflater.from(this);

        final View dialogGroup = factory.inflate(R.layout.alert_dialog_group_chat, null);
        TextView tvServer = (TextView) dialogGroup.findViewById(R.id.chat_server);
        // tvServer.setText(ImApp.DEFAULT_GROUPCHAT_SERVER);// need to make this a list

       // final Spinner listAccounts = (Spinner) dialogGroup.findViewById(R.id.choose_list);
       // setupAccountSpinner(listAccounts);

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_or_join_group_chat)
                .setView(dialogGroup)
                .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */

                        String chatRoom = null;
                        String chatServer = null;
                        String nickname = null;

                        TextView tv = (TextView)dialogGroup.findViewById(R.id.chat_room);
                        chatRoom = tv.getText().toString();

                        tv = (TextView) dialogGroup.findViewById(R.id.chat_server);
                        chatServer = tv.getText().toString();

                        tv = (TextView) dialogGroup.findViewById(R.id.nickname);
                        nickname = tv.getText().toString();

                        try
                        {
                            IImConnection conn = mApp.getConnection(mApp.getDefaultProviderId());
                            if (conn.getState() == ImConnection.LOGGED_IN)
                                startGroupChat (chatRoom, chatServer, nickname, conn);

                        } catch (RemoteException re) {

                        }

                        dialog.dismiss();

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                        dialog.dismiss();
                    }
                })
                .create().show();



    }

    private IImConnection mLastConnGroup = null;
    private long mRequestedChatId = -1;

    public void startGroupChat (String room, String server, String nickname, IImConnection conn)
    {
        mLastConnGroup = conn;

        new AsyncTask<String, Long, String>() {

            private ProgressDialog dialog;


            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(MainActivity.this);

                dialog.setMessage(getString(R.string.connecting_to_group_chat_));
                dialog.setCancelable(true);
                dialog.show();
            }

            @Override
            protected String doInBackground(String... params) {

                String roomAddress = (params[0] + '@' + params[1]).toLowerCase(Locale.US).replace(' ', '_');
                String nickname = params[2];

                try {
                    IChatSessionManager manager = mLastConnGroup.getChatSessionManager();
                    IChatSession session = manager.getChatSession(roomAddress);
                    if (session == null) {
                        session = manager.createMultiUserChatSession(roomAddress, nickname, true);
                        if (session != null)
                        {
                            mRequestedChatId = session.getId();
                            publishProgress(mRequestedChatId);

                        } else {
                            return getString(R.string.unable_to_create_or_join_group_chat);

                        }
                    } else {
                        mRequestedChatId = session.getId();
                        publishProgress(mRequestedChatId);
                    }

                    return null;

                } catch (RemoteException e) {
                    return e.toString();
                }

            }

            @Override
            protected void onProgressUpdate(Long... showChatId) {
                //showChat(showChatId[0]);
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                if (result != null)
                {
                 //   mHandler.showServiceErrorAlert(result);

                }


            }
        }.execute(room, server, nickname);



    }



}
