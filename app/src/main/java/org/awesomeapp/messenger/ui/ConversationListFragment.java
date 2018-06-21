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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.tasks.ChatSessionInitTask;
import org.awesomeapp.messenger.tasks.MigrateAccountTask;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.widgets.ConversationViewHolder;

import im.zom.messenger.R;

public class ConversationListFragment extends Fragment {

    private ConversationListRecyclerViewAdapter mAdapter = null;
    private Uri mUri;
    private MyLoaderCallbacks mLoaderCallbacks;
    private LoaderManager mLoaderManager;
    private int mLoaderId = 1001;
    private RecyclerView mRecView;

    private int mChatType = Imps.Chats.CHAT_TYPE_ACTIVE; //default chat type

    private View mEmptyView;
    private View mEmptyViewImage;

    private View mUpgradeView;
    private TextView mUpgradeDesc;
    private ImageView mUpgradeImage;
    private Button mUpgradeAction;

    private boolean mFilterArchive = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.awesome_fragment_message_list, container, false);

        mRecView =  (RecyclerView)view.findViewById(R.id.recyclerview);
        mEmptyView = view.findViewById(R.id.empty_view);

        mUpgradeView = view.findViewById(R.id.upgrade_view);
        mUpgradeImage = (ImageView)view.findViewById(R.id.upgrade_view_image);
        mUpgradeDesc = (TextView)view.findViewById(R.id.upgrade_view_text);
        mUpgradeAction = (Button)view.findViewById(R.id.upgrade_action);

        mUpgradeAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doUpgrade();
            }
        });




        mEmptyViewImage = view.findViewById(R.id.empty_view_image);
        mEmptyViewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity)getActivity()).inviteContact();
            }
        });

        setupRecyclerView(mRecView);

        //not set color
        /**
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        int themeColorBg = settings.getInt("themeColorBg",-1);
        view.setBackgroundColor(themeColorBg);
            */

        checkUpgrade();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLoaderManager != null)
            mLoaderManager.restartLoader(mLoaderId, null, mLoaderCallbacks);
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        Uri baseUri = Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY;
        Uri.Builder builder = baseUri.buildUpon();
        mUri = builder.build();

        if (mLoaderManager == null || mAdapter == null) {
            mLoaderManager = getLoaderManager();
            mLoaderCallbacks = new MyLoaderCallbacks();
            mLoaderManager.initLoader(mLoaderId, null, mLoaderCallbacks);

            Cursor cursor = null;
            mAdapter = new ConversationListRecyclerViewAdapter(getActivity(), cursor);
        }
        else
        {
            mRecView.setAdapter(mAdapter);
        }

        // init swipe to dismiss logic

        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
               ItemTouchHelper.RIGHT, ItemTouchHelper.RIGHT) {

            public static final float ALPHA_FULL = 1.0f;

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                // We only want the active item to change
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (viewHolder instanceof ConversationViewHolder) {
                        // Let the view holder know that this item is being moved or dragged
                        ConversationViewHolder itemViewHolder = (ConversationViewHolder) viewHolder;
                        itemViewHolder.onItemSelected();
                    }
                }

                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {


                    // Get RecyclerView item from the ViewHolder
                    View itemView = viewHolder.itemView;

                    Paint p = new Paint();
                    Bitmap icon;

                    if (dX > 0) {
            /* Note, ApplicationManager is a helper class I created
               myself to get a context outside an Activity class -
               feel free to use your own method */


                        if (mChatType == Imps.Chats.CHAT_TYPE_ARCHIVED)
                        {
                            icon = BitmapFactory.decodeResource(
                                    getActivity().getResources(), R.drawable.ic_unarchive_white_24dp);

                            p.setColor(getResources().getColor(R.color.holo_green_dark));


                        }
                        else
                        {
                            icon = BitmapFactory.decodeResource(
                                    getActivity().getResources(), R.drawable.ic_archive_white_24dp);

                            p.setARGB(255, 150, 150, 150);

                        }


                        // Draw Rect with varying right side, equal to displacement dX
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                                (float) itemView.getBottom(), p);

                        // Set the image icon for Right swipe
                        c.drawBitmap(icon,
                                (float) itemView.getLeft() + convertDpToPx(16),
                                (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight())/2,
                                p);
                    }
                    // Fade out the view as it is swiped out of the parent's bounds
                    final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            private int convertDpToPx(int dp){
                return Math.round(dp * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(ALPHA_FULL);

                if (viewHolder instanceof ConversationViewHolder) {
                    // Tell the view holder it's time to restore the idle state
                    ConversationViewHolder itemViewHolder = (ConversationViewHolder) viewHolder;
                    itemViewHolder.onItemClear();
                }
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // callback for drag-n-drop, false to skip this feature
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {


                // callback for swipe to dismiss, removing item from data and adapter
                int position = viewHolder.getAdapterPosition();

                final long itemId = mAdapter.getItemId(position);

                if (mChatType == Imps.Chats.CHAT_TYPE_ACTIVE) {
                    archiveConversation(itemId);

                    Snackbar snack = Snackbar.make(mRecView, getString(R.string.action_archived), Snackbar.LENGTH_LONG);
                    snack.setAction(R.string.action_undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            unarchiveConversation(itemId);
                        }
                    });
                    snack.show();
                }
                else
                {
                    unarchiveConversation(itemId);
                    Snackbar snack = Snackbar.make(mRecView, getString(R.string.action_unarchived), Snackbar.LENGTH_SHORT);
                    snack.show();
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
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


    public boolean getArchiveFilter ()
    {
        return mFilterArchive;
    }


    public void setArchiveFilter (boolean filterAchive)
    {
        mFilterArchive = filterAchive;

        if (mFilterArchive)
            mChatType = Imps.Chats.CHAT_TYPE_ARCHIVED;
        else
            mChatType = Imps.Chats.CHAT_TYPE_ACTIVE;

        if (mLoaderManager != null)
            mLoaderManager.restartLoader(mLoaderId, null, mLoaderCallbacks);
    }

    private void endConversation (long itemId)
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, itemId);
        getActivity().getContentResolver().delete(chatUri, null, null);

    }

    private void archiveConversation (long itemId)
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, itemId);
        ContentValues values = new ContentValues();
        values.put(Imps.Chats.CHAT_TYPE,Imps.Chats.CHAT_TYPE_ARCHIVED);
        getActivity().getContentResolver().update(chatUri,values,Imps.Chats.CONTACT_ID + "=" + itemId,null);

    }

    private void unarchiveConversation (long itemId)
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, itemId);
        ContentValues values = new ContentValues();
        values.put(Imps.Chats.CHAT_TYPE,Imps.Chats.CHAT_TYPE_ACTIVE);
        getActivity().getContentResolver().update(chatUri,values,Imps.Chats.CONTACT_ID + "=" + itemId,null);

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
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.conversation_view, parent, false);
            view.setBackgroundResource(mBackground);

            ConversationViewHolder viewHolder = (ConversationViewHolder)view.getTag();

            if (viewHolder == null) {
                viewHolder = new ConversationViewHolder(view);
                view.setTag(viewHolder);
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
                long lastReadDate = cursor.getLong(ConversationListItem.COLUMN_LAST_READ_DATE);
                int subscription = cursor.getInt(ConversationListItem.COLUMN_SUBSCRIPTION_STATUS);

                int chatType = cursor.getInt(ConversationListItem.COLUMN_CHAT_TYPE);
                boolean isMuted = chatType == Imps.Chats.CHAT_TYPE_MUTED;

                String lastMsgType = null;

                if ((!TextUtils.isEmpty(lastMsg)) && lastMsg.startsWith("vfs:")) {
                    if (lastMsg.endsWith(".jpg") || lastMsg.endsWith(".png") || lastMsg.endsWith(".gif"))
                        lastMsgType = "image/*";
                    else if (lastMsg.endsWith(".m4a") || lastMsg.endsWith(".3gp") || lastMsg.endsWith(".mp3"))
                        lastMsgType = "audio/*";
                    else if (lastMsg.endsWith("mp4"))
                        lastMsgType = "video/*";
                    else if (lastMsg.endsWith("apk"))
                        lastMsgType = "application/apk";
                    else if (lastMsg.endsWith("pdf"))
                        lastMsgType = "application/pdf";
                }

                ConversationListItem clItem = ((ConversationListItem)viewHolder.itemView.findViewById(R.id.convoitemview));

                clItem.bind(viewHolder, chatId, providerId, accountId, address, nickname, type, lastMsg, lastMsgDate, lastMsgType, presence, subscription, null, true, false, isMuted);

                if (lastReadDate != -1 && lastReadDate < lastMsgDate) {
                    SpannableStringBuilder ssb = new SpannableStringBuilder(viewHolder.mLine1.getText());
                    StyleSpan bold = new StyleSpan(Typeface.BOLD);
                    ForegroundColorSpan black = new ForegroundColorSpan(Color.BLACK);
                    ssb.setSpan(bold, 0, ssb.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(black, 0, ssb.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    viewHolder.mLine1.setText(ssb);
                }

                clItem.setOnClickListener(new View.OnClickListener() {
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
                final String messageType = cursor.getString(cursor.getColumnIndexOrThrow(Imps.Messages.MIME_TYPE));

                boolean isMuted = false;

                if (address != null) {

                    if (viewHolder.itemView instanceof  ConversationListItem) {
                        ((ConversationListItem) viewHolder.itemView).bind(viewHolder, chatId, -1, -1, address, nickname, -1, body, messageDate, messageType, -1,-1, mSearchString, true, false, isMuted);

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

                if (mFilterArchive)
                    buf.append(Imps.Chats.CHAT_TYPE + '=' + Imps.Chats.CHAT_TYPE_ARCHIVED);
                else
                    buf.append("(" + Imps.Chats.CHAT_TYPE + " IS NULL")
                            .append(" OR " + Imps.Chats.CHAT_TYPE + '=' + Imps.Chats.CHAT_TYPE_MUTED)
                            .append(" OR " + Imps.Chats.CHAT_TYPE + '=' + Imps.Chats.CHAT_TYPE_ACTIVE + ")");

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
                Imps.Chats.LAST_UNREAD_MESSAGE,
                Imps.Chats.LAST_READ_DATE,
                Imps.Chats.CHAT_TYPE
      //          Imps.Contacts.AVATAR_HASH,
        //        Imps.Contacts.AVATAR_DATA

        };


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    private void checkUpgrade ()
    {
        if (((ImApp)getActivity().getApplication()).needsAccountUpgrade())
        {
            mUpgradeView.setVisibility(View.VISIBLE);
        }

    }


    private MigrateAccountTask.MigrateAccountListener mMigrateTaskListener;

    private synchronized void doUpgrade () {

        if (mMigrateTaskListener == null) {

            mMigrateTaskListener = new MigrateAccountTask.MigrateAccountListener() {
                @Override
                public void migrateComplete(OnboardingAccount account) {

                    mUpgradeAction.setText(getString(R.string.upgrade_complete_action));
                    mUpgradeAction.setBackgroundColor(getResources().getColor(R.color.message_background_light));
                    mUpgradeAction.setTextColor(getResources().getColor(R.color.zom_primary));
                    mUpgradeAction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {


                            mUpgradeView.setVisibility(View.GONE);

                        }
                    });
                }

                @Override
                public void migrateFailed(long providerId, long accountId) {

                    mUpgradeDesc.setText(getString(R.string.upgrade_failed));
                    mUpgradeAction.setText(getString(R.string.upgrade_complete_action));
                    mUpgradeAction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            mUpgradeView.setVisibility(View.GONE);

                        }
                    });
                }
            };

            mUpgradeAction.setText(getString(R.string.upgrade_progress_action));
            mUpgradeAction.setBackgroundColor(getResources().getColor(R.color.message_background_dark));
            mUpgradeAction.setTextColor(getResources().getColor(R.color.message_background_light));

            mUpgradeAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //do nothing

                }
            });

            mUpgradeImage.setImageResource(R.drawable.olo_thinking);


            ((ImApp) getActivity().getApplication()).doUpgrade(getActivity(), mMigrateTaskListener);

        }
    }
}
