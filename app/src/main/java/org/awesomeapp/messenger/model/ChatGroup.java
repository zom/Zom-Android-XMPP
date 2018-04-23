/*
 * Copyright (C) 2007 Esmertec AG. Copyright (C) 2007 The Android Open Source
 * Project
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

package org.awesomeapp.messenger.model;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatGroup extends ImEntity {

    private ChatGroupManager mManager;
    private Address mAddress;
    private String mName;
    private HashMap<String, Contact> mMembers;
    private HashMap<String, Contact> mGroupAddressToContactMap;
    private List<Contact> mOwners;
    private List<Contact> mAdmins;
    private CopyOnWriteArrayList<GroupMemberListener> mMemberListeners;

    public ChatGroup(Address address, String name, ChatGroupManager manager) {

        mAddress = address;
        mName = name;
        mManager = manager;
        mMembers = new HashMap<>();
        mGroupAddressToContactMap = new HashMap<>();

        mMemberListeners = new CopyOnWriteArrayList<GroupMemberListener>();
    }

    @Override
    public Address getAddress() {
        return mAddress;
    }

    /**
     * Gets the name of the group.
     *
     * @return the name of the group.
     */
    public String getName() {
        return mName;
    }

    /*
    Set's the name of the group. The XMPP "subject" can change
     */
    public void setName (String name) {
        mName = name;

        for (GroupMemberListener listener : mMemberListeners) {
            listener.onSubjectChanged(this,name);
        }
    }

    public void addMemberListener(GroupMemberListener listener) {
        mMemberListeners.add(listener);
    }

    public void removeMemberListener(GroupMemberListener listener) {
        mMemberListeners.remove(listener);
    }

    /**
     * Gets an unmodifiable collection of the members of the group.
     *
     * @return an unmodifiable collection of the members of the group.
     */
    public List<Contact> getMembers() {
        return Collections.unmodifiableList(new ArrayList(mMembers.values()));
    }

    /**
     * Gets an unmodifiable collection of the members of the group.
     *
     * @return an unmodifiable collection of the members of the group.
     */
    public Contact getMember(String jid) {
        Contact member = mMembers.get(jid);

        if (member == null)
            member = mGroupAddressToContactMap.get(jid);

        return member;
    }

    /**
     * Notifies that a contact has joined into this group.
     *
     * @param newContact the {@link Contact} who has joined into the group.
     */
    public synchronized void notifyMemberJoined(String groupAddress, Contact newContact) {

        // Clear the DB on first join
        if (mMembers.size() == 0) {
            //clearMembers(true);
        }

        Contact contact = mMembers.get(newContact.getAddress().getBareAddress());

        if (contact == null) {
            mMembers.put(newContact.getAddress().getBareAddress(), newContact);

            if (groupAddress != null)
                mGroupAddressToContactMap.put(groupAddress, newContact);

            for (GroupMemberListener listener : mMemberListeners) {
                listener.onMemberJoined(this, newContact);
            }
        } else {
            if (groupAddress != null)
                mGroupAddressToContactMap.put(groupAddress, contact);
        }
    }

    public synchronized void notifyMemberRoleUpdate(Contact newContact, String role, String affiliation) {
        Contact contact = mMembers.get(newContact.getAddress().getBareAddress());
        if (contact != null) {
            for (GroupMemberListener listener : mMemberListeners) {
                listener.onMemberRoleChanged(this, contact, role, affiliation);
            }
        }

    }

    /**
     * Notifies that a contact has left this group.
     *
     * @param contact the contact who has left this group.
     */
    public void notifyMemberLeft(String groupAddress, Contact contact) {
        if (contact == null && !TextUtils.isEmpty(groupAddress)) {
            contact = mGroupAddressToContactMap.get(groupAddress);
        }
        if (contact != null && mMembers.remove(contact.getAddress().getBareAddress())!=null) {

            Object[] keys = mGroupAddressToContactMap.keySet().toArray();

            for (Object groupAddressEntry : keys)
            {
                Contact member = mGroupAddressToContactMap.get(groupAddressEntry);
                if (contact.getAddress().equals(member.getAddress()))
                    mGroupAddressToContactMap.remove(groupAddressEntry);
            }

            for (GroupMemberListener listener : mMemberListeners) {
                listener.onMemberLeft(this, contact);
            }
        }
    }

    /**
     * Notifies that previous operation on this group has failed.
     *
     * @param error the error information.
     */
    void notifyGroupMemberError(ImErrorInfo error) {
        for (GroupMemberListener listener : mMemberListeners) {
            listener.onError(this, error);
        }
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    /*
    clear the list of members
     */
    public synchronized void clearMembers (boolean deleteFromDB) {
        Object[] members = mMembers.values().toArray();
        for (Object member : members) {
            if (deleteFromDB) {
                notifyMemberLeft(null, (Contact) member);
            } else {
                notifyMemberRoleUpdate((Contact) member, "none", null);
            }
        }
        if (deleteFromDB) {
            for (GroupMemberListener listener : mMemberListeners) {
                listener.onMembersReset();
            }
        }
    }

    /*
    set the list of members
     */
    public void setMembers (List<Contact> members)
    {
        clearMembers(true);

        for (Contact newContact : members)
        {
            notifyMemberJoined(null, newContact);
        }

    }

    public void setOwners (List<Contact> owners)
    {
        mOwners = owners;
    }

    public List<Contact> getOwners ()
    {
        return mOwners;
    }

    public void setAdmins (List<Contact> admins)
    {
        mAdmins = admins;
    }

    public List<Contact> getAdmins ()
    {
        return mAdmins;
    }

    public synchronized void beginMemberUpdates() {
        for (GroupMemberListener listener : mMemberListeners) {
            listener.onBeginMemberUpdates(this);
        }
    }

    public synchronized void endMemberUpdates() {
        for (GroupMemberListener listener : mMemberListeners) {
            listener.onEndMemberUpdates(this);
        }
    }

}
