/*
 * Copyright (C) 2009 Myriad Group AG Copyright (C) 2009 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.awesomeapp.messenger.ui;

import org.awesomeapp.messenger.service.IImConnection;
import im.zom.messenger.R;
import org.awesomeapp.messenger.model.ImConnection;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.ImServiceConstants;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AccountListItem extends LinearLayout {

    private Activity mActivity;

    private SignInManager mSignInManager;
    private ContentResolver mResolver;
    private CompoundButton mSignInSwitch;
    private boolean mUserChanged = false;

    private boolean mIsSignedIn;

    private TextView mProviderName;
    private TextView mLoginName;

    private int mProviderIdColumn;
    private int mActiveAccountIdColumn;
    private int mActiveAccountUserNameColumn;
    private int mAccountPresenceStatusColumn;
    private int mAccountConnectionStatusColumn;
    private int mActiveAccountNickNameColumn;

    private long mAccountId;
    private long mProviderId;

    private boolean mShowLongName = false;

    private static Handler mHandler = new Handler()
    {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            //update notifications from async task
        }

    };

    public AccountListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public void init(Activity activity, Cursor c, boolean showLongName, SignInManager signInManager) {

        mActivity = activity;
        mResolver = mActivity.getContentResolver();
        mSignInManager = signInManager;

        mShowLongName = showLongName;

        mProviderIdColumn = c.getColumnIndexOrThrow(Imps.Provider._ID);

        mSignInSwitch = (CompoundButton) findViewById(R.id.statusSwitch);
        mProviderName = (TextView) findViewById(R.id.providerName);
        mLoginName = (TextView) findViewById(R.id.loginName);

        mActiveAccountIdColumn = c.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);
        mActiveAccountUserNameColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_USERNAME);
        mAccountPresenceStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_PRESENCE_STATUS);
        mAccountConnectionStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_CONNECTION_STATUS);
        mActiveAccountNickNameColumn= c
                .getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_NICKNAME);

        setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(mActivity, AccountSettingsActivity.class);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);

                mActivity.startActivity(intent);
            }

        });

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                ImApp app = (ImApp)mActivity.getApplication();

                app.setDefaultAccount(mProviderId, mAccountId);

                Snackbar.make(v, "Default account changed", Snackbar.LENGTH_LONG).show();

                return true;
            }
        });


        if (mSignInSwitch != null)
        {


            mSignInSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (isChecked)
                        mSignInManager.signIn(mProviderId, mAccountId);
                    else
                        mSignInManager.signOut(mProviderId, mAccountId);

                    mUserChanged = true;
                }

            });


        }


    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void bindView(Cursor cursor) {
        final Resources r = getResources();

        mProviderId = cursor.getInt(mProviderIdColumn);

        mAccountId = cursor.getLong(mActiveAccountIdColumn);
        setTag(mAccountId);

        if (!cursor.isNull(mActiveAccountIdColumn)) {

            final String nickname = cursor.getString(mActiveAccountNickNameColumn);

            final String activeUserName = cursor.getString(mActiveAccountUserNameColumn);

            final int connectionStatus = cursor.getInt(mAccountConnectionStatusColumn);
            final String presenceString = getPresenceString(cursor, getContext());

            mHandler.postDelayed(new Runnable () {
                public void run ()
                {
                    runBindTask(r, (int)mProviderId, (int)mAccountId, nickname, activeUserName, connectionStatus, presenceString);
                }
            }
                    , 200l);

        }
    }

    @Override
    protected void onDetachedFromWindow() {

        super.onDetachedFromWindow();
    }

    private void runBindTask(final Resources r, final int providerId, final int accountId, final String nickname, final String activeUserName,
            final int dbConnectionStatus, final String presenceString) {

            String mProviderNameText;
            String mSecondRowText;

            try
            {
                    Cursor pCursor = mResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString( providerId)},null);

                    Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor, mResolver,
                            providerId,     false /* keep updated */, mHandler /* no handler */);

                    String userDomain = settings.getDomain();
                    int connectionStatus = dbConnectionStatus;

                    IImConnection conn = ((ImApp)mActivity.getApplication()).getConnection(providerId,accountId);
                    if (conn == null)
                    {
                        connectionStatus = ImConnection.DISCONNECTED;
                    }
                    else
                    {
                        try {
                            connectionStatus = conn.getState();
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    if (mShowLongName)
                        mProviderNameText = activeUserName;// + '@' + userDomain;
                    else
                        mProviderNameText = nickname;

                    switch (connectionStatus) {

                    case ImConnection.LOGGING_IN:
                        mSecondRowText = r.getString(R.string.signing_in_wait);
                        mIsSignedIn = true;

                        break;

                    case ImConnection.SUSPENDING:
                    case ImConnection.SUSPENDED:
                        mSecondRowText = r.getString(R.string.error_suspended_connection);
                        mIsSignedIn = true;

                        break;



                    case ImConnection.LOGGED_IN:
                        mIsSignedIn = true;
                        mSecondRowText = computeSecondRowText(presenceString, r, settings, true);

                        break;

                    case ImConnection.LOGGING_OUT:
                        mIsSignedIn = false;
                        mSecondRowText = r.getString(R.string.signing_out_wait);

                        break;

                    default:

                        mIsSignedIn = false;
                        mSecondRowText = computeSecondRowText(presenceString, r, settings, true);
                        break;
                    }

                    settings.close();
                    pCursor.close();

                    applyView(mProviderNameText, mIsSignedIn, mSecondRowText);
                }
                catch (NullPointerException npe)
                {
                    Log.d(ImApp.LOG_TAG,"null on QueryMap (this shouldn't happen anymore, but just in case)",npe);
                }




    }

    private void applyView(String providerNameText, boolean isSignedIn, String secondRowText) {

        if (mProviderName != null)
        {
            mProviderName.setText(providerNameText);


            if (mSignInSwitch != null && (!mUserChanged))
            {
                mSignInSwitch.setOnCheckedChangeListener(null);
                mSignInSwitch.setChecked(isSignedIn);
                mSignInSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked)
                            mSignInManager.signIn(mProviderId, mAccountId);
                        else
                            mSignInManager.signOut(mProviderId, mAccountId);

                        mUserChanged = true;
                    }

                });
            }

            if (mLoginName != null)
            {
                mLoginName.setText(secondRowText);




            }
        }

    }

    private String computeSecondRowText(String presenceString, Resources r,
            final Imps.ProviderSettings.QueryMap settings, boolean showPresence) {
        String secondRowText;
        StringBuffer secondRowTextBuffer = new StringBuffer();


        if (showPresence && presenceString.length() > 0)
        {
            secondRowTextBuffer.append(presenceString);
            secondRowTextBuffer.append(" - ");
        }


        if (settings.getServer() != null && settings.getServer().length() > 0)
        {

            secondRowTextBuffer.append(settings.getServer());

        }
        else if (settings.getDomain() != null & settings.getDomain().length() > 0)
        {
            secondRowTextBuffer.append(settings.getDomain());
        }


        if (settings.getPort() != 5222 && settings.getPort() != 0)
            secondRowTextBuffer.append(':').append(settings.getPort());


        secondRowText = secondRowTextBuffer.toString();
        return secondRowText;
    }

    public Long getAccountID ()
    {
        return mAccountId;
    }


    private String getPresenceString(Cursor cursor, Context context) {
        int presenceStatus = cursor.getInt(mAccountPresenceStatusColumn);

        switch (presenceStatus) {


        case Imps.Presence.AVAILABLE:
            return context.getString(R.string.presence_available);

        case Imps.Presence.IDLE:
            return context.getString(R.string.presence_idle);

        case Imps.Presence.AWAY:
            return context.getString(R.string.presence_away);

        case Imps.Presence.DO_NOT_DISTURB:

            return context.getString(R.string.presence_busy);

        case Imps.Presence.INVISIBLE:
            return context.getString(R.string.presence_invisible);

        default:
            return "";
        }
    }

    public interface SignInManager
    {
        public void signIn (long providerId, long accountId);
        public void signOut (long providerId, long accountId);
    }




}

