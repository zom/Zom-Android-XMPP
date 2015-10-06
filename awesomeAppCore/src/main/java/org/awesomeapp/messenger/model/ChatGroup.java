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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatGroup extends ImEntity {
    private ChatGroupManager mManager;
    private Address mAddress;
    private String mName;
    private HashMap<String, Contact> mMembers;
    private CopyOnWriteArrayList<GroupMemberListener> mMemberListeners;

    public ChatGroup(Address address, String name, ChatGroupManager manager) {
        this(address, name, null, manager);
    }

    public ChatGroup(Address address, String name, Collection<Contact> members,
            ChatGroupManager manager) {

        mAddress = address;
        mName = name;
        mManager = manager;
        mMembers = new HashMap<String,Contact>();

        if (members != null)
            for (Contact contact : members)
                mMembers.put(contact.getAddress().getAddress(), contact);

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
     * Adds a member to this group. TODO: more docs on async callbacks.
     *
     * @param contact the member to add.
     */
    public synchronized void addMemberAsync(Contact contact) {
        //mManager.addGroupMemberAsync(this, contact);

        notifyMemberJoined(contact);
    }

    /**
     * Removes a member from this group. TODO: more docs on async callbacks.
     *
     * @param contact the member to remove.
     */
    public synchronized void removeMemberAsync(Contact contact) {
        //mManager.removeGroupMemberAsync(this, contact);

        notifyMemberLeft(contact);
    }

    /**
     * Notifies that a contact has joined into this group.
     *
     * @param newContact the {@link Contact} who has joined into the group.
     */
    void notifyMemberJoined(Contact newContact) {

        Contact contact = mMembers.get(newContact.getAddress().getAddress());

        if (contact == null) {
            mMembers.put(newContact.getAddress().getAddress(), newContact);
            for (GroupMemberListener listener : mMemberListeners) {
                listener.onMemberJoined(this, newContact);
            }
        }
        else
        {

            //just update the presence
            contact.setPresence(newContact.getPresence());
        }
    }

    /**
     * Notifies that a contact has left this group.
     *
     * @param contact the contact who has left this group.
     */
    void notifyMemberLeft(Contact contact) {
        if (mMembers.remove(contact.getAddress().getAddress())!=null) {
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
}
