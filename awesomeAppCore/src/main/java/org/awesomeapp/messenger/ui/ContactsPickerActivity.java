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

import info.guardianproject.otr.app.im.R;

import org.awesomeapp.messenger.ui.legacy.ContactListFilterView.ContactListListener;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/** Activity used to pick a contact. */
public class ContactsPickerActivity extends AppCompatActivity {

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
    private Uri mUri = Imps.Contacts.CONTENT_URI_CONTACTS_BY;

    private Handler mHandler = new Handler();

    private String mExcludeClause;
    Uri mData;

    private String mSearchString;

    SearchView mSearchView = null;
    ListView mListView = null;

    // The loader's unique id. Loader ids are specific to the Activity or
    // Fragment in which they reside.
    private static final int LOADER_ID = 1;

    // The callbacks through which we will interact with the LoaderManager.
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private boolean mHideOffline = false;
    private boolean mShowInvitations = false;

    private boolean mIsCABDestroyed= true;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_contact_picker_multi, menu);
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
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
            mIsCABDestroyed = true;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ((ImApp)getApplication()).setAppTheme(this);
        
        setContentView(R.layout.contacts_picker_activity);
        
        if (getIntent().getData() != null)
            mUri = getIntent().getData();

        mListView = (ListView)findViewById(R.id.contactsList);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                multiStart(i);
                //getSupportActionBar().startActionMode(mActionModeCallback);

                return true;
            }
        });


        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            private int nr = 0;

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.menu_contact_picker_multi, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {

                if (item.getItemId() == R.id.action_start_chat)
                {
                    SparseBooleanArray checkedPos = mListView.getCheckedItemPositions();
                    multiFinish(checkedPos);

                    return true;
                }

                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                nr = 0;
                mAdapter.clearSelection();
                mIsCABDestroyed = true;
            }

            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {

                mAdapter.setNewSelection(position, checked);

                if (!checked)
                    mAdapter.removeSelection(position);
            }

        });

        mListView.setOnItemClickListener(new OnItemClickListener ()
        {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                if(mIsCABDestroyed) {
                    mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    //do your action command  here
                }

                if (mListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE_MODAL) {
                    //mAdapter.getItem(position);
                    boolean newChecked = !mListView.isItemChecked(position);

                    mListView.setItemChecked(position, newChecked);
                    mAdapter.setNewSelection(position, newChecked);

                    if (!newChecked)
                        mAdapter.removeSelection(position);

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


        ContentResolver cr = getContentResolver();
        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS)},null);
        Imps.ProviderSettings.QueryMap globalSettings = new Imps.ProviderSettings.QueryMap(pCursor, cr, Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS, true, null);
        mHideOffline = globalSettings.getHideOfflineContacts();

        globalSettings.close();
        
        if (getIntent() != null && getIntent().hasExtra("invitations"))
        {
            mShowInvitations = getIntent().getBooleanExtra("invitations", false);
        }
        
        

        doFilterAsync("");
    }

    private void multiStart (int i)
    {

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.startActionMode(mActionModeCallback);

        if (i != -1)
            mAdapter.setNewSelection(i, true);

        mIsCABDestroyed = false; // mark readiness to switch back to SINGLE CHOICE after the CABis destroyed

    }

    private void multiFinish (SparseBooleanArray positions)
    {

        ArrayList<String> users = new ArrayList<String>();
        ArrayList<Integer> providers = new ArrayList<Integer>();
        ArrayList<Integer> accounts = new ArrayList<Integer>();

        for (int i = 0; i < positions.size(); i++)
        {
            if (positions.get(i)) {
                Cursor cursor = (Cursor) mAdapter.getItem(i);

                users.add(cursor.getString(ContactListItem.COLUMN_CONTACT_USERNAME));
                providers.add((int) cursor.getLong(ContactListItem.COLUMN_CONTACT_PROVIDER));
                accounts.add((int) cursor.getLong(ContactListItem.COLUMN_CONTACT_ACCOUNT));
            }
        }

        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_RESULT_USERNAMES, users);
        data.putIntegerArrayListExtra(EXTRA_RESULT_PROVIDER, providers);
        data.putIntegerArrayListExtra(EXTRA_RESULT_PROVIDER, accounts);
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
            case R.id.menu_new_group_chat:
                multiStart(-1);
             //   getSupportActionBar().startActionMode(mActionModeCallback);
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

    private class ContactAdapter extends ResourceCursorAdapter {

        private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

        public ContactAdapter(Context context, int view) {
            super(context, view, null,0);

        }

        public void setNewSelection(int position, boolean value) {
            mSelection.put(position, value);
            notifyDataSetChanged();
        }

        public boolean isPositionChecked(int position) {
            Boolean result = mSelection.get(position);
            return result == null ? false : result;
        }

        public Set<Integer> getCurrentCheckedPosition() {
            return mSelection.keySet();
        }

        public void removeSelection(int position) {
            mSelection.remove(position);
            notifyDataSetChanged();
        }

        public void clearSelection() {
            mSelection = new HashMap<Integer, Boolean>();
            notifyDataSetChanged();

        }

        /*
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            View view = super.newView(context, cursor, parent);

            ContactListItem.ViewHolder holder = null;

            holder = new ContactListItem.ViewHolder();

            holder.mLine1 = (TextView) view.findViewById(R.id.line1);
            holder.mLine2 = (TextView) view.findViewById(R.id.line2);

            holder.mAvatar = (ImageView)view.findViewById(R.id.avatar);
            holder.mStatusIcon = (ImageView)view.findViewById(R.id.statusIcon);

            holder.mContainer = view.findViewById(R.id.message_container);

            holder.mMediaThumb = (ImageView)view.findViewById(R.id.media_thumbnail);

            view.setTag(holder);

           return view;



        }
*/

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = super.getView(position, convertView, parent);//let the adapter handle setting up the row views
            v.setBackgroundColor(getResources().getColor(R.color.background_light));

            if (mSelection.get(position) != null) {
                v.setBackgroundColor(getResources().getColor(R.color.holo_blue_light));
            }

            return super.getView(position, convertView, parent);


        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ContactListItem v = (ContactListItem) view;
            v.bind(cursor, mSearchString, false);
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
                android.database.DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
                buf.append(" OR ");
                buf.append(Imps.Contacts.USERNAME);
                buf.append(" LIKE ");
                android.database.DatabaseUtils.appendValueToSql(buf, "%" + mSearchString + "%");
                buf.append(')');
                buf.append(" AND ");
            }

//            normal types not temporary

            buf.append(Imps.Contacts.TYPE).append('=').append(Imps.Contacts.TYPE_NORMAL);

            if (mShowInvitations)
            {
                buf.append(" AND (");                
                buf.append(Imps.Contacts.SUBSCRIPTION_TYPE).append('=').append(Imps.Contacts.SUBSCRIPTION_TYPE_FROM);
                buf.append(" )");
            }

            if(mHideOffline)
            {
                buf.append(" AND ");
                buf.append(Imps.Contacts.PRESENCE_STATUS).append("!=").append(Imps.Presence.OFFLINE);
            }

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



}
