/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/n8fr8/StudioProjects/Gibberbot/awesomeAppCore/src/main/aidl/org/awesomeapp/messenger/crypto/IOtrChatSession.aidl
 */
package org.awesomeapp.messenger.crypto;
public interface IOtrChatSession extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.awesomeapp.messenger.crypto.IOtrChatSession
{
private static final java.lang.String DESCRIPTOR = "org.awesomeapp.messenger.crypto.IOtrChatSession";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.awesomeapp.messenger.crypto.IOtrChatSession interface,
 * generating a proxy if needed.
 */
public static org.awesomeapp.messenger.crypto.IOtrChatSession asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.awesomeapp.messenger.crypto.IOtrChatSession))) {
return ((org.awesomeapp.messenger.crypto.IOtrChatSession)iin);
}
return new org.awesomeapp.messenger.crypto.IOtrChatSession.Stub.Proxy(obj);
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
case TRANSACTION_startChatEncryption:
{
data.enforceInterface(DESCRIPTOR);
this.startChatEncryption();
reply.writeNoException();
return true;
}
case TRANSACTION_stopChatEncryption:
{
data.enforceInterface(DESCRIPTOR);
this.stopChatEncryption();
reply.writeNoException();
return true;
}
case TRANSACTION_isChatEncrypted:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isChatEncrypted();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getChatStatus:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getChatStatus();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_initSmpVerification:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.initSmpVerification(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_respondSmpVerification:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.respondSmpVerification(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_verifyKey:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.verifyKey(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unverifyKey:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.unverifyKey(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isKeyVerified:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.isKeyVerified(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getLocalFingerprint:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getLocalFingerprint();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getRemoteFingerprint:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getRemoteFingerprint();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_generateLocalKeyPair:
{
data.enforceInterface(DESCRIPTOR);
this.generateLocalKeyPair();
reply.writeNoException();
return true;
}
case TRANSACTION_getLocalUserId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getLocalUserId();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getRemoteUserId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getRemoteUserId();
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.awesomeapp.messenger.crypto.IOtrChatSession
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
     * Start the OTR encryption on this chat session.
     */
@Override public void startChatEncryption() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_startChatEncryption, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Stop the OTR encryption on this chat session.
     */
@Override public void stopChatEncryption() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopChatEncryption, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Tells if the chat session has OTR encryption running.
     */
@Override public boolean isChatEncrypted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isChatEncrypted, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/** OTR session status - ordinal of SessionStatus */
@Override public int getChatStatus() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getChatStatus, _data, _reply, 0);
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
     * start the SMP verification process
     */
@Override public void initSmpVerification(java.lang.String question, java.lang.String answer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(question);
_data.writeString(answer);
mRemote.transact(Stub.TRANSACTION_initSmpVerification, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * respond to the SMP verification process
     */
@Override public void respondSmpVerification(java.lang.String answer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(answer);
mRemote.transact(Stub.TRANSACTION_respondSmpVerification, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Verify the key for a given address.
     */
@Override public void verifyKey(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_verifyKey, _data, _reply, 0);
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
@Override public void unverifyKey(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_unverifyKey, _data, _reply, 0);
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
@Override public boolean isKeyVerified(java.lang.String address) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(address);
mRemote.transact(Stub.TRANSACTION_isKeyVerified, _data, _reply, 0);
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
     * Returns the fingerprint for the local user's key for a given account address.
     */
@Override public java.lang.String getLocalFingerprint() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
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
     * Returns the fingerprint for a remote user's key for a given account address.
     */
@Override public java.lang.String getRemoteFingerprint() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
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
     * generate a new local private/public key pair.
     */
@Override public void generateLocalKeyPair() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_generateLocalKeyPair, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Returns the user id (jabberid) for the local user
     */
@Override public java.lang.String getLocalUserId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLocalUserId, _data, _reply, 0);
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
     * Returns the user id (jabberid) for the remote user
     */
@Override public java.lang.String getRemoteUserId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRemoteUserId, _data, _reply, 0);
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
static final int TRANSACTION_startChatEncryption = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_stopChatEncryption = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_isChatEncrypted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getChatStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_initSmpVerification = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_respondSmpVerification = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_verifyKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_unverifyKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_isKeyVerified = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getLocalFingerprint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getRemoteFingerprint = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_generateLocalKeyPair = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getLocalUserId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_getRemoteUserId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
}
/**
     * Start the OTR encryption on this chat session.
     */
public void startChatEncryption() throws android.os.RemoteException;
/**
     * Stop the OTR encryption on this chat session.
     */
public void stopChatEncryption() throws android.os.RemoteException;
/**
     * Tells if the chat session has OTR encryption running.
     */
public boolean isChatEncrypted() throws android.os.RemoteException;
/** OTR session status - ordinal of SessionStatus */
public int getChatStatus() throws android.os.RemoteException;
/**
     * start the SMP verification process
     */
public void initSmpVerification(java.lang.String question, java.lang.String answer) throws android.os.RemoteException;
/**
     * respond to the SMP verification process
     */
public void respondSmpVerification(java.lang.String answer) throws android.os.RemoteException;
/**
     * Verify the key for a given address.
     */
public void verifyKey(java.lang.String address) throws android.os.RemoteException;
/**
     * Revoke the verification for the key for a given address.
     */
public void unverifyKey(java.lang.String address) throws android.os.RemoteException;
/**
     * Tells if the fingerprint of the remote user address has been verified.
     */
public boolean isKeyVerified(java.lang.String address) throws android.os.RemoteException;
/**
     * Returns the fingerprint for the local user's key for a given account address.
     */
public java.lang.String getLocalFingerprint() throws android.os.RemoteException;
/**
     * Returns the fingerprint for a remote user's key for a given account address.
     */
public java.lang.String getRemoteFingerprint() throws android.os.RemoteException;
/**
     * generate a new local private/public key pair.
     */
public void generateLocalKeyPair() throws android.os.RemoteException;
/**
     * Returns the user id (jabberid) for the local user
     */
public java.lang.String getLocalUserId() throws android.os.RemoteException;
/**
     * Returns the user id (jabberid) for the remote user
     */
public java.lang.String getRemoteUserId() throws android.os.RemoteException;
}
