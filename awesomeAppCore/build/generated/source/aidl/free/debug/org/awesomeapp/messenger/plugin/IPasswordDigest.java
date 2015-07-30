/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/plugin/IPasswordDigest.aidl
 */
package org.awesomeapp.messenger.plugin;
/**
 * The password digest method used in IMPS login transaction.
 */
public interface IPasswordDigest extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.plugin.IPasswordDigest
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.plugin.IPasswordDigest";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.plugin.IPasswordDigest interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.plugin.IPasswordDigest asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.plugin.IPasswordDigest))) {
return ((org.awesomeapp.messenger.plugin.IPasswordDigest)iin);
}
return new org.awesomeapp.messenger.plugin.IPasswordDigest.Stub.Proxy(obj);
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
case TRANSACTION_getSupportedDigestSchema:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String[] _result = this.getSupportedDigestSchema();
reply.writeNoException();
reply.writeStringArray(_result);
return true;
}
case TRANSACTION_digest:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
java.lang.String _result = this.digest(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.plugin.IPasswordDigest
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
     * Gets an array of supported digest schema.
     *
     * @return an array of digest schema
     */
@Override public java.lang.String[] getSupportedDigestSchema() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSupportedDigestSchema, _data, _reply, 0);
_reply.readException();
_result = _reply.createStringArray();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Generates the digest bytes of the password.
     *
     * @param schema The digest schema to use.
     * @param nonce The nonce string returned by the server.
     * @param password The user password.
     * @return The digest bytes of the password.
     */
@Override public java.lang.String digest(java.lang.String schema, java.lang.String nonce, java.lang.String password) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(schema);
_data.writeString(nonce);
_data.writeString(password);
mRemote.transact(Stub.TRANSACTION_digest, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getSupportedDigestSchema = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_digest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
/**
     * Gets an array of supported digest schema.
     *
     * @return an array of digest schema
     */
public java.lang.String[] getSupportedDigestSchema() throws android.os.RemoteException;
/**
     * Generates the digest bytes of the password.
     *
     * @param schema The digest schema to use.
     * @param nonce The nonce string returned by the server.
     * @param password The user password.
     * @return The digest bytes of the password.
     */
public java.lang.String digest(java.lang.String schema, java.lang.String nonce, java.lang.String password) throws android.os.RemoteException;
}
