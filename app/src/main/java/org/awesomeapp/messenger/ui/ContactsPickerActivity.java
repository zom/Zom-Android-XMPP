/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v7.widget.SearchView;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.widgets.FlowLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import im.zom.messenger.R;

/** Activity used to pick a contact. */
public class ContactsPickerActivity extends BaseActivity {

    public final static String EXTRA_EXCLUDED_CONTACTS = "excludes";

    public final static String EXTRA_RESULT_USERNAME = "result";
    public final static String EXTRA_RESULT_USERNAMES = "results";

    public final static String EXTRA_RESULT_PROVIDER = "provider";
    public final static String EXTRA_RESULT_ACCOUNT = "account";
    public final static String EXTRA_RESULT_MESSAGE = "message";

    private int REQUEST_CODE_ADD_CONTACT = 9999;

    private ContactAdapter mAdapter;

    private MyLoaderCallbacks mLoaderCallbacks;

    private ContactListListener mListener = null;
    private Uri mUri = Imps.Contacts.CONTENT_URI;

    private Handler mHandler = new Handler();

    private String mExcludeClause;
    Uri mData;

    private String mSearchString;

    SearchView mSearchView = null;
    FlowLayout mSelectedContacts;
    View mLayoutContactSelect;
    View mLayoutGroupSelect;
    ListView mListView = null;

    // The loader's unique id. Loader ids are specific to the Activity or
    // Fragment in which they reside.
    private static final int LOADER_ID = 1;

