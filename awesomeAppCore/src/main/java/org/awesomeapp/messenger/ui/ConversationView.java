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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DataSetObserver;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Browser;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.java.otr4j.session.SessionStatus;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.IOtrChatSession;
import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.provider.ImpsAddressUtils;
import org.awesomeapp.messenger.service.IChatListener;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IContactListListener;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.service.ImServiceConstants;
import org.awesomeapp.messenger.ui.MessageListItem.DeliveryState;
import org.awesomeapp.messenger.ui.MessageListItem.EncryptionState;
import org.awesomeapp.messenger.ui.legacy.BrandingResources;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.legacy.Markup;
import org.awesomeapp.messenger.ui.legacy.PresenceUtils;
import org.awesomeapp.messenger.ui.legacy.SimpleAlertHandler;
import org.awesomeapp.messenger.ui.legacy.adapter.ChatListenerAdapter;
import org.awesomeapp.messenger.ui.stickers.Sticker;
import org.awesomeapp.messenger.ui.stickers.StickerGroup;
import org.awesomeapp.messenger.ui.stickers.StickerManager;
import org.awesomeapp.messenger.ui.stickers.StickerPagerAdapter;
import org.awesomeapp.messenger.ui.stickers.StickerSelectListener;
import org.awesomeapp.messenger.ui.widgets.RoundedAvatarDrawable;
import org.awesomeapp.messenger.util.Debug;
import org.awesomeapp.messenger.util.LogCleaner;
import org.awesomeapp.messenger.util.SystemServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import info.guardianproject.otr.app.im.R;

