package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.awesomeapp.messenger.ImApp;
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

public class GroupDisplayActivity extends BaseActivity {

    private String mName = null;
    private String mAddress = null;
    private long mProviderId = -1;
    private long mAccountId = -1;
    private long mLastChatId = -1;

    private IImConnection mConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.awesome_activity_group);

        mName = getIntent().getStringExtra("nickname");
        mAddress = getIntent().getStringExtra("address");
        mProviderId = getIntent().getLongExtra("provider", -1);
        mAccountId = getIntent().getLongExtra("account", -1);
        mLastChatId  = getIntent().getLongExtra("chat", -1);

        mConn = ((ImApp)getApplication()).getConnection(mProviderId,mAccountId);

        TextView tv = (TextView)findViewById(R.id.tvNickname);
        tv = (TextView)findViewById(R.id.tvNickname);
        tv.setText(mName);

        tv = (TextView)findViewById(R.id.tvUsername);
        tv.setText(mAddress);

        tv = (TextView)findViewById(R.id.tvMembers);
        String[] projection = { Imps.GroupMembers.NICKNAME };
        Uri memberUri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, mLastChatId);
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(memberUri, projection, null, null, null);
        StringBuilder buf = new StringBuilder();

        if (c != null) {

            while(c.moveToNext())
            {
                buf.append(c.getString(0));

                if (!c.isLast())
                    buf.append(", ");
            }

            c.close();

            if (buf.length() > 0)
                tv.setText(buf.toString());
        }

        ImageView iv = (ImageView)findViewById(R.id.qrcode);

        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mAddress);

            /**
            try {
                String inviteLink = OnboardingManager.generateInviteLink(this, mAddress, "");
                new QrGenAsyncTask(this, iv, ImApp.DEFAULT_AVATAR_WIDTH).execute(inviteLink);
            } catch (IOException ioe) {
                Log.e(ImApp.LOG_TAG, "couldn't generate QR code", ioe);
            }*/

            iv.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    String inviteString;
                    try {
                        inviteString = OnboardingManager.generateInviteLink(GroupDisplayActivity.this, mAddress, "", mName);
                        OnboardingManager.inviteScan(GroupDisplayActivity.this, inviteString);
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
                        String inviteLink = OnboardingManager.generateInviteLink(GroupDisplayActivity.this, mAddress, "", mName);
                        new QrShareAsyncTask(GroupDisplayActivity.this).execute(inviteLink);
                    } catch (IOException ioe) {
                        Log.e(ImApp.LOG_TAG, "couldn't generate QR code", ioe);
                    }
                }
            });

        }
        catch (Exception e)
        {

        }

        Button btn = (Button)findViewById(R.id.btnStartChat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startChat();

            }
        });

    }



    public void startChat ()
    {

        new ChatSessionInitTask(((ImApp)getApplication()),mProviderId, mAccountId, Imps.Contacts.TYPE_GROUP)
        {
            @Override
            protected void onPostExecute(Long chatId) {

                if (chatId != -1) {
                    Intent intent = new Intent(GroupDisplayActivity.this, ConversationDetailActivity.class);
                    intent.putExtra("id", chatId);
                    startActivity(intent);

                    finish();
                }

                super.onPostExecute(chatId);
            }
        }
        .executeOnExecutor(ImApp.sThreadPoolExecutor,mAddress);



    }

}
