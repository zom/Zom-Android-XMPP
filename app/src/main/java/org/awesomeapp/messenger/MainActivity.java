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
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IConnectionListener;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.service.ImServiceConstants;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.tasks.ChatSessionInitTask;
import org.awesomeapp.messenger.ui.AccountFragment;
import org.awesomeapp.messenger.ui.AccountsActivity;
import org.awesomeapp.messenger.ui.AddContactActivity;
import org.awesomeapp.messenger.ui.BaseActivity;
import org.awesomeapp.messenger.ui.ContactsListFragment;
import org.awesomeapp.messenger.ui.ContactsPickerActivity;
import org.awesomeapp.messenger.ui.ConversationDetailActivity;
import org.awesomeapp.messenger.ui.ConversationListFragment;
import org.awesomeapp.messenger.ui.LockScreenActivity;
import org.awesomeapp.messenger.ui.MoreFragment;
import org.awesomeapp.messenger.ui.camera.CameraActivity;
import org.awesomeapp.messenger.ui.legacy.SettingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.ui.qr.CameraView;
import org.awesomeapp.messenger.util.AssetUtil;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;
import org.awesomeapp.messenger.util.XmppUriHelper;
import org.ironrabbit.type.CustomTypefaceManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import im.zom.messenger.R;
import info.guardianproject.iocipher.VirtualFileSystem;

/**
 * TODO
 */
public class MainActivity extends BaseActivity implements IConnectionListener {

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private FloatingActionButton mFab;
    private Toolbar mToolbar;

    private ImApp mApp;

    public final static int REQUEST_ADD_CONTACT = 9999;
    public final static int REQUEST_CHOOSE_CONTACT = REQUEST_ADD_CONTACT+1;
    public final static int REQUEST_CHANGE_SETTINGS = REQUEST_CHOOSE_CONTACT+1;

    private ConversationListFragment mConversationList;
    private ContactsListFragment mContactList;
    private MoreFragment mMoreFragment;
    private AccountFragment mAccountFragment;

    private IImConnection mConn;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (Preferences.doBlockScreenshots()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(R.layout.awesome_activity_main);

        mApp = (ImApp)getApplication();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);

        setSupportActionBar(mToolbar);

        final ActionBar ab = getSupportActionBar();

        mConversationList = new ConversationListFragment();
        mContactList = new ContactsListFragment();
        mMoreFragment = new MoreFragment();
        mAccountFragment = new AccountFragment();

        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(mConversationList, getString(R.string.title_chats), R.drawable.ic_message_white_36dp);
        adapter.addFragment(mContactList, getString(R.string.contacts), R.drawable.ic_people_white_36dp);
        adapter.addFragment(mMoreFragment, getString(R.string.title_more), R.drawable.ic_more_horiz_white_36dp);

        mAccountFragment = new AccountFragment();
      //  fragAccount.setArguments();

        adapter.addFragment(mAccountFragment, getString(R.string.title_me), R.drawable.ic_face_white_24dp);

