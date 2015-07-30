/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/service/IContactListListener.aidl
 */
package org.awesomeapp.messenger.service;
public interface IContactListListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.service.IContactListListener
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.service.IContactListListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.service.IContactListListener interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.service.IContactListListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.service.IContactListListener))) {
return ((org.awesomeapp.messenger.service.IContactListListener)iin);
}
return new org.awesomeapp.messenger.service.IContactListListener.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_onContactChange:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
org.awesomeapp.messenger.service.IContactList _arg1;
_arg1 = org.awesomeapp.messenger.service.IContactList.Stub.asInterface(data.readStrongBinder());
org.awesomeapp.messenger.model.Contact _arg2;
if ((0!=data.readInt())) {
_arg2 = org.awesomeapp.messenger.model.Contact.CREATOR.createFromParcel(data);
}
else {
_arg2 = null;
}
this.onContactChange(_arg0, _arg1, _arg2);
return true;
}
case TRANSACTION_onAllContactListsLoaded:
{
data.enforceInterface(DESCRIPTOR);
this.onAllContactListsLoaded();
return true;
}
case TRANSACTION_onContactsPresenceUpdate:
{
data.enforceInterface(DESCRIPTOR);
org.awesomeapp.messenger.model.Contact[] _arg0;
_arg0 = data.createTypedArray(org.awesomeapp.messenger.model.Contact.CREATOR);
this.onContactsPresenceUpdate(_arg0);
return true;
}
case TRANSACTION_onContactError:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
org.awesomeapp.messenger.model.ImErrorInfo _arg1;
if ((0!=data.readInt())) {
_arg1 = org.awesomeapp.messenger.model.ImErrorInfo.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
java.lang.String _arg2;
_arg2 = data.readString();
org.awesomeapp.messenger.model.Contact _arg3;
if ((0!=data.readInt())) {
_arg3 = org.awesomeapp.messenger.model.Contact.CREATOR.createFromParcel(data);
}
else {
_arg3 = null;
}
this.onContactError(_arg0, _arg1, _arg2, _arg3);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.service.IContactListListener
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
     * Called when:
     *  <ul>
     *  <li> a contact list has been created, deleted, renamed or loaded, or
     *  <li> a contact has been added to or removed from a list, or
     *  <li> a contact has been blocked or unblocked
     *  </ul>
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onContactChange(int, ContactList, Contact)
     */
@Override public void onContactChange(int type, org.awesomeapp.messenger.service.IContactList list, org.awesomeapp.messenger.model.Contact contact) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(type);
_data.writeStrongBinder((((list!=null))?(list.asBinder()):(null)));
if ((contact!=null)) {
_data.writeInt(1);
contact.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onContactChange, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
     * Called when all the contact lists have been loaded from server.
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onAllContactListsLoaded()
     */
@Override public void onAllContactListsLoaded() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onAllContactListsLoaded, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
     * Called when one or more contacts' presence information has updated.
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onContactsPresenceUpdate(Contact[])
     */
@Override public void onContactsPresenceUpdate(org.awesomeapp.messenger.model.Contact[] contacts) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedArray(contacts, 0);
mRemote.transact(Stub.TRANSACTION_onContactsPresenceUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
/**
     * Called when a previous contact related request has failed.
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onContactError(int, ImErrorInfo, String, Contact)
     */
@Override public void onContactError(int errorType, org.awesomeapp.messenger.model.ImErrorInfo error, java.lang.String listName, org.awesomeapp.messenger.model.Contact contact) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(errorType);
if ((error!=null)) {
_data.writeInt(1);
error.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeString(listName);
if ((contact!=null)) {
_data.writeInt(1);
contact.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onContactError, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onContactChange = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onAllContactListsLoaded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onContactsPresenceUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onContactError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
}
/**
     * Called when:
     *  <ul>
     *  <li> a contact list has been created, deleted, renamed or loaded, or
     *  <li> a contact has been added to or removed from a list, or
     *  <li> a contact has been blocked or unblocked
     *  </ul>
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onContactChange(int, ContactList, Contact)
     */
public void onContactChange(int type, org.awesomeapp.messenger.service.IContactList list, org.awesomeapp.messenger.model.Contact contact) throws android.os.RemoteException;
/**
     * Called when all the contact lists have been loaded from server.
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onAllContactListsLoaded()
     */
public void onAllContactListsLoaded() throws android.os.RemoteException;
/**
     * Called when one or more contacts' presence information has updated.
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onContactsPresenceUpdate(Contact[])
     */
public void onContactsPresenceUpdate(org.awesomeapp.messenger.model.Contact[] contacts) throws android.os.RemoteException;
/**
     * Called when a previous contact related request has failed.
     *
     * @see org.awesomeapp.messenger.engine.ContactListListener#onContactError(int, ImErrorInfo, String, Contact)
     */
public void onContactError(int errorType, org.awesomeapp.messenger.model.ImErrorInfo error, java.lang.String listName, org.awesomeapp.messenger.model.Contact contact) throws android.os.RemoteException;
}
