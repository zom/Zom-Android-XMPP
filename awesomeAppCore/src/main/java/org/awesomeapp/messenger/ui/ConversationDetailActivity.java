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
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

//import com.bumptech.glide.Glide;

import org.awesomeapp.messenger.provider.Imps;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.awesomeapp.messenger.service.IChatSession;
import info.guardianproject.otr.app.im.R;
import org.awesomeapp.messenger.util.SecureMediaStore;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.util.SystemServices;

public class ConversationDetailActivity extends AppCompatActivity {

    private long mChatId = -1;
    private ConversationView mConvoView = null;

    MediaRecorder mMediaRecorder = null;
    File mAudioFilePath = null;

    private ImApp mApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.awesome_activity_detail);

        Intent intent = getIntent();
        mApp = (ImApp)getApplication();

        mChatId = intent.getLongExtra("id",-1);

        mConvoView = new ConversationView(this);
        mConvoView.bindChat(mChatId);
        mConvoView.setSelected(true);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(mConvoView.getTitle());


        loadBackdrop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mConvoView.setSelected(true);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBackdrop() {
        final ImageView imageView = (ImageView) findViewById(R.id.backdrop);

        if (mConvoView.getHeader()!=null)
            imageView.setImageDrawable(mConvoView.getHeader());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversation_detail, menu);
        return true;
    }

    void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SEND_IMAGE);
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
        startActivityForResult(intent, REQUEST_TAKE_PICTURE);
    }


    void startFilePicker() {
        Intent selectFile = new Intent(Intent.ACTION_GET_CONTENT);
        Intent intentChooser = Intent.createChooser(selectFile, "Select File");

        if (intentChooser != null)
            startActivityForResult(Intent.createChooser(selectFile, "Select File"), REQUEST_SEND_FILE);
    }

    void startAudioPicker() {


        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        if (!isCallable(intent))
        {
            intent = new Intent("android.provider.MediaStore.RECORD_SOUND");
            intent.addCategory("android.intent.category.DEFAULT");

            if (!isCallable(intent))
            {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");

                if (!isCallable(intent))
                    return;

            }
        }

        startActivityForResult(intent, REQUEST_SEND_AUDIO); // intent and requestCode of 1

    }
    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public void handleSendDelete( Uri contentUri, boolean delete, boolean resizeImage) {
        try {
            // import
            SystemServices.FileInfo info = SystemServices.getFileInfoFromURI(this, contentUri);
            String sessionId = mConvoView.getChatId()+"";
            Uri vfsUri;
            if (resizeImage)
                vfsUri = SecureMediaStore.resizeAndImportImage(this, sessionId, contentUri, info.type);
            else {

                if (contentUri.getScheme() != null)
                    vfsUri = SecureMediaStore.importContent(sessionId, info.path);
                else
                {
                    vfsUri = SecureMediaStore.importContent(sessionId, info.path,getResources().getAssets().open(info.path));
                }
            }
            // send
            boolean sent = handleSendData(vfsUri, info.type);
            if (!sent) {
                // not deleting if not sent
                return;
            }
            // autu delete
            if (delete) {
                boolean deleted = delete(contentUri);
                if (!deleted) {
                    throw new IOException("Error deleting " + contentUri);
                }
            }
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

            if (requestCode == REQUEST_SEND_IMAGE || requestCode == REQUEST_SEND_FILE || requestCode == REQUEST_SEND_AUDIO) {
                Uri uri = resultIntent.getData() ;

                if( uri == null ) {
                    return ;
                }
                boolean deleteAudioFile = (requestCode == REQUEST_SEND_AUDIO);
                boolean resizeImage = requestCode == REQUEST_SEND_IMAGE; //resize if is an image, not shared as "file"
                handleSendDelete(uri, deleteAudioFile, resizeImage);
            }
            else if (requestCode == REQUEST_TAKE_PICTURE)
            {
                if (mLastPhoto != null) {
                    handleSendDelete(mLastPhoto, true, true);
                    mLastPhoto = null;
                }
                /**
                File file = new File(getRealPathFromURI(mLastPhoto));
                final Handler handler = new Handler();
                MediaScannerConnection.scanFile(
                        this, new String[]{file.toString()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, final Uri uri) {

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        handleSendDelete(mLastPhoto, true, true);
                                    }
                                });
                            }
                        });
                 */
            }



        }
    }

    private boolean handleSendData(Uri uri, String mimeType) {
        try {
            SystemServices.FileInfo info = SystemServices.getFileInfoFromURI(this, uri);

            if (mimeType != null)
                info.type = mimeType;

            if (info != null && info.path != null && SecureMediaStore.exists(info.path))
            {
                IChatSession session = mConvoView.getChatSession();

                if (session != null) {
                    if (info.type == null)
                        if (mimeType != null)
                            info.type = mimeType;
                        else
                            info.type = "application/octet-stream";

                    String offerId = UUID.randomUUID().toString();
                    session.offerData(offerId, info.path, info.type );

                    int type = mConvoView.isOtrSessionVerified() ? Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED : Imps.MessageType.OUTGOING_ENCRYPTED;
                    Imps.insertMessageInDb(
                            getContentResolver(), false, session.getId(), true, null, uri.toString(),
                            System.currentTimeMillis(), type,
                            0, offerId, info.type);
                    return true; // sent
                }
            }
            else
            {
                Toast.makeText(this, R.string.sorry_we_cannot_share_that_file_type, Toast.LENGTH_LONG).show();
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
        mMediaRecorder = new MediaRecorder();

        mAudioFilePath = new File(getFilesDir(),"audiotemp.aac");

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOutputFile(mAudioFilePath.getAbsolutePath());

        try {
            mIsAudioRecording = true;
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"couldn't start audio",e);
        }
    }

    public int getAudioAmplitude ()
    {
        return mMediaRecorder.getMaxAmplitude();
    }

    public void stopAudioRecording (boolean send)
    {
        if (mMediaRecorder != null && mAudioFilePath != null) {
            
            mMediaRecorder.stop();

            mMediaRecorder.reset();
            mMediaRecorder.release();

            if (send) {
                Uri uriAudio = Uri.fromFile(mAudioFilePath);
                boolean deleteFile = true;
                boolean resizeImage = false;
                handleSendDelete(uriAudio, deleteFile, resizeImage);
            }
            else
            {
                mAudioFilePath.delete();
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

    public static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    public static final int REQUEST_SEND_IMAGE = REQUEST_PICK_CONTACTS + 1;
    public static final int REQUEST_SEND_FILE = REQUEST_SEND_IMAGE + 1;
    public static final int REQUEST_SEND_AUDIO = REQUEST_SEND_FILE + 1;
    public static final int REQUEST_TAKE_PICTURE = REQUEST_SEND_AUDIO + 1;
    public static final int REQUEST_SETTINGS = REQUEST_TAKE_PICTURE + 1;
    public static final int REQUEST_TAKE_PICTURE_SECURE = REQUEST_SETTINGS + 1;
    public static final int REQUEST_ADD_CONTACT = REQUEST_TAKE_PICTURE_SECURE + 1;
}
