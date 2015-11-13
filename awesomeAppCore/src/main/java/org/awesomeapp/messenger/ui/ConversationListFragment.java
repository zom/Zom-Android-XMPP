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
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

//import com.bumptech.glide.Glide;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.tasks.ChatSessionInitTask;

import info.guardianproject.otr.app.im.R;

public class ConversationListFragment extends Fragment {

    private MessageListRecyclerViewAdapter mAdapter = null;
    private Uri mUri;
    private MyLoaderCallbacks mLoaderCallbacks;
    private LoaderManager mLoaderManager;
    private int mLoaderId = 1001;
    private RecyclerView mRecView;

    private View mEmptyView;
    private View mEmptyViewImage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.awesome_fragment_message_list, container, false);

        mRecView =  (RecyclerView)view.findViewById(R.id.recyclerview);
        mEmptyView = view.findViewById(R.id.empty_view);


        mEmptyViewImage = view.findViewById(R.id.empty_view_image);
        mEmptyViewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity)getActivity()).inviteContact();
            }
        });

        setupRecyclerView(mRecView);
        return view;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        Uri baseUri = Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY;
        Uri.Builder builder = baseUri.buildUpon();
        mUri = builder.build();

        mLoaderManager = getLoaderManager();
        mLoaderCallbacks = new MyLoaderCallbacks();
        mLoaderManager.initLoader(mLoaderId, null, mLoaderCallbacks);

        Cursor cursor = null;
        mAdapter = new MessageListRecyclerViewAdapter(getActivity(),cursor);


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


                // callback for swipe to dismiss, removing item from data and adapter
                int position = viewHolder.getAdapterPosition();

                //delete / endchat
                //items.remove(viewHolder.getAdapterPosition());
                long itemId = mAdapter.getItemId(position);

                endConversation(itemId);

                Snackbar.make(mRecView, "You ended the conversation", Snackbar.LENGTH_LONG)
                     .setAction("UNDO", new View.OnClickListener() {
                         @Override
                         public void onClick(View v) {
                             //if they click, then cancel timer that will be used to end the chat
                         }
                     }).show();
            }
        });
        swipeToDismissTouchHelper.attachToRecyclerView(recyclerView);


        if (mAdapter.getItemCount() == 0) {
            mRecView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mEmptyViewImage.setVisibility(View.VISIBLE);

        }
        else {
            mRecView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mEmptyViewImage.setVisibility(View.GONE);

        }

    }

    private void endConversation (long itemId)
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, itemId);
        getActivity().getContentResolver().delete(chatUri,null,null);

    }

    public static class MessageListRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<MessageListRecyclerViewAdapter.ViewHolder> {

        private final TypedValue mTypedValue = new TypedValue();
        private int mBackground;
        private Context mContext;

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public final ConversationListItem mView;

            public ViewHolder(ConversationListItem view) {
                super(view);
                mView = view;
            }

        }

        public MessageListRecyclerViewAdapter(Context context, Cursor cursor) {
            super(context,cursor);
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
            mBackground = mTypedValue.resourceId;
            mContext = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ConversationListItem view = (ConversationListItem)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.conversation_view, parent, false);
            view.setBackgroundResource(mBackground);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {

            final long chatId =  cursor.getLong(ConversationListItem.COLUMN_CONTACT_ID);

            viewHolder.mView.bind(cursor, null, true, false);

            viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, ConversationDetailActivity.class);
                    intent.putExtra("id", chatId);
                    context.startActivity(intent);
                }
            });

            int providerId = cursor.getInt(ConversationListItem.COLUMN_CONTACT_PROVIDER);
            int accountId = cursor.getInt(ConversationListItem.COLUMN_CONTACT_ACCOUNT);
            int contactType = cursor.getInt(ConversationListItem.COLUMN_CONTACT_TYPE);
            String remoteAddress = cursor.getString(ConversationListItem.COLUMN_CONTACT_USERNAME);

           // new ChatSessionInitTask((ImApp)((Activity)mContext).getApplication(),providerId,accountId,contactType).execute(remoteAddress);


        }

    }

    String mSearchString = null;

    class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            StringBuilder buf = new StringBuilder();

            //search nickname, jabber id, or last message
            if (mSearchString != null) {

                buf.append(Imps.Contacts.NICKNAME);
                buf.append(" LIKE ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
                buf.append(" OR ");
                buf.append(Imps.Contacts.USERNAME);
                buf.append(" LIKE ");
                buf.append(" OR ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
                buf.append(Imps.Chats.LAST_UNREAD_MESSAGE);
                buf.append(" LIKE ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
            }

            CursorLoader loader = new CursorLoader(getActivity(), mUri, CHAT_PROJECTION,
                    buf == null ? null : buf.toString(), null, Imps.Contacts.TIME_ORDER);

            //     loader.setUpdateThrottle(10L);
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
                mEmptyView.setVisibility(View.VISIBLE);
                mEmptyViewImage.setVisibility(View.VISIBLE);

            }
            else {
                mRecView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mEmptyViewImage.setVisibility(View.GONE);

            }
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
