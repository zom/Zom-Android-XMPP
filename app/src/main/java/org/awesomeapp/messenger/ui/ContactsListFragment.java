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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.ErrorResUtils;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.provider.Imps;

import java.io.IOException;

import im.zom.messenger.R;
import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

import org.awesomeapp.messenger.ImApp;

//import com.bumptech.glide.Glide;

public class ContactsListFragment extends Fragment {

    private ContactListRecyclerViewAdapter mAdapter = null;
    private Uri mUri;
    private MyLoaderCallbacks mLoaderCallbacks;
    private LoaderManager mLoaderManager;
    private int mLoaderId = 1001;
    private static RecyclerView mRecView;
    private View mEmptyView;
    String mSearchString = null;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.awesome_fragment_contacts_list, container, false);

        mRecView = (RecyclerView)view.findViewById(R.id.recyclerview);
        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity)getActivity()).inviteContact();
            }
        });
        /*
        VerticalRecyclerViewFastScroller fastScroller =
                (VerticalRecyclerViewFastScroller) view.findViewById(R.id.fast_scroller);
        SectionTitleIndicator sectionTitleIndicator =
                (SectionTitleIndicator) view.findViewById(R.id.fast_scroller_section_title_indicator);


        // Connect the recycler to the scroller (to let the scroller scroll the list)
        fastScroller.setRecyclerView(mRecView);

        // Connect the scroller to the recycler (to let the recycler scroll the scroller's handle)
        mRecView.setOnScrollListener(fastScroller.getOnScrollListener());

        // Connect the section indicator to the scroller
        fastScroller.setSectionIndicator(sectionTitleIndicator);
        */

        setupRecyclerView(mRecView);

        return view;
    }

    public int getContactCount ()
    {
        if (mAdapter != null)
            return mAdapter.getItemCount();
        else
            return 1;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        Uri baseUri = Imps.Contacts.CONTENT_URI;
        Uri.Builder builder = baseUri.buildUpon();
        mUri = builder.build();

        mLoaderManager = getLoaderManager();
        mLoaderCallbacks = new MyLoaderCallbacks();
        mLoaderManager.initLoader(mLoaderId, null, mLoaderCallbacks);

        Cursor cursor = null;
        mAdapter = new ContactListRecyclerViewAdapter(getActivity(),cursor);

        if (mAdapter.getItemCount() == 0) {
            mRecView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);

        }
        else {
            mRecView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);

        }

        /**
        // init swipe to dismiss logic
        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.RIGHT, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // callback for drag-n-drop, false to skip this feature
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                final long itemId = mAdapter.getItemId( viewHolder.getAdapterPosition());
                 final String address= ((ContactListRecyclerViewAdapter.ViewHolder)viewHolder).mAddress;

                Snackbar.make(mRecView, "Remove " + address + "?", Snackbar.LENGTH_LONG)
                        .setAction("Yes", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //if they click, then cancel timer that will be used to end the chat
                                deleteContact(itemId, address);

                            }
                        }).show();
            }
        });

        swipeToDismissTouchHelper.attachToRecyclerView(recyclerView);
         */

    }

    private static void deleteContact (Activity activity, long itemId, String address, long providerId, long accountId)
    {

        try {

            IImConnection mConn;
            ImApp app = ((ImApp)activity.getApplication());
            mConn = app.getConnection(providerId, accountId);

            IContactListManager manager = mConn.getContactListManager();

            int res = manager.removeContact(address);
            if (res != ImErrorInfo.NO_ERROR) {
                //mHandler.showAlert(R.string.error,
                  //      ErrorResUtils.getErrorRes(getResources(), res, address));
            }

        }
        catch (RemoteException re)
        {

        }


    }

    public static class ContactListRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<ContactViewHolder> {

        private final TypedValue mTypedValue = new TypedValue();
        private int mBackground;
        private Context mContext;

        public ContactListRecyclerViewAdapter(Context context, Cursor cursor) {
            super(context,cursor);
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
            mBackground = mTypedValue.resourceId;
            mContext = context;


        }

        @Override
        public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


            ContactListItem view = (ContactListItem)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.contact_view, parent, false);
            view.setBackgroundResource(mBackground);

            ContactViewHolder holder = (ContactViewHolder)view.getTag();

            if (holder == null) {
                holder = new ContactViewHolder(view);
                holder.mLine1 = (TextView) view.findViewById(R.id.line1);
                holder.mLine2 = (TextView) view.findViewById(R.id.line2);

                holder.mAvatar = (ImageView)view.findViewById(R.id.avatar);
               // holder.mStatusIcon = (ImageView)view.findViewById(R.id.statusIcon);
               // holder.mStatusText = (TextView)view.findViewById(R.id.statusText);
                //holder.mEncryptionIcon = (ImageView)view.findViewById(R.id.encryptionIcon);

                holder.mSubBox = view.findViewById(R.id.subscriptionBox);
                holder.mButtonSubApprove = (Button)view.findViewById(R.id.btnApproveSubscription);
                holder.mButtonSubDecline = (Button)view.findViewById(R.id.btnDeclineSubscription);

                holder.mContainer = view.findViewById(R.id.message_container);

                // holder.mMediaThumb = (ImageView)findViewById(R.id.media_thumbnail);
                view.setTag(holder);
            }

            return holder;

        }

        @Override
        public void onBindViewHolder(final ContactViewHolder viewHolder, Cursor cursor) {

           viewHolder.mAddress =  cursor.getString(ContactListItem.COLUMN_CONTACT_USERNAME);
            String nickname =  cursor.getString(ContactListItem.COLUMN_CONTACT_NICKNAME);

            if (TextUtils.isEmpty(nickname))
            {
                nickname = viewHolder.mAddress.split("@")[0].split("\\.")[0];
            }
            else
            {
                nickname = nickname.split("@")[0].split("\\.")[0];
            }

            viewHolder.mNickname = nickname;

            viewHolder.mProviderId = cursor.getLong(ContactListItem.COLUMN_CONTACT_PROVIDER);
            viewHolder.mAccountId = cursor.getLong(ContactListItem.COLUMN_CONTACT_ACCOUNT);

            viewHolder.mView.bind(viewHolder, cursor,"", false, false);

            viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    /*
                    if (mContext instanceof ContactListActivity)
                        ((ContactListActivity)mContext).startChat(viewHolder.mProviderId, viewHolder.mAccountId, viewHolder.mAddress);
                    else if (mContext instanceof MainActivity)
                        ((MainActivity)mContext).startChat(viewHolder.mProviderId,viewHolder.mAccountId, viewHolder.mAddress);
                        */

                    Intent intent = new Intent(mContext,ContactDisplayActivity.class);
                    intent.putExtra("address", viewHolder.mAddress);
                    intent.putExtra("nickname", viewHolder.mNickname);
                    intent.putExtra("provider", viewHolder.mProviderId);
                    intent.putExtra("account", viewHolder.mAccountId);

                    mContext.startActivity(intent);

                }
            });

            viewHolder.mView.setOnLongClickListener(new View.OnLongClickListener()
            {

                @Override
                public boolean onLongClick(View view) {

                    if (mContext instanceof MainActivity) {

                        String message = mContext.getString(R.string.confirm_delete_contact,viewHolder.mNickname);

                        Snackbar.make(mRecView, message, Snackbar.LENGTH_LONG)
                                .setAction(mContext.getString(R.string.yes), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        //if they click, then cancel timer that will be used to end the chat

                                        deleteContact(((MainActivity) mContext), viewHolder.mView.getId(), viewHolder.mAddress, viewHolder.mProviderId, viewHolder.mAccountId);
                                    }
                                }).show();

                        return true;
                    }




                    return false;
                }
            });

        }

    }

    class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            StringBuilder buf = new StringBuilder();

            if (mSearchString != null) {
                buf.append('(');
                buf.append(Imps.Contacts.NICKNAME);
                buf.append(" LIKE ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
                buf.append(" OR ");
                buf.append(Imps.Contacts.USERNAME);
                buf.append(" LIKE ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
                buf.append(')');
                buf.append(" AND ");
            }

            buf.append(Imps.Contacts.TYPE).append('=').append(Imps.Contacts.TYPE_NORMAL);

            CursorLoader loader = new CursorLoader(getActivity(), mUri, CHAT_PROJECTION,
                    buf == null ? null : buf.toString(), null, Imps.Contacts.SUB_AND_ALPHA_SORT_ORDER);

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
            if (newCursor == null)
                return; // the app was quit or something while this was working
            newCursor.setNotificationUri(getActivity().getContentResolver(), mUri);

            mAdapter.changeCursor(newCursor);

            if (mRecView.getAdapter() == null)
                mRecView.setAdapter(mAdapter);

            if (mAdapter.getItemCount() == 0) {
                mRecView.setVisibility(View.GONE);
//                mEmptyView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.VISIBLE);

            }
            else {
                mRecView.setVisibility(View.VISIBLE);
  //              mEmptyView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.GONE);

            };


        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

            mAdapter.swapCursor(null);

        }

        public final String[] CHAT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
                Imps.Contacts.ACCOUNT, Imps.Contacts.USERNAME,
                Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                Imps.Contacts.SUBSCRIPTION_TYPE,
                Imps.Contacts.SUBSCRIPTION_STATUS,
                Imps.Presence.PRESENCE_STATUS,
                Imps.Presence.PRESENCE_CUSTOM_STATUS,
                Imps.Chats.LAST_MESSAGE_DATE,
                Imps.Chats.LAST_UNREAD_MESSAGE
        ///        Imps.Contacts.AVATAR_HASH,
           //     Imps.Contacts.AVATAR_DATA

        };


    }
}
