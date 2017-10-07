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
    private HashMap<String, Contact> mRealAddressToContactMap;
    private HashMap<String, Contact> mNicknameToContactMap;
    private Contact mOwner;
    private CopyOnWriteArrayList<GroupMemberListener> mMemberListeners;

    public ChatGroup(Address address, String name, ChatGroupManager manager) {

        mAddress = address;
        mName = name;
        mManager = manager;
        mMembers = new HashMap<>();
        mGroupAddressToContactMap = new HashMap<>();
        mRealAddressToContactMap = new HashMap<>();
        mNicknameToContactMap = new HashMap<>();

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

    private boolean isGroupAddress(Address address) {
        return address.getBareAddress().equals(mAddress.getBareAddress());
    }

    /**
     * Notifies that a contact has joined into this group.
     *
     * @param newContact the {@link Contact} who has joined into the group.
     */
    public synchronized void notifyMemberJoined(Contact newContact) {
        Contact contact = mMembers.get(newContact.getAddress().getAddress());
        if (contact == null) {
            if (isGroupAddress(newContact.getAddress())) {
                // This is a group address
                contact = mGroupAddressToContactMap.get(newContact.getAddress().getAddress());
            } else {
                contact = mRealAddressToContactMap.get(newContact.getAddress().getBareAddress());
            }
        }
        if (contact == null && newContact.getName() != null) {
            // Try to find by nickname
            contact = mNicknameToContactMap.get(newContact.getName());
        }

        if (contact != null) {
            if (isGroupAddress(contact.getAddress()) && !isGroupAddress(newContact.getAddress())) {
                mMembers.remove(contact.getAddress().getAddress());
                mGroupAddressToContactMap.remove(contact.getAddress().getAddress());
                if (!TextUtils.isEmpty(contact.getName()))
                    mNicknameToContactMap.remove(contact.getName());
                mMembers.put(newContact.getAddress().getBareAddress(), newContact);
                mRealAddressToContactMap.put(newContact.getAddress().getBareAddress(), newContact);
                if (!TextUtils.isEmpty(newContact.getName())) {
                    mNicknameToContactMap.put(newContact.getName(), newContact);
                } else if (!TextUtils.isEmpty(contact.getName())) {
                    // Reuse old nick
                    mNicknameToContactMap.put(contact.getName(), newContact);
                }
                for (GroupMemberListener listener : mMemberListeners) {
                    listener.onMemberChanged(this, contact, newContact);
                }
            }
        } else {
            mMembers.put(newContact.getAddress().getAddress(), newContact);
            if (isGroupAddress(newContact.getAddress())) {
                // This is a group address
                mGroupAddressToContactMap.put(newContact.getAddress().getAddress(), newContact);
            } else {
                mRealAddressToContactMap.put(newContact.getAddress().getBareAddress(), newContact);
            }
            if (!TextUtils.isEmpty(newContact.getName())) {
                mNicknameToContactMap.put(newContact.getName(), newContact);
            }
            if (!TextUtils.isEmpty(newContact.getName())) {
                mNicknameToContactMap.put(newContact.getName(), newContact);
            }
            for (GroupMemberListener listener : mMemberListeners) {
                listener.onMemberJoined(this, newContact);
            }
        }
    }

    /**
     * Notifies that a contact has left this group.
     *
     * @param contact the contact who has left this group.
     */
    public void notifyMemberLeft(Contact contact) {

        if (mMembers.remove(contact.getAddress().getBareAddress())!=null) {

            Object[] keys = mGroupAddressToContactMap.keySet().toArray();

            for (Object groupAddress : keys)
            {
                Contact member = mGroupAddressToContactMap.get(groupAddress);
                if (contact.getAddress().equals(member.getAddress()))
                    mGroupAddressToContactMap.remove(groupAddress);
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
    clear the list of members, i.e. mark all as role "none" in the db
     */
    public synchronized void clearMembers ()
    {
/*
        Object[] members = mMembers.values().toArray();
        for (Object member : members)
        {
            notifyMemberLeft((Contact)member);
        }

*/
        for (GroupMemberListener listener : mMemberListeners) {
            listener.onMembersReset(this);
        }
    }

    /*
    set the list of members
     */
    public void setMembers (List<Contact> members)
    {
        //clearMembers();
        for (Contact newContact : members)
        {
            notifyMemberJoined(newContact);
        }

    }

    public void setOwner (Contact owner)
    {

        mOwner = owner;
    }

    public Contact getOwner ()
    {
        return mOwner;
    }

    public void notifyMemberRoleChanged(Contact contact, String role, String affiliation) {
        for (GroupMemberListener listener : mMemberListeners) {
            listener.onMemberRoleChanged(this, contact, role, affiliation);
        }
    }
}
