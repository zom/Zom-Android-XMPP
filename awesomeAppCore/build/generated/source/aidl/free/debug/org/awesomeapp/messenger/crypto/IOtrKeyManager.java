/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/crypto/IOtrKeyManager.aidl
 */
package org.awesomeapp.messenger.crypto;
public interface IOtrKeyManager extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.crypto.IOtrKeyManager
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.crypto.IOtrKeyManager";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.crypto.IOtrKeyManager interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.crypto.IOtrKeyManager asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.crypto.IOtrKeyManager))) {
return ((org.awesomeapp.messenger.crypto.IOtrKeyManager)iin);
}
return new org.awesomeapp.messenger.crypto.IOtrKeyManager.Stub.Proxy(obj);
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
case TRANSACTION_verifyUser:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.verifyUser(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unverifyUser:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.unverifyUser(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isVerifiedUser:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.isVerifiedUser(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getLocalFingerprint:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getLocalFingerprint(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getRemoteFingerprint:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getRemoteFingerprint(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getRemoteFingerprints:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String[] _result = this.getRemoteFingerprints(_arg0);
reply.writeNoException();
reply.writeStringArray(_result);
return true;
}
case TRANSACTION_generateLocalKeyPair:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.generateLocalKeyPair(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_importOtrKeyStoreWithPassword:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
boolean _result = this.importOtrKeyStoreWithPassword(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.crypto.IOtrKeyManager
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
     * Verify the key for a given address.
     */
@Override public void verifyUser(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_verifyUser, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Revoke the verification for the key for a given address.
     */
@Override public void unverifyUser(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_unverifyUser, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Tells if the fingerprint of the remote user address has been verified.
     */
@Override public boolean isVerifiedUser(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_isVerifiedUser, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Returns the fingerprint for the local user's key for a given JID
     */
@Override public java.lang.String getLocalFingerprint(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_getLocalFingerprint, _data, _reply, 0);
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
     * Returns the fingerprint for a remote user's key for a given JID
     */
@Override public java.lang.String getRemoteFingerprint(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_getRemoteFingerprint, _data, _reply, 0);
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
     * Returns the fingerprints for a remote user's keys for a given user@domain
     */
@Override public java.lang.String[] getRemoteFingerprints(java.lang.String addressNoResource) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(addressNoResource);
mRemote.transact(Stub.TRANSACTION_getRemoteFingerprints, _data, _reply, 0);
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
     * generate a new local private/public key pair.
     */
@Override public void generateLocalKeyPair(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_generateLocalKeyPair, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
    * import otr key store
    */
@Override public boolean importOtrKeyStoreWithPassword(java.lang.String filePath, java.lang.String password) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filePath);
_data.writeString(password);
mRemote.transact(Stub.TRANSACTION_importOtrKeyStoreWithPassword, _data, _reply, 0);
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
static final int TRANSACTION_verifyUser = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unverifyUser = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_isVerifiedUser = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getLocalFingerprint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getRemoteFingerprint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getRemoteFingerprints = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_generateLocalKeyPair = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_importOtrKeyStoreWithPassword = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
}
/**
     * Verify the key for a given address.
     */
public void verifyUser(java.lang.String address) throws android.os.RemoteException;
/**
     * Revoke the verification for the key for a given address.
     */
public void unverifyUser(java.lang.String address) throws android.os.RemoteException;
/**
     * Tells if the fingerprint of the remote user address has been verified.
     */
public boolean isVerifiedUser(java.lang.String address) throws android.os.RemoteException;
/**
     * Returns the fingerprint for the local user's key for a given JID
     */
public java.lang.String getLocalFingerprint(java.lang.String address) throws android.os.RemoteException;
/**
     * Returns the fingerprint for a remote user's key for a given JID
     */
public java.lang.String getRemoteFingerprint(java.lang.String address) throws android.os.RemoteException;
/**
     * Returns the fingerprints for a remote user's keys for a given user@domain
     */
public java.lang.String[] getRemoteFingerprints(java.lang.String addressNoResource) throws android.os.RemoteException;
/**
     * generate a new local private/public key pair.
     */
public void generateLocalKeyPair(java.lang.String address) throws android.os.RemoteException;
/**
    * import otr key store
    */
public boolean importOtrKeyStoreWithPassword(java.lang.String filePath, java.lang.String password) throws android.os.RemoteException;
}
