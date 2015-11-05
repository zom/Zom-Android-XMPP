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
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.hockeyapp.android.UpdateManager;

import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.AccountFragment;
import org.awesomeapp.messenger.ui.AddContactActivity;
import org.awesomeapp.messenger.ui.ContactsListFragment;
import org.awesomeapp.messenger.ui.ContactsPickerActivity;
import org.awesomeapp.messenger.ui.ConversationDetailActivity;
import org.awesomeapp.messenger.ui.ConversationListFragment;
import org.awesomeapp.messenger.ui.LockScreenActivity;
import org.awesomeapp.messenger.ui.MoreFragment;
import org.awesomeapp.messenger.ui.legacy.SettingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.util.LogCleaner;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;
import org.awesomeapp.messenger.util.XmppUriHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.otr.app.im.R;


/**
 * TODO
 */
public class MainActivity extends AppCompatActivity {

 //   private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private FloatingActionButton mFab;
    private Toolbar mToolbar;

    private ImApp mApp;

    public final static int REQUEST_ADD_CONTACT = 9999;
    public final static int REQUEST_CHOOSE_CONTACT = REQUEST_ADD_CONTACT+1;

    private ConversationListFragment mConversationList;
    private ContactsListFragment mContactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.awesome_activity_main);

        mApp = (ImApp)getApplication();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

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

        mConversationList = new ConversationListFragment();
        mContactList = new ContactsListFragment();

        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(mConversationList, getString(R.string.title_chats), R.drawable.ic_message_white_36dp);
        adapter.addFragment(mContactList, getString(R.string.contacts), R.drawable.ic_people_white_36dp);
        adapter.addFragment(new MoreFragment(), getString(R.string.title_more), R.drawable.ic_more_horiz_white_36dp);
        adapter.addFragment(new AccountFragment(), getString(R.string.title_me), R.drawable.ic_face_white_24dp);

        mViewPager.setAdapter(adapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        TabLayout.Tab tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_discuss);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_people_white_36dp);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_explore_white_24dp);
        tabLayout.addTab(tab);

        tab = tabLayout.newTab();
        tab.setIcon(R.drawable.ic_face_white_24dp);
        tabLayout.addTab(tab);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                mViewPager.setCurrentItem(tab.getPosition());

                StringBuffer sb = new StringBuffer();
                sb.append(getString(R.string.app_name));
                sb.append(" | ");

                switch (tab.getPosition()) {
                    case 0:
                        sb.append(getString(R.string.chats));
                        break;
                    case 1:
                        sb.append(getString(R.string.friends));
                        break;
                    case 2:
                        sb.append(getString(R.string.title_more));
                        break;
                    case 3:
                        sb.append(getString(R.string.me_title));
                        break;
                }

                mToolbar.setTitle(sb.toString());
                mFab.setVisibility(View.VISIBLE);

                if (tab.getPosition() == 1) {
                    mFab.setImageResource(R.drawable.ic_person_add_white_36dp);
                }
                else if (tab.getPosition() == 2) {
//                    mFab.setImageResource(R.drawable.ic_photo_camera_white_36dp);
                    mFab.setVisibility(View.GONE);

                }
                else if (tab.getPosition() == 3)
                {
                    mFab.setVisibility(View.GONE);
                }
                else {
                    mFab.setImageResource(R.drawable.ic_add_white_24dp);
                }

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

                if (tabIdx == 0) {

                    if (mContactList.getContactCount() > 0) {
                        Intent intent = new Intent(MainActivity.this, ContactsPickerActivity.class);
                        startActivityForResult(intent, REQUEST_CHOOSE_CONTACT);
                    }
                    else
                    {
                        inviteContact();
                    }

                } else if (tabIdx == 1) {
                    inviteContact();
                } else if (tabIdx == 2) {
                    startPhotoTaker();
                }
                else if (tabIdx == 3) {
                    Intent i = new Intent(MainActivity.this, OnboardingActivity.class);
                    startActivity(i);
                }


            }
        });

        //don't wnat this to happen to often
        checkForUpdates();

    }

    public void inviteContact ()
    {
        Intent i = new Intent(MainActivity.this, AddContactActivity.class);
        startActivityForResult(i,MainActivity.REQUEST_ADD_CONTACT);
    }


    @Override
    protected void onResume() {
        super.onResume();

        //if VFS is not mounted, then send to WelcomeActivity
        if (!VirtualFileSystem.get().isMounted()) {
            finish();
            startActivity(new Intent(this, RouterActivity.class));

        } else {
            ImApp app = (ImApp) getApplication();
          //  app.getTrustManager().bindDisplayActivity(this);

            app.checkForCrashes(this);

            mApp.initAccountInfo();

            mApp.maybeInit(this);


        }

        handleIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        handleIntent();
    }

    private void handleIntent ()
    {

        Intent intent = getIntent();

        if (intent != null)
        {
            Uri data = intent.getData();

            if (data != null) {
                String type = getContentResolver().getType(data);
                if (Imps.Chats.CONTENT_ITEM_TYPE.equals(type)) {

                    long chatId = ContentUris.parseId(data);

                    Intent intentChat = new Intent(this, ConversationDetailActivity.class);
                    intentChat.putExtra("id", chatId);
                    startActivity(intentChat);
                }


            }

            setIntent(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_CONTACT)
            {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                long providerId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, -1);
                long accountId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT,-1);

                startChat(providerId, accountId, username);
            }
            else if (requestCode == REQUEST_CHOOSE_CONTACT)
            {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (username != null) {
                    long providerId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, -1);
                    long accountId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT, -1);

                    startChat(providerId, accountId, username);
                }
                else {

                    ArrayList<String> users = data.getStringArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAMES);
                    if (users != null)
                    {
                        //int[] providers = data.getIntArrayExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER);
                        //int[] accounts = data.getIntArrayExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT);

                        //start group and do invite here

                        startGroupChat(users);
                    }

                }
            }
            else if (requestCode == ConversationDetailActivity.REQUEST_TAKE_PICTURE)
            {
                if (mLastPhoto != null)
                    importPhoto ();

            }
            else if (requestCode == OnboardingManager.REQUEST_SCAN) {

                ArrayList<String> resultScans = data.getStringArrayListExtra("result");
                for (String resultScan : resultScans)
                {

                    try {
                        if (resultScan.startsWith("xmpp:"))
                        {
                            String address = XmppUriHelper.parse(Uri.parse(resultScan)).get(XmppUriHelper.KEY_ADDRESS);
                            String fingerprint =  XmppUriHelper.getOtrFingerprint(resultScan);

                            new AddContactAsyncTask(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), mApp).execute(address, fingerprint);

                        }
                        else {
                            //parse each string and if they are for a new user then add the user
                            String[] parts = OnboardingManager.decodeInviteLink(resultScan);

                            new AddContactAsyncTask(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), mApp).execute(parts[0], parts[1]);
                        }

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

    private void startGroupChat (ArrayList<String> invitees)
    {


        String chatRoom = "groupchat" + UUID.randomUUID().toString().substring(0,8);
        String chatServer = ""; //use the default
        String nickname = mApp.getDefaultUsername().split("@")[0];
        try
        {
            IImConnection conn = mApp.getConnection(mApp.getDefaultProviderId(),mApp.getDefaultAccountId());
            if (conn.getState() == ImConnection.LOGGED_IN)
            {
                this.startGroupChat(chatRoom, chatServer, nickname, invitees, conn);

            }
        } catch (RemoteException re) {

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mLastPhoto != null)
            savedInstanceState.putString("lastphoto", mLastPhoto.toString());

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

       String lastPhotoPath =  savedInstanceState.getString("lastphoto");
        if (lastPhotoPath != null)
            mLastPhoto = Uri.parse(lastPhotoPath);
    }

    private void importPhoto ()
    {

        // import
        SystemServices.FileInfo info = SystemServices.getFileInfoFromURI(this, mLastPhoto);
        String sessionId = "self";
        String offerId = UUID.randomUUID().toString();

        try {
            Uri vfsUri = SecureMediaStore.resizeAndImportImage(this, sessionId, mLastPhoto, info.type);

            delete(mLastPhoto);

            //adds in an empty message, so it can exist in the gallery and be forwarded
            Imps.insertMessageInDb(
                    getContentResolver(), false, new Date().getTime(), true, null, vfsUri.toString(),
                    System.currentTimeMillis(), Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED,
                    0, offerId, info.type);

            mLastPhoto = null;
        }
        catch (IOException ioe)
        {
            Log.e(ImApp.LOG_TAG,"error importing photo",ioe);
        }

    }

    private boolean delete(Uri uri) {
        if (uri.getScheme().equals("content")) {
            int deleted = getContentResolver().delete(uri,null,null);
            return deleted == 1;
        }
        if (uri.getScheme().equals("file")) {
            java.io.File file = new java.io.File(uri.toString().substring(5));
            return file.delete();
        }
        return false;
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

            case R.id.menu_settings:
                Intent sintent = new Intent(this, SettingActivity.class);
                startActivity(sintent);
                return true;

            case R.id.menu_group_chat:
                showGroupChatDialog();
                return true;

            case R.id.menu_lock:
                handleLock();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleLock ()
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.contains(ImApp.PREFERENCE_KEY_TEMP_PASS))
        {
            //need to setup new user passphrase
            Intent intent = new Intent(this, LockScreenActivity.class);
            intent.setAction(LockScreenActivity.ACTION_CHANGE_PASSPHRASE);
            startActivity(intent);
        }
        else {

            //time to do the lock
            Intent intent = new Intent(this, RouterActivity.class);
            intent.setAction(RouterActivity.ACTION_LOCK_APP);
            startActivity(intent);
            finish();
        }
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

    public void startChat (long providerId, long accountId, String username)
    {
        long chatId = startChat(providerId, accountId, username,Imps.ContactsColumns.TYPE_NORMAL,true, null);

        if (chatId != -1) {
            Intent intent = new Intent(this, ConversationDetailActivity.class);
            intent.putExtra("id", chatId);
            startActivity(intent);
        }
    }

    private long startChat (long providerId, long accountId, String address,int userType, boolean isNewChat, String message)
    {
        IImConnection conn = ((ImApp)getApplication()).getConnection(providerId,accountId);
        long mRequestedChatId = -1;

        if (conn != null)
        {
            try {
                IChatSessionManager manager = conn.getChatSessionManager();
                IChatSession session = manager.getChatSession(address);

                //even if there is an existing session, it might be ended, so let's start a new one!

                if (manager != null) {

                    // Create session.  Stash requested contact ID for when we get called back.
                    if (userType == Imps.ContactsColumns.TYPE_GROUP)
                        session = manager.createMultiUserChatSession(address, null, null, isNewChat);
                    else
                        session = manager.createChatSession(address, isNewChat);

                    if (session != null)
                    {
                        mRequestedChatId = session.getId();
                        if (message != null)
                            session.sendMessage(message,false);
                    }

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
        //TextView tvServer = (TextView) dialogGroup.findViewById(R.id.chat_server);
        // tvServer.setText(ImApp.DEFAULT_GROUPCHAT_SERVER);// need to make this a list

       // final Spinner listAccounts = (Spinner) dialogGroup.findViewById(R.id.choose_list);
       // setupAccountSpinner(listAccounts);

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_group)
                .setView(dialogGroup)
                .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */

                        String chatRoom = null;
                        String chatServer = "";
                        String nickname = "";

                        TextView tv = (TextView) dialogGroup.findViewById(R.id.chat_room);
                        chatRoom = tv.getText().toString();

                        /**
                         tv = (TextView) dialogGroup.findViewById(R.id.chat_server);
                         chatServer = tv.getText().toString();

                         tv = (TextView) dialogGroup.findViewById(R.id.nickname);
                         nickname = tv.getText().toString();
                         **/

                        try {
                            IImConnection conn = mApp.getConnection(mApp.getDefaultProviderId(), mApp.getDefaultAccountId());
                            if (conn.getState() == ImConnection.LOGGED_IN)
                                startGroupChat(chatRoom, chatServer, nickname, null, conn);

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

    public void startGroupChat (String room, String server, String nickname, final ArrayList<String> invitees, IImConnection conn)
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

                String subject = params[0];
                String chatRoom = "group" + UUID.randomUUID().toString().substring(0,8);
                String server = params[1];


                try {

                    IChatSessionManager manager = mLastConnGroup.getChatSessionManager();

                    String roomAddress = (chatRoom + '@' + server).toLowerCase(Locale.US);
                    String nickname = params[2];

                    IChatSession session = manager.getChatSession(roomAddress);

                    if (session == null) {
                        session = manager.createMultiUserChatSession(roomAddress, subject, nickname, true);

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

                    if (invitees != null && invitees.size() > 0) {

                        //wait a second for the server to sort itself out
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                        }

                        for (String invitee : invitees)
                            session.inviteContact(invitee);
                    }

                    return null;

                } catch (RemoteException e) {
                    return e.toString();
                }

            }

            @Override
            protected void onProgressUpdate(Long... showChatId) {
                showChat(showChatId[0]);
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

    private void showChat (long chatId)
    {
        Intent intent = new Intent(this, ConversationDetailActivity.class);
        intent.putExtra("id",chatId);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UpdateManager.unregister();
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, ImApp.HOCKEY_APP_ID);

    }

    Uri mLastPhoto = null;

    void startPhotoTaker() {

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),  "cs_" + new Date().getTime() + ".jpg");
        mLastPhoto = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                mLastPhoto);

        // start the image capture Intent
        startActivityForResult(intent, ConversationDetailActivity.REQUEST_TAKE_PICTURE);
    }

    /**
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.awesome_activity_main);

    }*/
}
