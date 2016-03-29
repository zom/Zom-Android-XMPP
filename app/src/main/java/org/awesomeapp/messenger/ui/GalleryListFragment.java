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

import org.awesomeapp.messenger.provider.Imps;

import java.sql.Date;

import im.zom.messenger.R;

//import com.bumptech.glide.Glide;

public class GalleryListFragment extends Fragment {

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
                R.layout.awesome_fragment_gallery_list, container, false);

        mRecView =  (RecyclerView)view.findViewById(R.id.recyclerview);
        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyViewImage = view.findViewById(R.id.empty_view_image);

        setupRecyclerView(mRecView);
        return view;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        Uri baseUri = Imps.Messages.CONTENT_URI;
        Uri.Builder builder = baseUri.buildUpon();
        mUri = builder.build();

        mLoaderManager = getLoaderManager();
        mLoaderCallbacks = new MyLoaderCallbacks();
        mLoaderManager.initLoader(mLoaderId, null, mLoaderCallbacks);

        Cursor cursor = null;
        mAdapter = new MessageListRecyclerViewAdapter(getActivity(),cursor);

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

    public static class MessageListRecyclerViewAdapter
            extends CursorRecyclerViewAdapter<GalleryMediaViewHolder> {

        private final TypedValue mTypedValue = new TypedValue();
        private int mBackground;
        private Context mContext;


        public MessageListRecyclerViewAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
            mBackground = mTypedValue.resourceId;
            mContext = context;

            setHasStableIds(true);
        }

        @Override
        public long getItemId (int position)
        {
            Cursor c = getCursor();
            c.moveToPosition(position);
            long chatId =  c.getLong(3); //timestamp! id is first column
            return chatId;
        }


        @Override
        public GalleryMediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            GalleryListItem view = (GalleryListItem)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.gallery_item_view, parent, false);
            //view.setBackgroundResource(mBackground);

            GalleryMediaViewHolder viewHolder = (GalleryMediaViewHolder)view.getTag();

            if (viewHolder == null)
            {
                viewHolder =  new GalleryMediaViewHolder(view,view.getContext());
                view.setTag(viewHolder);
            }

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(GalleryMediaViewHolder viewHolder, Cursor cursor) {

            int id = cursor.getInt(0);
            String mimeType = "image/jpeg";
            String nickname = cursor.getString(1);
            String body = cursor.getString(2);
            java.util.Date ts = new Date(cursor.getLong(3));

            viewHolder.bind(id, mimeType, body, ts);

        }

    }

    String mSearchString = null;

    class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            StringBuilder buf = new StringBuilder();

            buf.append(Imps.Messages.MIME_TYPE);
            buf.append(" LIKE ");
            buf.append("'image/jpeg'");
            buf.append(" AND ").append(Imps.Messages.BODY).append(" IS NOT NULL) GROUP BY (").append(Imps.Messages.BODY); //GROUP BY

            CursorLoader loader = new CursorLoader(getActivity(), mUri, MESSAGE_PROJECTION,
                    buf == null ? null : buf.toString(), null, "maxDate DESC");

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
            else if (mRecView.getVisibility() == View.GONE) {
                mRecView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mEmptyViewImage.setVisibility(View.GONE);

            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

            mAdapter.swapCursor(null);

        }


        public final String[] MESSAGE_PROJECTION = {
                "MAX (" + Imps.Messages._ID + ") as _id",
                Imps.Messages.NICKNAME,
                Imps.Messages.BODY,
               "MAX (" + Imps.Messages.DATE + ") as maxDate"
        };


    }
}
