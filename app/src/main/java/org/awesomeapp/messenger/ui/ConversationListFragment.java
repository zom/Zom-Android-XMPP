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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

//import com.bumptech.glide.Glide;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.tasks.ChatSessionInitTask;
import org.awesomeapp.messenger.ui.widgets.ConversationViewHolder;

import im.zom.messenger.R;

public class ConversationListFragment extends Fragment {

    private ConversationListRecyclerViewAdapter mAdapter = null;
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

        //not set color
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        int themeColorBg = settings.getInt("themeColorBg",-1);
        view.setBackgroundColor(themeColorBg);


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
        mAdapter = new ConversationListRecyclerViewAdapter(getActivity(),cursor);


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

                Snackbar.make(mRecView, getString(R.string.action_archived), Snackbar.LENGTH_LONG).show();
            }
        });
        swipeToDismissTouchHelper.attachToRecyclerView(recyclerView);


        if (mAdapter.getItemCount() == 0) {
            mRecView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mEmptyViewImage.setVisibility(View.VISIBLE);

        }
        else if (mRecView.getVisibility() == View.GONE) {
            mRecView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mEmptyViewImage.setVisibility(View.GONE);

        }

    }

    private void endConversation (long itemId)
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, itemId);
        getActivity().getContentResolver().delete(chatUri, null, null);

    }

    public static class ConversationListRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<ConversationViewHolder> {

        private final TypedValue mTypedValue = new TypedValue();
        private int mBackground;
        private Context mContext;

        public ConversationListRecyclerViewAdapter(Context context, Cursor cursor) {
            super(context,cursor);
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
            mBackground = mTypedValue.resourceId;
            mContext = context;

            setHasStableIds(true);
        }

        public long getItemId (int position)
        {
            Cursor c = getCursor();
            c.moveToPosition(position);
            long chatId = c.getLong(ConversationListItem.COLUMN_CONTACT_ID);
            return chatId;
        }


        @Override
        public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ConversationListItem view = (ConversationListItem)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.conversation_view, parent, false);
            view.setBackgroundResource(mBackground);
            ConversationViewHolder viewHolder = (ConversationViewHolder)view.getTag();

            if (viewHolder == null) {
                viewHolder = new ConversationViewHolder(view);
                view.setTag(viewHolder);
                view.applyStyleColors(viewHolder);
            }

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ConversationViewHolder viewHolder, Cursor cursor) {

            if (TextUtils.isEmpty(mSearchString)) {

                final long chatId = cursor.getLong(ConversationListItem.COLUMN_CONTACT_ID);
                final String address = cursor.getString(ConversationListItem.COLUMN_CONTACT_USERNAME);
                final String nickname = cursor.getString(ConversationListItem.COLUMN_CONTACT_NICKNAME);

                final long providerId = cursor.getLong(ConversationListItem.COLUMN_CONTACT_PROVIDER);
                final long accountId = cursor.getLong(ConversationListItem.COLUMN_CONTACT_ACCOUNT);
                final int type = cursor.getInt(ConversationListItem.COLUMN_CONTACT_TYPE);
                final String lastMsg = cursor.getString(ConversationListItem.COLUMN_LAST_MESSAGE);

                long lastMsgDate = cursor.getLong(ConversationListItem.COLUMN_LAST_MESSAGE_DATE);
                final int presence = cursor.getInt(ConversationListItem.COLUMN_CONTACT_PRESENCE_STATUS);

                ((ConversationListItem) viewHolder.itemView).bind(viewHolder, chatId, providerId, accountId, address, nickname, type, lastMsg, lastMsgDate, presence, null, true, false);

                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, ConversationDetailActivity.class);
                        intent.putExtra("id", chatId);
                        intent.putExtra("address", address);
                        intent.putExtra("nickname", nickname);

                        context.startActivity(intent);
                    }
                });
            }
            else
            {
                final long chatId = cursor.getLong(cursor.getColumnIndexOrThrow(Imps.Messages.THREAD_ID));
                final String nickname = cursor.getString(cursor.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
                final String address = cursor.getString(cursor.getColumnIndexOrThrow(Imps.Messages.CONTACT));
                final String body = cursor.getString(cursor.getColumnIndexOrThrow(Imps.Messages.BODY));
                final long messageDate = cursor.getLong(cursor.getColumnIndexOrThrow(Imps.Messages.DATE));

                if (address != null) {
                    ((ConversationListItem) viewHolder.itemView).bind(viewHolder, chatId, -1, -1, address, nickname, -1, body, messageDate, -1, mSearchString, true, false);

                    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context context = v.getContext();
                            Intent intent = new Intent(context, ConversationDetailActivity.class);
                            intent.putExtra("id", chatId);
                            intent.putExtra("address", nickname);
                            intent.putExtra("nickname", nickname);

                            context.startActivity(intent);
                        }
                    });
                }
            }


        }



    }

    static String mSearchString = null;

    public void doSearch (String searchString)
    {
        mSearchString = searchString;

        if (mLoaderManager != null)
            mLoaderManager.restartLoader(mLoaderId, null, mLoaderCallbacks);

    }

    class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        private int mLastCount = 0;

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            StringBuilder buf = new StringBuilder();

            CursorLoader loader = null;

            //search nickname, jabber id, or last message
            if (!TextUtils.isEmpty(mSearchString)) {

                mUri = Imps.Messages.CONTENT_URI_MESSAGES_BY_SEARCH;

           //     buf.append("contacts." + Imps.Contacts.NICKNAME);
            //    buf.append(" LIKE ");
            //    DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
             //     buf.append(" OR ");
                buf.append(Imps.Messages.BODY);
                buf.append(" LIKE ");
                DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");

                loader = new CursorLoader(getActivity(), mUri, null,
                        buf == null ? null : buf.toString(), null, Imps.Messages.REVERSE_SORT_ORDER);
            }
            else
            {
                mUri = Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY;
                loader = new CursorLoader(getActivity(), mUri, CHAT_PROJECTION,
                        buf == null ? null : buf.toString(), null, Imps.Contacts.TIME_ORDER);
            }

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


            if (mLastCount == 0 && mAdapter.getItemCount() > 0)
            {
                mRecView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mEmptyViewImage.setVisibility(View.GONE);

            }
            else if (mAdapter.getItemCount() == 0) {
                mRecView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
                mEmptyViewImage.setVisibility(View.VISIBLE);

            }

            mLastCount = mAdapter.getItemCount();

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
      //          Imps.Contacts.AVATAR_HASH,
        //        Imps.Contacts.AVATAR_DATA

        };


    }


}
