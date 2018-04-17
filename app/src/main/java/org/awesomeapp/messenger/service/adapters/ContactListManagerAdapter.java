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

package org.awesomeapp.messenger.service.adapters;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.service.IContactList;
import org.awesomeapp.messenger.service.IContactListListener;
import org.awesomeapp.messenger.service.ISubscriptionListener;
import im.zom.messenger.R;

import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.model.ChatGroup;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.ContactList;
import org.awesomeapp.messenger.model.ContactListListener;
import org.awesomeapp.messenger.model.ContactListManager;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.model.ImException;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.ImServiceConstants;
import org.awesomeapp.messenger.service.RemoteImService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class ContactListManagerAdapter extends
        org.awesomeapp.messenger.service.IContactListManager.Stub {

    ImConnectionAdapter mConn;
    ContentResolver mResolver;

    private ContactListManager mAdaptee;
    private ContactListListenerAdapter mContactListListenerAdapter;
    private SubscriptionRequestListenerAdapter mSubscriptionListenerAdapter;

    final RemoteCallbackList<IContactListListener> mRemoteContactListeners = new RemoteCallbackList<IContactListListener>();
    final RemoteCallbackList<ISubscriptionListener> mRemoteSubscriptionListeners = new RemoteCallbackList<ISubscriptionListener>();

    HashMap<String, ContactListAdapter> mContactLists;
    // Temporary contacts are created when a peer is encountered, and that peer
    // is not yet on any contact list.
    HashMap<String, Contact> mTemporaryContacts;
    // Offline contacts are created from the local DB before the server contact lists
    // are loaded.
    //HashMap<String, Contact> mOfflineContacts;

    HashSet<String> mValidatedContactLists;
    HashSet<String> mValidatedContacts;
    HashSet<String> mValidatedBlockedContacts;

    private Uri mAvatarUrl;
    private Uri mContactUrl;

    static final long FAKE_TEMPORARY_LIST_ID = -1;
    static final long LOCAL_GROUP_LIST_ID = 9999;
    static final String[] CONTACT_LIST_ID_PROJECTION = { Imps.ContactList._ID };

    RemoteImService mContext;

    public ContactListManagerAdapter(ImConnectionAdapter conn) {
        mAdaptee = conn.getAdaptee().getContactListManager();
        mConn = conn;
        mContext = conn.getContext();
        mResolver = mContext.getContentResolver();

        init();
    }

    private void init ()
    {
        mContactListListenerAdapter = new ContactListListenerAdapter();
        mSubscriptionListenerAdapter = new SubscriptionRequestListenerAdapter();
        mContactLists = new HashMap<String, ContactListAdapter>();
        mTemporaryContacts = new HashMap<String, Contact>();
       // mOfflineContacts = new HashMap<String, Contact>();

        mValidatedContacts = new HashSet<String>();
        mValidatedContactLists = new HashSet<String>();
        mValidatedBlockedContacts = new HashSet<String>();

        mAdaptee.addContactListListener(mContactListListenerAdapter);
        mAdaptee.setSubscriptionRequestListener(mSubscriptionListenerAdapter);

        Uri.Builder builder = Imps.Avatars.CONTENT_URI_AVATARS_BY.buildUpon();
        ContentUris.appendId(builder,  mConn.getProviderId());
        ContentUris.appendId(builder, mConn.getAccountId());

        mAvatarUrl = builder.build();

        builder = Imps.Contacts.CONTENT_URI_CONTACTS_BY.buildUpon();
        ContentUris.appendId(builder,  mConn.getProviderId());
        ContentUris.appendId(builder, mConn.getAccountId());

        mContactUrl = builder.build();

        if (mConn.getAccountId() != -1)
            seedInitialPresences();

    }

    public String[] getOfflineAddresses ()
    {
        Cursor contactCursor = mResolver.query(mContactUrl, new String[] { Imps.Contacts.USERNAME, Imps.Contacts.NICKNAME },
                null, null, null);

        String[] addresses = new String[contactCursor.getCount()];
        int i = 0;
        while (contactCursor.moveToNext())
        {
            addresses[i++] = contactCursor.getString(0);

        }
        contactCursor.close();
        return addresses;
    }

    public int createContactList(String name, List<Contact> contacts) {
        try {
            mAdaptee.createContactListAsync(name, contacts);
        } catch (ImException e) {
            return e.getImError().getCode();
        }

        return ImErrorInfo.NO_ERROR;
    }

    public int deleteContactList(String name) {
        try {
            mAdaptee.deleteContactListAsync(name);
        } catch (ImException e) {
            return e.getImError().getCode();
        }

        return ImErrorInfo.NO_ERROR;
    }

    public List<ContactListAdapter> getContactLists() {
        return new ArrayList<ContactListAdapter>(mContactLists.values());
    }

    public int removeContact(String address) {

        closeChatSession(address);

        /**
        if (isTemporary(address)) {
            synchronized (mTemporaryContacts) {
                mTemporaryContacts.remove(address);
            }
        } else {**/
            synchronized (mContactLists) {
                for (ContactListAdapter list : mContactLists.values()) {
                    int resCode = list.removeContact(address);
                    if (ImErrorInfo.ILLEGAL_CONTACT_ADDRESS == resCode) {
                        // Did not find in this list, continue to remove from
                        // other list.
                        continue;
                    }
                    if (ImErrorInfo.NO_ERROR != resCode) {
                        return resCode;
                    }
                }

            }
       // }

        String selection = Imps.Contacts.USERNAME + "=?";
        String[] selectionArgs = { address };
        mResolver.delete(mContactUrl, selection, selectionArgs);

        return ImErrorInfo.NO_ERROR;
    }

    public int setContactName(String address, String name) {
        // update the server
        try {
            mAdaptee.setContactName(address,name);
        } catch (ImException e) {
            return e.getImError().getCode();
        }
        // update locally
        String selection = Imps.Contacts.USERNAME + "=?";
        String[] selectionArgs = { address };
        ContentValues values = new ContentValues(1);
        values.put( Imps.Contacts.NICKNAME, name);
        int updated = mResolver.update(mContactUrl, values, selection, selectionArgs);
        if( updated != 1 ) {
            return ImErrorInfo.ILLEGAL_CONTACT_ADDRESS;
        }

        return ImErrorInfo.NO_ERROR;
    }

    public void approveSubscription(Contact address) {
        mAdaptee.approveSubscriptionRequest(address);
    }

    public void declineSubscription(Contact address) {
        mAdaptee.declineSubscriptionRequest(address);
    }

    public int archiveContact(String address, int contactType, boolean doArchive) {
        if (doArchive) {
            contactType = contactType | Imps.Contacts.TYPE_FLAG_HIDDEN;
        } else {
            contactType = contactType & (~Imps.Contacts.TYPE_FLAG_HIDDEN);
        }
        if (updateContactType(address, contactType))
            return ImErrorInfo.NO_ERROR;
        else
            return -1;
    }


    public int blockContact(String address) {
        try {
            mAdaptee.blockContactAsync(address);
        } catch (ImException e) {
            return e.getImError().getCode();
        }

        return ImErrorInfo.NO_ERROR;
    }

    public int unBlockContact(String address) {
        try {
            mAdaptee.unblockContactAsync(address);
        } catch (ImException e) {
            RemoteImService.debug(e.getMessage());
            return e.getImError().getCode();
        }

        return ImErrorInfo.NO_ERROR;
    }

    public boolean isBlocked(String address) {
        try {
            return mAdaptee.isBlocked(address);
        } catch (ImException e) {
            RemoteImService.debug(e.getMessage());
            return false;
        }
    }

    public void registerContactListListener(IContactListListener listener) {
        if (listener != null) {
            mRemoteContactListeners.register(listener);
        }
    }

    public void unregisterContactListListener(IContactListListener listener) {
        if (listener != null) {
            mRemoteContactListeners.unregister(listener);
        }
    }

    public void registerSubscriptionListener(ISubscriptionListener listener) {
        if (listener != null) {
            mRemoteSubscriptionListeners.register(listener);
        }
    }

    public void unregisterSubscriptionListener(ISubscriptionListener listener) {
        if (listener != null) {
            mRemoteSubscriptionListeners.unregister(listener);
        }
    }

    public IContactList getContactList(String name) {
        return getContactListAdapter(name);
    }

    public void loadContactLists() {
        if (mAdaptee.getState() == ContactListManager.LISTS_NOT_LOADED) {
            clearValidatedContactsAndLists();
            mAdaptee.loadContactListsAsync();
       }
        
    }

    public int getState() {
        return mAdaptee.getState();
    }

    public Contact getContactByAddress(String address) {
        if (mAdaptee.getState() == ContactListManager.LISTS_NOT_LOADED) {
            //if (mOfflineContacts != null)
             //   return mOfflineContacts.get(address);
            //else
                return null;
        }

        Contact c = mAdaptee.getContact(address);
        if (c == null) {
            synchronized (mTemporaryContacts) {
                return mTemporaryContacts.get(address);
            }
        } else {
            return c;
        }
    }

    /**
    public Contact[] createTemporaryContacts(String[] addresses) {
        Contact[] contacts = mAdaptee.createTemporaryContacts(addresses);

        for (Contact c : contacts)
            insertTemporary(c);
        return contacts;
    }**/

    public long queryOrInsertContact(Contact c) {
        long result;

        String username = mAdaptee.normalizeAddress(c.getAddress().getAddress());
        String selection = Imps.Contacts.USERNAME + "=?";
        String[] selectionArgs = { username };
        String[] projection = { Imps.Contacts._ID };

        Cursor cursor = mResolver.query(mContactUrl, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getLong(0);
        } else {
           // result = insertContactContent(c);
            Uri uriNewContact = insertContactContent(c, FAKE_TEMPORARY_LIST_ID, Imps.Contacts.TYPE_NORMAL, Imps.Contacts.SUBSCRIPTION_TYPE_NONE, Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING);
            result = ContentUris.parseId(uriNewContact);

        }

        if (cursor != null) {
            cursor.close();
        }
        return result;
    }
    
    public long queryGroup(ChatGroup c) {
        long result = -1;

        String username = mAdaptee.normalizeAddress(c.getAddress().getAddress());
        String selection = Imps.Contacts.USERNAME + "=?";
        String[] selectionArgs = { username };
        String[] projection = { Imps.Contacts._ID };

        Cursor cursor = mResolver.query(mContactUrl, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getLong(0);
        } 
        
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }

    /**
    private long insertTemporary(Contact c) {
        synchronized (mTemporaryContacts) {
            mTemporaryContacts.put(mAdaptee.normalizeAddress(c.getAddress().getAddress()), c);
        }
        Uri uri = insertContactContent(c, FAKE_TEMPORARY_LIST_ID, Imps.Contacts.TYPE_NORMAL, Imps.Contacts.SUBSCRIPTION_TYPE_NONE, Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING);
        return ContentUris.parseId(uri);
    }**/

    /**
     * Tells if a contact is a temporary one which is not in the list of
     * contacts that we subscribe presence for. Usually created because of the
     * user is having a chat session with this contact.
     *
     * @param address the address of the contact.
     * @return <code>true</code> if it's a temporary contact; <code>false</code>
     *         otherwise.
     */

    /**
    public boolean isTemporary(String address) {
        synchronized (mTemporaryContacts) {
            return mTemporaryContacts.containsKey(address);
        }
    }**/

    ContactListAdapter getContactListAdapter(String name) {
        synchronized (mContactLists) {
            for (ContactListAdapter list : mContactLists.values()) {
                if (name.equals(list.getName())) {
                    return list;
                }
            }

            return null;
        }
    }

    ContactListAdapter getContactListAdapter(Address address) {
        synchronized (mContactLists) {
            return mContactLists.get(address.getAddress());
        }
    }

    private class Exclusion {
        private StringBuilder mSelection;
        private List<String> mSelectionArgs;
        private String mExclusionColumn;

        Exclusion(String exclusionColumn, Collection<String> items) {
            mSelection = new StringBuilder();
            mSelectionArgs = new ArrayList<String>();
            mExclusionColumn = exclusionColumn;
            for (String s : items) {
                add(s);
            }
        }

        public void add(String exclusionItem) {
            if (mSelection.length() == 0) {
                mSelection.append(mExclusionColumn + "!=?");
            } else {
                mSelection.append(" AND " + mExclusionColumn + "!=?");
            }
            mSelectionArgs.add(exclusionItem);
        }

        public String getSelection() {
            return mSelection.toString();
        }

        public String[] getSelectionArgs() {
            return (String[]) mSelectionArgs.toArray(new String[0]);
        }
    }

    private void removeObsoleteContactsAndLists() {
        // remove all contacts for this provider & account which have not been
        // added since login, yet still exist in db from a prior login
        Exclusion exclusion = new Exclusion(Imps.Contacts.USERNAME, mValidatedContacts);
        mResolver.delete(mContactUrl, exclusion.getSelection(), exclusion.getSelectionArgs());

        // remove all blocked contacts for this provider & account which have not been
        // added since login, yet still exist in db from a prior login
        exclusion = new Exclusion(Imps.BlockedList.USERNAME, mValidatedBlockedContacts);
        Uri.Builder builder = Imps.BlockedList.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, mConn.getProviderId());
        ContentUris.appendId(builder, mConn.getAccountId());
        Uri uri = builder.build();
        mResolver.delete(uri, exclusion.getSelection(), exclusion.getSelectionArgs());

        // remove all contact lists for this provider & account which have not been
        // added since login, yet still exist in db from a prior login
        exclusion = new Exclusion(Imps.ContactList.NAME, mValidatedContactLists);
        builder = Imps.ContactList.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, mConn.getProviderId());
        ContentUris.appendId(builder, mConn.getAccountId());
        uri = builder.build();
        mResolver.delete(uri, exclusion.getSelection(), exclusion.getSelectionArgs());

    }

    interface ContactListBroadcaster {
        void broadcast(IContactListListener listener) throws RemoteException;
    }

    interface SubscriptionBroadcaster {
        void broadcast(ISubscriptionListener listener) throws RemoteException;
    }

    final class ContactListListenerAdapter implements ContactListListener {
        private boolean mAllContactsLoaded;

        // class to hold contact changes made before mAllContactsLoaded
        private class StoredContactChange {
            int mType;
            ContactList mList;
            Contact mContact;

            StoredContactChange(int type, ContactList list, Contact contact) {
                mType = type;
                mList = list;
                mContact = contact;
            }
        }

        private Vector<StoredContactChange> mDelayedContactChanges = new Vector<StoredContactChange>();

        private void broadcast(ContactListBroadcaster callback) {
            try {
                synchronized (mRemoteContactListeners) {
                    final int N = mRemoteContactListeners.beginBroadcast();
                    for (int i = 0; i < N; i++) {
                        IContactListListener listener = mRemoteContactListeners.getBroadcastItem(i);
                        try {
                            callback.broadcast(listener);
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing the
                            // dead listeners.
                        }
                    }
                    mRemoteContactListeners.finishBroadcast();
                }
            }
            catch (Exception e){}
        }

        public void onContactsPresenceUpdate(final Contact[] contacts) {
            // The client listens only to presence updates for now. Update
            // the avatars first to ensure it can get the new avatar when
            // presence updated.
            // TODO: Don't update avatar now since none of the server supports it
            // updateAvatarsContent(contacts);
            updatePresenceContent(contacts);

            ChatSessionManagerAdapter chatSessionManager = (ChatSessionManagerAdapter) mConn
                    .getChatSessionManager();

            for (Contact contact : contacts)
            {

                ChatSessionAdapter session = (ChatSessionAdapter) chatSessionManager
                        .getChatSession(contact.getAddress().getBareAddress());

                if (session != null && (!session.isGroupChatSession()))
                {
                    session.presenceChanged(contact.getPresence().getStatus());
                }

            }

            broadcast(new ContactListBroadcaster() {
                public void broadcast(IContactListListener listener) throws RemoteException {
                    listener.onContactsPresenceUpdate(contacts);
                }
            });
        }

        public void onContactChange(final int type, final ContactList list, final Contact contact) {
            ContactListAdapter removed = null;
            String notificationText = null;

            switch (type) {
            case LIST_CREATED:
                break;

            case LIST_LOADED:
                addContactListContent(list);
                break;

            case LIST_DELETED:
                removed = removeContactListFromDataBase(list.getName());
                // handle case where a list is deleted before mAllContactsLoaded
                if (!mAllContactsLoaded) {
                    // if a cached contact list is deleted before the actual contact list is
                    // downloaded from the server, we will have to remove the list again once
                    // once mAllContactsLoaded is true
                    if (!mValidatedContactLists.contains(list.getName())) {
                        mDelayedContactChanges.add(new StoredContactChange(type, list, contact));
                    }
                }
                break;

            case LIST_CONTACT_ADDED:

                ContactListAdapter cla = getContactListAdapter(list.getAddress());
                if (cla != null)
                {
                    long listId = cla.getDataBaseId();

                    boolean exists = updateContact(contact, listId);

                    int subType = Imps.Contacts.SUBSCRIPTION_TYPE_TO;
                    int subStatus = Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING;

                    if (!exists)
                         insertContactContent(contact, listId, contact.getType(),subType,subStatus);

                    if (contact.getSubscriptionStatus() != -1)
                        subStatus = contact.getSubscriptionStatus();

                    if (contact.getSubscriptionType() != -1)
                        subType = contact.getSubscriptionType();

                    contact.setSubscriptionStatus(subStatus);
                    contact.setSubscriptionType(subType);

                    insertOrUpdateSubscription(contact.getAddress().getBareAddress(),contact.getName(),subType,subStatus);

                    //}

                    notificationText = mContext.getResources().getString(R.string.add_contact_success,
                            contact.getName());
                    // handle case where a contact is added before mAllContactsLoaded
                    if (!mAllContactsLoaded) {
                        // if a contact is added to a cached contact list before the actual contact
                        // list is downloaded from the server, we will have to add the contact to
                        // the contact list once mAllContactsLoaded is true
                        if (!mValidatedContactLists.contains(list.getName())) {
                            mDelayedContactChanges.add(new StoredContactChange(type, list, contact));
                        }
                    }
                }
                break;

            case LIST_CONTACT_REMOVED:
                deleteContactFromDataBase(contact, list);
                // handle case where a contact is removed before mAllContactsLoaded
                if (!mAllContactsLoaded) {
                    // if a contact is added to a cached contact list before the actual contact
                    // list is downloaded from the server, we will have to add the contact to
                    // the contact list once mAllContactsLoaded is true
                    if (!mValidatedContactLists.contains(list.getName())) {
                        mDelayedContactChanges.add(new StoredContactChange(type, list, contact));
                    }
                }

                // Clear ChatSession if any.
                String address = mAdaptee.normalizeAddress(contact.getAddress().getAddress());
                closeChatSession(address);

                notificationText = mContext.getResources().getString(
                        R.string.delete_contact_success, contact.getName());
                break;

            case LIST_RENAMED:
                updateListNameInDataBase(list);
                // handle case where a list is renamed before mAllContactsLoaded
                if (!mAllContactsLoaded) {
                    // if a contact list name is updated before the actual contact list is
                    // downloaded from the server, we will have to update the list name again
                    // once mAllContactsLoaded is true
                    if (!mValidatedContactLists.contains(list.getName())) {
                        mDelayedContactChanges.add(new StoredContactChange(type, list, contact));
                    }
                }
                break;

            case CONTACT_BLOCKED:
                insertBlockedContactToDataBase(contact);
                address = mAdaptee.normalizeAddress(contact.getAddress().getAddress());
                updateContactType(address, Imps.Contacts.TYPE_BLOCKED);
                closeChatSession(address);
                notificationText = mContext.getResources().getString(
                        R.string.block_contact_success, contact.getName());
                break;

            case CONTACT_UNBLOCKED:
                removeBlockedContactFromDataBase(contact);
                notificationText = mContext.getResources().getString(
                        R.string.unblock_contact_success, contact.getName());
                // handle case where a contact is unblocked before mAllContactsLoaded
                if (!mAllContactsLoaded) {
                    // if a contact list name is updated before the actual contact list is
                    // downloaded from the server, we will have to update the list name again
                    // once mAllContactsLoaded is true
                    if (!mValidatedBlockedContacts.contains(contact.getName())) {
                        mDelayedContactChanges.add(new StoredContactChange(type, list, contact));
                    }
                }
                break;

            default:
                RemoteImService.debug("Unknown list update event!");
                break;
            }

            final ContactListAdapter listAdapter;
            if (type == LIST_DELETED) {
                listAdapter = removed;
            } else {
                listAdapter = (list == null) ? null : getContactListAdapter(list.getAddress());
            }

            broadcast(new ContactListBroadcaster() {
                public void broadcast(IContactListListener listener) throws RemoteException {
                    listener.onContactChange(type, listAdapter, contact);
                }
            });

            if (mAllContactsLoaded && notificationText != null) {
           //     mContext.showToast(notificationText, Toast.LENGTH_SHORT);
            }
        }

        public void onContactError(final int errorType, final ImErrorInfo error,
                final String listName, final Contact contact) {
            broadcast(new ContactListBroadcaster() {
                public void broadcast(IContactListListener listener) throws RemoteException {
                    listener.onContactError(errorType, error, listName, contact);
                }
            });
        }

        public void handleDelayedContactChanges() {
            for (StoredContactChange change : mDelayedContactChanges) {
                onContactChange(change.mType, change.mList, change.mContact);
            }
        }

        public void onAllContactListsLoaded() {
            mAllContactsLoaded = true;

            handleDelayedContactChanges();
        //    removeObsoleteContactsAndLists();

            broadcast(new ContactListBroadcaster() {
                public void broadcast(IContactListListener listener) throws RemoteException {
                    listener.onAllContactListsLoaded();
                }
            });
        }
    }

    final class SubscriptionRequestListenerAdapter extends ISubscriptionListener.Stub {


        public void onSubScriptionChanged (final Contact from, long providerId, long accountId, final int subType, final int subStatus)
        {
            String username = mAdaptee.normalizeAddress(from.getAddress().getAddress());
            String nickname = from.getName();
            from.setSubscriptionType(subType);
            from.setSubscriptionStatus(subStatus);
            Uri uri = insertOrUpdateSubscription(username, nickname,
                    subType,
                    subStatus);

            boolean hadListener = broadcast(new SubscriptionBroadcaster() {
                public void broadcast(ISubscriptionListener listener) throws RemoteException {
                    listener.onSubScriptionChanged(from,  mConn.getProviderId(), mConn.getAccountId(),subType,subStatus);
                }
            });
        }

        public void onSubScriptionRequest(final Contact from, long providerId, long accountId) {
                        
            String username = mAdaptee.normalizeAddress(from.getAddress().getAddress());

            if (!isSubscribed(username)) {

                String nickname = from.getName();
                queryOrInsertContact(from);
                from.setSubscriptionStatus( Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING);
                from.setSubscriptionType(Imps.Contacts.SUBSCRIPTION_TYPE_FROM);
                Uri uri = insertOrUpdateSubscription(username, nickname,
                        Imps.Contacts.SUBSCRIPTION_TYPE_FROM,
                        Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING);

                boolean hadListener = broadcast(new SubscriptionBroadcaster() {
                    public void broadcast(ISubscriptionListener listener) throws RemoteException {
                        listener.onSubScriptionRequest(from, mConn.getProviderId(), mConn.getAccountId());
                    }
                });


                if (!hadListener) {
                    mContext.getStatusBarNotifier().notifySubscriptionRequest(mConn.getProviderId(), mConn.getAccountId(),
                            ContentUris.parseId(uri), username, nickname);
                }
            }
            /**
            else
            {
                try {
                    Thread.sleep(2000);//wait two seconds
                }
                catch(Exception e){}
                approveSubscription(from);
            }**/
        }

        public void onUnSubScriptionRequest(final Contact from, long providerId, long accountId) {
            String username = mAdaptee.normalizeAddress(from.getAddress().getAddress());
            String nickname = from.getName();

            from.setSubscriptionStatus(Imps.Contacts.SUBSCRIPTION_STATUS_NONE);
            from.setSubscriptionType(Imps.Contacts.SUBSCRIPTION_TYPE_NONE);
            queryOrInsertContact(from); // FIXME Miron
            Uri uri = insertOrUpdateSubscription(username, nickname,
                    Imps.Contacts.SUBSCRIPTION_TYPE_NONE,
                    Imps.Contacts.SUBSCRIPTION_STATUS_NONE);

        }


        private synchronized boolean broadcast(SubscriptionBroadcaster callback) {
            boolean hadListener = false;

            final int N = mRemoteSubscriptionListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                ISubscriptionListener listener = mRemoteSubscriptionListeners.getBroadcastItem(i);
                try {
                    callback.broadcast(listener);
                    hadListener = true;
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing the
                    // dead listeners.
                }
            }
            mRemoteSubscriptionListeners.finishBroadcast();

            return hadListener;
        }

        public void onSubscriptionApproved(final Contact contact, long providerId, long accountId) {

            if (!isSubscribed(contact.getAddress().getBareAddress())) {
                onSubScriptionChanged(contact, providerId, accountId, Imps.Contacts.SUBSCRIPTION_TYPE_BOTH,
                        Imps.Contacts.SUBSCRIPTION_STATUS_NONE);

                boolean hadListener = broadcast(new SubscriptionBroadcaster() {
                    public void broadcast(ISubscriptionListener listener) throws RemoteException {
                        listener.onSubscriptionApproved(contact, mConn.getProviderId(), mConn.getAccountId());
                        }
                });

                if (!hadListener)
                    mContext.getStatusBarNotifier().notifySubscriptionApproved(contact, providerId, accountId);
            }
        }

        public void onSubscriptionDeclined(final Contact contact, long providerId, long accountId) {

            onSubScriptionChanged(contact, providerId, accountId, Imps.Contacts.SUBSCRIPTION_STATUS_NONE,
                    Imps.Contacts.SUBSCRIPTION_STATUS_NONE);

            broadcast(new SubscriptionBroadcaster() {
                public void broadcast(ISubscriptionListener listener) throws RemoteException {
                    listener.onSubscriptionDeclined(contact,  mConn.getProviderId(), mConn.getAccountId());
                }
            });
        }

        public void onApproveSubScriptionError(final String contact, final ImErrorInfo error) {
            String displayableAddress = getDisplayableAddress(contact);
            String msg = mContext
                    .getString(R.string.approve_subscription_error, displayableAddress);
            mContext.showToast(msg, Toast.LENGTH_SHORT);
        }

        public void onDeclineSubScriptionError(final String contact, final ImErrorInfo error) {
            String displayableAddress = getDisplayableAddress(contact);
            String msg = mContext
                    .getString(R.string.decline_subscription_error, displayableAddress);
            mContext.showToast(msg, Toast.LENGTH_SHORT);
        }
    }

    String getDisplayableAddress(String impsAddress) {
        if (impsAddress.startsWith("wv:")) {
            return impsAddress.substring(3);
        }
        return impsAddress;
    }

    void insertBlockedContactToDataBase(Contact contact) {
        // Remove the blocked contact if it already exists, to avoid duplicates and
        // handle the odd case where a blocked contact's nickname has changed
        removeBlockedContactFromDataBase(contact);

        Uri.Builder builder = Imps.BlockedList.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder,  mConn.getProviderId());
        ContentUris.appendId(builder, mConn.getAccountId());
        Uri uri = builder.build();

        String username = mAdaptee.normalizeAddress(contact.getAddress().getAddress());
        ContentValues values = new ContentValues(2);
        values.put(Imps.BlockedList.USERNAME, username);
        values.put(Imps.BlockedList.NICKNAME, contact.getName());

        mResolver.insert(uri, values);

        mValidatedBlockedContacts.add(username);
    }

    void removeBlockedContactFromDataBase(Contact contact) {
        String address = mAdaptee.normalizeAddress(contact.getAddress().getAddress());

        Uri.Builder builder = Imps.BlockedList.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder,  mConn.getProviderId());
        ContentUris.appendId(builder, mConn.getAccountId());

        Uri uri = builder.build();
        mResolver.delete(uri, Imps.BlockedList.USERNAME + "=?", new String[] { address });

        //int type = isTemporary(address) ? Imps.Contacts.TYPE_TEMPORARY : Imps.Contacts.TYPE_NORMAL;

        updateContactType(address, Imps.Contacts.TYPE_NORMAL);
    }

    /**
    void moveTemporaryContactToList(String address, long listId) {
        synchronized (mTemporaryContacts) {
            mTemporaryContacts.remove(address);
        }
        ContentValues values = new ContentValues(2);
        values.put(Imps.Contacts.TYPE, Imps.Contacts.TYPE_NORMAL);
        values.put(Imps.Contacts.CONTACTLIST, listId);

        String selection = Imps.Contacts.USERNAME + "=? AND " + Imps.Contacts.TYPE + "="
                           + Imps.Contacts.TYPE_TEMPORARY;
        String[] selectionArgs = { address };

        mResolver.update(mContactUrl, values, selection, selectionArgs);
    }**/

    boolean updateContactType(String address, int type) {
        ContentValues values = new ContentValues(1);
        values.put(Imps.Contacts.TYPE, type);
        return updateContact(address, values);
    }

    /**
     * Insert or update subscription request from user into the database.
     *
     * @param username
     * @param nickname
     * @param subscriptionType
     * @param subscriptionStatus
     */
    Uri insertOrUpdateSubscription(String username, String nickname, int subscriptionType,
            int subscriptionStatus) {

        Cursor cursor = mResolver.query(mContactUrl, new String[] { Imps.Contacts._ID },
                Imps.Contacts.USERNAME + "=?", new String[] { username }, null);
        if (cursor == null) {
            RemoteImService.debug("query contact " + username + " failed");
            return null;
        }

        Uri uri;
        if (cursor.moveToFirst()) {
            ContentValues values = new ContentValues(2);
            values.put(Imps.Contacts.SUBSCRIPTION_TYPE, subscriptionType);
            values.put(Imps.Contacts.SUBSCRIPTION_STATUS, subscriptionStatus);

            long contactId = cursor.getLong(0);
            uri = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, contactId);
            mResolver.update(uri, values, null, null);
        } else {
            ContentValues values = new ContentValues(6);
            values.put(Imps.Contacts.USERNAME, username);
            values.put(Imps.Contacts.NICKNAME, nickname);
            values.put(Imps.Contacts.TYPE, Imps.Contacts.TYPE_NORMAL);
            values.put(Imps.Contacts.CONTACTLIST, FAKE_TEMPORARY_LIST_ID);
            values.put(Imps.Contacts.SUBSCRIPTION_TYPE, subscriptionType);
            values.put(Imps.Contacts.SUBSCRIPTION_STATUS, subscriptionStatus);

            uri = mResolver.insert(mContactUrl, values);
        }
        cursor.close();
        return uri;
    }
    
    boolean isSubscribed (String username)
    {
        boolean result = false;
        
        Cursor cursor = mResolver.query(mContactUrl, new String[] { Imps.Contacts._ID,},
                Imps.Contacts.USERNAME + "=?"
                        + " AND (" + Imps.Contacts.SUBSCRIPTION_TYPE + "=" + Imps.Contacts.SUBSCRIPTION_TYPE_BOTH
                        + " OR " + Imps.Contacts.SUBSCRIPTION_TYPE + "=" + Imps.Contacts.SUBSCRIPTION_TYPE_TO + ")"
                , new String[] { username }, null);
        if (cursor == null) {
            RemoteImService.debug("query contact " + username + " failed");
            return false;
        }

        if (cursor.moveToFirst()) {
            
            result = true;
        } 
        
        cursor.close();
        return result;
    }

    boolean updateContact(Contact contact, long listId)
    {
       String addr = mAdaptee.normalizeAddress(contact.getAddress().getAddress());
       boolean exists = updateContact(addr,getContactContentValues(contact, listId));
       
       if (exists)
       {
           String username = mAdaptee.normalizeAddress(contact.getAddress().getAddress());
           String selection = Imps.Contacts.USERNAME + "=?";
           String[] selectionArgs = { username };
           String[] projection = { Imps.Contacts._ID };

           Cursor cursor = mResolver.query(mContactUrl, projection, selection, selectionArgs, null);

           if (cursor != null)
           {
            
               while (cursor.moveToNext())
               {
                   long contactId = cursor.getLong(0);
                   ContentValues presenceValues = getPresenceValues(contact);
                   selection = Imps.Presence.CONTACT_ID + "=?";                   
                   String[] selectionArgs2 = {contactId+""};
                   mResolver.update(Imps.Presence.CONTENT_URI,presenceValues, selection, selectionArgs2);
               }
               
               cursor.close();
           } 
           
          
       }
       
       return exists;
    }

    boolean updateContact(String username, ContentValues values) {
        String selection = Imps.Contacts.USERNAME + "=?";
        String[] selectionArgs = { username };
        return (mResolver.update(mContactUrl, values, selection, selectionArgs)) > 0;
    }

    void updatePresenceContent(Contact[] contacts) {

        if (mAdaptee == null)
            return;

        ArrayList<String> usernames = new ArrayList<String>();
        ArrayList<String> nicknames = new ArrayList<String>();
        ArrayList<String> statusArray = new ArrayList<String>();
        ArrayList<String> customStatusArray = new ArrayList<String>();
        ArrayList<String> clientTypeArray = new ArrayList<String>();
        ArrayList<String> resourceArray = new ArrayList<String>();

        for (Contact c : contacts) {
            String username = mAdaptee.normalizeAddress(c.getAddress().getAddress());
            Presence p = c.getPresence();
            int status = convertPresenceStatus(p);
            String customStatus = p.getStatusText();
            int clientType = translateClientType(p);

            usernames.add(username);
            nicknames.add(c.getName());
            statusArray.add(String.valueOf(status));
            customStatusArray.add(customStatus);
            clientTypeArray.add(String.valueOf(clientType));

            if (!TextUtils.isEmpty(p.getResource()))
                resourceArray.add(p.getResource());
            else
                resourceArray.add("");

        }

        ContentValues values = new ContentValues();
        values.put(Imps.Contacts.ACCOUNT, mConn.getAccountId());
        values.put(Imps.Contacts.PROVIDER, mConn.getProviderId());
        putStringArrayList(values, Imps.Contacts.USERNAME, usernames);
        putStringArrayList(values, Imps.Contacts.NICKNAME, nicknames);
        mResolver.update(Imps.Contacts.BULK_CONTENT_URI, values, null, null);

        values = new ContentValues();
        values.put(Imps.Contacts.ACCOUNT, mConn.getAccountId());
        values.put(Imps.Contacts.PROVIDER, mConn.getProviderId());
        putStringArrayList(values, Imps.Contacts.USERNAME, usernames);
        putStringArrayList(values, Imps.Presence.PRESENCE_STATUS, statusArray);
        putStringArrayList(values, Imps.Presence.PRESENCE_CUSTOM_STATUS, customStatusArray);
        putStringArrayList(values, Imps.Presence.CONTENT_TYPE, clientTypeArray);
        putStringArrayList(values, Imps.Presence.JID_RESOURCE, resourceArray);
        mResolver.update(Imps.Presence.BULK_CONTENT_URI, values, null, null);

    }

    void updateAvatarsContent(Contact[] contacts) {
        ArrayList<ContentValues> avatars = new ArrayList<ContentValues>();
        ArrayList<String> usernames = new ArrayList<String>();

        for (Contact contact : contacts) {
            byte[] avatarData = contact.getPresence().getAvatarData();
            if (avatarData == null) {
                continue;
            }

            String username = mAdaptee.normalizeAddress(contact.getAddress().getAddress());

            ContentValues values = new ContentValues(2);
            values.put(Imps.Avatars.CONTACT, username);
            values.put(Imps.Avatars.DATA, avatarData);
            avatars.add(values);
            usernames.add(username);
        }
        if (avatars.size() > 0) {
            // ImProvider will replace the avatar content if it already exist.
            mResolver.bulkInsert(mAvatarUrl, avatars.toArray(new ContentValues[avatars.size()]));

            // notify avatar changed
            Intent i = new Intent(ImServiceConstants.ACTION_AVATAR_CHANGED);
            i.putExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS, usernames);
            i.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID,  mConn.getProviderId());
            i.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mConn.getAccountId());
            mContext.sendBroadcast(i);
        }
    }

    ContactListAdapter removeContactListFromDataBase(String name) {
        ContactListAdapter listAdapter = getContactListAdapter(name);
        if (listAdapter == null) {
            return null;
        }
        long id = listAdapter.getDataBaseId();

        // delete contacts of this list first
        mResolver.delete(mContactUrl, Imps.Contacts.CONTACTLIST + "=?",
                new String[] { Long.toString(id) });

        mResolver.delete(ContentUris.withAppendedId(Imps.ContactList.CONTENT_URI, id), null, null);
        synchronized (mContactLists) {
            return mContactLists.remove(listAdapter.getAddress().getAddress());
        }
    }

    void addContactListContent(ContactList list) {
        String selection = Imps.ContactList.NAME + "=? AND " + Imps.ContactList.PROVIDER
                           + "=? AND " + Imps.ContactList.ACCOUNT + "=?";
        String[] selectionArgs = { list.getName(), Long.toString( mConn.getProviderId()),
                                  Long.toString(mConn.getAccountId()) };
        Cursor cursor = mResolver.query(Imps.ContactList.CONTENT_URI, CONTACT_LIST_ID_PROJECTION,
                selection, selectionArgs, null); // no sort order
        long listId = 0;
        Uri uri = null;
        try {
            if (cursor.moveToFirst()) {
                listId = cursor.getLong(0);
                uri = ContentUris.withAppendedId(Imps.ContactList.CONTENT_URI, listId);
            }
        } finally {
            cursor.close();
        }
        if (uri == null) {
            ContentValues contactListValues = new ContentValues(3);
            contactListValues.put(Imps.ContactList.NAME, list.getName());
            contactListValues.put(Imps.ContactList.PROVIDER,  mConn.getProviderId());
            contactListValues.put(Imps.ContactList.ACCOUNT, mConn.getAccountId());

            uri = mResolver.insert(Imps.ContactList.CONTENT_URI, contactListValues);
            listId = ContentUris.parseId(uri);
        }

        mValidatedContactLists.add(list.getName());

        mContactLists.put(list.getAddress().getAddress(), new ContactListAdapter(list, listId));

        Cursor contactCursor = mResolver.query(mContactUrl, new String[] { Imps.Contacts.USERNAME, Imps.Contacts.NICKNAME, Imps.Contacts.TYPE },
                Imps.Contacts.CONTACTLIST + "=?", new String[] { "" + listId }, null);
        Set<String> existingUsernames = new HashSet<String>();


        while (contactCursor.moveToNext()) {
            String address = contactCursor.getString(0);
            existingUsernames.add(address);
            Contact contact = list.getContact(address);
            if (contact != null) {
                contact.setName(contactCursor.getString(1));
                contact.setType(contactCursor.getInt(2));
            }
        }

        contactCursor.close();

        Collection<Contact> contacts = list.getContacts();
        if (contacts == null || contacts.size() == 0) {
            return;
        }

        Iterator<Contact> iter = contacts.iterator();
        while (iter.hasNext()) {
            Contact c = iter.next();

            String address = mAdaptee.normalizeAddress(c.getAddress().getAddress());
            /**
            if (isTemporary(address)) {
                if (!existingUsernames.contains(address)) {
                    moveTemporaryContactToList(address, listId);
                }
                iter.remove();
            }**/
            mValidatedContacts.add(address);
        }

        ArrayList<String> usernames = new ArrayList<String>();
        ArrayList<String> nicknames = new ArrayList<String>();
        ArrayList<String> contactTypeArray = new ArrayList<String>();
        for (Contact c : contacts) {

            if (updateContact(c,listId))
                continue; //contact existed and was updated to this list

            String username = mAdaptee.normalizeAddress(c.getAddress().getAddress());
            String nickname = c.getName();

            int type = c.getType();
            /**
            if (isTemporary(username)) {
                type = Imps.Contacts.TYPE_TEMPORARY;
            }**/
            if (isBlocked(username)) {
                type = Imps.Contacts.TYPE_BLOCKED;
            }

            usernames.add(username);
            nicknames.add(nickname);
            contactTypeArray.add(String.valueOf(type));
        }
        ContentValues values = new ContentValues(6);

        values.put(Imps.Contacts.PROVIDER,  mConn.getProviderId());
        values.put(Imps.Contacts.ACCOUNT, mConn.getAccountId());
        values.put(Imps.Contacts.CONTACTLIST, listId);
        putStringArrayList(values, Imps.Contacts.USERNAME, usernames);
        putStringArrayList(values, Imps.Contacts.NICKNAME, nicknames);
        putStringArrayList(values, Imps.Contacts.TYPE, contactTypeArray);

        mResolver.insert(Imps.Contacts.BULK_CONTENT_URI, values);
    }

    private void putStringArrayList(ContentValues values, String key, ArrayList<String> nicknames) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(nicknames);
            os.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        values.put(key, bos.toByteArray());

    }

    void updateListNameInDataBase(ContactList list) {
        ContactListAdapter listAdapter = getContactListAdapter(list.getAddress());

        Uri uri = ContentUris.withAppendedId(Imps.ContactList.CONTENT_URI,
                listAdapter.getDataBaseId());
        ContentValues values = new ContentValues(1);
        values.put(Imps.ContactList.NAME, list.getName());

        mResolver.update(uri, values, null, null);
    }

    void deleteContactFromDataBase(Contact contact, ContactList list) {

        String username = mAdaptee.normalizeAddress(contact.getAddress().getAddress());

        //if list is provided, then delete from one list
        if (list != null && list.getAddress() != null)
        {
            String selection = Imps.Contacts.USERNAME + "=? AND " + Imps.Contacts.CONTACTLIST + "=?";
            ContactListAdapter cla = getContactListAdapter(list.getAddress());
            
            if (cla != null)
            {
                long listId = getContactListAdapter(list.getAddress()).getDataBaseId();
                String[] selectionArgs = { username, Long.toString(listId) };
                mResolver.delete(mContactUrl, selection, selectionArgs);
            }
        }
        else //if it is null, delete from all
        {
            String selection = Imps.Contacts.USERNAME + "=?";
            String[] selectionArgs = { username };
            mResolver.delete(mContactUrl, selection, selectionArgs);

        }

        // clear the history message if the contact doesn't exist in any list
        // anymore.
        if (mAdaptee.getContact(contact.getAddress()) == null) {
            clearHistoryMessages(username);
        }
    }

    Uri insertContactContent(Contact contact, long listId, int type, int subType, int subStatus) {

        ContentValues values = getContactContentValues(contact, listId);
        values.put(Imps.Contacts.TYPE, type);
        values.put(Imps.Contacts.SUBSCRIPTION_TYPE,subType);
        values.put(Imps.Contacts.SUBSCRIPTION_STATUS,subStatus);

        Uri uri = mResolver.insert(mContactUrl, values);

        ContentValues presenceValues = getPresenceValues(contact);
        mResolver.insert(Imps.Presence.CONTENT_URI, presenceValues);

        return uri;
    }

    private ContentValues getContactContentValues(Contact contact, long listId) {

        final String username = mAdaptee.normalizeAddress(contact.getAddress().getAddress());
        final String nickname = contact.getName();

        int type = contact.getType();

        /**
        if (isTemporary(username)) {
            type = Imps.Contacts.TYPE_TEMPORARY;
        }**/

        if (isBlocked(username)) {
            type = Imps.Contacts.TYPE_BLOCKED;
        }

        ContentValues values = new ContentValues(4);
        values.put(Imps.Contacts.USERNAME, username);
        values.put(Imps.Contacts.NICKNAME, nickname);
        values.put(Imps.Contacts.CONTACTLIST, listId);
        values.put(Imps.Contacts.TYPE, type);
        return values;
    }

    void clearHistoryMessages(String contact) {
        Uri uri = Imps.Messages.getContentUriByContact(mConn.getAccountId(), contact);
        mResolver.delete(uri, null, null);
    }

    private ContentValues getPresenceValues(Contact c) {
        Presence p = c.getPresence();
        ContentValues values = new ContentValues(3);        
        values.put(Imps.Contacts.PRESENCE_STATUS, convertPresenceStatus(p));
        values.put(Imps.Contacts.PRESENCE_CUSTOM_STATUS, p.getStatusText());
        values.put(Imps.Presence.CLIENT_TYPE, translateClientType(p));
        return values;
    }

    private int translateClientType(Presence presence) {
        int clientType = presence.getClientType();
        switch (clientType) {
        case Presence.CLIENT_TYPE_MOBILE:
            return Imps.Presence.CLIENT_TYPE_MOBILE;
        default:
            return Imps.Presence.CLIENT_TYPE_DEFAULT;
        }
    }

    /**
     * Converts the presence status to the value defined for ImProvider.
     *
     * @param presence The presence from the IM engine.
     * @return The status value defined in for ImProvider.
     */
    public static int convertPresenceStatus(Presence presence) {
        switch (presence.getStatus()) {
        case Presence.AVAILABLE:
            return Presence.AVAILABLE;

        case Presence.IDLE:
            return Presence.IDLE;

        case Presence.AWAY:
            return Presence.AWAY;

        case Presence.DO_NOT_DISTURB:
            return Presence.DO_NOT_DISTURB;

        case Presence.OFFLINE:
            return Presence.OFFLINE;
        }

        // impossible...
        RemoteImService.debug("Illegal presence status value " + presence.getStatus());
        return Presence.AVAILABLE;
    }

    
    public void clearOnLogout() {
        clearValidatedContactsAndLists();
        clearTemporaryContacts();
       // clearPresence();
    }

    /**
     * Clears the list of validated contacts and contact lists. As contacts and
     * contacts lists are added after login, contacts and contact lists are
     * stored as "validated contacts". After initial download of contacts is
     * complete, any contacts and contact lists that remain in the database, but
     * are not in the validated list, are obsolete and should be removed. This
     * function resets that list for use upon login.
     */
    private void clearValidatedContactsAndLists() {
        // clear the list of validated contacts, contact lists, and blocked contacts
    //    mValidatedContacts.clear();
     //   mValidatedContactLists.clear();
     //   mValidatedBlockedContacts.clear();
    }

    /**
     * Clear the temporary contacts in the database. As contacts are persist
     * between IM sessions, the temporary contacts need to be cleared after
     * logout.
     */
    private void clearTemporaryContacts() {
    //    String selection = Imps.Contacts.CONTACTLIST + "=" + FAKE_TEMPORARY_LIST_ID;
    //    mResolver.delete(mContactUrl, selection, null);
    }

    /**
     * Clears the presence of the all contacts. As contacts are persist between
     * IM sessions, the presence need to be cleared after logout.
     */
    void clearPresence() {
        StringBuilder where = new StringBuilder();
        where.append(Imps.Presence.CONTACT_ID);
        where.append(" in (select _id from contacts where ");
        where.append(Imps.Contacts.ACCOUNT);
        where.append("=");
        where.append(mConn.getAccountId());
        where.append(")");
        mResolver.delete(Imps.Presence.CONTENT_URI, where.toString(), null);
    }

    void closeChatSession(String address) {
        ChatSessionManagerAdapter chatSessionManager = (ChatSessionManagerAdapter) mConn
                .getChatSessionManager();
        ChatSessionAdapter session = (ChatSessionAdapter) chatSessionManager
                .getChatSession(address);
        if (session != null) {
            session.leave();
        }
    }

    void updateChatPresence(String address, String nickname, Presence p) {
        ChatSessionManagerAdapter sessionManager = (ChatSessionManagerAdapter) mConn
                .getChatSessionManager();
        // TODO: This only find single chat sessions, we need to go through all
        // active chat sessions and find if the contact is a participant of the
        // session.
        ChatSessionAdapter session = (ChatSessionAdapter) sessionManager.getChatSession(address);
        if (session != null) {
            session.insertPresenceUpdatesMsg(nickname, p);
        }
    }

    private void seedInitialPresences() {
        Builder builder = Imps.Presence.SEED_PRESENCE_BY_ACCOUNT_CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, mConn.getAccountId());
        mResolver.insert(builder.build(), new ContentValues(0));
    }



}
