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

import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact extends ImEntity implements Parcelable {

    private Address mAddress;
    private String mName;
    private Presence mPresence;

    private String mForwardAddress;

    private int mSubscriptionType = -1;

    private int mSubscriptionStatus = -1;

    //This is based on the Imps.Contacts.TYPE values
    private int mType;

    public Contact(Address address) {
        mAddress = address;

        mName = address.getUser();

        mPresence = new Presence();

        mForwardAddress = "";

        mType = Imps.Contacts.TYPE_NORMAL;
    }

    public Contact(Address address, String name, int type) {
        mAddress = address;

        if (name != null)
            mName = name;
        else
            mName = address.getUser();

        mPresence = new Presence();

        mForwardAddress = "";

        mType = type;
    }

    public Contact(Parcel source) {
        mAddress = AddressParcelHelper.readFromParcel(source);
        mName = source.readString();
        mPresence = new Presence(source);
        mForwardAddress = source.readString();
        mType = source.readInt();
    }

    public Address getAddress() {
        return mAddress;
    }

    public void setType (int type)
    {
        mType = type;
    }

    public int getType ()
    {
        return mType;
    }

    public String getName() {
        return mName;
    }

    public void setName( String aName ) {
        mName = aName;
    }

    public Presence getPresence() {
        return mPresence;
    }

    public void setForwardingAddress (String forwardAddress)
    {
        mForwardAddress = forwardAddress;
    }

    public String getForwardingAddress ()
    {
        return mForwardAddress;
    }

    public boolean equals(Object other) {

        return other instanceof Contact && mAddress.getBareAddress().equals(((Contact) other).getAddress().getBareAddress());
    }

    public int hashCode() {
        return mAddress.hashCode();
    }

    /* Set the presence of the Contact. Note that this method is public but not
     * provide to the user.
     *
     * @param presence the new presence
     */
    public void setPresence(Presence presence) {
        mPresence = presence;
        if (mPresence != null && mPresence.getResource() != null)
            mAddress = new XmppAddress(mAddress.getBareAddress() + '/' + mPresence.getResource());
    }

    public void writeToParcel(Parcel dest, int flags) {
        AddressParcelHelper.writeToParcel(dest, mAddress);
        dest.writeString(mName);
        mPresence.writeToParcel(dest, 0);
        dest.writeString(mForwardAddress);
    }

    public int describeContents() {
        return 0;
    }

    public final static Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };


    public int getSubscriptionType() {
        return mSubscriptionType;
    }

    public void setSubscriptionType(int mSubscriptionType) {
        this.mSubscriptionType = mSubscriptionType;
    }

    public int getSubscriptionStatus() {
        return mSubscriptionStatus;
    }

    public void setSubscriptionStatus(int mSubscriptionStatus) {
        this.mSubscriptionStatus = mSubscriptionStatus;
    }

}