        mViewPager.setAdapter(adapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        TabLayout.Tab tab = mTabLayout.newTab();
        tab.setIcon(R.drawable.ic_discuss);
        mTabLayout.addTab(tab);

        tab = mTabLayout.newTab();
        tab.setIcon(R.drawable.ic_people_white_36dp);
        mTabLayout.addTab(tab);

        tab = mTabLayout.newTab();
        tab.setIcon(R.drawable.ic_explore_white_24dp);
        mTabLayout.addTab(tab);

        tab = mTabLayout.newTab();
        tab.setIcon(R.drawable.ic_face_white_24dp);
        mTabLayout.addTab(tab);

        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                mViewPager.setCurrentItem(tab.getPosition());
                setToolbarTitle(tab.getPosition());
                applyStyleColors ();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                setToolbarTitle(tab.getPosition());
                applyStyleColors ();
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



            }
        });

        setToolbarTitle(0);

        //don't wnat this to happen to often
        checkForUpdates();

        installRingtones ();

        applyStyle();

        if (Preferences.doCheckBatteryOptimizations())
        {
            requestChangeBatteryOptimizations();
            Preferences.checkedBatteryOptimizations();
        }
    }

    private void installRingtones ()
    {
        AssetUtil.installRingtone(getApplicationContext(),R.raw.bell,"Zom Bell");
        AssetUtil.installRingtone(getApplicationContext(),R.raw.chant,"Zom Chant");
        AssetUtil.installRingtone(getApplicationContext(),R.raw.yak,"Zom Yak");
        AssetUtil.installRingtone(getApplicationContext(),R.raw.dranyen,"Zom Dranyen");

    }

    private void setToolbarTitle (int tabPosition)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getString(R.string.app_name_zom));
        sb.append(" | ");

        switch (tabPosition) {
            case 0:

                if (mConversationList.getArchiveFilter())
                    sb.append(getString(R.string.action_archive));
                else
                    sb.append(getString(R.string.chats));

                break;
            case 1:

                if ((mContactList.getCurrentType() & Imps.Contacts.TYPE_FLAG_HIDDEN) != 0)
                    sb.append(getString(R.string.action_archive));
                else
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

        if (mFab != null) {
            mFab.setVisibility(View.VISIBLE);

            if (tabPosition == 1) {
                mFab.setImageResource(R.drawable.ic_person_add_white_36dp);
            } else if (tabPosition == 2) {
                //                    mFab.setImageResource(R.drawable.ic_photo_camera_white_36dp);
                mFab.setVisibility(View.GONE);

            } else if (tabPosition == 3) {
                mFab.setVisibility(View.GONE);
            } else {
                mFab.setImageResource(R.drawable.ic_add_white_24dp);
            }
        }

    }

    public void inviteContact ()
    {
        Intent i = new Intent(MainActivity.this, AddContactActivity.class);
        startActivityForResult(i, MainActivity.REQUEST_ADD_CONTACT);
    }


    @Override
    public void onResume() {
        super.onResume();

        applyStyleColors ();

        //if VFS is not mounted, then send to WelcomeActivity
        if (!VirtualFileSystem.get().isMounted()) {
            finish();
            startActivity(new Intent(this, RouterActivity.class));

        } else {
            ImApp app = (ImApp) getApplication();
            mApp.maybeInit(this);
            mApp.initAccountInfo();
        }


        handleIntent(getIntent());

        if (mApp.getDefaultAccountId() == -1)
        {
            startActivity(new Intent(this,RouterActivity.class));
        }
        else {
            if (mConn == null) {
                mConn = mApp.getConnection(mApp.getDefaultProviderId(), mApp.getDefaultAccountId());
                if (mConn != null) {
                    try {
                        mConn.registerConnectionListener(this);
                    } catch (Exception e) {
                        Log.e(ImApp.LOG_TAG, "unable to register connection listener", e);
                    }

                }
            }

            checkConnection();

        }

    }

    private Snackbar mSbStatus;

    private boolean checkConnection() {
        try {

            if (mSbStatus != null)
                mSbStatus.dismiss();

            if (!isNetworkAvailable())
            {
                mSbStatus = Snackbar.make(mViewPager, "No Internet", Snackbar.LENGTH_INDEFINITE);
                mSbStatus.show();
                return false;
            }

            if (mApp.getDefaultProviderId() != -1) {
                final IImConnection conn = mApp.getConnection(mApp.getDefaultProviderId(), mApp.getDefaultAccountId());
                final int connState = conn.getState();

                if (connState == ImConnection.DISCONNECTED
                        || connState == ImConnection.SUSPENDED
                        || connState == ImConnection.SUSPENDING) {

                    mSbStatus = Snackbar.make(mViewPager, R.string.error_suspended_connection, Snackbar.LENGTH_INDEFINITE);
                    mSbStatus.setAction(getString(R.string.connect), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mSbStatus.dismiss();
                            Intent i = new Intent(MainActivity.this, AccountsActivity.class);
                            startActivity(i);
                        }
                    });
                    mSbStatus.show();

                    return false;
                }
                else if (connState == ImConnection.LOGGED_IN)
                {
                    //do nothing
                }
                else if (connState == ImConnection.LOGGING_IN)
                {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (connState == ImConnection.LOGGING_IN) {
                                mSbStatus = Snackbar.make(mViewPager, R.string.signing_in_wait, Snackbar.LENGTH_INDEFINITE);
                                mSbStatus.show();
                            }
                        }
                    }, 5000); //Timer is in ms here.

                }
                else if (connState == ImConnection.LOGGING_OUT)
                {
                    mSbStatus = Snackbar.make(mViewPager, R.string.signing_out_wait, Snackbar.LENGTH_INDEFINITE);
                    mSbStatus.show();
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent (Intent intent)
    {


        if (intent != null)
        {
            Uri data = intent.getData();
            String type = intent.getType();
          if (data != null && Imps.Chats.CONTENT_ITEM_TYPE.equals(type)) {

                long chatId = ContentUris.parseId(data);
                Intent intentChat = new Intent(this, ConversationDetailActivity.class);
                intentChat.putExtra("id", chatId);
                startActivity(intentChat);
            }
            else if (Imps.Contacts.CONTENT_ITEM_TYPE.equals(type))
            {
                long providerId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID,mApp.getDefaultProviderId());
                long accountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,mApp.getDefaultAccountId());
                String username = intent.getStringExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS);
                startChat(providerId, accountId, username,  true);
            }
            else if (intent.hasExtra("username"))
            {
                //launch a new chat based on the intent value
                startChat(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), intent.getStringExtra("username"),  true);
            }

            setIntent(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_CHANGE_SETTINGS)
            {
                finish();
                startActivity(new Intent(this, MainActivity.class));
            }
            else if (requestCode == REQUEST_ADD_CONTACT)
            {

                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (username != null) {
                    long providerId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, -1);
                    long accountId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT,-1);

                    startChat(providerId, accountId, username,  false);
                }

            }
            else if (requestCode == REQUEST_CHOOSE_CONTACT)
            {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (username != null) {
                    long providerId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, -1);
                    long accountId = data.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT, -1);

                    startChat(providerId, accountId, username, true);
                }
                else {

                    ArrayList<String> users = data.getStringArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAMES);
                    if (users != null)
                    {
                        //start group and do invite here
                        startGroupChat(users);
                    }

                }
            }
            else if (requestCode == ConversationDetailActivity.REQUEST_TAKE_PICTURE)
            {
                try {
                    if (mLastPhoto != null)
                        importPhoto();
                }
                catch (Exception e)
                {
                    Log.w(ImApp.LOG_TAG, "error importing photo",e);

                }
            }
            else if (requestCode == OnboardingManager.REQUEST_SCAN) {

                ArrayList<String> resultScans = data.getStringArrayListExtra("result");
                for (String resultScan : resultScans)
                {

                    try {

                        String address = null;

                        if (resultScan.startsWith("xmpp:"))
                        {
                            address = XmppUriHelper.parse(Uri.parse(resultScan)).get(XmppUriHelper.KEY_ADDRESS);
                            String fingerprint =  XmppUriHelper.getOtrFingerprint(resultScan);
                            new AddContactAsyncTask(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), mApp).execute(address, fingerprint);

                        }
                        else {
                            //parse each string and if they are for a new user then add the user
                            OnboardingManager.DecodedInviteLink diLink = OnboardingManager.decodeInviteLink(resultScan);

                            new AddContactAsyncTask(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), mApp).execute(diLink.username,diLink.fingerprint,diLink.nickname);
                        }

                        if (address != null)
                            startChat(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), address, true);

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

    private void importPhoto () throws FileNotFoundException, UnsupportedEncodingException {

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


    private SearchView mSearchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));

        if (mSearchView != null )
        {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setIconifiedByDefault(false);

            SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener()
            {
                public boolean onQueryTextChange(String query)
                {
                    if (mTabLayout.getSelectedTabPosition() == 0)
                        mConversationList.doSearch(query);
                    else if (mTabLayout.getSelectedTabPosition() == 1)
                        mContactList.doSearch(query);

                    return true;
                }

                public boolean onQueryTextSubmit(String query)
                {
                    if (mTabLayout.getSelectedTabPosition() == 0)
                        mConversationList.doSearch(query);
                    else if (mTabLayout.getSelectedTabPosition() == 1)
                        mContactList.doSearch(query);

                    return true;
                }
            };

            mSearchView.setOnQueryTextListener(queryTextListener);

            mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    mConversationList.doSearch(null);
                    return false;
                }
            });
        }

        MenuItem mItem = menu.findItem(R.id.menu_lock_reset);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (!settings.contains(ImApp.PREFERENCE_KEY_TEMP_PASS))
            mItem.setVisible(true);

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
                startActivityForResult(sintent,  REQUEST_CHANGE_SETTINGS);
                return true;

            case R.id.menu_list_normal:
                clearFilters();
                return true;

            case R.id.menu_list_archive:
                enableArchiveFilter();
                return true;

            case R.id.menu_lock:
                handleLock();
                return true;

            case R.id.menu_new_account:
                Intent i = new Intent(MainActivity.this, AccountsActivity.class);
                startActivity(i);
                return true;

            case R.id.menu_lock_reset:
                resetPassphrase();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearFilters ()
    {

        if (mTabLayout.getSelectedTabPosition() == 0)
            mConversationList.setArchiveFilter(false);
        else
            mContactList.setArchiveFilter(false);

        setToolbarTitle(mTabLayout.getSelectedTabPosition());

    }

    private void enableArchiveFilter ()
    {

        if (mTabLayout.getSelectedTabPosition() == 0)
            mConversationList.setArchiveFilter(true);
        else
            mContactList.setArchiveFilter(true);


        setToolbarTitle(mTabLayout.getSelectedTabPosition());

    }

    public void resetPassphrase ()
    {
        /**
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.setAction(LockScreenActivity.ACTION_RESET_PASSPHRASE);
        startActivity(intent);**/

        //need to setup new user passphrase
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.setAction(LockScreenActivity.ACTION_CHANGE_PASSPHRASE);
        startActivity(intent);
    }


    public void handleLock ()
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

    @Override
    public void onStateChanged(IImConnection connection, int state, ImErrorInfo error) throws RemoteException {
        checkConnection();
    }

    @Override
    public void onUserPresenceUpdated(IImConnection connection) throws RemoteException {

    }

    @Override
    public void onUpdatePresenceError(IImConnection connection, ImErrorInfo error) throws RemoteException {

    }

    @Override
    public IBinder asBinder() {
        return mConn.asBinder();
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



    public void startChat (long providerId, long accountId, String username, final boolean openChat)
    {

        //startCrypto is not actually used anymore, as we move to OMEMO

        if (username != null)
            new ChatSessionInitTask(((ImApp)getApplication()),providerId, accountId, Imps.Contacts.TYPE_NORMAL, true)
            {
                @Override
                protected void onPostExecute(Long chatId) {

                    if (chatId != -1 && openChat) {
                        Intent intent = new Intent(MainActivity.this, ConversationDetailActivity.class);
                        intent.putExtra("id", chatId);
                        startActivity(intent);
                    }

                    super.onPostExecute(chatId);
                }

            }.executeOnExecutor(ImApp.sThreadPoolExecutor,new Contact(new XmppAddress(username)));
    }

    public void showGroupChatDialog ()
    {

        // This example shows how to add a custom layout to an AlertDialog
        LayoutInflater factory = LayoutInflater.from(this);

        final View dialogGroup = factory.inflate(R.layout.alert_dialog_group_chat, null);
        //TextView tvServer = (TextView) dialogGroup.findViewById(R.id.chat_server);
        // tvServer.setText(ImApp.DEFAULT_GROUPCHAT_SERVER);// need to make this a list

       // final Spinner listAccounts = (Spinner) dialogGroup.findViewById(R.id.choose_list);
       // setupAccountSpinner(listAccounts);

        AlertDialog dialog = new AlertDialog.Builder(this)
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
                .create();
        dialog.show();

        Typeface typeface;

        if ((typeface = CustomTypefaceManager.getCurrentTypeface(this))!=null) {
            TextView textView = (TextView) dialog.findViewById(android.R.id.message);
            if (textView != null)
                textView.setTypeface(typeface);

            textView = (TextView) dialog.findViewById(R.id.alertTitle);
            if (textView != null)
                textView.setTypeface(typeface);

            Button btn = (Button)dialog.findViewById(android.R.id.button1);
            if (btn != null)
                btn.setTypeface(typeface);

            btn = (Button)dialog.findViewById(android.R.id.button2);
            if (btn != null)
                btn.setTypeface(typeface);


            btn = (Button)dialog.findViewById(android.R.id.button3);
            if (btn != null)
                btn.setTypeface(typeface);


        }


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
                            session.markAsSeen(); // We created this, so mark as seen
                            session.sendTypingStatus(true);
                            session.setMuted(false);
                            session.setGroupChatSubject(subject);
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
        }.executeOnExecutor(ImApp.sThreadPoolExecutor,room, server, nickname);


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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mConn != null) {
            try {
                mConn.unregisterConnectionListener(this);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkForUpdates() {
        // Remove this for store builds!
     //   UpdateManager.register(this, ImApp.HOCKEY_APP_ID);

        //only check github for updates if there is no Google Play
        if (!hasGooglePlay()) {
            try {

                String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

                //if this is a full release, without -beta -rc etc, then check the appupdater!
                if (version.indexOf("-") == -1) {

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    long timeNow = new Date().getTime();
                    long timeSinceLastCheck = prefs.getLong("updatetime", -1);

                    //only check for updates once per day
                    if (timeSinceLastCheck == -1 || (timeNow - timeSinceLastCheck) > 86400) {

                        AppUpdater appUpdater = new AppUpdater(this);
                        appUpdater.setDisplay(Display.DIALOG);
                        appUpdater.setUpdateFrom(UpdateFrom.XML);
                        appUpdater.setUpdateXML(ImApp.URL_UPDATER);

                        //  appUpdater.showAppUpdated(true);
                        appUpdater.start();

                        prefs.edit().putLong("updatetime", timeNow).commit();
                    }
                }
            } catch (Exception e) {
                Log.d("AppUpdater", "error checking app updates", e);
            }
        }
    }

    boolean hasGooglePlay() {
        try {
            getApplication().getPackageManager().getPackageInfo("com.android.vending", 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;


    }


    Uri mLastPhoto = null;

    void startPhotoTaker() {

        /**
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),  "cs_" + new Date().getTime() + ".jpg");
        mLastPhoto = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                mLastPhoto);

        // start the image capture Intent
        startActivityForResult(intent, ConversationDetailActivity.REQUEST_TAKE_PICTURE);
         **/
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, ConversationDetailActivity.REQUEST_TAKE_PICTURE);

    }

    /**
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.awesome_activity_main);

    }*/

    public void applyStyle() {

        //first set font
        checkCustomFont();
        Typeface typeface = CustomTypefaceManager.getCurrentTypeface(this);

        if (typeface != null) {
            for (int i = 0; i < mToolbar.getChildCount(); i++) {
                View view = mToolbar.getChildAt(i);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;

                    tv.setTypeface(typeface);
                    break;
                }
            }
        }

        applyStyleColors ();
    }

    private void applyStyleColors ()
    {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        //not set color

        int themeColorHeader = settings.getInt("themeColor",-1);
        int themeColorText = settings.getInt("themeColorText",-1);
        int themeColorBg = settings.getInt("themeColorBg",-1);

        if (themeColorHeader != -1) {

            if (themeColorText == -1)
                themeColorText = getContrastColor(themeColorHeader);

            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(themeColorHeader);
                getWindow().setStatusBarColor(themeColorHeader);
                getWindow().setTitleColor(getContrastColor(themeColorHeader));
            }

            mToolbar.setBackgroundColor(themeColorHeader);
            mToolbar.setTitleTextColor(getContrastColor(themeColorHeader));

            mTabLayout.setBackgroundColor(themeColorHeader);
            mTabLayout.setTabTextColors(themeColorText, themeColorText);

            mFab.setBackgroundColor(themeColorHeader);

        }

        if (themeColorBg != -1)
        {
            if (mConversationList != null && mConversationList.getView() != null)
                mConversationList.getView().setBackgroundColor(themeColorBg);

            if (mContactList != null &&  mContactList.getView() != null)
                mContactList.getView().setBackgroundColor(themeColorBg);

            if (mMoreFragment != null && mMoreFragment.getView() != null)
                mMoreFragment.getView().setBackgroundColor(themeColorBg);

            if (mAccountFragment != null && mAccountFragment.getView() != null)
                mAccountFragment.getView().setBackgroundColor(themeColorBg);


        }

    }

    public static int getContrastColor(int colorIn) {
        double y = (299 * Color.red(colorIn) + 587 * Color.green(colorIn) + 114 * Color.blue(colorIn)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    private void checkCustomFont ()
    {

        if (Preferences.isLanguageTibetan())
        {
            CustomTypefaceManager.loadFromAssets(this,true);

        }
        else
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();

            final int N = mInputMethodProperties.size();
            boolean loadTibetan = false;
            for (int i = 0; i < N; i++) {

                InputMethodInfo imi = mInputMethodProperties.get(i);

                //imi contains the information about the keyboard you are using
                if (imi.getPackageName().equals("org.ironrabbit.bhoboard")) {
                    //                    CustomTypefaceManager.loadFromKeyboard(this);
                    loadTibetan = true;

                    break;
                }

            }

            CustomTypefaceManager.loadFromAssets(this, loadTibetan);
        }

    }

    private void requestChangeBatteryOptimizations ()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.battery_opt_title)
                    .setMessage(R.string.battery_opt_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {


                            Intent myIntent = new Intent();
                            myIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(myIntent);

                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {

                            /* User clicked cancel so do some stuff */
                            dialog.dismiss();
                        }
                    })
                    .create();
            dialog.show();
        }

    }

}