    // The callbacks through which we will interact with the LoaderManager.
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_contact_picker_multi, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            actionMode.setTitle(R.string.add_people);
            MenuItem item = menu.findItem(R.id.action_start_chat);
            if (item != null) {
                item.setEnabled(mAdapter.getCurrentSelection().size() > 0);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.action_start_chat)
            {
                SparseBooleanArray checkedPos = mListView.getCheckedItemPositions();
                multiFinish(checkedPos);

                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mAdapter.clearSelection();
        }
    };


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((ImApp)getApplication()).setAppTheme(this);
        
        setContentView(R.layout.contacts_picker_activity);
        
        if (getIntent().getData() != null)
            mUri = getIntent().getData();

        mLayoutContactSelect = findViewById(R.id.layoutContactSelect);
        mLayoutGroupSelect = findViewById(R.id.layoutGroupSelect);
        mSelectedContacts = (FlowLayout) findViewById(R.id.flSelectedContacts);

        View btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setGroupMode(true);
            }
        });

        mListView = (ListView)findViewById(R.id.contactsList);
        setGroupMode(false);

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                multiStart(i);
                //getSupportActionBar().startActionMode(mActionModeCallback);

                return true;
            }
        });

        // Uncomment this to set as list view header instead.
        //((ViewGroup)mSelectedContacts.getParent()).removeView(mSelectedContacts);
        //mSelectedContacts.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
        //mListView.addHeaderView(mSelectedContacts);

        mListView.setOnItemClickListener(new OnItemClickListener ()
        {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (mListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
                    if (mAdapter.isPositionChecked(position)) {
                        mAdapter.removeSelection(position);
                    } else {
                        mAdapter.setSelection(position);
                    }
                    mListView.setItemChecked(position, mAdapter.isPositionChecked(position));
                }
                else {
                    Cursor cursor = (Cursor) mAdapter.getItem(position);
                    Intent data = new Intent();
                    data.putExtra(EXTRA_RESULT_USERNAME, cursor.getString(ContactListItem.COLUMN_CONTACT_USERNAME));
                    data.putExtra(EXTRA_RESULT_PROVIDER, cursor.getLong(ContactListItem.COLUMN_CONTACT_PROVIDER));
                    data.putExtra(EXTRA_RESULT_ACCOUNT, cursor.getLong(ContactListItem.COLUMN_CONTACT_ACCOUNT));

                    setResult(RESULT_OK, data);
                    finish();
                }
            }

        });

        doFilterAsync("");
    }

    private void multiStart (int i)
    {
        setGroupMode(true);
        if (i != -1) {
            mListView.setItemChecked(i, true);
            mAdapter.setSelection(i);
        }
    }

    private void setGroupMode(boolean groupMode) {
        mLayoutContactSelect.setVisibility(groupMode ? View.GONE : View.VISIBLE);
        mLayoutGroupSelect.setVisibility(groupMode ? View.VISIBLE : View.GONE);
        int newChoiceMode = (groupMode ? ListView.CHOICE_MODE_MULTIPLE : ListView.CHOICE_MODE_SINGLE);
        if (mListView.getChoiceMode() != newChoiceMode) {
            mListView.setChoiceMode(newChoiceMode);
            if (groupMode) {
                mActionMode = mListView.startActionMode(mActionModeCallback);
            } else if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }
        }
    }

    private void multiFinish (SparseBooleanArray positions)
    {

        ArrayList<String> users = new ArrayList<>();
        ArrayList<Integer> providers = new ArrayList<>();
        ArrayList<Integer> accounts = new ArrayList<>();

        for (int i = 0; i < positions.size(); i++)
        {
            if (positions.valueAt(i)) {
                Cursor cursor = (Cursor) mAdapter.getItem(positions.keyAt(i));

                users.add(cursor.getString(ContactListItem.COLUMN_CONTACT_USERNAME));
                providers.add((int) cursor.getLong(ContactListItem.COLUMN_CONTACT_PROVIDER));
                accounts.add((int) cursor.getLong(ContactListItem.COLUMN_CONTACT_ACCOUNT));
            }
        }

        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_RESULT_USERNAMES, users);
        data.putIntegerArrayListExtra(EXTRA_RESULT_PROVIDER, providers);
        data.putIntegerArrayListExtra(EXTRA_RESULT_ACCOUNT, accounts);
        setResult(RESULT_OK, data);
        finish();
    }



    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        if (response == RESULT_OK)
            if (request == REQUEST_CODE_ADD_CONTACT)
            {
                String newContact = data.getExtras().getString(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (newContact != null)
                {
                    Intent dataNew = new Intent();
                    
                    long providerId = data.getExtras().getLong(ContactsPickerActivity.EXTRA_RESULT_PROVIDER);

                    dataNew.putExtra(EXTRA_RESULT_USERNAME, newContact);
                    dataNew.putExtra(EXTRA_RESULT_PROVIDER, providerId);
                    setResult(RESULT_OK, dataNew);

                    finish();

                }
            }


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_list_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));

        if (mSearchView != null )
        {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setIconifiedByDefault(false);

            SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener()
            {
                public boolean onQueryTextChange(String newText)
                {
                    mSearchString = newText;
                    doFilterAsync(mSearchString);
                    return true;
                }

                public boolean onQueryTextSubmit(String query)
                {
                    mSearchString = query;
                    doFilterAsync(mSearchString);

                    return true;
                }
            };

            mSearchView.setOnQueryTextListener(queryTextListener);
        }



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void doFilterAsync (final String query)
    {

            doFilter(query);
    }

    boolean mAwaitingUpdate = false;

    public synchronized void doFilter(String filterString) {

        mSearchString = filterString;

        if (mAdapter == null) {

            mAdapter = new ContactAdapter(ContactsPickerActivity.this, R.layout.contact_view);
            mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    updateTagView();

                    // If multi select action mode, enable/disable "done" menu entry
                    if (mActionMode != null) {
                        MenuItem item = mActionMode.getMenu().findItem(R.id.action_start_chat);
                        if (item != null) {
                            item.setEnabled(mAdapter.getCurrentSelection().size() > 0);
                        }
                    }
                }
            });
           mListView.setAdapter(mAdapter);

            mLoaderCallbacks = new MyLoaderCallbacks();
            getSupportLoaderManager().initLoader(LOADER_ID, null, mLoaderCallbacks);
        } else {

            if (!mAwaitingUpdate)
            {
                mAwaitingUpdate = true;
                mHandler.postDelayed(new Runnable ()
                {

                    public void run ()
                    {

                        getSupportLoaderManager().restartLoader(LOADER_ID, null, mLoaderCallbacks);
                        mAwaitingUpdate = false;
                    }
                },1000);
            }

        }
    }

    private Cursor mCursor;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCursor != null && (!mCursor.isClosed()))
            mCursor.close();


    }

    private void updateTagView() {
        mSelectedContacts.removeAllViews();
        for (Integer index : mAdapter.getCurrentSelection()) {
            View view = createTagViewForIndex(index);
            mSelectedContacts.addView(view);
        }
    }

    private View createTagViewForIndex(int index) {
        Cursor cursor = (Cursor) mAdapter.getItem(index);

        View view = LayoutInflater.from(mSelectedContacts.getContext()).inflate(R.layout.picked_contact_item, mSelectedContacts, false);

        // TODO - Feel a little awkward to create a ContactListItem here just to use the binding code.
        // I guess we should move that somewhere else.
        ContactListItem cli = new ContactListItem(this, null);
        ContactViewHolder cvh = new ContactViewHolder(view);
        cli.bind(cvh, cursor, null,false);
        View btnClose = view.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            private int index;

            public View.OnClickListener init(int index) {
                this.index = index;
                return this;
            }

            @Override
            public void onClick(View v) {
                mAdapter.removeSelection(this.index);
            }
        }.init(index));
        return view;
    }

    private class ContactAdapter extends ResourceCursorAdapter {

        private ArrayList<Integer> mSelection = new ArrayList<>();

        public ContactAdapter(Context context, int view) {
            super(context, view, null,0);

        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public void setSelection(int position) {
            if (!mSelection.contains(position)) {
                mSelection.add(position);
                notifyDataSetChanged();
            }
        }

        public boolean isPositionChecked(int position) {
            return mSelection.contains(position);
        }

        public ArrayList<Integer> getCurrentSelection() {
            return mSelection;
        }

        public void removeSelection(int position) {
            if (mSelection.contains(position)) {
                mSelection.remove((Integer)position);
                notifyDataSetChanged();
                if (mSelection.size() == 0) {
                    setGroupMode(false);
                }
            }
        }

        public void clearSelection() {
            mSelection.clear();
            notifyDataSetChanged();
            setGroupMode(false);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = super.getView(position, convertView, parent);//let the adapter handle setting up the row views
            v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ContactListItem v = (ContactListItem) view;

            ContactViewHolder holder = v.getViewHolder();
            if (holder == null) {
                holder = new ContactViewHolder(v);

                // holder.mMediaThumb = (ImageView)findViewById(R.id.media_thumbnail);
                v.setViewHolder(holder);
            }


            v.bind(holder, cursor, mSearchString, false);
            holder.mAvatarCheck.setVisibility(isPositionChecked(cursor.getPosition()) ? View.VISIBLE : View.GONE);
        }
    }

    class MyLoaderCallbacks implements LoaderCallbacks<Cursor> {
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

            CursorLoader loader = new CursorLoader(ContactsPickerActivity.this, mUri, ContactListItem.CONTACT_PROJECTION,
                    buf == null ? null : buf.toString(), null, Imps.Contacts.MODE_AND_ALPHA_SORT_ORDER);
        //    loader.setUpdateThrottle(50L);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
            mAdapter.swapCursor(newCursor);



        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

            mAdapter.swapCursor(null);



        }

    }


    public interface ContactListListener {

        public void openChat (Cursor c);
        public void showProfile (Cursor c);
    }
}
