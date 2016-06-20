package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.IOtrChatSession;
import org.awesomeapp.messenger.crypto.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.tasks.ChatSessionInitTask;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.ui.qr.QrDisplayActivity;
import org.awesomeapp.messenger.ui.qr.QrGenAsyncTask;
import org.awesomeapp.messenger.ui.qr.QrScanActivity;
import org.awesomeapp.messenger.ui.qr.QrShareAsyncTask;
import org.awesomeapp.messenger.util.LogCleaner;
import org.ironrabbit.type.CustomTypefaceManager;

import java.io.IOException;

import im.zom.messenger.R;


public class ContactDisplayActivity extends BaseActivity {

    private String mNickname = null;
    private String mUsername = null;
    private long mProviderId = -1;
    private long mAccountId = -1;
    private IImConnection mConn;
    private String mRemoteFingerprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.awesome_activity_contact);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        applyStyleForToolbar();

        mNickname = getIntent().getStringExtra("nickname");
        mUsername = getIntent().getStringExtra("address");
        mProviderId = getIntent().getLongExtra("provider", -1);
        mAccountId = getIntent().getLongExtra("account", -1);

        mConn = ((ImApp)getApplication()).getConnection(mProviderId,mAccountId);

        if (TextUtils.isEmpty(mNickname))
            mNickname = mUsername;

        mNickname = mNickname.split("@")[0].split("\\.")[0];
        setTitle("");

        TextView tv = (TextView)findViewById(R.id.tvNickname);
        tv = (TextView)findViewById(R.id.tvNickname);
        tv.setText(mNickname);

        tv = (TextView)findViewById(R.id.tvUsername);
        tv.setText(mUsername);

        if (!TextUtils.isEmpty(mUsername)) {
            try {
                Drawable avatar = DatabaseUtils.getAvatarFromAddress(getContentResolver(), mUsername, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT, false);
                if (avatar != null) {
                    ImageView iv = (ImageView) findViewById(R.id.imageAvatar);
                    iv.setImageDrawable(avatar);
                }
            } catch (Exception e) {
            }
        }

        ImageView btnQrShare = (ImageView) findViewById(R.id.qrshare);
        ImageView iv = (ImageView)findViewById(R.id.qrcode);
        tv = (TextView)findViewById(R.id.tvFingerprint);

        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mUsername);

            if (session != null) {
                mRemoteFingerprint = session.getDefaultOtrChatSession().getRemoteFingerprint();

                //   mRemoteFingerprint = OtrAndroidKeyManagerImpl.getInstance(this).getRemoteFingerprint( session.getOtrChatSession(0).getRemoteUserId());

                if (!TextUtils.isEmpty(mRemoteFingerprint)) {
                    tv.setText(prettyPrintFingerprint(mRemoteFingerprint));

                    iv.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            String inviteString;
                            try {
                                inviteString = OnboardingManager.generateInviteLink(ContactDisplayActivity.this, mUsername, mRemoteFingerprint);

                                Intent intent = new Intent(ContactDisplayActivity.this, QrDisplayActivity.class);
                                intent.putExtra(Intent.EXTRA_TEXT, inviteString);
                                intent.setType("text/plain");
                                startActivity(intent);

                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        }

                    });

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
                } else {
                    iv.setVisibility(View.GONE);
                    tv.setVisibility(View.GONE);
                    btnQrShare.setVisibility(View.GONE);
                }
            }
            else {
                iv.setVisibility(View.GONE);
                tv.setVisibility(View.GONE);
                btnQrShare.setVisibility(View.GONE);
            }
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error displaying contact",e);
        }

        View btn = findViewById(R.id.btnStartChat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startChat();

            }
        });

    }


    public void applyStyleForToolbar() {

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);

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
        int selColor = settings.getInt("themeColor",-1);

        if (selColor != -1) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(selColor);
                getWindow().setStatusBarColor(selColor);
            }

            mToolbar.setBackgroundColor(selColor);
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

       getMenuInflater().inflate(R.menu.menu_contact_detail, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_verify_or_view:
                verifyRemoteFingerprint();
                return true;
            case R.id.menu_verify_question:
                initSmpUI();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                }

                super.onPostExecute(chatId);
            }
        }.executeOnExecutor(ImApp.sThreadPoolExecutor,mUsername);

        finish();

    }

    private void initSmp(String question, String answer) {
        try {


                IChatSessionManager manager = mConn.getChatSessionManager();
                IChatSession session = manager.getChatSession(mUsername);
                IOtrChatSession iOtrSession = session.getDefaultOtrChatSession();
                iOtrSession.initSmpVerification(question, answer);


        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init SMP", e);

        }
    }

    private void verifyRemoteFingerprint() {


        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mUsername);
            IOtrChatSession otrChatSession = session.getDefaultOtrChatSession();
            otrChatSession.verifyKey(otrChatSession.getRemoteUserId());

            Snackbar.make(findViewById(R.id.contactmain), getString(R.string.action_verified), Snackbar.LENGTH_LONG).show();


        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init otr", e);

        }

    }

    private void initSmpUI() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View viewSmp = inflater.inflate(R.layout.smp_question_dialog, null, false);

        if (viewSmp != null)
        {
            new AlertDialog.Builder(this).setTitle(this.getString(R.string.otr_qa_title)).setView(viewSmp)
                    .setPositiveButton(this.getString(R.string.otr_qa_send), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            EditText eiQuestion = (EditText) viewSmp.findViewById(R.id.editSmpQuestion);
                            EditText eiAnswer = (EditText) viewSmp.findViewById(R.id.editSmpAnswer);
                            String question = eiQuestion.getText().toString();
                            String answer = eiAnswer.getText().toString();
                            initSmp(question, answer);
                        }
                    }).setNegativeButton(this.getString(R.string.otr_qa_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Do nothing.
                }
            }).show();
        }
    }



}
