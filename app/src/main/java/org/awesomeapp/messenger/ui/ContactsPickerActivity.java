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
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.ListViewCompat;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.widgets.FlowLayout;

import java.util.ArrayList;
import java.util.HashMap;

import im.zom.messenger.R;

/** Activity used to pick a contact. */
public class ContactsPickerActivity extends BaseActivity {

    public final static String EXTRA_EXCLUDED_CONTACTS = "excludes";
    public final static String EXTRA_SHOW_GROUPS = "show_groups";

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

    private ArrayList<String> excludedContacts;
    private String mExcludeClause;
    Uri mData;
    private boolean mShowGroups = false;

    private String mSearchString;

    SearchView mSearchView = null;
    FlowLayout mSelectedContacts;
    View mLayoutContactSelect;
    View mLayoutGroupSelect;
    ListView mListView = null;
    private MenuItem mMenuStartGroupChat;

    // The loader's unique id. Loader ids are specific to the Activity or
    // Fragment in which they reside.
    private static final int LOADER_ID = 1;

    // The callbacks through which we will interact with the LoaderManager.
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    // TODO - Maybe extend the Contact class with provider and account instead?
    private class SelectedContact {
        public long id;
        public String username;
        public Integer account;
        public Integer provider;

        SelectedContact(long id, String username, int account, int provider) {
            this.id = id;
            this.username = username;
            this.account = account;
            this.provider = provider;
        }
    }
    private LongSparseArray<SelectedContact> mSelection = new LongSparseArray<>();

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
        mSelectedContacts.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                // When the tag view grows we don't want the list to jump around, so
                // compensate for this by trying to scroll the list.
                final int diff = bottom - oldBottom;
                ListViewCompat.scrollListBy(mListView, diff);
            }
        });

        boolean isGroupOnlyMode = isGroupOnlyMode();
        excludedContacts = getIntent().getStringArrayListExtra(EXTRA_EXCLUDED_CONTACTS);
        mShowGroups = getIntent().getBooleanExtra(EXTRA_SHOW_GROUPS,false);

        View btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setGroupMode(true);
            }
        });
        btnCreateGroup.setVisibility(isGroupOnlyMode ? View.GONE : View.VISIBLE);

        View btnAddContact = findViewById(R.id.btnAddFriend);
        btnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ContactsPickerActivity.this, AddContactActivity.class);
                startActivityForResult(i, REQUEST_CODE_ADD_CONTACT);
            }
        });
        btnAddContact.setVisibility(isGroupOnlyMode ? View.GONE : View.VISIBLE);

        // Make sure the tag view can not be more than a third of the screen
        View root = findViewById(R.id.llRoot);
        if (root != null) {
            root.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if ((bottom - top) != (oldBottom - oldTop)) {
                        ViewGroup.LayoutParams lp = mSelectedContacts.getLayoutParams();
                        lp.height = (bottom - top) / 3;
                        mSelectedContacts.setLayoutParams(lp);
                    }
                }
            });
        }

        mListView = (ListView)findViewById(R.id.contactsList);
        setGroupMode(isGroupOnlyMode);

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
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
                if (mListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
                    if (isSelected(id)) {
                        unselect(id);
                    } else {
                        select(position);
                    }
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

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void multiStart (int i)
    {
        setGroupMode(true);
        if (i != -1) {
            select(i);
        }
    }

    private boolean isGroupOnlyMode() {
        return getIntent().hasExtra(EXTRA_EXCLUDED_CONTACTS);
    }

    private boolean isGroupMode() {
      return mListView.getChoiceMode() != ListView.CHOICE_MODE_SINGLE;
    }

    private void setGroupMode(boolean groupMode) {
        setTitle(groupMode ? R.string.add_people : R.string.choose_friend);
        mLayoutContactSelect.setVisibility(groupMode ? View.GONE : View.VISIBLE);
        mLayoutGroupSelect.setVisibility(groupMode ? View.VISIBLE : View.GONE);
        int newChoiceMode = (groupMode ? ListView.CHOICE_MODE_MULTIPLE : ListView.CHOICE_MODE_SINGLE);
        if (mListView.getChoiceMode() != newChoiceMode) {
            mListView.setChoiceMode(newChoiceMode);
        }
        updateStartGroupChatMenu();
    }

    private void multiFinish ()
    {
        if (mSelection.size() > 0) {
            ArrayList<String> users = new ArrayList<>();
            ArrayList<Integer> providers = new ArrayList<>();
            ArrayList<Integer> accounts = new ArrayList<>();

            for (int i = 0; i < mSelection.size(); i++) {
                SelectedContact contact = mSelection.valueAt(i);
                    users.add(contact.username);
                    providers.add(contact.provider);
                    accounts.add(contact.account);
            }

            Intent data = new Intent();
            data.putStringArrayListExtra(EXTRA_RESULT_USERNAMES, users);
            data.putIntegerArrayListExtra(EXTRA_RESULT_PROVIDER, providers);
            data.putIntegerArrayListExtra(EXTRA_RESULT_ACCOUNT, accounts);
            setResult(RESULT_OK, data);
            finish();
        }
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

        mMenuStartGroupChat = menu.findItem(R.id.action_start_chat);
        updateStartGroupChatMenu();

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

    private void updateStartGroupChatMenu() {
        if (mMenuStartGroupChat != null) {
            mMenuStartGroupChat.setVisible(isGroupMode());
            mMenuStartGroupChat.setEnabled(mSelection.size() > 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                if (isGroupMode() && !isGroupOnlyMode()) {
                    setGroupMode(false);
                } else {
                    finish();
                }
                return true;
            case R.id.action_start_chat:
                multiFinish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isGroupMode() && !isGroupOnlyMode()) {
            setGroupMode(false);
            return;
        }
        super.onBackPressed();
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

    private void createTagView(int index, SelectedContact contact) {
        Cursor cursor = (Cursor) mAdapter.getItem(index);
        long itemId = mAdapter.getItemId(index);
        View view = LayoutInflater.from(mSelectedContacts.getContext()).inflate(R.layout.picked_contact_item, mSelectedContacts, false);
        view.setTag(contact);

        // TODO - Feel a little awkward to create a ContactListItem here just to use the binding code.
        // I guess we should move that somewhere else.
        ContactListItem cli = new ContactListItem(this, null);
        ContactViewHolder cvh = new ContactViewHolder(view);
        cli.bind(cvh, cursor, null,false);
        View btnClose = view.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            private long itemId;
            private View view;

            public View.OnClickListener init(long itemId, View view) {
                this.itemId = itemId;
                this.view = view;
                return this;
            }

            @Override
            public void onClick(View v) {
                unselect(this.itemId);
            }
        }.init(itemId, view));
        mSelectedContacts.addView(view);
    }

    private void removeTagView(SelectedContact contact) {
        View view = mSelectedContacts.findViewWithTag(contact);
        if (view != null) {
            mSelectedContacts.removeView(view);
        }
    }

    private void select(int index) {
        long id = mAdapter.getItemId(index);
        if (!isSelected(id)) {
            Cursor cursor = (Cursor) mAdapter.getItem(index);
            String userName = cursor.getString(ContactListItem.COLUMN_CONTACT_USERNAME);

            SelectedContact contact = new SelectedContact(id,
                    userName,
                    (int) cursor.getLong(ContactListItem.COLUMN_CONTACT_ACCOUNT),
                    (int) cursor.getLong(ContactListItem.COLUMN_CONTACT_PROVIDER));
            mSelection.put(id, contact);
            createTagView(index, contact);
            mAdapter.notifyDataSetChanged();
            updateStartGroupChatMenu();
        }
    }

    private boolean isSelected(long id) {
        return mSelection.indexOfKey(id) >= 0;
    }

    private void unselect(long id) {
        if (isSelected(id)) {
            removeTagView(mSelection.get(id));
            mSelection.remove((Long)id);
            mAdapter.notifyDataSetChanged();
            if (mSelection.size() == 0) {
                setGroupMode(false);
            } else {
                updateStartGroupChatMenu();
            }
        }
    }

    private class ContactAdapter extends ResourceCursorAdapter {

        public ContactAdapter(Context context, int view) {
            super(context, view, null,0);
        }

        @Override
        public boolean hasStableIds() {
            return true;
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
            v.setShowPresence(true);

            ContactViewHolder holder = v.getViewHolder();
            if (holder == null) {
                holder = new ContactViewHolder(v);

                // holder.mMediaThumb = (ImageView)findViewById(R.id.media_thumbnail);
                v.setViewHolder(holder);
            }


            v.bind(holder, cursor, mSearchString, false);
            int index = cursor.getPosition();
            long itemId = getItemId(index);
            holder.mAvatarCheck.setVisibility(isSelected(itemId) ? View.VISIBLE : View.GONE);
            String userName = cursor.getString(ContactListItem.COLUMN_CONTACT_USERNAME);
            if (excludedContacts != null && excludedContacts.contains(userName)) {
                holder.mLine1.setTextColor((holder.mLine1.getCurrentTextColor() & 0x00ffffff) | 0x80000000);
                holder.mLine1.setText(getString(R.string.is_already_in_your_group, holder.mLine1.getText()));
            } else {
                holder.mLine1.setTextColor(holder.mLine1.getCurrentTextColor() | 0xff000000);
            }
        }
    }

    class MyLoaderCallbacks implements LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            StringBuilder buf = new StringBuilder();

            if (!TextUtils.isEmpty(mSearchString)) {
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

            buf.append('(');
            buf.append(Imps.Contacts.TYPE).append('=').append(Imps.Contacts.TYPE_NORMAL);

            if (mShowGroups) {
                buf.append(" OR (")
                        // Mask out TYPE_FLAG_UNSEEN, we want unseen groups as well!
                .append(Imps.Contacts.TYPE)
                .append(" & (~")
                .append(Imps.Contacts.TYPE_FLAG_UNSEEN)
                .append("))")
                .append('=').append(Imps.Contacts.TYPE_GROUP);
            }
          //  buf.append(") ");

            buf.append(")) GROUP BY (" + Imps.Contacts.USERNAME);

            CursorLoader loader = new CursorLoader(ContactsPickerActivity.this, mUri, ContactListItem.CONTACT_PROJECTION,
                    buf == null ? null : buf.toString(), null, Imps.Contacts.MODE_AND_ALPHA_SORT_ORDER);

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
