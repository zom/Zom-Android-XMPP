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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.Preferences;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.ui.camera.CameraActivity;
import org.awesomeapp.messenger.ui.widgets.ShareRequest;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;
import org.ironrabbit.type.CustomTypefaceManager;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import im.zom.messenger.BuildConfig;
import im.zom.messenger.R;

//import com.bumptech.glide.Glide;

public class ConversationDetailActivity extends BaseActivity {

    private long mChatId = -1;
    private String mAddress = null;
    private String mNickname = null;

    private ConversationView mConvoView = null;

    MediaRecorder mMediaRecorder = null;
    File mAudioFilePath = null;

    private ImApp mApp;

    //private AppBarLayout appBarLayout;
    private View mRootLayout;
    private Toolbar mToolbar;

    private PrettyTime mPrettyTime;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 1) {
                if (!mConvoView.isGroupChat()) {
                    if (mConvoView.getRemotePresence() == Presence.AVAILABLE)
                    {
                        getSupportActionBar().setSubtitle(getString(R.string.presence_available));
                    }
                    else if (mConvoView.getLastSeen() != null) {
                        Date lastSeen = new Date();
                        if (mConvoView.getLastSeen().before(lastSeen))
                            lastSeen = mConvoView.getLastSeen();
                        getSupportActionBar().setSubtitle(mPrettyTime.format(lastSeen));
                    } else {
                        if (mConvoView.getRemotePresence() == Presence.AWAY)
                            getSupportActionBar().setSubtitle(getString(R.string.presence_away));
                        else if (mConvoView.getRemotePresence() == Presence.OFFLINE)
                            getSupportActionBar().setSubtitle(getString(R.string.presence_offline));
                        else if (mConvoView.getRemotePresence() == Presence.DO_NOT_DISTURB)
                            getSupportActionBar().setSubtitle(getString(R.string.presence_busy));
                        else
                            getSupportActionBar().setSubtitle(getString(R.string.presence_available));

                    }
                }
            }
        }
    };


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            //check if the broadcast is our desired one
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//here define your method to be executed when screen is going to sleep
                mConvoView.setSelected(false);
            }
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
//here define your method to be executed when screen is going to sleep
                mConvoView.setSelected(true);
            }

        }};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.awesome_activity_detail);

        mApp = (ImApp)getApplication();

        mConvoView = new ConversationView(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
      //  appBarLayout = (AppBarLayout)findViewById(R.id.appbar);
        mRootLayout = findViewById(R.id.main_content);

        mPrettyTime = new PrettyTime(getCurrentLocale());

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        applyStyleForToolbar();

        collapseToolbar();

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

    }

    public void updateLastSeen (Date lastSeen)
    {
       mHandler.sendEmptyMessage(1);
    }

    public void applyStyleForToolbar() {


        getSupportActionBar().setTitle(mConvoView.getTitle());

        if (!mConvoView.isGroupChat()) {
            if (mConvoView.getLastSeen() != null) {
                getSupportActionBar().setSubtitle(new PrettyTime().format(mConvoView.getLastSeen()));
            } else if (mConvoView.getRemotePresence() != -1) {
                if (mConvoView.getRemotePresence() == Presence.AWAY)
                    getSupportActionBar().setSubtitle(getString(R.string.presence_away));
                else if (mConvoView.getRemotePresence() == Presence.OFFLINE)
                    getSupportActionBar().setSubtitle(getString(R.string.presence_offline));
                else if (mConvoView.getRemotePresence() == Presence.DO_NOT_DISTURB)
                    getSupportActionBar().setSubtitle(getString(R.string.presence_busy));
                else
                    getSupportActionBar().setSubtitle(getString(R.string.presence_available));

            } else {
                getSupportActionBar().setSubtitle(mConvoView.getSubtitle());
            }
        }
        else
        {
            getSupportActionBar().setSubtitle("");

        }

        //first set font
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

        //not set color
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int themeColorHeader = settings.getInt("themeColor",-1);
        int themeColorText = settings.getInt("themeColorText",-1);
        int themeColorBg = settings.getInt("themeColorBg",-1);

        if (themeColorHeader != -1) {

            if (themeColorText == -1)
                themeColorText = getContrastColor(themeColorHeader);

            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(themeColorHeader);
                getWindow().setStatusBarColor(themeColorHeader);
                getWindow().setTitleColor(themeColorText);
            }

      //      appBarLayout.setBackgroundColor(themeColorHeader);
         //   collapsingToolbar.setBackgroundColor(themeColorHeader);
            mToolbar.setBackgroundColor(themeColorHeader);
            mToolbar.setTitleTextColor(themeColorText);

        }

        if (themeColorBg != -1)
        {
            if (mRootLayout != null)
                mRootLayout.setBackgroundColor(themeColorBg);

            View viewInput = findViewById(R.id.inputLayout);
            viewInput.setBackgroundColor(themeColorBg);

            if (themeColorText != -1) {
                mConvoView.mComposeMessage.setTextColor(themeColorText);
                mConvoView.mComposeMessage.setHintTextColor(themeColorText);
            }
        }

    }

    @SuppressLint("Range")
    public static int getContrastColor(int colorIn) {
         double y = (299 * Color.red(colorIn) + 587 * Color.green(colorIn) + 114 * Color.blue(colorIn)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }


    private void processIntent(Intent intent)
    {

        mApp = (ImApp)getApplication();

        mChatId = intent.getLongExtra("id", -1);

        mAddress = intent.getStringExtra("address");
        mNickname = intent.getStringExtra("nickname");

        if (mChatId != -1) {
            boolean bound = mConvoView.bindChat(mChatId, mAddress, mNickname);

            if (bound) {
                mConvoView.startListening();
                applyStyleForToolbar();
            }
            else
                finish();
        }
        else {
            finish ();
        }

    }

    public void collapseToolbar(){

     //   appBarLayout.setExpanded(false);
    }

    public void expandToolbar(){

    //    appBarLayout.setExpanded(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        processIntent(intent);

        mConvoView.setSelected(true);

        IntentFilter regFilter = new IntentFilter();
        regFilter .addAction(Intent.ACTION_SCREEN_OFF);
        regFilter .addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(receiver, regFilter);

        // Set last read date now!
        if (mChatId != -1) {
            ContentValues values = new ContentValues(1);
            values.put(Imps.Chats.LAST_READ_DATE, System.currentTimeMillis());
            getContentResolver().update(ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mChatId), values, null, null);
        }



    }

    @Override
    protected void onPause() {
        super.onPause();

        mConvoView.setSelected(false);

        unregisterReceiver(receiver);

        // Set last read date now!
        if (mChatId != -1) {
            ContentValues values = new ContentValues(1);
            values.put(Imps.Chats.LAST_READ_DATE, System.currentTimeMillis());
            getContentResolver().update(ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mChatId), values, null, null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        processIntent(intent);

        // Set last read date now!
        if (mChatId != -1) {
            ContentValues values = new ContentValues(1);
            values.put(Imps.Chats.LAST_READ_DATE, System.currentTimeMillis());
            getContentResolver().update(ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mChatId), values, null, null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_end_conversation:
                mConvoView.closeChatSession(true);
                finish();
                return true;
            case R.id.menu_verify_or_view:
                mConvoView.showVerifyDialog();
                return true;
            case R.id.menu_group_info:
                mConvoView.showGroupInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mConvoView.isGroupChat())
        {
            getMenuInflater().inflate(R.menu.menu_conversation_detail_group, menu);
        }
        else {
            getMenuInflater().inflate(R.menu.menu_conversation_detail, menu);
        }

        return true;
    }

    void showAddContact ()
    {
        Intent intent = new Intent(this, ContactsPickerActivity.class);
        startActivityForResult(intent, REQUEST_PICK_CONTACTS);
    }

    void startImagePicker() {
        startActivityForResult(getPickImageChooserIntent(), REQUEST_SEND_IMAGE);

    }

    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {


        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, getString(R.string.choose_photos));

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    Uri mLastPhoto = null;
    private final static int MY_PERMISSIONS_REQUEST_AUDIO = 1;
    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 2;
    private final static int MY_PERMISSIONS_REQUEST_FILE = 3;


    void startPhotoTaker() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        if (permissionCheck ==PackageManager.PERMISSION_DENIED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {


                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar.make(mConvoView.getHistoryView(), R.string.grant_perms, Snackbar.LENGTH_LONG).show();
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {

           if (Preferences.useProofMode())
           {
               Intent intent = new Intent(this, CameraActivity.class);
               intent.putExtra(CameraActivity.SETTING_ONE_AND_DONE,true);
               startActivityForResult(intent, ConversationDetailActivity.REQUEST_TAKE_PICTURE);
           }
           else {
               // create Intent to take a picture and return control to the calling application
               Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
               File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "cs_" + new Date().getTime() + ".jpg");

               mLastPhoto = FileProvider.getUriForFile(this,
                       BuildConfig.APPLICATION_ID + ".provider",
                       photo);

               intent.putExtra(MediaStore.EXTRA_OUTPUT,
                       mLastPhoto);

               // start the image capture Intent
               startActivityForResult(intent, ConversationDetailActivity.REQUEST_TAKE_PICTURE);

           }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    void startFilePicker(String mimeType) {

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck ==PackageManager.PERMISSION_DENIED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {


                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar.make(mConvoView.getHistoryView(), R.string.grant_perms, Snackbar.LENGTH_LONG).show();
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_FILE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {

            // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            // browser.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.setType(mimeType);

            startActivityForResult(Intent.createChooser(intent, getString(R.string.invite_share)), REQUEST_SEND_FILE);
        }
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public void handleSendDelete(Uri contentUri, String defaultType, boolean delete, boolean resizeImage, boolean importContent) {

        handleSendDelete(mConvoView.getChatSession(),contentUri,defaultType,delete,resizeImage,importContent);
    }

    public void handleSendDelete(IChatSession session, Uri contentUri, String defaultType, boolean delete, boolean resizeImage, boolean importContent) {
        try {

            // import
            SystemServices.FileInfo info = SystemServices.getFileInfoFromURI(this, contentUri);

            if (info.type == null)
                info.type = defaultType;

            String sessionId = mConvoView.getChatId()+"";

            Uri sendUri;

            if (resizeImage)
                sendUri = SecureMediaStore.resizeAndImportImage(this, sessionId, contentUri, info.type);
            else if (importContent) {

                if (contentUri.getScheme() == null || contentUri.getScheme().equals("assets"))
                    sendUri = SecureMediaStore.importContent(sessionId,info.name, info.stream);
                else if (contentUri.getScheme().startsWith("http"))
                {
                    sendUri = SecureMediaStore.importContent(sessionId,info.name, new URL(contentUri.toString()).openConnection().getInputStream());
                }
                else
                    sendUri = SecureMediaStore.importContent(sessionId,info.name, info.stream);
            }
            else
            {
                sendUri = contentUri;
                info.type = getContentResolver().getType(sendUri);
                if (info.type == null)
                    info.type = defaultType;
            }

            // send
            boolean sent = handleSendData(session, sendUri, info.type);
            if (!sent) {
                // not deleting if not sent
                return;
            }
            // auto delete
            if (delete) {
                boolean deleted = delete(contentUri);
                if (!deleted) {
                    throw new IOException("Error deleting " + contentUri);
                }
            }

            if (info.stream != null)
                info.stream.close();

        } catch (Exception e) {
            //  Toast.makeText(this, "Error sending file", Toast.LENGTH_LONG).show(); // TODO i18n
            Log.e(ImApp.LOG_TAG, "error sending file", e);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_PICK_CONTACTS) {

                if (resultIntent == null)
                    return;

                ArrayList<String> invitees = new ArrayList<String>();

                String username = resultIntent.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (username != null)
                    invitees.add(username);
                else
                    invitees = resultIntent.getStringArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAMES);

                mConvoView.inviteContacts(invitees);

            }
            if (requestCode == REQUEST_SEND_IMAGE) {


                if (resultIntent == null)
                    return;

                Uri uri = resultIntent.getData() ;

                if( uri == null ) {
                    return ;
                }

                ShareRequest request = new ShareRequest();
                request.deleteFile = false;
                request.resizeImage = true;
                request.importContent = true;
                request.media = uri;
                request.mimeType = "image/jpeg";

                try {
                    mConvoView.setMediaDraft(request);
                }
                catch (Exception e){
                    Log.w(ImApp.LOG_TAG,"error setting media draft",e);
                }
                /**
                boolean deleteFile = false;
                boolean resizeImage = true;
                boolean importContent = true;

                handleSendDelete(uri, "image/jpeg", deleteFile, resizeImage, importContent);
                 **/
            }
            else if (requestCode == REQUEST_SEND_FILE || requestCode == REQUEST_SEND_AUDIO) {


                if (resultIntent == null)
                    return;

                Uri uri = resultIntent.getData() ;

                if( uri == null ) {
                    return;
                }

                String defaultType = resultIntent.getType();

                boolean deleteFile = false;
                boolean resizeImage = false;
                boolean importContent = false;

                handleSendDelete(uri, defaultType, deleteFile, resizeImage, importContent);
            }
            else if (requestCode == REQUEST_TAKE_PICTURE)
            {
                ShareRequest request = new ShareRequest();

                if (Preferences.useProofMode()) {
                    request.deleteFile = false;
                    request.resizeImage = false;
                    request.importContent = false;
                    request.media = resultIntent.getData();
                }
                else
                {
                    request.deleteFile = false;
                    request.resizeImage = true;
                    request.importContent = true;
                    request.media = mLastPhoto;
                }

                request.mimeType = "image/jpeg";

                try {
                    mConvoView.setMediaDraft(request);
                }
                catch (Exception e){
                    Log.w(ImApp.LOG_TAG,"error setting media draft",e);
                }

            }
            else if (requestCode == REQUEST_IMAGE_VIEW)
            {
                if (resultIntent != null &&
                        resultIntent.hasExtra("resendImageUri"))
                {
                    ShareRequest request = new ShareRequest();
                    request.deleteFile = false;
                    request.resizeImage = false;
                    request.importContent = false;
                    request.media = Uri.parse(resultIntent.getStringExtra("resendImageUri"));
                    request.mimeType = resultIntent.getStringExtra("resendImageMimeType");

                    sendShareRequest(request);

                }

                mConvoView.requeryCursor();
            }



        }
    }

    public void sendShareRequest (ShareRequest sreq)
    {
        handleSendDelete(sreq.media,sreq.mimeType,sreq.deleteFile,sreq.resizeImage,sreq.importContent);
    }

    public boolean handleSendData(IChatSession session, Uri uri, String mimeType) {
        try {

            if (session != null) {

                String offerId = UUID.randomUUID().toString();

                return session.offerData(offerId, uri.toString(), mimeType );
            }

        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG,"error sending file",e);
        }
        return false; // was not sent
    }

    boolean mIsAudioRecording = false;

    public boolean isAudioRecording ()
    {
        return mIsAudioRecording;
    }

    public void startAudioRecording ()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permissionCheck ==PackageManager.PERMISSION_DENIED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {


                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar.make(mConvoView.getHistoryView(), R.string.grant_perms, Snackbar.LENGTH_LONG).show();
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {

            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am.getMode() == AudioManager.MODE_NORMAL) {

                mMediaRecorder = new MediaRecorder();

                String fileName = UUID.randomUUID().toString().substring(0, 8) + ".m4a";
                mAudioFilePath = new File(getFilesDir(), fileName);

                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

                //maybe we can modify these in the future, or allow people to tweak them
                mMediaRecorder.setAudioChannels(1);
                mMediaRecorder.setAudioEncodingBitRate(22050);
                mMediaRecorder.setAudioSamplingRate(64000);

                mMediaRecorder.setOutputFile(mAudioFilePath.getAbsolutePath());

                try {
                    mIsAudioRecording = true;
                    mMediaRecorder.prepare();
                    mMediaRecorder.start();
                } catch (Exception e) {
                    Log.e(ImApp.LOG_TAG, "couldn't start audio", e);
                }
            }
        }
    }

    public int getAudioAmplitude ()
    {
        return mMediaRecorder.getMaxAmplitude();
    }

    public void stopAudioRecording (boolean send)
    {
        if (mMediaRecorder != null && mAudioFilePath != null && mIsAudioRecording) {

            try {

                mMediaRecorder.stop();

                mMediaRecorder.reset();
                mMediaRecorder.release();

                if (send) {
                    Uri uriAudio = Uri.fromFile(mAudioFilePath);
                    boolean deleteFile = true;
                    boolean resizeImage = false;
                    boolean importContent = true;
                    handleSendDelete(uriAudio, "audio/mp4", deleteFile, resizeImage, importContent);
                } else {
                    mAudioFilePath.delete();
                }
            }
            catch (IllegalStateException ise)
            {
                Log.w(ImApp.LOG_TAG,"error stopping audio recording: " + ise);
            }
            catch (RuntimeException re) //stop can fail so we should catch this here
            {
                Log.w(ImApp.LOG_TAG,"error stopping audio recording: " + re);
            }

            mIsAudioRecording = false;
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

    private TextView getActionBarTextView(Toolbar toolbar) {
        TextView titleTextView = null;

        try {
            Field f = toolbar.getClass().getDeclaredField("mTitleTextView");
            f.setAccessible(true);
            titleTextView = (TextView) f.get(toolbar);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
        return titleTextView;
    }


    @TargetApi(Build.VERSION_CODES.N)
    public Locale getCurrentLocale(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            return getResources().getConfiguration().locale;
        }
    }


    public static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    public static final int REQUEST_SEND_IMAGE = REQUEST_PICK_CONTACTS + 1;
    public static final int REQUEST_SEND_FILE = REQUEST_SEND_IMAGE + 1;
    public static final int REQUEST_SEND_AUDIO = REQUEST_SEND_FILE + 1;
    public static final int REQUEST_TAKE_PICTURE = REQUEST_SEND_AUDIO + 1;
    public static final int REQUEST_SETTINGS = REQUEST_TAKE_PICTURE + 1;
    public static final int REQUEST_TAKE_PICTURE_SECURE = REQUEST_SETTINGS + 1;
    public static final int REQUEST_ADD_CONTACT = REQUEST_TAKE_PICTURE_SECURE + 1;
    public static final int REQUEST_IMAGE_VIEW = REQUEST_ADD_CONTACT + 1;

}
