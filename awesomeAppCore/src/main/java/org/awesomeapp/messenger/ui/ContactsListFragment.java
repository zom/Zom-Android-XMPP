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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.ui.legacy.AddContactActivity;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.provider.Imps;

import java.io.IOException;

import info.guardianproject.otr.app.im.R;
import org.awesomeapp.messenger.ImApp;

//import com.bumptech.glide.Glide;

public class ContactsListFragment extends Fragment {

    private ContactListRecyclerViewAdapter mAdapter = null;
    private Uri mUri;
    private MyLoaderCallbacks mLoaderCallbacks;
    private LoaderManager mLoaderManager;
    private int mLoaderId = 1001;
    private RecyclerView mRecView;
    private View mEmptyViewImage;
    String mSearchString = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.awesome_fragment_contacts_list, container, false);

        mRecView = (RecyclerView)view.findViewById(R.id.recyclerview);
    //    mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyViewImage = view.findViewById(R.id.empty_view_image);

        setupRecyclerView(mRecView);

        setupActions (view);

// ...


        return view;
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
//            mEmptyView.setVisibility(View.VISIBLE);
            mEmptyViewImage.setVisibility(View.VISIBLE);

        }
        else {
            mRecView.setVisibility(View.VISIBLE);
  //          mEmptyView.setVisibility(View.GONE);
            mEmptyViewImage.setVisibility(View.GONE);

        }

    }

    private void setupActions (View view)
    {
        Button btnInviteSms = (Button)view.findViewById(R.id.btnInviteSMS);
        btnInviteSms.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                ImApp app = ((ImApp)getActivity().getApplication());
                String nickname = new XmppAddress(app.getDefaultUsername()).getUser();
                String inviteString = OnboardingManager.generateInviteMessage(getActivity(), nickname, app.getDefaultUsername(), app.getDefaultOtrKey());
                OnboardingManager.inviteSMSContact(getActivity(), null, inviteString);
            }

        });

        Button btnInviteShare = (Button)view.findViewById(R.id.btnInviteShare);
        btnInviteShare.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {

                ImApp app = ((ImApp)getActivity().getApplication());

                String nickname = new XmppAddress(app.getDefaultUsername()).getUser();

                String inviteString = OnboardingManager.generateInviteMessage(getActivity(),  nickname, app.getDefaultUsername(), app.getDefaultOtrKey());
                OnboardingManager.inviteShare(getActivity(), inviteString);

            }

        });

        Button btnInviteQR = (Button)view.findViewById(R.id.btnInviteScan);
        btnInviteQR.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
                ImApp app = ((ImApp)getActivity().getApplication());

                String inviteString;
                try {
                    inviteString = OnboardingManager.generateInviteLink(getActivity(), app.getDefaultUsername(), app.getDefaultOtrKey());
                    OnboardingManager.inviteScan(getActivity(), inviteString);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        });

        Button btnInviteAdd = (Button)view.findViewById(R.id.btnInviteAdd);
        btnInviteAdd.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {

                Intent i = new Intent(getActivity(), AddContactActivity.class);
                getActivity().startActivityForResult(i,MainActivity.REQUEST_ADD_CONTACT);
            }

        });

    }


    public static class ContactListRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<ContactListRecyclerViewAdapter.ViewHolder> {

        private final TypedValue mTypedValue = new TypedValue();
        private int mBackground;
        private Context mContext;

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public final ContactListItem mView;
            public long mProviderId = -1;

            public ViewHolder(ContactListItem view) {
                super(view);
                mView = view;
            }

        }

        public ContactListRecyclerViewAdapter(Context context, Cursor cursor) {
            super(context,cursor);
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
            mBackground = mTypedValue.resourceId;
            mContext = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ContactListItem view = (ContactListItem)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.contact_view, parent, false);
            view.setBackgroundResource(mBackground);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

            final String chatUsername =  cursor.getString(ContactListItem.COLUMN_CONTACT_USERNAME);

            viewHolder.mView.bind(cursor,"", false, false);
            viewHolder.mProviderId = cursor.getLong(ContactListItem.COLUMN_CONTACT_PROVIDER);

            final long chatProviderId = viewHolder.mProviderId;

            viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mContext instanceof ContactListActivity)
                        ((ContactListActivity)mContext).startChat(chatProviderId, chatUsername);
                    else if (mContext instanceof MainActivity)
                        ((MainActivity)mContext).startChat(chatProviderId, chatUsername);

                }
            });

        }

    }

    class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            StringBuilder buf = new StringBuilder();

            if (mSearchString != null) {

                buf.append(Imps.Contacts.NICKNAME);
                buf.append(" LIKE ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
                buf.append(" OR ");
                buf.append(Imps.Contacts.USERNAME);
                buf.append(" LIKE ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
            }

            CursorLoader loader = new CursorLoader(getActivity(), mUri, CHAT_PROJECTION,
                    buf == null ? null : buf.toString(), null, Imps.Contacts.ALPHA_SORT_ORDER);

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
                mEmptyViewImage.setVisibility(View.VISIBLE);

            }
            else {
                mRecView.setVisibility(View.VISIBLE);
  //              mEmptyView.setVisibility(View.GONE);
                mEmptyViewImage.setVisibility(View.GONE);

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
                Imps.Chats.LAST_UNREAD_MESSAGE,
                Imps.Contacts.AVATAR_HASH,
                Imps.Contacts.AVATAR_DATA

        };


    }
}
