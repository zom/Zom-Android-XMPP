/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
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

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.OtrAndroidKeyManagerImpl;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.util.XmppUriHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import im.zom.messenger.R;


public class AddContactActivity extends BaseActivity {
    private static final String TAG = "AddContactActivity";

    private static final String[] CONTACT_LIST_PROJECTION = { Imps.ContactList._ID,
                                                             Imps.ContactList.NAME, };
    private static final int CONTACT_LIST_NAME_COLUMN = 1;

    private MultiAutoCompleteTextView mAddressList;
    //private Spinner mListSpinner;
  //  Button mInviteButton;
    ImApp mApp;
    SimpleAlertHandler mHandler;

    private Cursor mCursorProviders;
    private long mProviderId, mAccountId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        mApp = (ImApp)getApplication();
     //   mApp.setAppTheme(this);
        mHandler = new SimpleAlertHandler(this);

        setContentView(R.layout.add_contact_activity);
//        setTitle(R.string.add_contact_title);

        TextView label = (TextView) findViewById(R.id.input_contact_label);
        label.setText(R.string.input_contact_label);

        mAddressList = (MultiAutoCompleteTextView) findViewById(R.id.email);
        mAddressList.setTokenizer(new Rfc822Tokenizer());
        mAddressList.addTextChangedListener(mTextWatcher);

        mAddressList.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    inviteBuddies();
                }
                return false;
            }
        });

  //      mListSpinner = (Spinner) findViewById(R.id.choose_list);

