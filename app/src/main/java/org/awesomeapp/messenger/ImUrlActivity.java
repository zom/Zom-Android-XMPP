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
package org.awesomeapp.messenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.awesomeapp.messenger.crypto.otr.OtrDataHandler;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.service.ImServiceConstants;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.tasks.ChatSessionInitTask;
import org.awesomeapp.messenger.ui.AccountViewFragment;
import org.awesomeapp.messenger.ui.ContactsPickerActivity;
import org.awesomeapp.messenger.ui.ConversationDetailActivity;
import org.awesomeapp.messenger.ui.LockScreenActivity;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.util.LogCleaner;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;
import org.awesomeapp.messenger.util.SystemServices.FileInfo;
import org.bouncycastle.crypto.tls.TlsExtensionsUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import im.zom.messenger.R;

import static org.awesomeapp.messenger.ui.ContactsPickerActivity.EXTRA_EXCLUDED_CONTACTS;
import static org.awesomeapp.messenger.ui.camera.ProofMode.PROOF_FILE_TAG;

public class ImUrlActivity extends Activity {
    private static final String TAG = "ImUrlActivity";

    private static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    private static final int REQUEST_CREATE_ACCOUNT = RESULT_FIRST_USER + 2;
    private static final int REQUEST_SIGNIN_ACCOUNT = RESULT_FIRST_USER + 3;
    private static final int REQUEST_START_MUC =  RESULT_FIRST_USER + 4;

    private String mProviderName;
    private String mToAddress;
    private String mFromAddress;
    private String mHost;

    private IImConnection mConn;
    private IChatSessionManager mChatSessionManager;

