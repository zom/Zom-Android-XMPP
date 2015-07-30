/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/service/IDataListener.aidl
 */
package org.awesomeapp.messenger.service;
public interface IDataListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.service.IDataListener
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.service.IDataListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.service.IDataListener interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.service.IDataListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.service.IDataListener))) {
return ((org.awesomeapp.messenger.service.IDataListener)iin);
}
return new org.awesomeapp.messenger.service.IDataListener.Stub.Proxy(obj);
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
case TRANSACTION_onTransferComplete:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
java.lang.String _arg3;
_arg3 = data.readString();
java.lang.String _arg4;
_arg4 = data.readString();
java.lang.String _arg5;
_arg5 = data.readString();
this.onTransferComplete(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
reply.writeNoException();
return true;
}
case TRANSACTION_onTransferFailed:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
java.lang.String _arg3;
_arg3 = data.readString();
java.lang.String _arg4;
_arg4 = data.readString();
this.onTransferFailed(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_onTransferProgress:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
java.lang.String _arg3;
_arg3 = data.readString();
float _arg4;
_arg4 = data.readFloat();
this.onTransferProgress(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_onTransferRequested:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
java.lang.String _arg3;
_arg3 = data.readString();
boolean _result = this.onTransferRequested(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.service.IDataListener
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
@Override public void onTransferComplete(boolean outgoing, java.lang.String offerId, java.lang.String from, java.lang.String url, java.lang.String type, java.lang.String fileLocalPath) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((outgoing)?(1):(0)));
_data.writeString(offerId);
_data.writeString(from);
_data.writeString(url);
_data.writeString(type);
_data.writeString(fileLocalPath);
mRemote.transact(Stub.TRANSACTION_onTransferComplete, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onTransferFailed(boolean outgoing, java.lang.String offerId, java.lang.String from, java.lang.String url, java.lang.String reason) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((outgoing)?(1):(0)));
_data.writeString(offerId);
_data.writeString(from);
_data.writeString(url);
_data.writeString(reason);
mRemote.transact(Stub.TRANSACTION_onTransferFailed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onTransferProgress(boolean outgoing, java.lang.String offerId, java.lang.String from, java.lang.String url, float f) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((outgoing)?(1):(0)));
_data.writeString(offerId);
_data.writeString(from);
_data.writeString(url);
_data.writeFloat(f);
mRemote.transact(Stub.TRANSACTION_onTransferProgress, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean onTransferRequested(java.lang.String offerId, java.lang.String from, java.lang.String to, java.lang.String transferUrl) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(offerId);
_data.writeString(from);
_data.writeString(to);
_data.writeString(transferUrl);
mRemote.transact(Stub.TRANSACTION_onTransferRequested, _data, _reply, 0);
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
static final int TRANSACTION_onTransferComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onTransferFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onTransferProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onTransferRequested = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
}
public void onTransferComplete(boolean outgoing, java.lang.String offerId, java.lang.String from, java.lang.String url, java.lang.String type, java.lang.String fileLocalPath) throws android.os.RemoteException;
public void onTransferFailed(boolean outgoing, java.lang.String offerId, java.lang.String from, java.lang.String url, java.lang.String reason) throws android.os.RemoteException;
public void onTransferProgress(boolean outgoing, java.lang.String offerId, java.lang.String from, java.lang.String url, float f) throws android.os.RemoteException;
public boolean onTransferRequested(java.lang.String offerId, java.lang.String from, java.lang.String to, java.lang.String transferUrl) throws android.os.RemoteException;
}