//        setupAccountSpinner();


        Intent intent = getIntent();
        String scheme = intent.getScheme();
        if (TextUtils.equals(scheme, "xmpp"))
        {
            addContactFromUri(intent.getData());
        }

        setupActions ();
    }


    private void setupActions ()
    {

        View btnInviteSms = findViewById(R.id.btnInviteSMS);
        btnInviteSms.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                ImApp app = ((ImApp)getApplication());

                String nickname = app.getDefaultNickname();
                if (nickname == null)
                    nickname = new XmppAddress(app.getDefaultUsername()).getUser();

                String inviteString = OnboardingManager.generateInviteMessage(AddContactActivity.this, nickname, app.getDefaultUsername(), app.getDefaultOtrKey());
                OnboardingManager.inviteSMSContact(AddContactActivity.this, null, inviteString);
            }

        });

        View btnInviteShare = findViewById(R.id.btnInviteShare);
        btnInviteShare.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {

                ImApp app = ((ImApp)getApplication());

                String nickname = app.getDefaultNickname();
                if (nickname == null)
                    nickname = new XmppAddress(app.getDefaultUsername()).getUser();

                String inviteString = OnboardingManager.generateInviteMessage(AddContactActivity.this,  nickname, app.getDefaultUsername(), app.getDefaultOtrKey());
                OnboardingManager.inviteShare(AddContactActivity.this, inviteString);

            }

        });

        View btnInviteQR = findViewById(R.id.btnInviteScan);
        btnInviteQR.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (hasCameraPermission()) {
                    ImApp app = ((ImApp) getApplication());

                    String nickname = app.getDefaultNickname();
                    if (nickname == null)
                        nickname = new XmppAddress(app.getDefaultUsername()).getUser();

                    String inviteString;
                    try {
                        inviteString = OnboardingManager.generateInviteLink(AddContactActivity.this, app.getDefaultUsername(), app.getDefaultOtrKey(), nickname);
                        OnboardingManager.inviteScan(AddContactActivity.this, inviteString);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        });


    }

    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    boolean hasCameraPermission () {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        if (permissionCheck == PackageManager.PERMISSION_DENIED)
        {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);

            return false;
        }
        else {

            return true;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mCursorProviders != null && (!mCursorProviders.isClosed()))
                mCursorProviders.close();
        
    }



    private void setupAccountSpinner ()
    {
        final Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;

        mCursorProviders = managedQuery(uri,  PROVIDER_PROJECTION,
        Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
        new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
        Imps.Provider.DEFAULT_SORT_ORDER);
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, mCursorProviders, 
                new String[] { 
                       Imps.Provider.ACTIVE_ACCOUNT_USERNAME
                       },
                new int[] { android.R.id.text1 });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // TODO Something is causing the managedQuery() to return null, use null guard for now
        if (mCursorProviders != null && mCursorProviders.getCount() > 0)
        {
            mCursorProviders.moveToFirst();
            mProviderId = mCursorProviders.getLong(PROVIDER_ID_COLUMN);
            mAccountId = mCursorProviders.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
        }

        /**
        mListSpinner.setAdapter(adapter);
        mListSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                if (mCursorProviders == null)
                    return;
                mCursorProviders.moveToPosition(arg2);
                mProviderId = mCursorProviders.getLong(PROVIDER_ID_COLUMN);
                mAccountId = mCursorProviders.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
             }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });
*/
    }
    
    public class ProviderListItemFactory implements LayoutInflater.Factory {
        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            if (name != null && name.equals(AccountListItem.class.getName())) {
            //    return new ProviderListItem(context, AddContactActivity.this, null);
                return new AccountListItem(context, attrs);
            }
            return null;
        }

    }

    private int searchInitListPos(Cursor c, String listName) {
        if (TextUtils.isEmpty(listName)) {
            return 0;
        }
        c.moveToPosition(-1);
        while (c.moveToNext()) {
            if (listName.equals(c.getString(CONTACT_LIST_NAME_COLUMN))) {
                return c.getPosition();
            }
        }
        return 0;
    }

    private String getDomain (long providerId)
    {
        //mDefaultDomain = Imps.ProviderSettings.getStringValue(getContentResolver(), mProviderId,
          //      ImpsConfigNames.DEFAULT_DOMAIN);
        ContentResolver cr = getContentResolver();
        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(providerId)}, null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                pCursor, cr, providerId, false /* don't keep updated */, null /* no handler */);

        String domain = settings.getDomain();//get domain of current user

        settings.close();
        pCursor.close();

        return domain;
    }

    void inviteBuddies() {
        Rfc822Token[] recipients = Rfc822Tokenizer.tokenize(mAddressList.getText());

        for (Rfc822Token recipient : recipients) {
            new AddContactAsyncTask(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), mApp).execute(recipient.getAddress(), null, null);
        }

        if (recipients.length > 0) {
            Intent intent = new Intent();
            intent.putExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME, recipients[0].getAddress());
            intent.putExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, mApp.getDefaultProviderId());
            setResult(RESULT_OK, intent);
        }

        finish();

    }

    private IContactList getContactList(IImConnection conn) {
        if (conn == null) {
            return null;
        }

        try {
            IContactListManager contactListMgr = conn.getContactListManager();
            String listName = "";//getSelectedListName();

            if (!TextUtils.isEmpty(listName)) {
                return contactListMgr.getContactList(listName);
            } else {
                // Use the default list
                List<IBinder> lists = contactListMgr.getContactLists();
                for (IBinder binder : lists) {
                    IContactList list = IContactList.Stub.asInterface(binder);
                    if (list.isDefault()) {
                        return list;
                    }
                }
                // No default list, use the first one as default list
                if (!lists.isEmpty()) {
                    return IContactList.Stub.asInterface(lists.get(0));
                }
                return null;
            }
        } catch (RemoteException e) {
            // If the service has died, there is no list for now.
            return null;
        }
    }

    /**
    private String getSelectedListName() {
        Cursor c = (Cursor) mListSpinner.getSelectedItem();
        return (c == null) ? null : c.getString(CONTACT_LIST_NAME_COLUMN);
    }*/

    private View.OnClickListener mButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mApp.callWhenServiceConnected(mHandler, new Runnable() {
                public void run() {
                    inviteBuddies();
                }
            });
        }
    };


    private View.OnClickListener mScanHandler = new View.OnClickListener() {
        public void onClick(View v) {
         //   new IntentIntegrator(AddContactActivity.this).initiateScan();

        }
    };

    private TextWatcher mTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {

        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // noop
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // noop
        }
    };

    private static void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<AddContactActivity> " + msg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        if (resultCode == RESULT_OK) {
            if (requestCode == OnboardingManager.REQUEST_SCAN) {

                ArrayList<String> resultScans = resultIntent.getStringArrayListExtra("result");
                for (String resultScan : resultScans)
                {

                    try {
                        if (resultScan.startsWith("xmpp:"))
                        {
                            String address = XmppUriHelper.parse(Uri.parse(resultScan)).get(XmppUriHelper.KEY_ADDRESS);
                            String fingerprint =  XmppUriHelper.getOtrFingerprint(resultScan);

                            new AddContactAsyncTask(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), mApp).execute(address, fingerprint);

                            Intent intent=new Intent();
                            intent.putExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME, address);
                            intent.putExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, mApp.getDefaultProviderId());
                            setResult(RESULT_OK, intent);

                        }
                        else {
                            //parse each string and if they are for a new user then add the user
                            String[] parts = OnboardingManager.decodeInviteLink(resultScan);
                            String address = parts[0];
                            String fingerprint = null, nickname = null;
                            if (parts.length > 1)
                                fingerprint = parts[1];
                            if (parts.length > 2)
                                nickname = parts[2];

                            new AddContactAsyncTask(mApp.getDefaultProviderId(), mApp.getDefaultAccountId(), mApp).execute(address, fingerprint, nickname);

                            Intent intent=new Intent();
                            intent.putExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME, address);
                            intent.putExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, mApp.getDefaultProviderId());
                            setResult(RESULT_OK, intent);
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

        finish();

    }

    /**
     * Implement {@code xmpp:} URI parsing according to the RFC: http://tools.ietf.org/html/rfc5122
     * @param uri the URI to be parsed
     */
    private void addContactFromUri(Uri uri) {
        Log.i(TAG, "addContactFromUri: " + uri + "  scheme: " + uri.getScheme());
        Map<String, String> parsedUri = XmppUriHelper.parse(uri);
        if (!parsedUri.containsKey(XmppUriHelper.KEY_ADDRESS)) {
            Toast.makeText(this, "error parsing address: " + uri, Toast.LENGTH_LONG).show();
            return;
        }
        String address = parsedUri.get(XmppUriHelper.KEY_ADDRESS);
        this.mAddressList.setText(address);
      //  this.mInviteButton.setBackgroundColor(R.drawable.btn_green);

        //store this for future use... ideally the user comes up as verified the first time!
        String fingerprint = parsedUri.get(XmppUriHelper.KEY_OTR_FINGERPRINT);
        if (!TextUtils.isEmpty(fingerprint)) {
            Log.i(TAG, "fingerprint: " + fingerprint);
            OtrAndroidKeyManagerImpl.getInstance(this).verifyUser(address, fingerprint);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private static final String[] PROVIDER_PROJECTION = {
                                                         Imps.Provider._ID,
                                                         Imps.Provider.NAME,
                                                         Imps.Provider.FULLNAME,
                                                         Imps.Provider.CATEGORY,
                                                         Imps.Provider.ACTIVE_ACCOUNT_ID,
                                                         Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                                                         Imps.Provider.ACTIVE_ACCOUNT_PW,
                                                         Imps.Provider.ACTIVE_ACCOUNT_LOCKED,
                                                         Imps.Provider.ACTIVE_ACCOUNT_KEEP_SIGNED_IN,
                                                         Imps.Provider.ACCOUNT_PRESENCE_STATUS,
                                                         Imps.Provider.ACCOUNT_CONNECTION_STATUS
                                                         
                                                        };

    static final int PROVIDER_ID_COLUMN = 0;
    static final int PROVIDER_NAME_COLUMN = 1;
    static final int PROVIDER_FULLNAME_COLUMN = 2;
    static final int PROVIDER_CATEGORY_COLUMN = 3;
    static final int ACTIVE_ACCOUNT_ID_COLUMN = 4;
    static final int ACTIVE_ACCOUNT_USERNAME_COLUMN = 5;
    static final int ACTIVE_ACCOUNT_PW_COLUMN = 6;
    static final int ACTIVE_ACCOUNT_LOCKED = 7;
    static final int ACTIVE_ACCOUNT_KEEP_SIGNED_IN = 8;
    static final int ACCOUNT_PRESENCE_STATUS = 9;
    static final int ACCOUNT_CONNECTION_STATUS = 10;
}
