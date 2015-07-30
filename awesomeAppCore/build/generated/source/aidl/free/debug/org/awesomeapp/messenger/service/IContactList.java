/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/service/IContactList.aidl
 */
package org.awesomeapp.messenger.service;
public interface IContactList extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.service.IContactList
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.service.IContactList";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.service.IContactList interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.service.IContactList asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.service.IContactList))) {
return ((org.awesomeapp.messenger.service.IContactList)iin);
}
return new org.awesomeapp.messenger.service.IContactList.Stub.Proxy(obj);
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
case TRANSACTION_getName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_setName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.setName(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_addContact:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _result = this.addContact(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_removeContact:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _result = this.removeContact(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setDefault:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setDefault(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isDefault:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isDefault();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.service.IContactList
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
     * Gets the name of the list.
     */
@Override public java.lang.String getName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Sets the name of the list.
     */
@Override public void setName(java.lang.String name) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(name);
mRemote.transact(Stub.TRANSACTION_setName, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Adds a new contact to the list.
     */
@Override public int addContact(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_addContact, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Removes a contact in the list.
     */
@Override public int removeContact(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_removeContact, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Sets the list to the default list.
     */
@Override public void setDefault(boolean isDefault) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((isDefault)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setDefault, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Tells if the list is the default list.
     */
@Override public boolean isDefault() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isDefault, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_setName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_addContact = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_removeContact = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_setDefault = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_isDefault = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
}
/**
     * Gets the name of the list.
     */
public java.lang.String getName() throws android.os.RemoteException;
/**
     * Sets the name of the list.
     */
public void setName(java.lang.String name) throws android.os.RemoteException;
/**
     * Adds a new contact to the list.
     */
public int addContact(java.lang.String address) throws android.os.RemoteException;
/**
     * Removes a contact in the list.
     */
public int removeContact(java.lang.String address) throws android.os.RemoteException;
/**
     * Sets the list to the default list.
     */
public void setDefault(boolean isDefault) throws android.os.RemoteException;
/**
     * Tells if the list is the default list.
     */
public boolean isDefault() throws android.os.RemoteException;
}
