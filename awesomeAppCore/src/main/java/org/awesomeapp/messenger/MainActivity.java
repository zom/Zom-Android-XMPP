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

import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.ContactsListFragment;
import org.awesomeapp.messenger.ui.ConversationDetailActivity;
import org.awesomeapp.messenger.ui.ConversationListFragment;
import org.awesomeapp.messenger.ui.GalleryFragment;
import org.awesomeapp.messenger.ui.MoreFragment;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;

import java.util.ArrayList;
import java.util.List;

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

    private final static int REQUEST_ADD_CONTACT = 9999;

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
        adapter.addFragment(new ConversationListFragment(), getString(R.string.title_chats),R.drawable.ic_message_white_36dp);
        adapter.addFragment(new ContactsListFragment(), getString(R.string.contacts), R.drawable.ic_face_white_36dp);
        adapter.addFragment(new GalleryFragment(), "Photos", R.drawable.ic_photo_library_white_36dp);
        adapter.addFragment(new MoreFragment(), "More", R.drawable.ic_more_horiz_white_36dp);

        mViewPager.setAdapter(adapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        TabLayout.Tab tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_discuss);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_face_white_36dp);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_photo_library_white_36dp);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_more_horiz_white_36dp);
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
                mViewPager.setCurrentItem(1); //show contacts
                Snackbar.make(mViewPager, "Choose a friend to start zoming!", Snackbar.LENGTH_LONG).show();
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
            if (requestCode == OnboardingManager.REQUEST_SCAN) {

                ArrayList<String> resultScans = data.getStringArrayListExtra("result");
                for (String resultScan : resultScans)
                {

                    try {
                        //parse each string and if they are for a new user then add the user
                        String[] parts = OnboardingManager.decodeInviteLink(resultScan);

                        new AddContactAsyncTask(mApp.getDefaultProviderId(),mApp.getDefaultAccountId(), mApp).execute(parts[0],parts[1]);

                        //if they are for a group chat, then add the group
                    }
                    catch (Exception e)
                    {
                        Log.w(ImApp.LOG_TAG, "error parsing QR invite link", e);
                    }
                }
            }
            else if (requestCode == REQUEST_ADD_CONTACT)
            {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                long providerId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER,-1);
                startChat(providerId, username,Imps.ContactsColumns.TYPE_NORMAL,true, null);
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



}
