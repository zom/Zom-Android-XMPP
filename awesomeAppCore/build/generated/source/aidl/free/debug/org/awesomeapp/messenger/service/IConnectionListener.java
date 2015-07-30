/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/service/IConnectionListener.aidl
 */
package org.awesomeapp.messenger.service;
public interface IConnectionListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.service.IConnectionListener
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.service.IConnectionListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.service.IConnectionListener interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.service.IConnectionListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.service.IConnectionListener))) {
return ((org.awesomeapp.messenger.service.IConnectionListener)iin);
}
return new org.awesomeapp.messenger.service.IConnectionListener.Stub.Proxy(obj);
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
case TRANSACTION_onStateChanged:
{
data.enforceInterface(DESCRIPTOR);
org.awesomeapp.messenger.service.IImConnection _arg0;
_arg0 = org.awesomeapp.messenger.service.IImConnection.Stub.asInterface(data.readStrongBinder());
int _arg1;
_arg1 = data.readInt();
org.awesomeapp.messenger.model.ImErrorInfo _arg2;
if ((0!=data.readInt())) {
_arg2 = org.awesomeapp.messenger.model.ImErrorInfo.CREATOR.createFromParcel(data);
}
else {
_arg2 = null;
}
this.onStateChanged(_arg0, _arg1, _arg2);
return true;
}
case TRANSACTION_onUserPresenceUpdated:
{
data.enforceInterface(DESCRIPTOR);
org.awesomeapp.messenger.service.IImConnection _arg0;
_arg0 = org.awesomeapp.messenger.service.IImConnection.Stub.asInterface(data.readStrongBinder());
this.onUserPresenceUpdated(_arg0);
return true;
}
case TRANSACTION_onUpdatePresenceError:
{
data.enforceInterface(DESCRIPTOR);
org.awesomeapp.messenger.service.IImConnection _arg0;
_arg0 = org.awesomeapp.messenger.service.IImConnection.Stub.asInterface(data.readStrongBinder());
org.awesomeapp.messenger.model.ImErrorInfo _arg1;
if ((0!=data.readInt())) {
_arg1 = org.awesomeapp.messenger.model.ImErrorInfo.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
this.onUpdatePresenceError(_arg0, _arg1);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.service.IConnectionListener
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
@Override public void onStateChanged(org.awesomeapp.messenger.service.IImConnection connection, int state, org.awesomeapp.messenger.model.ImErrorInfo error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((connection!=null))?(connection.asBinder()):(null)));
_data.writeInt(state);
if ((error!=null)) {
_data.writeInt(1);
error.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onStateChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void onUserPresenceUpdated(org.awesomeapp.messenger.service.IImConnection connection) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((connection!=null))?(connection.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_onUserPresenceUpdated, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void onUpdatePresenceError(org.awesomeapp.messenger.service.IImConnection connection, org.awesomeapp.messenger.model.ImErrorInfo error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((connection!=null))?(connection.asBinder()):(null)));
if ((error!=null)) {
_data.writeInt(1);
error.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onUpdatePresenceError, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onStateChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onUserPresenceUpdated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onUpdatePresenceError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public void onStateChanged(org.awesomeapp.messenger.service.IImConnection connection, int state, org.awesomeapp.messenger.model.ImErrorInfo error) throws android.os.RemoteException;
public void onUserPresenceUpdated(org.awesomeapp.messenger.service.IImConnection connection) throws android.os.RemoteException;
public void onUpdatePresenceError(org.awesomeapp.messenger.service.IImConnection connection, org.awesomeapp.messenger.model.ImErrorInfo error) throws android.os.RemoteException;
}