    private Uri mSendUri;
    private String mSendType;
    private String mSendText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        doOnCreate();
    }



    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    public void onDBLocked() {

        Intent intent = new Intent(getApplicationContext(), RouterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    void handleIntent() {

        ContentResolver cr = getContentResolver();

        long providerId = -1;
        long accountId = -1;

        Collection<IImConnection> listConns = ((ImApp)getApplication()).getActiveConnections();

        if (TextUtils.isEmpty(mHost))
        {
            mConn = listConns.iterator().next();
        }
        else {
            //look for active connections that match the host we need
            for (IImConnection conn : listConns) {


                try {
                    long connProviderId = conn.getProviderId();

                    Cursor cursor = cr.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(connProviderId)}, null);

                    Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                            cursor, cr, connProviderId, false /* don't keep updated */, null /* no handler */);

                    try {
                        String domainToCheck = settings.getDomain();

                        if (domainToCheck != null && domainToCheck.length() > 0 && mHost.contains(domainToCheck)) {
                            mConn = conn;
                            providerId = connProviderId;
                            accountId = conn.getAccountId();

                            break;
                        }
                    } finally {
                        settings.close();
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                }


            }
        }

        //nothing active, let's see if non-active connections match
        if (mConn == null) {

            Cursor cursorProvider = initProviderCursor();

            if (cursorProvider == null || cursorProvider.isClosed() || cursorProvider.getCount() == 0) {

                createNewAccount();
                return;
            } else {


                while (cursorProvider.moveToNext())
                {
                    //make sure there is a stored password
                    if (!cursorProvider.isNull(ACTIVE_ACCOUNT_PW_COLUMN)) {

                        long cProviderId = cursorProvider.getLong(PROVIDER_ID_COLUMN);
                        Cursor cursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(cProviderId)},null);

                        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                                cursor, cr, cProviderId, false /* don't keep updated */, null /* no handler */);

                        //does the conference host we need, match the settings domain for a logged in account
                        String domainToCheck = settings.getDomain();

                        if (domainToCheck != null && domainToCheck.length() > 0 && mHost.contains(domainToCheck))
                        {
                            providerId = cProviderId;
                            accountId = cursorProvider.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
                            mConn = ((ImApp)getApplication()).getConnection(providerId,accountId);


                            //now sign in
                            signInAccount(accountId, providerId, cursorProvider.getString(ACTIVE_ACCOUNT_PW_COLUMN));


                            settings.close();
                            cursorProvider.close();

                            return;

                        }

                        settings.close();

                    }

                }

                cursorProvider.close();





            }

        }

        if (mConn != null)
        {
            try {
                int state = mConn.getState();
                accountId = mConn.getAccountId();
                providerId = mConn.getProviderId();

                if (state < ImConnection.LOGGED_IN) {

                    Cursor cursorProvider = initProviderCursor();

                    while(cursorProvider.moveToNext())
                    {
                        if (cursorProvider.getLong(ACTIVE_ACCOUNT_ID_COLUMN) == accountId)
                        {
                            signInAccount(accountId, providerId, cursorProvider.getString(ACTIVE_ACCOUNT_PW_COLUMN));

                            try {
                                Thread.sleep (500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }//wait here for three seconds
                            mConn = ((ImApp)getApplication()).getConnection(providerId,accountId);

                            break;
                        }
                    }

                    cursorProvider.close();
                }

                if (state == ImConnection.LOGGED_IN || state == ImConnection.SUSPENDED) {

                    Uri data = getIntent().getData();

                    if (data.getScheme().equals("immu")) {
                        Toast.makeText(this, "immu: URI handling not yet implemented!", Toast.LENGTH_LONG).show();
                    } else if (!isValidToAddress()) {
                        showContactList(accountId);
                    } else {
                        openChat(providerId, accountId);
                    }



                }
            } catch (RemoteException e) {
                // Ouch!  Service died!  We'll just disappear.
                Log.w("ImUrlActivity", "Connection disappeared!");
                finish();
            }
        }
        else
        {
            createNewAccount();
            return;
        }
    }

    /*
    private void addAccount(long providerId) {
        Intent intent = new Intent(this, AccountActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
//        intent.putExtra(ImApp.EXTRA_INTENT_SEND_TO_USER, mToAddress);

        if (mFromAddress != null)
            intent.putExtra("newuser", mFromAddress + '@' + mHost);

        startActivity(intent);
    }*/

    private void editAccount(long accountId) {
        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        Intent intent = new Intent(this, AccountViewFragment.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(accountUri);
        intent.putExtra(ImApp.EXTRA_INTENT_SEND_TO_USER, mToAddress);
        startActivityForResult(intent,REQUEST_SIGNIN_ACCOUNT);
    }

    private void signInAccount(long accountId, long providerId, String password) {
        //editAccount(accountId);
        // TODO sign in?  security implications?
        SignInHelper signInHelper = new SignInHelper(this, new SimpleAlertHandler(this));
        signInHelper.setSignInListener(new SignInHelper.SignInListener() {
            public void connectedToService() {
            }
            public void stateChanged(int state, long accountId) {
                if (state == ImConnection.LOGGED_IN) {

                    mHandlerRouter.post(new Runnable()
                    {
                       public void run ()
                       {
                           handleIntent();
                       }
                    });

                }

            }
        });

        signInHelper.signIn(password, providerId, accountId, true);
    }

    private void showContactList(long accountId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Imps.Contacts.CONTENT_URI);
        intent.addCategory(ImApp.IMPS_CATEGORY);
        intent.putExtra("accountId", accountId);

        startActivity(intent);
    }

    private void openChat(long provider, long account) {
        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mToAddress);
            if (session == null) {
                session = manager.createChatSession(mToAddress,false);
            }

            Uri data = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, session.getId());
            Intent intent = new Intent(Intent.ACTION_VIEW, data);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS, mToAddress);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, provider);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, account);
            intent.addCategory(ImApp.IMPS_CATEGORY);
            startActivity(intent);
        } catch (RemoteException e) {
            // Ouch!  Service died!  We'll just disappear.
            Log.w("ImUrlActivity", "Connection disappeared!");
        }
    }

    private boolean resolveInsertIntent(Intent intent) {
        Uri data = intent.getData();

        if (data.getScheme().equals("ima"))
        {
            createNewAccount();

            return true;
        }
        return false;
    }

   // private static final String USERNAME_PATTERN = "^[a-z0-9_-]{3,15}$";

    //private static final String USERNAME_NON_LETTERS_UNICODE = "[^\\p{L}\\p{Nd}]+";
    //private static final String USERNAME_NON_LETTERS_ALPHANUM = "[\\d[^\\w]]+";
    private static final String USERNAME_ONLY_ALPHANUM = "[^A-Za-z0-9]";

    private boolean resolveIntent(Intent intent) {
        Uri data = intent.getData();
        mHost = data.getHost();

        if (data.getScheme().equals("https") && mHost.equals("zom.im"))
        {
            //special zom.im invite link: https://zom.im/invite/<base64 encoded username?k=otrFingerprint

            try {
                //parse each string and if they are for a new user then add the user
                OnboardingManager.DecodedInviteLink diLink = OnboardingManager.decodeInviteLink(data.toString());
                ImApp app = (ImApp)getApplication();
                app.initAccountInfo();

                new AddContactAsyncTask(app.getDefaultProviderId(), app.getDefaultAccountId(), (ImApp)getApplication()).executeOnExecutor(ImApp.sThreadPoolExecutor,diLink.username,diLink.fingerprint,diLink.nickname);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("newcontact",diLink.username);
                setResult(RESULT_OK,resultIntent);

                //if they are for a group chat, then add the group
                return false; //the work is done so we will finish!
            }
            catch (Exception e)
            {
                Log.w(ImApp.LOG_TAG, "error parsing QR invite link", e);
            }

        }
        else if (data.getScheme().equals("immu")) {
            mFromAddress = data.getUserInfo();

            //remove username non-letters
            mFromAddress = mFromAddress.replaceAll(USERNAME_ONLY_ALPHANUM, "").toLowerCase(Locale.ENGLISH);

            String chatRoom = null;

            if (data.getPathSegments().size() > 0)
            {
                chatRoom = data.getPathSegments().get(0);

                //replace chat room name non-letters with underscores
                chatRoom = chatRoom.replaceAll("[\\d[^\\w]]+", "_");

                mToAddress = chatRoom + '@' + mHost;

                mProviderName = findMatchingProvider(mHost);

                return true;
            }

            return false;

        }
        else if (data.getScheme().equals("otr-in-band")) {
            this.openOtrInBand(data, intent.getType());

            return true;
        }
        else if (data.getScheme().equals("xmpp")) {
            mToAddress = data.getSchemeSpecificPart();

            try {
                //parse each string and if they are for a new user then add the user
                String[] parts =  mToAddress.split("\\?subscribe&otr-fingerprint=");

                ImApp app = (ImApp)getApplication();
                app.initAccountInfo();

                String username =parts[0];
                String fingerprint = null;
                String nickname = null;

                if (parts.length > 1)
                    fingerprint = parts[1];
                if (parts.length > 2)
                    nickname = parts[2];

                new AddContactAsyncTask(app.getDefaultProviderId(), app.getDefaultAccountId(), (ImApp)getApplication()).executeOnExecutor(ImApp.sThreadPoolExecutor,username, fingerprint);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("newcontact",username);
                setResult(RESULT_OK,resultIntent);

                //if they are for a group chat, then add the group
                return false; //the work is done so we will finish!
            }
            catch (Exception e)
            {
                Log.w(ImApp.LOG_TAG, "error parsing QR invite link", e);
            }


        }

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("resolveIntent: host=" + mHost);
        }

        if (TextUtils.isEmpty(mHost)) {
            Set<String> categories = intent.getCategories();
            if (categories != null) {
                Iterator<String> iter = categories.iterator();
                if (iter.hasNext()) {
                    String category = iter.next();
                    String providerName = getProviderNameForCategory(category);
                    mProviderName = findMatchingProvider(providerName);
                    if (mProviderName == null) {
                        Log.w(ImApp.LOG_TAG, "resolveIntent: IM provider " + category
                                             + " not supported");
                        return false;
                    }
                }
            }

            mToAddress = data.getSchemeSpecificPart();
        } else {
            mProviderName = findMatchingProvider(mHost);

            if (mProviderName == null) {
                Log.w(ImApp.LOG_TAG, "resolveIntent: IM provider " + mHost + " not supported");
                return false;
            }

            String path = data.getPath();

            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG))
                log("resolveIntent: path=" + path);

            if (!TextUtils.isEmpty(path)) {
                int index;
                if ((index = path.indexOf('/')) != -1) {
                    mToAddress = path.substring(index + 1);
                }
            }
        }

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("resolveIntent: provider=" + mProviderName + ", to=" + mToAddress);
        }

        return true;
    }

    private String getProviderNameForCategory(String providerCategory) {
        return Imps.ProviderNames.XMPP;
    }

    private String findMatchingProvider(String provider) {
        if (TextUtils.isEmpty(provider)) {
            return null;
        }

//        if (provider.equalsIgnoreCase("xmpp"))
  //          return Imps.ProviderNames.XMPP;


        return "Jabber (XMPP)";
        //return Imps.ProviderNames.XMPP;
    }

    private boolean isValidToAddress() {
        if (TextUtils.isEmpty(mToAddress)) {
            return false;
        }

        if (mToAddress.indexOf('/') != -1) {
            return false;
        }

        return true;
    }

    private static void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ImUrlActivity> " + msg);
    }

    void createNewAccount() {

        String username = getIntent().getData().getUserInfo();
        String appCreateAcct = String.format(getString(R.string.allow_s_to_create_a_new_chat_account_for_s_),username);

        new AlertDialog.Builder(this)
        .setTitle(R.string.prompt_create_new_account_)
        .setMessage(appCreateAcct)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                mHandlerRouter.sendEmptyMessage(1);
                dialog.dismiss();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        })
        .create().show();
    }

    Handler mHandlerRouter = new Handler ()
    {

        @Override
        public void handleMessage(Message msg) {

            if (msg.what == 1)
            {
                Uri uriAccountData = getIntent().getData();

                if (uriAccountData.getScheme().equals("immu"))
                {
                    //need to generate proper IMA url for account setup
                    String randomJid = ((int)(Math.random()*1000))+"";
                    String regUser = mFromAddress + randomJid;
                    String regPass =  UUID.randomUUID().toString().substring(0,16);
                    String regDomain = mHost.replace("conference.", "");
                    uriAccountData = Uri.parse("ima://" + regUser + ':' + regPass + '@' + regDomain);
                }

                Intent intent = new Intent(ImUrlActivity.this, AccountViewFragment.class);
                intent.setAction(Intent.ACTION_INSERT);
                intent.setData(uriAccountData);
                startActivityForResult(intent,REQUEST_CREATE_ACCOUNT);

            }
            else if (msg.what == 2)
            {
                doOnCreate();
            }
        }

    };

    void openOtrInBand(final Uri data, final String type) {

        if (type != null)
            mSendType = type;
        else
            mSendType = SystemServices.getMimeType(data.toString());
        
        if (mSendType != null ) {
            
            mSendUri = data;
            startContactPicker();
            return;
        }
        /**
        else  if (data.toString().startsWith(OtrDataHandler.URI_PREFIX_OTR_IN_BAND))
        {
             String localUrl = data.toString().replaceFirst(OtrDataHandler.URI_PREFIX_OTR_IN_BAND, "");
             FileInfo info = null;
             if (TextUtils.equals(data.getAuthority(), "com.android.contacts")) {
                 info = SystemServices.getContactAsVCardFile(this, data);
             } else {
                 info = SystemServices.getFileInfoFromURI(ImUrlActivity.this, data);
             }
             if (info != null && info.file.exists()) {
                 mSendUri = Uri.fromFile(info.file);
                 mSendType = type != null ? type : info.type;
                 startContactPicker();
                 return;
             }
        }**/
        
        Toast.makeText(this, R.string.unsupported_incoming_data, Toast.LENGTH_LONG).show();
        finish(); // make sure not to show this Activity's blank white screen
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_CONTACTS) {

                String username = resultIntent.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (username != null) {
                    long providerId = resultIntent.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER, -1);
                    long accountId = resultIntent.getLongExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT, -1);

                    sendOtrInBand(username, providerId, accountId);

                    startChat(providerId, accountId, username, true);

                }
                else {

                    //send to multiple
                    ArrayList<String> usernames = resultIntent.getStringArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAMES);
                    if (usernames != null)
                    {
                        ArrayList<Integer> providers = resultIntent.getIntegerArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_PROVIDER);
                        ArrayList<Integer> accounts = resultIntent.getIntegerArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_ACCOUNT);

                        if (providers != null && accounts != null)
                            for (int i = 0; i < providers.size(); i++)
                            {
                                sendOtrInBand(usernames.get(i), providers.get(i), accounts.get(i));
                            }


                        if (usernames.size() > 1)
                            startActivity(new Intent(this,MainActivity.class));
                        else
                        {
                            startChat(providers.get(0), accounts.get(0), usernames.get(0), true);

                        }

                    }

                    finish();
                }


            }
            else if (requestCode == REQUEST_SIGNIN_ACCOUNT || requestCode == REQUEST_CREATE_ACCOUNT)
            {

                mHandlerRouter.postDelayed(new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        doOnCreate();
                    }
                }, 500);

            }

        } else {
            finish();
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
                        Intent intent = new Intent(ImUrlActivity.this, ConversationDetailActivity.class);
                        intent.putExtra("id", chatId);
                        startActivity(intent);
                    }

                    finish();

                    super.onPostExecute(chatId);
                }

            }.executeOnExecutor(ImApp.sThreadPoolExecutor,new Contact(new XmppAddress(username)));
    }

    private void sendOtrInBand(String username, long providerId, long accountId) {

        try
        {
            IImConnection conn = ((ImApp)getApplication()).getConnection(providerId,accountId);

            if (conn == null)
                return; //can't send without a connection

            mChatSessionManager = conn.getChatSessionManager();

            IChatSession session = getChatSession(username);

            if (mSendText != null)
                session.sendMessage(mSendText,false);
            else if (mSendUri != null)
            {

                try {


                        String offerId = UUID.randomUUID().toString();

                        if (SecureMediaStore.isVfsUri(mSendUri)) {


                            boolean sent = session.offerData(offerId, mSendUri.toString(),mSendType );

                            if (sent)
                                return;
                        }
                        else
                        {
                            InputStream is = getContentResolver().openInputStream(mSendUri);
                            String fileName = mSendUri.getLastPathSegment();
                            FileInfo importInfo = SystemServices.getFileInfoFromURI(this, mSendUri);
                            Uri vfsUri = null;

                            if (!TextUtils.isEmpty(importInfo.type)) {
                                if (importInfo.type.startsWith("image"))
                                    vfsUri = SecureMediaStore.resizeAndImportImage(this, session.getId() + "", mSendUri, importInfo.type);
                                else
                                    vfsUri = SecureMediaStore.importContent(session.getId() + "", fileName, is);

                                boolean sent = session.offerData(offerId, vfsUri.toString(), importInfo.type);
                                if (sent)
                                    return;
                            }
                        }


                } catch (Exception e) {

                    Log.e(TAG,"error sending external file",e);
                }

                Toast.makeText(this, R.string.unable_to_securely_share_this_file, Toast.LENGTH_LONG).show();

            }
        }
        catch (RemoteException e)
        {
            Log.e(TAG,"Error sending data",e);
        }

    }

    private IChatSession getChatSession(String username) {
        if (mChatSessionManager != null) {
            try {
                IChatSession session = mChatSessionManager.getChatSession(username);

                if (session == null)
                    session = mChatSessionManager.createChatSession(username,false);

                return session;
            } catch (RemoteException e) {
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }
        return null;
    }

    private void startContactPicker() {

        Intent i = new Intent(this, ContactsPickerActivity.class);
        i.putExtra(ContactsPickerActivity.EXTRA_SHOW_GROUPS,true);
        startActivityForResult(i, REQUEST_PICK_CONTACTS);

        /**
        Uri.Builder builder = Imps.Contacts.CONTENT_URI.buildUpon();

        Collection<IImConnection> listConns = ((ImApp)getApplication()).getActiveConnections();

        for (IImConnection conn : listConns)
        {
            try {
                mChatSessionManager = conn.getChatSessionManager();
                long mProviderId = conn.getProviderId();
                long mAccountId = conn.getAccountId();

                ContentUris.appendId(builder,  mProviderId);
                ContentUris.appendId(builder,  mAccountId);
                Uri data = builder.build();

                i.setData(data);
                ArrayList<String> extras = new ArrayList<>();
                extras.add("");
                i.putExtra(EXTRA_EXCLUDED_CONTACTS,extras);
                i.putExtra(ContactsPickerActivity.EXTRA_SHOW_GROUPS,true);

                break;

            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }**/
    }

    void showLockScreen() {
        Intent intent = new Intent(this, LockScreenActivity.class);
      //  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(RouterActivity.EXTRA_ORIGINAL_INTENT, getIntent());
        startActivity(intent);
        finish();

    }

    private void doOnCreate ()
    {
        Intent intent = getIntent();
        if (Intent.ACTION_INSERT.equals(intent.getAction())) {
            if (!resolveInsertIntent(intent)) {
                finish();
                return;
            }
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {

            Uri streamUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            String mimeType = intent.getType();
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (streamUri != null)
                openOtrInBand(streamUri, mimeType);
            else if (intent.getData() != null)
                openOtrInBand(intent.getData(), mimeType);
            else if (sharedText != null)
            {
                //do nothing for now :(
                mSendText = sharedText;

                startContactPicker();

            }
            else
                finish();

        } else if (Intent.ACTION_SENDTO.equals(intent.getAction())|| Intent.ACTION_VIEW.equals(intent.getAction())) {
            if (!resolveIntent(intent)) {
                finish();
                return;
            }

            if (TextUtils.isEmpty(mToAddress)) {
                LogCleaner.warn(ImApp.LOG_TAG, "<ImUrlActivity>Invalid to address");
              //  finish();
                return;
            }

            ImApp mApp = (ImApp)getApplication();

            if (mApp.serviceConnected())
                handleIntent();
            else
            {
                mApp.callWhenServiceConnected(new Handler(), new Runnable() {
                    public void run() {

                       handleIntent();
                    }
                });
                Toast.makeText(ImUrlActivity.this, R.string.starting_the_chatsecure_service_, Toast.LENGTH_LONG).show();

            }
        } else {
            finish();
        }
    }

    private Cursor initProviderCursor ()
    {
        Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;
       // uri = uri.buildUpon().appendQueryParameter(ImApp.CACHEWORD_PASSWORD_KEY, pkey).build();

        //just init the contentprovider db
        return getContentResolver().query(uri, PROVIDER_PROJECTION,
                Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
                new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);

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