public class ConversationView {
    // This projection and index are set for the query of active chats
    public static final String[] CHAT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.ACCOUNT,
                                             Imps.Contacts.PROVIDER, Imps.Contacts.USERNAME,
                                             Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                             Imps.Presence.PRESENCE_STATUS,
                                             Imps.Chats.LAST_UNREAD_MESSAGE,
                                             Imps.Chats._ID,
                                             Imps.Contacts.SUBSCRIPTION_TYPE,
                                             Imps.Contacts.SUBSCRIPTION_STATUS,
                                             Imps.Contacts.AVATAR_DATA

    };

    public static final int CONTACT_ID_COLUMN = 0;
    public static final int ACCOUNT_COLUMN = 1;
    public static final int PROVIDER_COLUMN = 2;
    public static final int USERNAME_COLUMN = 3;
    public static final int NICKNAME_COLUMN = 4;
    public static final int TYPE_COLUMN = 5;
    public static final int PRESENCE_STATUS_COLUMN = 6;
    public static final int LAST_UNREAD_MESSAGE_COLUMN = 7;
    public static final int CHAT_ID_COLUMN = 8;
    public static final int SUBSCRIPTION_TYPE_COLUMN = 9;
    public static final int SUBSCRIPTION_STATUS_COLUMN = 10;
    public static final int AVATAR_COLUMN = 11;

    //static final int MIME_TYPE_COLUMN = 9;

    static final String[] INVITATION_PROJECT = { Imps.Invitation._ID, Imps.Invitation.PROVIDER,
                                                Imps.Invitation.SENDER, };
    static final int INVITATION_ID_COLUMN = 0;
    static final int INVITATION_PROVIDER_COLUMN = 1;
    static final int INVITATION_SENDER_COLUMN = 2;

    static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    static final StyleSpan STYLE_NORMAL = new StyleSpan(Typeface.NORMAL);

    Markup mMarkup;

    ConversationDetailActivity mActivity;
    ImApp mApp;
    private SimpleAlertHandler mHandler;
    IImConnection mConn;

    //private ImageView mStatusIcon;
   // private TextView mTitle;
    /*package*/RecyclerView mHistory;
    EditText mComposeMessage;
    private ImageButton mSendButton, mMicButton;
    private Button mButtonTalk;
    private ImageButton mButtonAttach;
    private View mViewAttach;

    private ImageView mButtonDeleteVoice;
    private View mViewDeleteVoice;

    private View mStatusWarningView;
    private TextView mWarningText;
    private ProgressBar mProgressTransfer;


    private ImageView mDeliveryIcon;
    private boolean mExpectingDelivery;

    private boolean mIsSelected = false;

    private SessionStatus mLastSessionStatus = null;
    private boolean mIsStartingOtr = false;
    private boolean mIsVerified = false;

    public void setSelected (boolean isSelected)
    {
        mIsSelected = isSelected;

        if (mIsSelected)
        {
          //  bindChat(mLastChatId);
            startListening();

            updateWarningView();
            mComposeMessage.requestFocus();
            userActionDetected();

            try
            {
                boolean isConnected = (mConn == null) ? false : mConn.getState() != ImConnection.SUSPENDED;

                if ((mLastSessionStatus == null || mLastSessionStatus == SessionStatus.PLAINTEXT) && isConnected) {


//                    boolean otrPolicyAuto = mNewChatActivity.getOtrPolicy() == OtrPolicy.OTRL_POLICY_ALWAYS
  //                          || this.mNewChatActivity.getOtrPolicy() == OtrPolicy.OPPORTUNISTIC;

                    boolean otrPolicyAuto = true;

                    if (mCurrentChatSession == null)
                        mCurrentChatSession = getChatSession();
                    if (mCurrentChatSession == null)
                        return;

                    IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();
                    
                    if (otrChatSession != null)
                    {
                        String remoteJID = otrChatSession.getRemoteUserId();
                        
                        boolean isChatSecure = (remoteJID != null && remoteJID.contains("ChatSecure"));
                            
                        if (otrPolicyAuto && isChatSecure) //if set to auto, and is chatsecure, then start encryption
                        {
                               //automatically attempt to turn on OTR after 1 second
                                mHandler.postDelayed(new Runnable (){
                                    public void run (){
                                        setOTRState(true);
                                        scheduleRequery(DEFAULT_QUERY_INTERVAL);

                                    }
                                 },100);
                        }
                    }

                }
            }
            catch (RemoteException re){}
        }
        else
        {
            stopListening();
        }

    }

    public void inviteContacts (ArrayList<String> invitees)
    {
        if (mConn == null)
            return;

        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mRemoteAddress);

            for (String invitee : invitees)
                session.inviteContact(invitee);
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error inviting contacts to group",e);
        }

    }

    private boolean checkConnection ()
    {
            if (mConn == null && mProviderId != -1) {
                mConn = mApp.getConnection(mProviderId, mAccountId);

                if (mConn == null)
                    return false;

            }

        return true;


    }

    public void setOTRState(boolean otrEnabled) {


        try {

            boolean isConnected = (mConn == null) ? false : mConn.getState() != ImConnection.SUSPENDED;

            if (isConnected)
            {
                if (mCurrentChatSession == null)
                    mCurrentChatSession = getChatSession();

                if (mCurrentChatSession != null)
                {
                    IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();

                    if (otrChatSession != null)
                    {

                        if (otrEnabled) {

                            otrChatSession.startChatEncryption();                  
                            mIsStartingOtr = true;
                            
                         //   Toast.makeText(getContext(),getResources().getString(R.string.starting_otr_chat), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            otrChatSession.stopChatEncryption();
                           // Toast.makeText(getContext(),getResources().getString(R.string.stopping_otr_chat), Toast.LENGTH_LONG).show();

                        }


                    }
                }

            }


            updateWarningView();

        }
        catch (RemoteException e) {
            Log.d(ImApp.LOG_TAG, "error getting remote activity", e);
        }


    }

    private ConversationRecyclerViewAdapter mMessageAdapter;
    private boolean isServiceUp;
    private IChatSession mCurrentChatSession;

    long mLastChatId=-1;
    String mRemoteNickname;
    String mRemoteAddress;
    RoundedAvatarDrawable mRemoteAvatar = null;
    Drawable mRemoteHeader = null;
    int mSubscriptionType;
    int mSubscriptionStatus;

    long mProviderId = -1;
    long mAccountId = -1;
    long mInvitationId;
    private Context mContext; // TODO
    private int mPresenceStatus;

    private int mViewType;

    private static final int VIEW_TYPE_CHAT = 1;
    private static final int VIEW_TYPE_INVITATION = 2;
    private static final int VIEW_TYPE_SUBSCRIPTION = 3;

    private static final long SHOW_TIME_STAMP_INTERVAL = 30 * 1000; // 15 seconds
    private static final long SHOW_DELIVERY_INTERVAL = 5 * 1000; // 5 seconds
    private static final long SHOW_MEDIA_DELIVERY_INTERVAL = 120 * 1000; // 2 minutes
    private static final long DEFAULT_QUERY_INTERVAL = 2000;
    private static final long FAST_QUERY_INTERVAL = 200;

    public SimpleAlertHandler getHandler() {
        return mHandler;
    }

    public int getType() {
        return mViewType;
    }

    private class RequeryCallback implements Runnable {
        public void run() {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("RequeryCallback");
            }
            requeryCursor();

        }
    }

    private RequeryCallback mRequeryCallback = null;


    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!(view instanceof MessageListItem)) {
                return;
            }

            URLSpan[] links = ((MessageListItem) view).getMessageLinks();
            if (links.length > 0) {

                final ArrayList<String> linkUrls = new ArrayList<String>(links.length);
                for (URLSpan u : links) {
                    linkUrls.add(u.getURL());
                }
                ArrayAdapter<String> a = new ArrayAdapter<String>(mActivity,
                        android.R.layout.select_dialog_item, linkUrls);
                AlertDialog.Builder b = new AlertDialog.Builder(mActivity);
                b.setTitle(R.string.select_link_title);
                b.setCancelable(true);
                b.setAdapter(a, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(linkUrls.get(which));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
                        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, mActivity.getPackageName());
                        mActivity.startActivity(intent);
                    }
                });
                b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                b.show();
            }
        }
    };

    private final static int PROMPT_FOR_DATA_TRANSFER = 9999;
    private final static int SHOW_DATA_PROGRESS = 9998;
    private final static int SHOW_DATA_ERROR = 9997;


    private IChatListener mChatListener = new ChatListenerAdapter() {
        @Override
        public boolean onIncomingMessage(IChatSession ses,
                org.awesomeapp.messenger.model.Message msg) {
//            scheduleRequery(FAST_QUERY_INTERVAL);
  //          updatePresenceDisplay();

            return mIsSelected;
        }

        @Override
        public void onContactJoined(IChatSession ses, Contact contact) {
          //  scheduleRequery(DEFAULT_QUERY_INTERVAL);
        }

        @Override
        public void onContactLeft(IChatSession ses, Contact contact) {
         //   scheduleRequery(DEFAULT_QUERY_INTERVAL);
        }

        @Override
        public void onSendMessageError(IChatSession ses,
                org.awesomeapp.messenger.model.Message msg, ImErrorInfo error) {
        //    scheduleRequery(FAST_QUERY_INTERVAL);
        }

        @Override
        public void onIncomingReceipt(IChatSession ses, String packetId) throws RemoteException {
            scheduleRequery(FAST_QUERY_INTERVAL);

        }

        @Override
        public void onStatusChanged(IChatSession ses) throws RemoteException {
            scheduleRequery(DEFAULT_QUERY_INTERVAL);
            updatePresenceDisplay();
        }


        @Override
        public void onIncomingFileTransfer(String transferFrom, String transferUrl) throws RemoteException {

            String[] path = transferUrl.split("/");
            String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);

            android.os.Message message = android.os.Message.obtain(null, PROMPT_FOR_DATA_TRANSFER, (int) (mProviderId >> 32),
                    (int) mProviderId, -1);
            message.getData().putString("from", transferFrom);
            message.getData().putString("file", sanitizedPath);
            mHandler.sendMessage(message);


            log("onIncomingFileTransfer: " + transferFrom + " @ " + transferUrl);

        }

        @Override
        public void onIncomingFileTransferProgress(String file, int percent)
                throws RemoteException {

            android.os.Message message = android.os.Message.obtain(null, SHOW_DATA_PROGRESS, (int) (mProviderId >> 32),
                    (int) mProviderId, -1);
            message.getData().putString("file", file);
            message.getData().putInt("progress", percent);

         //   scheduleRequery(FAST_QUERY_INTERVAL);

            mHandler.sendMessage(message);

            log ("onIncomingFileTransferProgress: " + file + " " + percent + "%");

        }

        @Override
        public void onIncomingFileTransferError(String file, String err) throws RemoteException {


            android.os.Message message = android.os.Message.obtain(null, SHOW_DATA_ERROR, (int) (mProviderId >> 32),
                    (int) mProviderId, -1);
            message.getData().putString("file", file);
            message.getData().putString("err", err);

            mHandler.sendMessage(message);

            log("onIncomingFileTransferProgress: " + file + " err: " + err);

        }


    };

    private void showPromptForData (String transferFrom, String filePath)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        builder.setTitle(mContext.getString(R.string.file_transfer));
        builder.setMessage(transferFrom + ' ' + mActivity.getString(R.string.wants_to_send_you_the_file)
                + " '" + filePath + "'. " + mActivity.getString(R.string.accept_transfer_));

        builder.setNeutralButton(R.string.button_yes_accept_all, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                try {
                    mCurrentChatSession.setIncomingFileResponse(true, true);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                dialog.dismiss();
            }

        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                try {
                    mCurrentChatSession.setIncomingFileResponse(true, false);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                dialog.dismiss();
            }

        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                try {
                    mCurrentChatSession.setIncomingFileResponse(false, false);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    private Runnable mUpdateChatCallback = new Runnable() {
        public void run() {
           // if (mCursor != null && mCursor.requery() && mCursor.moveToFirst()) {
                updateChat();
           // }
        }
    };

    private IContactListListener mContactListListener = new IContactListListener.Stub() {
        public void onAllContactListsLoaded() {
        }

        public void onContactChange(int type, IContactList list, Contact contact) {

           if (contact != null && contact.getPresence() != null)
               mPresenceStatus = contact.getPresence().getStatus();

        }

        public void onContactError(int errorType, ImErrorInfo error, String listName,
                Contact contact) {
        }

        public void onContactsPresenceUpdate(Contact[] contacts) {

            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("onContactsPresenceUpdate()");
            }

            for (Contact c : contacts) {
                if (c.getAddress().getBareAddress().equals(Address.stripResource(mRemoteAddress))) {

                    if (c != null && c.getPresence() != null)
                    {
                        mPresenceStatus = c.getPresence().getStatus();
                        updatePresenceDisplay();
                    }

                    mHandler.post(mUpdateChatCallback);
                    scheduleRequery(DEFAULT_QUERY_INTERVAL);
                    break;
                }
            }

            setGroupTitle();
        }
    };

    private boolean mIsListening;

    static final void log(String msg) {
        if (Debug.DEBUG_ENABLED)
            Log.d(ImApp.LOG_TAG, "<ChatView> " + msg);
    }

    public ConversationView(ConversationDetailActivity activity) {

        mActivity = activity;
        mContext = activity;

        mApp = (ImApp)mActivity.getApplication();
        mHandler = new ChatViewHandler(mActivity);

        initViews();
    }

    void registerForConnEvents() {
        mApp.registerForConnEvents(mHandler);
    }

    void unregisterForConnEvents() {
        mApp.unregisterForConnEvents(mHandler);
    }

    protected void initViews() {
      //  mStatusIcon = (ImageView) mActivity.findViewById(R.id.statusIcon);
     //   mDeliveryIcon = (ImageView) mActivity.findViewById(R.id.deliveryIcon);
       // mTitle = (TextView) mActivity.findViewById(R.id.title);
        mHistory = (RecyclerView) mActivity.findViewById(R.id.history);
        mHistory.setLayoutManager(new LinearLayoutManager(mHistory.getContext()));

        mComposeMessage = (EditText) mActivity.findViewById(R.id.composeMessage);
        mSendButton = (ImageButton) mActivity.findViewById(R.id.btnSend);
        mMicButton = (ImageButton) mActivity.findViewById(R.id.btnMic);
        mButtonTalk = (Button)mActivity.findViewById(R.id.buttonHoldToTalk);

        mButtonDeleteVoice = (ImageView)mActivity.findViewById(R.id.btnDeleteVoice);
        mViewDeleteVoice = mActivity.findViewById(R.id.viewDeleteVoice);

        mButtonDeleteVoice.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if(motionEvent.getAction() == MotionEvent.ACTION_MOVE)
                {
                    int resolvedColor = mHistory.getResources().getColor(android.R.color.holo_red_light);
                    mButtonDeleteVoice.setBackgroundColor(resolvedColor);
                }

                return false;
            }
        });


        mButtonAttach = (ImageButton) mActivity.findViewById(R.id.btnAttach);
        mViewAttach = mActivity.findViewById(R.id.attachPanel);

        mStatusWarningView = mActivity.findViewById(R.id.warning);
        mWarningText = (TextView) mActivity.findViewById(R.id.warningText);

        mProgressTransfer = (ProgressBar)mActivity.findViewById(R.id.progressTransfer);
       // mOtrSwitch = (CompoundButton)mActivity.findViewById(R.id.otrSwitch);

        mButtonAttach.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mViewAttach.getVisibility() == View.GONE) {
                    mViewAttach.setVisibility(View.VISIBLE);
                    // Check if no view has focus:
                    View view = mActivity.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
                else {
                    mViewAttach.setVisibility(View.GONE);
                    if (mStickerBox != null)
                        mStickerBox.setVisibility(View.GONE);
                }
            }

        });

        ((ImageButton) mActivity.findViewById(R.id.btnAttachPicture)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mActivity.startImagePicker();
            }

        });

        ((ImageButton) mActivity.findViewById(R.id.btnTakePicture)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mActivity.startPhotoTaker();
            }

        });

        ((ImageButton) mActivity.findViewById(R.id.btnAttachFile)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mActivity.startFilePicker();
            }

        });

        ((ImageButton) mActivity.findViewById(R.id.btnAttachSticker)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               showStickers();
            }

        });

        mMicButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //this is the tap to change to hold to talk mode
                if (mMicButton.getVisibility() == View.VISIBLE) {
                    mComposeMessage.setVisibility(View.GONE);
                    mMicButton.setVisibility(View.GONE);

                    // Check if no view has focus:
                    View view = mActivity.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }

                    mSendButton.setImageResource(R.drawable.ic_keyboard_black_36dp);
                    mSendButton.setVisibility(View.VISIBLE);
                    mButtonTalk.setVisibility(View.VISIBLE);

                }
            }

        });


        final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent e) {

                //this is for recording audio directly from one press
                mActivity.startAudioRecording();

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {


                if (mActivity.isAudioRecording()) {
                    boolean send = true;//inViewInBounds(mMicButton, (int) motionEvent.getX(), (int) motionEvent.getY());
                    mActivity.stopAudioRecording(send);
                }

                return super.onSingleTapUp(e);
            }
        });

        mMicButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);

            }
        });

        mButtonTalk.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch( View btnTalk , MotionEvent theMotion ) {
                switch ( theMotion.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        mActivity.startAudioRecording();
                        mButtonTalk.setText(mActivity.getString(R.string.recording_release));
                        mViewDeleteVoice.setVisibility(View.VISIBLE);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        boolean inBounds = inViewInBounds(btnTalk,(int)theMotion.getX(),(int)theMotion.getY());
                        if (!inBounds)
                            mButtonTalk.setText(mActivity.getString(R.string.recording_delete));
                        else {
                            mButtonTalk.setText(mActivity.getString(R.string.recording_release));
                            mViewDeleteVoice.setVisibility(View.VISIBLE);
                        }
                            break;
                    case MotionEvent.ACTION_UP:
                        mButtonTalk.setText(mActivity.getString(R.string.push_to_talk));
                        boolean send = inViewInBounds(btnTalk,(int)theMotion.getX(),(int)theMotion.getY());
                        mActivity.stopAudioRecording(send);
                        mViewDeleteVoice.setVisibility(View.GONE);

                        break;
                }
                return true;
            }
        });
        /**
        mHistory.setOnItemLongClickListener(new OnItemLongClickListener ()
        {

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {


             if (arg1 instanceof MessageView)
             {

                 String textToCopy = ((MessageView)arg1).getLastMessage();

                 int sdk = android.os.Build.VERSION.SDK_INT;
                 if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                     android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                     clipboard.setText(textToCopy); //
                 } else {
                     android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                     android.content.ClipData clip = android.content.ClipData.newPlainText("chat",textToCopy);
                     clipboard.setPrimaryClip(clip); //
                 }

                 Toast.makeText(mActivity, mContext.getString(R.string.toast_chat_copied_to_clipboard), Toast.LENGTH_SHORT).show();

                 return true;

             }

                return false;
            }

        });**/

        mWarningText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showVerifyDialog();
            }
        });

        mComposeMessage.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                            sendMessage();
                            return true;

                        case KeyEvent.KEYCODE_ENTER:
                            if (event.isAltPressed()) {
                                mComposeMessage.append("\n");
                                return true;
                            }
                    }

                }

                return false;
            }
        });

        mComposeMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    if (event.isAltPressed()) {
                        return false;
                    }
                }

                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && imm.isActive(v)) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                sendMessage();
                return true;
            }
        });

        // TODO: this is a hack to implement BUG #1611278, when dispatchKeyEvent() works with
        // the soft keyboard, we should remove this hack.
        mComposeMessage.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int before, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int after) {
                //log("TextWatcher: " + s);
                userActionDetected();
            }

            public void afterTextChanged(Editable s) {


            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (mComposeMessage.getVisibility() == View.VISIBLE)
                    sendMessage();
                else
                {
                    if (mLastSessionStatus == SessionStatus.PLAINTEXT)
                        mSendButton.setImageResource(R.drawable.ic_send_holo_light);
                    else if (mLastSessionStatus == SessionStatus.ENCRYPTED)
                        mSendButton.setImageResource(R.drawable.ic_send_secure);

                    mSendButton.setVisibility(View.GONE);
                    mButtonTalk.setVisibility(View.GONE);
                    mComposeMessage.setVisibility(View.VISIBLE);
                    mMicButton.setVisibility(View.VISIBLE);


                }
            }
        });

        Button btnApproveSubscription = (Button)mActivity.findViewById(R.id.btnApproveSubscription);
        btnApproveSubscription.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {

               // mNewChatActivity.approveSubscription(mProviderId, mRemoteAddress);

                mHandler.postDelayed(new Runnable () { public void run () {bindChat(mLastChatId); } }, 2000);


            }

        });

        Button btnDeclineSubscription = (Button)mActivity.findViewById(R.id.btnDeclineSubscription);
        btnDeclineSubscription.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View v) {

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        //    mNewChatActivity.declineSubscription(mProviderId, mRemoteAddress);

                    }
                }, 500);


            }

        });

        mMessageAdapter = new ConversationRecyclerViewAdapter(mActivity, null);
        mHistory.setAdapter(mMessageAdapter);

    }



    private boolean inViewInBounds(View view, int x, int y){
        Rect outRect = new Rect();
        int[] location = new int[2];

        view.getHitRect(outRect);

        return outRect.contains(x,y);
    }

    public void startListening() {
        if (!isServiceUp)
            return;
        mIsListening = true;

        /*
        if (mViewType == VIEW_TYPE_CHAT) {
            Cursor cursor = getMessageCursor();
            if (cursor == null) {
                long chatId = getChatId();
                if (chatId != -1)
                    startQuery(chatId);
            } else {
                //requeryCursor();
            }
        }*/

        registerChatListener();
        registerForConnEvents();

        updateWarningView();
    }

    public void stopListening() {
        //Cursor cursor = getMessageCursor();
        //if (cursor != null && (!cursor.isClosed())) {
         //   cursor.close();
       // }

        cancelRequery();
        unregisterChatListener();
        unregisterForConnEvents();
        mIsListening = false;
    }

    public void unbind() {
        stopListening();
    }


    void updateChat() {
        setViewType(VIEW_TYPE_CHAT);

//        updateSessionInfo();

        setStatusIcon();

        //n8fr8 + devrandom: commented out on 15 Oct 2013: we really do want the chat to update w/o a connection
        //so we can show message history in offline mode
        /*
        *
        if (!isServiceUp)
            return;

        IImConnection conn = mApp.getConnection(mProviderId);
        if (conn == null) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG))
                log("Connection has been signed out");

            return;
        }*/

        //mHistory.invalidate();
        checkConnection();

        startQuery(getChatId());
        // This is not needed, now that there is a ChatView per fragment.  It also causes a spurious detection of user action
        // on fragments adjacent to the current one, when they get initialized.
        //mComposeMessage.setText("");

        updateWarningView();
    }

    int mContactType = -1;

    private void updateSessionInfo(Cursor c) {

        if (c != null && (!c.isClosed()))
        {
            mProviderId = c.getLong(PROVIDER_COLUMN);
            mAccountId = c.getLong(ACCOUNT_COLUMN);
            mPresenceStatus = c.getInt(PRESENCE_STATUS_COLUMN);
            mContactType = c.getInt(TYPE_COLUMN);

            mRemoteNickname = c.getString(NICKNAME_COLUMN);
            mRemoteAddress = c.getString(USERNAME_COLUMN);

            mSubscriptionType = c.getInt(SUBSCRIPTION_TYPE_COLUMN);

            mSubscriptionStatus = c.getInt(SUBSCRIPTION_STATUS_COLUMN);
            if ((mSubscriptionType == Imps.Contacts.SUBSCRIPTION_TYPE_FROM)
                && (mSubscriptionStatus == Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING)) {
                bindSubscription(mProviderId, mRemoteAddress);
            }

            setGroupTitle();
        }

    }

    public String getTitle ()
    {
        return mRemoteNickname;

    }

    public RoundedAvatarDrawable getIcon ()
    {
        return mRemoteAvatar;
    }

    public Drawable getHeader ()
    {
        return mRemoteHeader;
    }

    private void updatePresenceDisplay ()
    {
        if (mRemoteAvatar == null)
            return;

        switch (mPresenceStatus) {
        case Presence.AVAILABLE:
            mRemoteAvatar.setBorderColor(mActivity.getResources().getColor(R.color.holo_green_light));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.IDLE:
            mRemoteAvatar.setBorderColor(mActivity.getResources().getColor(R.color.holo_green_dark));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.AWAY:
            mRemoteAvatar.setBorderColor(mActivity.getResources().getColor(R.color.holo_orange_light));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.DO_NOT_DISTURB:
            mRemoteAvatar.setBorderColor(mActivity.getResources().getColor(R.color.holo_red_dark));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.OFFLINE:
            mRemoteAvatar.setBorderColor(mActivity.getResources().getColor(R.color.holo_grey_light));
            mRemoteAvatar.setAlpha(100);
            break;


        default:
        }
    }


    private void setGroupTitle() {

        if (mContactType == Imps.Contacts.TYPE_GROUP) {

            mPresenceStatus = Presence.AVAILABLE;

            final String[] projection = { Imps.GroupMembers.NICKNAME };
            Uri memberUri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, mLastChatId);
            ContentResolver cr = mActivity.getContentResolver();
            Cursor c = cr.query(memberUri, projection, null, null, null);
            StringBuilder buf = new StringBuilder();

            if (c != null) {
                while (c.moveToNext()) {

                    String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
//                    int status = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_STATUS));
                    buf.append(nickname);
                    if (!c.isLast()) {
                        buf.append(',');
                    }
                }

            }

            if (buf.length() > 0)
                mRemoteNickname = buf.toString();

            c.close();
        }
    }


    private void setStatusIcon() {
        if (mContactType == Imps.Contacts.TYPE_GROUP) {
            // hide the status icon for group chat.
         //   mStatusIcon.setVisibility(GONE);
        } else {
          //  mStatusIcon.setVisibility(VISIBLE);
            BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
            int presenceResId = PresenceUtils.getStatusIconId(mPresenceStatus);
            //mStatusIcon.setImageDrawable(brandingRes.getDrawable(presenceResId));

        }
    }

    private void deleteChat ()
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mLastChatId);
        mActivity.getContentResolver().delete(chatUri,null,null);

    }

    public void bindChat(long chatId) {
        //log("bind " + this + " " + chatId);

        mLastChatId = chatId;

        Uri contactUri = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, chatId);
        Cursor c = mActivity.getContentResolver().query(contactUri, CHAT_PROJECTION, null, null, null);

        if (c == null)
            return;

        if (!c.moveToFirst()) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("Failed to query chat: " + chatId);
            }
            mLastChatId = -1;

            c.close();

        } else {

            updateSessionInfo(c);

            if (mRemoteAvatar == null)
            {
                try {mRemoteAvatar = DatabaseUtils.getAvatarFromCursor(c, AVATAR_COLUMN, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT);}
                catch (Exception e){}

                if (mRemoteAvatar == null)
                {
                    mRemoteAvatar = new RoundedAvatarDrawable(BitmapFactory.decodeResource(mActivity.getResources(),
                            R.drawable.avatar_unknown));

                }

                updatePresenceDisplay();

            }

            if (mRemoteHeader == null)
            {
                try {mRemoteHeader = DatabaseUtils.getHeaderImageFromCursor(c, AVATAR_COLUMN, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);}
                catch (Exception e){}
            }


            c.close();

            mCurrentChatSession = getChatSession();

            if (mCurrentChatSession == null)
                mCurrentChatSession = createChatSession();

            if (mCurrentChatSession != null) {
                isServiceUp = true;

            }

            updateChat();
        }

    }


    public void bindInvitation(long invitationId) {
        Uri uri = ContentUris.withAppendedId(Imps.Invitation.CONTENT_URI, invitationId);
        ContentResolver cr = mActivity.getContentResolver();
        Cursor cursor = cr.query(uri, INVITATION_PROJECT, null, null, null);
        try {
            if (!cursor.moveToFirst()) {
                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                    log("Failed to query invitation: " + invitationId);
                }
                //  mNewChatActivity.finish();
            } else {
                setViewType(VIEW_TYPE_INVITATION);

                mInvitationId = cursor.getLong(INVITATION_ID_COLUMN);
                mProviderId = cursor.getLong(INVITATION_PROVIDER_COLUMN);
                String sender = cursor.getString(INVITATION_SENDER_COLUMN);

                TextView mInvitationText = (TextView) mActivity.findViewById(R.id.txtInvitation);
                mInvitationText.setText(mContext.getString(R.string.invitation_prompt, sender));
              //  mNewChatActivity.setTitle(mContext.getString(R.string.chat_with, sender));
            }
        } finally {
            cursor.close();
        }


    }


    public void bindSubscription(long providerId, String from) {
        mProviderId = providerId;

      //  mRemoteAddressString = from;

        setViewType(VIEW_TYPE_SUBSCRIPTION);

        TextView text = (TextView) mActivity.findViewById(R.id.txtSubscription);
        String displayableAddr = ImpsAddressUtils.getDisplayableAddress(from);
        text.setText(mContext.getString(R.string.subscription_prompt, displayableAddr));
    //.displayableAdd    mNewChatActivity.setTitle(mContext.getString(R.string.chat_with, displayableAddr));

        mApp.dismissChatNotification(providerId, from);
    }


    private void setViewType(int type) {
        mViewType = type;
        if (type == VIEW_TYPE_CHAT) {
            mActivity.findViewById(R.id.invitationPanel).setVisibility(View.GONE);
            mActivity.findViewById(R.id.subscription).setVisibility(View.GONE);
            setChatViewEnabled(true);
        } else if (type == VIEW_TYPE_INVITATION) {
            //setChatViewEnabled(false);

           // mActivity.findViewById(R.id.invitationPanel).setVisibility(View.VISIBLE);
            mActivity.findViewById(R.id.btnAccept).requestFocus();
        } else if (type == VIEW_TYPE_SUBSCRIPTION) {
            //setChatViewEnabled(false);
           // mActivity.findViewById(R.id.subscription).setVisibility(View.VISIBLE);

            mActivity.findViewById(R.id.btnApproveSubscription).requestFocus();
        }
    }

    private void setChatViewEnabled(boolean enabled) {
        mComposeMessage.setEnabled(enabled);
        mSendButton.setEnabled(enabled);
        if (enabled) {
            // This can steal focus from the fragment that's i n front of the user
            //mComposeMessage.requestFocus();
        } else {
            mHistory.setAdapter(null);
        }

    }

    RecyclerView getHistoryView() {
        return mHistory;
    }

    private Uri mUri;
    private LoaderManager mLoaderManager;
    private int loaderId = 100001;

    private synchronized void startQuery(long chatId) {

        mUri = Imps.Messages.getContentUriByThreadId(chatId);

        mLoaderManager = mActivity.getSupportLoaderManager();

        if (mLoaderManager == null)
            mLoaderManager.initLoader(loaderId++, null, new MyLoaderCallbacks());
        else
            mLoaderManager.restartLoader(loaderId++, null, new MyLoaderCallbacks());

    }

    class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            CursorLoader loader = new CursorLoader(mActivity, mUri, null, null, null, Imps.Messages.DEFAULT_SORT_ORDER);

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {

            if (newCursor != null) {

                newCursor.setNotificationUri(mActivity.getApplicationContext().getContentResolver(), mUri);
                mMessageAdapter.swapCursor(new DeltaCursor(newCursor));

                if (!mMessageAdapter.isScrolling()) {

                    mHandler.post(new Runnable() {

                        public void run() {
                            if (mMessageAdapter.getItemCount() > 0) {
                                mHistory.getLayoutManager().scrollToPosition(mMessageAdapter.getItemCount() - 1);
                            }
                        }
                    });
                }

            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

            mMessageAdapter.swapCursor(null);

        }
    }

    void scheduleRequery(long interval) {


        if (mRequeryCallback == null) {
            mRequeryCallback = new RequeryCallback();
        } else {
            mHandler.removeCallbacks(mRequeryCallback);
        }

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("scheduleRequery");
        }
        mHandler.postDelayed(mRequeryCallback, interval);


    }

    void cancelRequery() {

        if (mRequeryCallback != null) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("cancelRequery");
            }
            mHandler.removeCallbacks(mRequeryCallback);
            mRequeryCallback = null;
        }
    }


    void requeryCursor() {

        mLoaderManager.restartLoader(loaderId++, null, new MyLoaderCallbacks());
        updateWarningView();

        /**
        if (mMessageAdapter.isScrolling()) {
            mMessageAdapter.setNeedRequeryCursor(true);
            return;
        }

        // This is redundant if there are messages in view, because the cursor requery will update everything.
        // However, if there are no messages, no update will trigger below, and we still want this to update.


        // TODO: async query?
        Cursor cursor = getMessageCursor();
        if (cursor != null) {
            cursor.requery();
        }*/
    }

    private Cursor getMessageCursor() {
        return mMessageAdapter == null ? null : mMessageAdapter.getCursor();
    }

    public void closeChatSession(boolean doDelete) {
        if (getChatSession() != null) {
            try {

                if (doDelete)
                    setOTRState(false);

                updateWarningView();
                getChatSession().leave();

            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }

        if (doDelete)
            deleteChat();

    }

    public void verifyScannedFingerprint (String scannedFingerprint)
    {
        try
        {
            IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();

            if (scannedFingerprint != null && scannedFingerprint.equalsIgnoreCase(otrChatSession.getRemoteFingerprint())) {
                verifyRemoteFingerprint();
            }
        }
        catch (RemoteException e)
        {
            LogCleaner.error(ImApp.LOG_TAG, "unable to perform manual key verification", e);
        }
    }

    public void showVerifyDialog() {
        if (getChatId() == -1)
            return;

        try {
            IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();
            if (otrChatSession == null) {
                return;
            }

            String localFingerprint = otrChatSession.getLocalFingerprint();
            String remoteFingerprint = otrChatSession.getRemoteFingerprint();
            if (TextUtils.isEmpty(localFingerprint) || TextUtils.isEmpty(remoteFingerprint)) {
                return;
            }

            StringBuffer message = new StringBuffer();
            message.append(mContext.getString(R.string.fingerprint_for_you)).append("\n")
                    .append(prettyPrintFingerprint(localFingerprint)).append("\n\n");
            message.append(mContext.getString(R.string.fingerprint_for_))
                    .append(otrChatSession.getRemoteUserId()).append("\n")
                    .append(prettyPrintFingerprint(remoteFingerprint)).append("\n\n");

            message.append(mContext.getString(R.string.are_you_sure_you_want_to_confirm_this_key_));

            new AlertDialog.Builder(mContext)
                    .setTitle(R.string.verify_key_)
                    .setMessage(message.toString())
                    .setPositiveButton(R.string.menu_verify_fingerprint,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    verifyRemoteFingerprint();
                                }
                            })
                    .setNegativeButton(R.string.menu_verify_secret,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    initSmpUI();
                                }
                            })
                    .setNeutralButton(R.string.menu_scan, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                          //  new IntentIntegrator(mNewChatActivity).initiateScan();

                        }
                    }).show();
        } catch (RemoteException e) {
            LogCleaner.error(ImApp.LOG_TAG, "unable to perform manual key verification", e);
        }
    }

    private void initSmpUI() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View viewSmp = inflater.inflate(R.layout.smp_question_dialog, null, false);

        if (viewSmp != null)
        {
            new AlertDialog.Builder(mContext).setTitle(mContext.getString(R.string.otr_qa_title)).setView(viewSmp)
                    .setPositiveButton(mContext.getString(R.string.otr_qa_send), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            EditText eiQuestion = (EditText) viewSmp.findViewById(R.id.editSmpQuestion);
                            EditText eiAnswer = (EditText) viewSmp.findViewById(R.id.editSmpAnswer);
                            String question = eiQuestion.getText().toString();
                            String answer = eiAnswer.getText().toString();
                            initSmp(question, answer);
                        }
                    }).setNegativeButton(mContext.getString(R.string.otr_qa_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
        }
    }

    private void initSmp(String question, String answer) {
        try {

            if (mCurrentChatSession != null)
            {
                IOtrChatSession iOtrSession = mCurrentChatSession.getOtrChatSession();
                iOtrSession.initSmpVerification(question, answer);
            }

        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init SMP", e);

        }
    }

    private void verifyRemoteFingerprint() {


        try {

            IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();
            otrChatSession.verifyKey(otrChatSession.getRemoteUserId());


        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init otr", e);

        }

        updateWarningView();


    }


    private static String prettyPrintFingerprint (String fingerprint)
    {
        StringBuffer spacedFingerprint = new StringBuffer();

        for (int i = 0; i + 8 <= fingerprint.length(); i+=8)
        {
            spacedFingerprint.append(fingerprint.subSequence(i,i+8));
            spacedFingerprint.append(' ');
        }

        return spacedFingerprint.toString();
    }

    public void blockContact() {
        // TODO: unify with codes in ContactListView
        DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    checkConnection();
                    mConn = mApp.getConnection(mProviderId,mAccountId);
                    IContactListManager manager = mConn.getContactListManager();
                    manager.blockContact(Address.stripResource(mRemoteAddress));
                  //  mNewChatActivity.finish();
                } catch (Exception e) {

                    mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                    LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
                }
            }
        };

        Resources r = mActivity.getResources();

        // The positive button is deliberately set as no so that
        // the no is the default value
        new AlertDialog.Builder(mContext).setTitle(R.string.confirm)
                .setMessage(r.getString(R.string.confirm_block_contact, mRemoteNickname))
                .setPositiveButton(R.string.yes, confirmListener) // default button
                .setNegativeButton(R.string.no, null).setCancelable(false).show();
    }

    public long getProviderId() {
        return mProviderId;
    }

    public long getAccountId() {
        return mAccountId;
    }

    public long getChatId() {
        return mLastChatId;
    }

    private IChatSession createChatSession() {

        try
        {
            checkConnection ();

            if (mConn != null) {
                    IChatSessionManager sessionMgr = mConn.getChatSessionManager();
                    if (sessionMgr != null) {

                        String remoteAddress = mRemoteAddress;
                        IChatSession session = null;

                        if (mContactType == Imps.Contacts.TYPE_GROUP)
                        {
                            session = sessionMgr.createMultiUserChatSession(remoteAddress,null,null, false);
                        }
                        else
                        {
                            remoteAddress = Address.stripResource(mRemoteAddress);

                            session = sessionMgr.createChatSession(remoteAddress,false);
                        }

                        return session;

                    }
            }

        } catch (Exception e) {

            //mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "issue getting chat session",e);
        }

        return null;
    }

    public IChatSession getChatSession() {

        try {

            if ( checkConnection ()) {

                if (mConn != null)
                {
                    IChatSessionManager sessionMgr = mConn.getChatSessionManager();
                    if (sessionMgr != null) {

                            IChatSession session = sessionMgr.getChatSession(Address.stripResource(mRemoteAddress));

                            return session;

                    }
                }
            }

        } catch (Exception e) {

            //mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "error getting chat session",e);
        }

        return null;
    }

    public boolean isGroupChat() {
        return this.mContactType == Imps.Contacts.TYPE_GROUP;
    }

    void sendMessage() {

        String msg = mComposeMessage.getText().toString();

        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }

        IChatSession session = getChatSession();

        if (session == null)
            session = createChatSession();

        if (session != null) {
            try {
                session.sendMessage(msg);
                mComposeMessage.setText("");
                mComposeMessage.requestFocus();
                //requeryCursor();
            } catch (RemoteException e) {

              //  mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            } catch (Exception e) {

              //  mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }
    }

    void registerChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("registerChatListener " + mLastChatId);
        }
        try {
            if (getChatSession() != null) {
                getChatSession().registerChatListener(mChatListener);
            }

            checkConnection();

            if (mConn != null)
            {
                IContactListManager listMgr = mConn.getContactListManager();
                listMgr.registerContactListListener(mContactListListener);

            }

        } catch (Exception e) {
            Log.w(ImApp.LOG_TAG, "<ChatView> registerChatListener fail:" + e.getMessage());
        }
    }

    void unregisterChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("unregisterChatListener " + mLastChatId);
        }
        try {
            if (getChatSession() != null) {
                getChatSession().unregisterChatListener(mChatListener);
            }
            checkConnection ();

            if (mConn != null) {
                IContactListManager listMgr = mConn.getContactListManager();
                listMgr.unregisterContactListListener(mContactListListener);
            }
        } catch (Exception e) {
            Log.w(ImApp.LOG_TAG, "<ChatView> unregisterChatListener fail:" + e.getMessage());
        }
    }

    void updateWarningView() {

        int visibility = View.GONE;
        int iconVisibility = View.GONE;
        String message = null;
        boolean isConnected;


        try {
            checkConnection();
            isConnected = (mConn == null) ? false : mConn.getState() == ImConnection.LOGGED_IN;

        } catch (Exception e) {

            isConnected = false;
        }

        if (this.isGroupChat())
        {
            //anything to do here?
            /*
            visibility = View.VISIBLE;
            message = getContext().getString(R.string.this_is_a_group_chat);
            mWarningText.setTextColor(Color.WHITE);
            mStatusWarningView.setBackgroundColor(Color.LTGRAY);
            */

            mButtonAttach.setVisibility(View.GONE);

            mSendButton.setImageResource(R.drawable.ic_send_holo_light);

            mComposeMessage.setHint(R.string.this_is_a_group_chat);


        }
        else if (mCurrentChatSession != null) {
            IOtrChatSession otrChatSession = null;

            try {
                otrChatSession = mCurrentChatSession.getOtrChatSession();

                //check if the chat is otr or not
                if (otrChatSession != null) {
                    try {
                        mLastSessionStatus = SessionStatus.values()[otrChatSession.getChatStatus()];
                    } catch (RemoteException e) {
                        Log.w("Gibber", "Unable to call remote OtrChatSession from ChatView", e);
                    }
                }


            } catch (RemoteException e) {
                LogCleaner.error(ImApp.LOG_TAG, "error getting OTR session in ChatView", e);
            }

            if (mContactType == Imps.Contacts.TYPE_GROUP) {
                message = "";
            }
            else if ((mSubscriptionType == Imps.Contacts.SUBSCRIPTION_TYPE_FROM)) {
                bindSubscription(mProviderId, mRemoteAddress);
                visibility = View.VISIBLE;
                //message = mContext.getString(R.string.contact_not_in_list_warning, mRemoteNickname);
                //mWarningText.setTextColor(Color.WHITE);
                //mStatusWarningView.setBackgroundColor(Color.DKGRAY);

            } else {

                visibility = View.GONE;

            }


            if (mLastSessionStatus == SessionStatus.PLAINTEXT) {

                mSendButton.setImageResource(R.drawable.ic_send_holo_light);
                mComposeMessage.setHint(R.string.compose_hint);


            }
            else if (mLastSessionStatus == SessionStatus.ENCRYPTED) {

                if (mIsStartingOtr) {
                    mIsStartingOtr = false; //it's started!
                }

                if (mSendButton.getVisibility() == View.GONE) {
                    mComposeMessage.setHint(R.string.compose_hint_secure);
                    mSendButton.setImageResource(R.drawable.ic_send_secure);
                }

                try
                {
                    String rFingerprint = otrChatSession.getRemoteFingerprint();
                    mIsVerified = otrChatSession.isKeyVerified(mRemoteAddress);

                }
                catch (RemoteException re){}


            } else if (mLastSessionStatus == SessionStatus.FINISHED) {

                mSendButton.setImageResource(R.drawable.ic_send_holo_light);
                mComposeMessage.setHint(R.string.compose_hint);

                mWarningText.setTextColor(Color.WHITE);
                mStatusWarningView.setBackgroundColor(Color.DKGRAY);
                message = mContext.getString(R.string.otr_session_status_finished);

                visibility = View.VISIBLE;
            }

        }

        if (!isConnected)
        {
          //  visibility = View.VISIBLE;
         //   iconVisibility = View.VISIBLE;
           // mWarningText.setTextColor(Color.WHITE);
           // mStatusWarningView.setBackgroundColor(Color.DKGRAY);
           // message = mContext.getString(R.string.disconnected_warning);
              mComposeMessage.setHint(R.string.error_suspended_connection);

        }

        mStatusWarningView.setVisibility(visibility);

        if (visibility == View.VISIBLE) {
            if (message != null && message.length() > 0)
            {
                mWarningText.setText(message);
                mWarningText.setVisibility(View.VISIBLE);
            }
            else
            {
                mWarningText.setVisibility(View.GONE);
            }
        }

//        mNewChatActivity.updateEncryptionMenuState();

    }

    public SessionStatus getOtrSessionStatus ()
    {
        return mLastSessionStatus;
    }

    public boolean isOtrSessionVerified ()
    {
        return mIsVerified;
    }

    public int getRemotePresence ()
    {
        return mPresenceStatus;
    }

    /*
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        userActionDetected();
        return mActivity.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            userActionDetected();
            return mActivity.dispatchTouchEvent(ev);
        } catch (ActivityNotFoundException e) {
           // if the user clicked a link, e.g. geo:60.17,24.829, and there is
            //  no app to handle that kind of link, catch the exception
            Toast.makeText(mActivity, R.string.error_no_app_to_handle_url, Toast.LENGTH_SHORT)
                    .show();
            return true;
        }
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        userActionDetected();
        return mActivity.dispatchTrackballEvent(ev);
    }*/

    private void userActionDetected() {
        // Check that we have a chat session and that our fragment is resumed
        // The latter filters out bogus TextWatcher events on restore from saved
        if (getChatSession() != null && mIsListening) {
            try {
                getChatSession().markAsRead();

              //  updateWarningView();

            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }

        toggleInputMode ();

    }

    private void toggleInputMode ()
    {
        if (mButtonTalk.getVisibility() == View.GONE) {
            if (mComposeMessage.getText().length() > 0 && mSendButton.getVisibility() == View.GONE) {
                mMicButton.setVisibility(View.GONE);
                mSendButton.setVisibility(View.VISIBLE);

            } else if (mComposeMessage.getText().length() == 0) {
                mMicButton.setVisibility(View.VISIBLE);
                mSendButton.setVisibility(View.GONE);

            }
        }
    }

    private final class ChatViewHandler extends SimpleAlertHandler {


        public ChatViewHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            long providerId = ((long) msg.arg1 << 32) | msg.arg2;
            if (providerId != mProviderId) {
                return;
            }

            switch (msg.what) {

            case ImApp.EVENT_CONNECTION_DISCONNECTED:
                log("Handle event connection disconnected.");
                updateWarningView();
                promptDisconnectedEvent(msg);
                return;
            case PROMPT_FOR_DATA_TRANSFER:
                showPromptForData(msg.getData().getString("from"),msg.getData().getString("file"));
                break;
            case SHOW_DATA_ERROR:

                String fileName = msg.getData().getString("file");
                String error = msg.getData().getString("err");

                Toast.makeText(mContext, "Error transferring file: " + error, Toast.LENGTH_LONG).show();
                mProgressTransfer.setVisibility(View.GONE);
                break;
            case SHOW_DATA_PROGRESS:

                int percent = msg.getData().getInt("progress");
                /**
                mProgressTransfer.setVisibility(View.VISIBLE);
                mProgressTransfer.setProgress(percent);
                mProgressTransfer.setMax(100);

                if (percent > 95)
                {
                    mProgressTransfer.setVisibility(View.GONE);
                    //requeryCursor();
                    mMessageAdapter.notifyDataSetChanged();
                    
                }*/

                break;
             default:
                 updateWarningView();
            }

            super.handleMessage(msg);
        }
    }

    public static class DeltaCursor implements Cursor {
        static final String DELTA_COLUMN_NAME = "delta";

        private Cursor mInnerCursor;
        private String[] mColumnNames;
        private int mDateColumn = -1;
        private int mDeltaColumn = -1;

        DeltaCursor(Cursor cursor) {
            mInnerCursor = cursor;

            String[] columnNames = cursor.getColumnNames();
            int len = columnNames.length;

            mColumnNames = new String[len + 1];

            for (int i = 0; i < len; i++) {
                mColumnNames[i] = columnNames[i];
                if (mColumnNames[i].equals(Imps.Messages.DATE)) {
                    mDateColumn = i;
                }
            }

            mDeltaColumn = len;
            mColumnNames[mDeltaColumn] = DELTA_COLUMN_NAME;

            //if (DBG) log("##### DeltaCursor constructor: mDeltaColumn=" +
            //        mDeltaColumn + ", columnName=" + mColumnNames[mDeltaColumn]);
        }

        public int getCount() {
            return mInnerCursor.getCount();
        }

        public int getPosition() {
            return mInnerCursor.getPosition();
        }

        public boolean move(int offset) {
            return mInnerCursor.move(offset);
        }

        public boolean moveToPosition(int position) {
            return mInnerCursor.moveToPosition(position);
        }

        public boolean moveToFirst() {
            return mInnerCursor.moveToFirst();
        }

        public boolean moveToLast() {
            return mInnerCursor.moveToLast();
        }

        public boolean moveToNext() {
            return mInnerCursor.moveToNext();
        }

        public boolean moveToPrevious() {
            return mInnerCursor.moveToPrevious();
        }

        public boolean isFirst() {
            return mInnerCursor.isFirst();
        }

        public boolean isLast() {
            return mInnerCursor.isLast();
        }

        public boolean isBeforeFirst() {
            return mInnerCursor.isBeforeFirst();
        }

        public boolean isAfterLast() {
            return mInnerCursor.isAfterLast();
        }

        public int getColumnIndex(String columnName) {
            if (DELTA_COLUMN_NAME.equals(columnName)) {
                return mDeltaColumn;
            }

            int columnIndex = mInnerCursor.getColumnIndex(columnName);
            return columnIndex;
        }

        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            if (DELTA_COLUMN_NAME.equals(columnName)) {
                return mDeltaColumn;
            }

            return mInnerCursor.getColumnIndexOrThrow(columnName);
        }

        public String getColumnName(int columnIndex) {
            if (columnIndex == mDeltaColumn) {
                return DELTA_COLUMN_NAME;
            }

            return mInnerCursor.getColumnName(columnIndex);
        }

        public int getColumnCount() {
            return mInnerCursor.getColumnCount() + 1;
        }

        public void deactivate() {
            mInnerCursor.deactivate();
        }

        public boolean requery() {
            return mInnerCursor.requery();
        }

        public void close() {
            mInnerCursor.close();
        }

        public boolean isClosed() {
            return mInnerCursor.isClosed();
        }

        public void registerContentObserver(ContentObserver observer) {
            mInnerCursor.registerContentObserver(observer);
        }

        public void unregisterContentObserver(ContentObserver observer) {
            mInnerCursor.unregisterContentObserver(observer);
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mInnerCursor.registerDataSetObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            mInnerCursor.unregisterDataSetObserver(observer);
        }

        public void setNotificationUri(ContentResolver cr, Uri uri) {
            mInnerCursor.setNotificationUri(cr, uri);
        }

        public boolean getWantsAllOnMoveCalls() {
            return mInnerCursor.getWantsAllOnMoveCalls();
        }

        public Bundle getExtras() {
            return mInnerCursor.getExtras();
        }

        public Bundle respond(Bundle extras) {
            return mInnerCursor.respond(extras);
        }

        public String[] getColumnNames() {
            return mColumnNames;
        }

        private void checkPosition() {
            int pos = mInnerCursor.getPosition();
            int count = mInnerCursor.getCount();

            if (-1 == pos || count == pos) {
                throw new CursorIndexOutOfBoundsException(pos, count);
            }
        }

        public byte[] getBlob(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return null;
            }

            return mInnerCursor.getBlob(column);
        }

        public String getString(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                long value = getDeltaValue();
                return Long.toString(value);
            }

            return mInnerCursor.getString(column);
        }

        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
            checkPosition();

            if (columnIndex == mDeltaColumn) {
                long value = getDeltaValue();
                String strValue = Long.toString(value);
                int len = strValue.length();
                char[] data = buffer.data;
                if (data == null || data.length < len) {
                    buffer.data = strValue.toCharArray();
                } else {
                    strValue.getChars(0, len, data, 0);
                }
                buffer.sizeCopied = strValue.length();
            } else {
                mInnerCursor.copyStringToBuffer(columnIndex, buffer);
            }
        }

        public short getShort(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return (short) getDeltaValue();
            }

            return mInnerCursor.getShort(column);
        }

        public int getInt(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return (int) getDeltaValue();
            }

            return mInnerCursor.getInt(column);
        }

        public long getLong(int column) {
            //if (DBG) log("DeltaCursor.getLong: column=" + column + ", mDeltaColumn=" + mDeltaColumn);
            checkPosition();

            if (column == mDeltaColumn) {
                return getDeltaValue();
            }

            return mInnerCursor.getLong(column);
        }

        public float getFloat(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return getDeltaValue();
            }

            return mInnerCursor.getFloat(column);
        }

        public double getDouble(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return getDeltaValue();
            }

            return mInnerCursor.getDouble(column);
        }

        public boolean isNull(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return false;
            }

            return mInnerCursor.isNull(column);
        }

        private long getDeltaValue() {
            int pos = mInnerCursor.getPosition();
            //Log.i(LOG_TAG, "getDeltaValue: mPos=" + mPos);

            long t2, t1;

            if (pos == getCount() - 1) {
                t1 = mInnerCursor.getLong(mDateColumn);
                t2 = System.currentTimeMillis();
            } else {
                mInnerCursor.moveToPosition(pos + 1);
                t2 = mInnerCursor.getLong(mDateColumn);
                mInnerCursor.moveToPosition(pos);
                t1 = mInnerCursor.getLong(mDateColumn);
            }

            return t2 - t1;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
		public int getType(int arg0) {
            return mInnerCursor.getType(arg0);
        }

        @TargetApi(19)
		@Override
        public Uri getNotificationUri() {
            return mInnerCursor.getNotificationUri();
        }

    }

    public class ConversationRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<ConversationRecyclerViewAdapter.ViewHolder> {

        private int mScrollState;
        private boolean mNeedRequeryCursor;

        private int mNicknameColumn;
        private int mBodyColumn;
        private int mDateColumn;
        private int mTypeColumn;
        private int mErrCodeColumn;
        private int mDeltaColumn;
        private int mDeliveredColumn;
        private int mMimeTypeColumn;
        private int mIdColumn;

        public ConversationRecyclerViewAdapter(Activity context, Cursor c) {
            super(context, c);
            if (c != null) {
                resolveColumnIndex(c);
            }
        }

        private void resolveColumnIndex(Cursor c) {
            mNicknameColumn = c.getColumnIndexOrThrow(Imps.Messages.NICKNAME);
            mBodyColumn = c.getColumnIndexOrThrow(Imps.Messages.BODY);
            mDateColumn = c.getColumnIndexOrThrow(Imps.Messages.DATE);
            mTypeColumn = c.getColumnIndexOrThrow(Imps.Messages.TYPE);
            mErrCodeColumn = c.getColumnIndexOrThrow(Imps.Messages.ERROR_CODE);
            mDeltaColumn = c.getColumnIndexOrThrow(DeltaCursor.DELTA_COLUMN_NAME);
            mDeliveredColumn = c.getColumnIndexOrThrow(Imps.Messages.IS_DELIVERED);
            mMimeTypeColumn = c.getColumnIndexOrThrow(Imps.Messages.MIME_TYPE);
            mIdColumn = c.getColumnIndexOrThrow(Imps.Messages._ID);
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor != null) {
                resolveColumnIndex(newCursor);
            }
            return super.swapCursor(newCursor);
        }


        @Override
        public int getItemViewType(int position) {

            Cursor c = getCursor();
            c.moveToPosition(position);
            int type = c.getInt(mTypeColumn);
            boolean isLeft = (type == Imps.MessageType.INCOMING_ENCRYPTED)||(type == Imps.MessageType.INCOMING)||(type == Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED);

            if (isLeft)
                return 0;
            else
                return 1;

        }

        public Cursor getItem (int position)
        {
            Cursor c = getCursor();
            c.moveToPosition(position);
            return c;
        }


        void setLinkifyForMessageView(MessageListItem messageView) {
            try {
                
                if (messageView == null)
                    return;
                
                ContentResolver cr = mActivity.getContentResolver();
                Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,
                        new String[] { Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE },
                        Imps.ProviderSettings.PROVIDER + "=?", new String[] { Long
                                .toString(Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS) },
                        null);
                Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                        pCursor, cr, Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS,
                        false /* keep updated */, null /* no handler */);
                
                if (settings != null)
                {
                    if (mConn !=null)
                        messageView.setLinkify(!mConn.isUsingTor() || settings.getLinkifyOnTor());
                    
                    settings.close();
                }
                
                if (pCursor != null)
                    pCursor.close();
                
            } catch (RemoteException e) {
                e.printStackTrace();
                messageView.setLinkify(false);
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public final View mView;

            public ViewHolder(View view) {
                super(view);
                mView = view;
            }

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;

            if (viewType == 0)
                view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_view_left, parent, false);
            else
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_view_right, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

            MessageListItem messageView = (MessageListItem) viewHolder.mView;

            setLinkifyForMessageView(messageView);

            int messageType = cursor.getInt(mTypeColumn);

            String nickname = isGroupChat() ? cursor.getString(mNicknameColumn) : mRemoteNickname;
            String mimeType = cursor.getString(mMimeTypeColumn);
            int id = cursor.getInt(mIdColumn);
            String body = cursor.getString(mBodyColumn);
            long delta = cursor.getLong(mDeltaColumn);
            boolean showTimeStamp = true;//(delta > SHOW_TIME_STAMP_INTERVAL);
            long timestamp = cursor.getLong(mDateColumn);

            Date date = showTimeStamp ? new Date(timestamp) : null;
            boolean isDelivered = cursor.getLong(mDeliveredColumn) > 0;
            long showDeliveryInterval = (mimeType == null) ? SHOW_DELIVERY_INTERVAL : SHOW_MEDIA_DELIVERY_INTERVAL;
            boolean showDelivery = ((System.currentTimeMillis() - timestamp) > showDeliveryInterval);

            DeliveryState deliveryState = DeliveryState.NEUTRAL;

            if (showDelivery && !isDelivered && mExpectingDelivery) {
                deliveryState = DeliveryState.UNDELIVERED;
            }
            else if (isDelivered)
            {
                deliveryState = DeliveryState.DELIVERED;
            }

            EncryptionState encState = EncryptionState.NONE;
            if (messageType == Imps.MessageType.INCOMING_ENCRYPTED)
            {
                messageType = Imps.MessageType.INCOMING;
                encState = EncryptionState.ENCRYPTED;
            }
            else if (messageType == Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED)
            {
                messageType = Imps.MessageType.INCOMING;
                 encState = EncryptionState.ENCRYPTED_AND_VERIFIED;
            }
            else if (messageType == Imps.MessageType.OUTGOING_ENCRYPTED)
            {
                messageType = Imps.MessageType.OUTGOING;
                encState = EncryptionState.ENCRYPTED;
            }
            else if (messageType == Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED)
            {
                messageType = Imps.MessageType.OUTGOING;
                 encState = EncryptionState.ENCRYPTED_AND_VERIFIED;
            }

            switch (messageType) {
            case Imps.MessageType.INCOMING:
                messageView.bindIncomingMessage(id, messageType, mRemoteAddress, nickname, mimeType, body, date, mMarkup, false, encState, isGroupChat(), mPresenceStatus);

                break;

            case Imps.MessageType.OUTGOING:
            case Imps.MessageType.POSTPONED:

                int errCode = cursor.getInt(mErrCodeColumn);
                if (errCode != 0) {
                    messageView.bindErrorMessage(errCode);
                } else {
                    messageView.bindOutgoingMessage(id, messageType, null, mimeType, body, date, mMarkup, false,
                            deliveryState, encState);
                }

                break;

            default:
                messageView.bindPresenceMessage(mRemoteAddress, messageType, isGroupChat(), false);
            }

           // updateWarningView();


            if (!mExpectingDelivery && isDelivered) {

                mExpectingDelivery = true;
            } else if (cursor.getPosition() == cursor.getCount() - 1) {
                /*
                // if showTimeStamp is false for the latest message, then set a timer to query the
                // cursor again in a minute, so we can update the last message timestamp if no new
                // message is received
                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                    log("delta = " + delta + ", showTs=" + showTimeStamp);
                }
                *//*
                if (!showDelivery) {
                    scheduleRequery(SHOW_DELIVERY_INTERVAL);
                } else if (!showTimeStamp) {
                    scheduleRequery(SHOW_TIME_STAMP_INTERVAL);
                } else {
                    cancelRequery();
                }*/
            }
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            int oldState = mScrollState;
            mScrollState = scrollState;

            if (getChatSession() != null) {
                try {
                    getChatSession().markAsRead();
                } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
                }
            }


            if (oldState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                if (mNeedRequeryCursor) {
                    requeryCursor();
                } else {
                    notifyDataSetChanged();
                }
            }
        }

        boolean isScrolling() {
            return mScrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING;
        }

        void setNeedRequeryCursor(boolean requeryCursor) {
            mNeedRequeryCursor = requeryCursor;
        }

    }

    public Cursor getMessageAtPosition(int position) {
        Object item = mMessageAdapter.getItem(position);

        return (Cursor) item;
    }

    public EditText getComposedMessage() {
        return mComposeMessage;
    }

    public void onServiceConnected() {
        if (!isServiceUp) {
            bindChat(mLastChatId);
            startListening();
        }

    }

    private static StickerManager sStickerManager = null;
    private ViewPager mStickerPager = null;
    private View mStickerBox = null;

    private void showStickers ()
    {
        if (mStickerPager == null)
        {

            initStickers();
            mStickerBox = mActivity.findViewById(R.id.stickerBox);
        }

        mStickerBox.setVisibility(mStickerBox.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private synchronized void initStickers ()
    {
        if (sStickerManager == null)
        {
            sStickerManager = StickerManager.getInstance(mContext);

            try {

                String basePath = "stickers/olo_and_shimi_1";
                AssetManager aMan = mActivity.getAssets();
                String[] filelist = aMan.list(basePath);

                String category = "Olo & Shimi";

                for (int i = 0; i < filelist.length; i++) {
                    Sticker sticker = new Sticker();
                    sticker.name = filelist[i];
                    sticker.category = category;
                    sticker.assetUri = Uri.parse(basePath + '/' +  filelist[i]);
                    sticker.res = mActivity.getResources();
                    sticker.emoticon =  filelist[i];

                    sStickerManager.addPattern(sticker.emoticon, sticker);
                    sStickerManager.addEmojiToCategory(category, sticker);
                }

                basePath = "stickers/zomkyi";
                filelist = aMan.list(basePath);

                category = "Zomkyi";

                for (int i = 0; i < filelist.length; i++) {
                    Sticker sticker = new Sticker();
                    sticker.name = filelist[i];
                    sticker.category = category;
                    sticker.assetUri = Uri.parse(basePath + '/' +  filelist[i]);
                    sticker.res = mActivity.getResources();
                    sticker.emoticon =  filelist[i];

                    sStickerManager.addPattern(sticker.emoticon, sticker);
                    sStickerManager.addEmojiToCategory(category, sticker);
                }



            }
            catch (Exception fe)
            {
                Log.e(ImApp.LOG_TAG,"could not load emoji definition",fe);
            }

        }


        mStickerPager = (ViewPager)mActivity.findViewById(R.id.stickerPager);

        Collection<StickerGroup> emojiGroups = sStickerManager.getEmojiGroups();

        if (emojiGroups.size() > 0)
        {
            StickerPagerAdapter emojiPagerAdapter = new StickerPagerAdapter(mActivity, new ArrayList<StickerGroup>(emojiGroups),
                    new StickerSelectListener() {
                        @Override
                        public void onStickerSelected(Sticker s) {

                            mActivity.handleSendDelete(s.assetUri,"image/png", false, false, true);
                         //   mActivity.handleSendData(Uri.parse(s.assetPath),"image/png");

                            mViewAttach.setVisibility(View.GONE);
                            showStickers();
                        }
                    });

            mStickerPager.setAdapter(emojiPagerAdapter);

        }


    }

}
