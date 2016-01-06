package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.tasks.ChatSessionInitTask;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.ui.qr.QrGenAsyncTask;
import org.awesomeapp.messenger.ui.qr.QrShareAsyncTask;
import org.awesomeapp.messenger.util.LogCleaner;

import java.io.IOException;

import im.zom.messenger.R;


public class ContactDisplayActivity extends Activity {

    private String mUsername = null;
    private long mProviderId = -1;
    private long mAccountId = -1;
    private IImConnection mConn;
    private String mRemoteFingerprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.awesome_activity_contact);

        mUsername = getIntent().getStringExtra("contact");
        mProviderId = getIntent().getLongExtra("provider", -1);
        mAccountId = getIntent().getLongExtra("account", -1);

        mConn = ((ImApp)getApplication()).getConnection(mProviderId,mAccountId);

        TextView tv = (TextView)findViewById(R.id.tvNickname);
        tv = (TextView)findViewById(R.id.tvNickname);
        tv.setText(mUsername.split("@")[0]);

        tv = (TextView)findViewById(R.id.tvUsername);
        tv.setText(mUsername);

        try {
            Drawable avatar = DatabaseUtils.getAvatarFromAddress(getContentResolver(), mUsername, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT, false);
            if (avatar != null)
            {
                ImageView iv = (ImageView)findViewById(R.id.imageAvatar);
                iv.setImageDrawable(avatar);
            }
        }
        catch (Exception e){}


        ImageView iv = (ImageView)findViewById(R.id.qrcode);
        tv = (TextView)findViewById(R.id.tvFingerprint);

        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mUsername);

            mRemoteFingerprint = session.getOtrChatSession(0).getRemoteFingerprint();

            //String remoteFingerprint = OtrAndroidKeyManagerImpl.getInstance(this).getRemoteFingerprint(jabberId);

            if (mRemoteFingerprint != null) {
                tv.setText(prettyPrintFingerprint(mRemoteFingerprint));

                /**
                try {
                    String inviteLink = OnboardingManager.generateInviteLink(this, mUsername, mRemoteFingerprint);
                    new QrGenAsyncTask(this, iv, 256).execute(inviteLink);
                } catch (IOException ioe) {
                    Log.e(ImApp.LOG_TAG, "couldn't generate QR code", ioe);
                }*/

                iv.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        String inviteString;
                        try {
                            inviteString = OnboardingManager.generateInviteLink(ContactDisplayActivity.this, mUsername, mRemoteFingerprint);
                            OnboardingManager.inviteScan(ContactDisplayActivity.this, inviteString);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }

                });

                ImageView btnQrShare = (ImageView) findViewById(R.id.qrshare);
                btnQrShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        try {
                            String inviteLink = OnboardingManager.generateInviteLink(ContactDisplayActivity.this, mUsername, mRemoteFingerprint);
                            new QrShareAsyncTask(ContactDisplayActivity.this).execute(inviteLink);
                        } catch (IOException ioe) {
                            Log.e(ImApp.LOG_TAG, "couldn't generate QR code", ioe);
                        }
                    }
                });
            }
        }
        catch (Exception e)
        {

        }

        View btn = findViewById(R.id.btnStartChat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startChat();

            }
        });

    }

    private String prettyPrintFingerprint(String fingerprint) {
        StringBuffer spacedFingerprint = new StringBuffer();

        for (int i = 0; i + 8 <= fingerprint.length(); i += 8) {
            spacedFingerprint.append(fingerprint.subSequence(i, i + 8));
            spacedFingerprint.append(' ');
        }

        return spacedFingerprint.toString();
    }

    public void startChat ()
    {

        new ChatSessionInitTask(((ImApp)getApplication()),mProviderId, mAccountId, Imps.Contacts.TYPE_NORMAL)
        {
            @Override
            protected void onPostExecute(Long chatId) {

                if (chatId != -1) {
                    Intent intent = new Intent(ContactDisplayActivity.this, ConversationDetailActivity.class);
                    intent.putExtra("id", chatId);
                    startActivity(intent);

                    finish();
                }

                super.onPostExecute(chatId);
            }
        }.execute(mUsername);


    }


}
