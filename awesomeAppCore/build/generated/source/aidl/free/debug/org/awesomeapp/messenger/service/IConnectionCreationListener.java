/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/service/IConnectionCreationListener.aidl
 */
package org.awesomeapp.messenger.service;
public interface IConnectionCreationListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.service.IConnectionCreationListener
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.service.IConnectionCreationListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.service.IConnectionCreationListener interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.service.IConnectionCreationListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.service.IConnectionCreationListener))) {
return ((org.awesomeapp.messenger.service.IConnectionCreationListener)iin);
}
return new org.awesomeapp.messenger.service.IConnectionCreationListener.Stub.Proxy(obj);
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
case TRANSACTION_onConnectionCreated:
{
data.enforceInterface(DESCRIPTOR);
org.awesomeapp.messenger.service.IImConnection _arg0;
_arg0 = org.awesomeapp.messenger.service.IImConnection.Stub.asInterface(data.readStrongBinder());
this.onConnectionCreated(_arg0);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.service.IConnectionCreationListener
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
     * Called when a connection is created in the service.
     */
@Override public void onConnectionCreated(org.awesomeapp.messenger.service.IImConnection connection) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((connection!=null))?(connection.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_onConnectionCreated, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onConnectionCreated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
/**
     * Called when a connection is created in the service.
     */
public void onConnectionCreated(org.awesomeapp.messenger.service.IImConnection connection) throws android.os.RemoteException;
}
