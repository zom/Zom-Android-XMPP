package org.awesomeapp.messenger.plugin.xmpp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.crypto.TorProxyInfo;
import org.awesomeapp.messenger.crypto.omemo.Omemo;
import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.model.ChatGroup;
import org.awesomeapp.messenger.model.ChatGroupManager;
import org.awesomeapp.messenger.model.ChatSession;
import org.awesomeapp.messenger.model.ChatSessionManager;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.ContactList;
import org.awesomeapp.messenger.model.ContactListListener;
import org.awesomeapp.messenger.model.ContactListManager;
import org.awesomeapp.messenger.model.ImConnection;
import org.awesomeapp.messenger.model.ImEntity;
import org.awesomeapp.messenger.model.ImErrorInfo;
import org.awesomeapp.messenger.model.ImException;
import org.awesomeapp.messenger.model.Invitation;
import org.awesomeapp.messenger.model.Message;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.provider.ImpsErrorInfo;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.adapters.ChatSessionAdapter;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.util.Debug;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ExceptionCallback;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.HostAddress;

import org.jivesoftware.smackx.address.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.chatstates.provider.ChatStateExtensionProvider;
import org.jivesoftware.smackx.commands.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.debugger.android.AndroidDebugger;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.disco.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.httpfileupload.UploadProgressListener;
import org.jivesoftware.smackx.httpfileupload.UploadService;
import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.iqlast.LastActivityManager;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;
import org.jivesoftware.smackx.iqprivate.PrivateDataManager;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.AutoJoinFailedCallback;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MucConfigFormManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.muc.provider.MUCAdminProvider;
import org.jivesoftware.smackx.muc.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.muc.provider.MUCUserProvider;
import org.jivesoftware.smackx.offline.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.offline.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.omemo.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.privacy.PrivacyListManager;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;
import org.jivesoftware.smackx.privacy.provider.PrivacyProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.sharedgroups.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.si.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.jivesoftware.smackx.xhtmlim.provider.XHTMLExtensionProvider;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import de.duenndns.ssl.MemorizingTrustManager;
import eu.siacs.conversations.Downloader;
import eu.siacs.conversations.Uploader;
import im.zom.messenger.R;

public class XmppConnection extends ImConnection {

    private static final String DISCO_FEATURE = "http://jabber.org/protocol/disco#info";
    final static String TAG = "ZomXMPP";
    private final static boolean PING_ENABLED = true;

    private XmppContactListManager mContactListManager;
    private Contact mUser;
    private BareJid mUserJid;
    private boolean mUseTor;

    // watch out, this is a different XMPPConnection class than XmppConnection! ;)
    // Synchronized by executor thread
    private XMPPTCPConnection mConnection;
    private XmppStreamHandler mStreamHandler;
    private ChatManager mChatManager;

    private Roster mRoster;

    private XmppChatSessionManager mSessionManager;
    private XMPPTCPConnectionConfiguration.Builder mConfig;

    private Omemo mOmemoInstance;

    // True if we are in the process of reconnecting.  Reconnection is retried once per heartbeat.
    // Synchronized by executor thread.
    private boolean mNeedReconnect;

    private boolean mRetryLogin;
    private ThreadPoolExecutor mExecutor;
    private Timer mTimerPresence;

    private ProxyInfo mProxyInfo = null;

    private long mAccountId = -1;
    private long mProviderId = -1;

    private boolean mIsGoogleAuth = false;

    private final static String SSLCONTEXT_TYPE = "TLS";

    private SSLContext sslContext;
    private SecureRandom secureRandom;
    private MemorizingTrustManager mMemTrust;

    private final static int SOTIMEOUT = 1000 * 120;
    private final static int CONNECT_TIMEOUT = 1000 * 30;

    private PingManager mPingManager;

    private String mUsername;
    private String mPassword;
    private String mResource;
    private int mPriority;

    private int mGlobalId;
    private static int mGlobalCount;

    private SecureRandom rndForTorCircuits = null;

    // Maintains a sequence counting up to the user configured heartbeat interval
    private int heartbeatSequence = 0;

    private HashMap<String, String> qAvatar = new HashMap <>();

    private ArrayDeque<org.jivesoftware.smack.packet.Presence> qPresence = new ArrayDeque<org.jivesoftware.smack.packet.Presence>();
    private ArrayDeque<org.jivesoftware.smack.packet.Stanza> qPacket = new ArrayDeque<org.jivesoftware.smack.packet.Stanza>();
    private ArrayDeque<Contact> qNewContact = new ArrayDeque<Contact>();


    private final static String DEFAULT_CONFERENCE_SERVER = "conference.zom.im";

    private final static String PRIVACY_LIST_DEFAULT = "defaultprivacylist";

    public XmppConnection(Context context) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        super(context);

        synchronized (XmppConnection.class) {
            mGlobalId = mGlobalCount++;
        }

        Debug.onConnectionStart();

        SmackConfiguration.setDefaultPacketReplyTimeout(SOTIMEOUT);

        // Create a single threaded executor.  This will serialize actions on the underlying connection.
        createExecutor();

        addProviderManagerExtensions();

        XmppStreamHandler.addExtensionProviders();

       // DeliveryReceipts.addExtensionProviders();

       // ServiceDiscoveryManager.setIdentityName("ChatSecure");
       // ServiceDiscoveryManager.setIdentityType("phone");
    }

    public void initUser(long providerId, long accountId) throws ImException
    {
        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(providerId)}, null);

        if (cursor == null)
            throw new ImException("unable to query settings");

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, providerId, false, null);

        mProviderId = providerId;
        mAccountId = accountId;
        mUser = makeUser(providerSettings, contentResolver);
        try {
            mUserJid = JidCreate.bareFrom(mUser.getAddress().getAddress());
        }
        catch (Exception e){}
        mUseTor = providerSettings.getUseTor();

        providerSettings.close();
    }

    private synchronized Contact makeUser(Imps.ProviderSettings.QueryMap providerSettings, ContentResolver contentResolver) {

        Contact contactUser = null;

        String nickname = Imps.Account.getNickname(contentResolver, mAccountId);
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);
        String domain = providerSettings.getDomain();
        String xmppName = userName + '@' + domain + '/' + providerSettings.getXmppResource();
        contactUser = new Contact(new XmppAddress(xmppName), nickname, Imps.Contacts.TYPE_NORMAL);

        return contactUser;
    }

    private void createExecutor() {
       mExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
              new LinkedBlockingQueue<Runnable>());

    }


    private boolean executeNow(Runnable runnable) {

        new Thread(runnable).start();
        return true;
    }

    private boolean execute(Runnable runnable) {

        if (mExecutor == null)
            createExecutor (); //if we disconnected, will need to recreate executor here, because join() made it null

        try {
            mExecutor.execute(runnable);
        } catch (RejectedExecutionException ex) {
            return false;
        }
        return true;
    }

    // Execute a runnable only if we are idle
    private boolean executeIfIdle(Runnable runnable) {
        if (mExecutor.getActiveCount() + mExecutor.getQueue().size() == 0) {
            return execute(runnable);
       }

       return false;
    }

    // This runs in executor thread, and since there is only one such thread, we will definitely
    // succeed in shutting down the executor if we get here.
    public void join() {
        final ExecutorService executor = mExecutor;
        mExecutor = null;
        // This will send us an interrupt, which we will ignore.  We will terminate
        // anyway after the caller is done.  This also drains the executor queue.
        if (executor != null)
            executor.shutdownNow();
    }

    // For testing
    boolean joinGracefully() throws InterruptedException {
        final ExecutorService executor = mExecutor;
        mExecutor = null;
        // This will send us an interrupt, which we will ignore.  We will terminate
        // anyway after the caller is done.  This also drains the executor queue.
        if (executor != null) {
            executor.shutdown();
            return executor.awaitTermination(1, TimeUnit.SECONDS);
        }

        return false;
    }

    public void sendPacket(org.jivesoftware.smack.packet.Stanza packet) {
        qPacket.add(packet);
    }

    void postpone(final org.jivesoftware.smack.packet.Stanza packet) {
        if (packet instanceof org.jivesoftware.smack.packet.Message) {
            boolean groupChat = ((org.jivesoftware.smack.packet.Message) packet).getType().equals( org.jivesoftware.smack.packet.Message.Type.groupchat);
            ChatSession session = findOrCreateSession(packet.getTo().toString(), groupChat);
            if (session != null)
                session.onMessagePostponed(packet.getStanzaId());
        }
    }


    private boolean mLoadingAvatars = false;

    private void loadVCardsAsync ()
    {

        if (!mLoadingAvatars)
        {
            executeIfIdle(new AvatarLoader());
        }
    }

    private class AvatarLoader implements Runnable
    {
        @Override
        public void run () {

            mLoadingAvatars = true;

            ContentResolver resolver = mContext.getContentResolver();

            try
            {
                synchronized (qAvatar) {
                    Iterator<String> keys = qAvatar.keySet().iterator();

                    while (keys.hasNext()) {
                        loadVCard(resolver, keys.next());
                    }

                    qAvatar.clear();
                }
            }
            catch (Exception e) {}

            mLoadingAvatars = false;
        }
    }

    private boolean loadVCard (ContentResolver resolver, String jid)
    {
        try {
                debug(TAG, "loading vcard for: " + jid);

                EntityBareJid bareJid = JidCreate.entityBareFrom(jid);

                VCardManager vCardManager = VCardManager.getInstanceFor(mConnection);
                VCard vCard = vCardManager.loadVCard(bareJid);

                Contact contact = mContactListManager.getContact(bareJid.toString());

                if (!TextUtils.isEmpty(vCard.getNickName()))
                {
                    if (!vCard.getNickName().equals(contact.getName()))
                    {
                        contact.setName(vCard.getNickName());
                        mContactListManager.doSetContactName(contact.getAddress().getBareAddress(), contact.getName());
                    }

                }

                //check for a forwarding address
                if (vCard.getJabberId() != null && (!vCard.getJabberId().equals(bareJid.toString())))
                {
                    contact.setForwardingAddress(vCard.getJabberId());

                }
                else
                {
                    contact.setForwardingAddress(null);
                }

                    // If VCard is loaded, then save the avatar to the personal folder.
                String avatarHash = vCard.getAvatarHash();

                if (avatarHash != null)
                {
                    byte[] avatarBytes = vCard.getAvatar();

                    if (avatarBytes != null)
                    {

                        debug(TAG, "found avatar image in vcard for: " + bareJid.toString());
                        debug(TAG, "start avatar length: " + avatarBytes.length);

                        DatabaseUtils.insertAvatarBlob(resolver, Imps.Avatars.CONTENT_URI, mProviderId, mAccountId, avatarBytes, avatarHash, bareJid.toString());

                        return true;
                    }
                }



        } catch (Exception e) {

            debug(TAG, "err loading vcard: " + e.toString());

            if (e.getMessage() != null)
            {
                String streamErr = e.getMessage();

                if (streamErr != null && (streamErr.contains("404") || streamErr.contains("503")))
                {
                    return false;
                }
            }

        }

        return false;
    }

    @Override
    protected void doUpdateUserPresenceAsync(Presence presence) {
        org.jivesoftware.smack.packet.Presence packet = makePresencePacket(presence);

        sendPacket(packet);
        mUserPresence = presence;
        notifyUserPresenceUpdated();
    }

    private org.jivesoftware.smack.packet.Presence makePresencePacket(Presence presence) {
        String statusText = presence.getStatusText();
        org.jivesoftware.smack.packet.Presence.Type type = org.jivesoftware.smack.packet.Presence.Type.available;
        org.jivesoftware.smack.packet.Presence.Mode mode = org.jivesoftware.smack.packet.Presence.Mode.available;
        int priority = mPriority;
        final int status = presence.getStatus();
        if (status == Presence.AWAY) {
            priority = 10;
            mode = org.jivesoftware.smack.packet.Presence.Mode.away;
        } else if (status == Presence.IDLE) {
            priority = 15;
            mode = org.jivesoftware.smack.packet.Presence.Mode.away;
        } else if (status == Presence.DO_NOT_DISTURB) {
            priority = 5;
            mode = org.jivesoftware.smack.packet.Presence.Mode.dnd;
        } else if (status == Presence.OFFLINE) {
            priority = 0;
            type = org.jivesoftware.smack.packet.Presence.Type.unavailable;
            statusText = "Offline";
        }

        // The user set priority is the maximum allowed
        if (priority > mPriority)
            priority = mPriority;

        org.jivesoftware.smack.packet.Presence packet = new org.jivesoftware.smack.packet.Presence(
                type, statusText, priority, mode);

        try {
            byte[] avatar = DatabaseUtils.getAvatarBytesFromAddress(mContext.getContentResolver(), mUser.getAddress().getBareAddress());
            if (avatar != null) {
                VCardTempXUpdatePresenceExtension vcardExt = new VCardTempXUpdatePresenceExtension(avatar);
                packet.addExtension(vcardExt);
            }
        }
        catch (Exception e)
        {
            debug(TAG,"error upading presence with avatar hash",e);
        }

        return packet;
    }

    @Override
    public int getCapability() {

        return ImConnection.CAPABILITY_SESSION_REESTABLISHMENT | ImConnection.CAPABILITY_GROUP_CHAT;
    }

    private XmppChatGroupManager mChatGroupManager = null;

    @Override
    public synchronized ChatGroupManager getChatGroupManager() {

        if (mChatGroupManager == null)
            mChatGroupManager = new XmppChatGroupManager();

        return mChatGroupManager;
    }

    public class XmppChatGroupManager extends ChatGroupManager
    {

        private Hashtable<String,MultiUserChat> mMUCs = new Hashtable<String,MultiUserChat>();

        public MultiUserChat getMultiUserChat (String chatRoomJid)
        {
            return mMUCs.get(chatRoomJid);
        }
        
        public void reconnectAll ()
        {
            Enumeration<MultiUserChat> eMuc = mMUCs.elements();
            while (eMuc.hasMoreElements())
            {
                MultiUserChat muc = eMuc.nextElement();
                if (!muc.isJoined())
                {
                    try {
                        muc.join(Resourcepart.from(mUser.getName()));
                    } catch (Exception e) {
                        Log.w(TAG,"unable to join MUC: " + e.getMessage());
                    }
                }
            }
        }

        @Override
        public String getDefaultGroupChatService ()
        {
            try {
                // Create a MultiUserChat using a Connection for a room
                //TODO fix this with new smack
                MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);

                if (!mucMgr.providesMucService(JidCreate.domainBareFrom(mUserJid)))
                    return DEFAULT_CONFERENCE_SERVER;

                Collection<DomainBareJid> servers = mucMgr.getXMPPServiceDomains();


                //just grab the first one
                for (DomainBareJid server : servers)
                    return server.toString();

            }
            catch (Exception xe)
            {
                Log.w(TAG, "Error discovering MUC server",xe);

                //unable to find conference server
                return DEFAULT_CONFERENCE_SERVER;
            }

            return DEFAULT_CONFERENCE_SERVER;
        }

        @Override
        public synchronized boolean createChatGroupAsync(String chatRoomJid, String subject, String nickname) throws Exception {

            ChatGroup chatGroup;
            MultiUserChat muc;

            if (mConnection == null || getState() != ImConnection.LOGGED_IN)
                return false;

            // Create a MultiUserChat using a Connection for a room
            MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);
            mucMgr.setAutoJoinOnReconnect(true);
            mucMgr.setAutoJoinFailedCallback(new AutoJoinFailedCallback() {
                @Override
                public void autoJoinFailed(MultiUserChat multiUserChat, Exception e) {
                    debug("MUC","There was an error autojoining the group: " + multiUserChat.getRoom().toString(),e);
                    
                }
            });

            Address address = new XmppAddress (chatRoomJid);

            String[] parts = chatRoomJid.split("@");
            String room = parts[0];
            String server = parts[1];

            if (TextUtils.isEmpty(subject))
                subject = room;

            if (TextUtils.isEmpty(nickname))
                nickname = mUsername;

            chatGroup = mGroups.get(chatRoomJid);
            muc = mMUCs.get(chatRoomJid);

            if (chatGroup != null && muc !=null)
            {
                // Create the room
                if (!muc.isJoined()) {
                    MultiUserChat.MucCreateConfigFormHandle handle = muc.createOrJoin(Resourcepart.from(nickname));
                }

            }
            else
            {

                if (chatRoomJid.endsWith("@"))
                {
                     //let's add a host to that!
                    Collection<DomainBareJid> servers = mucMgr.getXMPPServiceDomains();

                    if (servers.iterator().hasNext())
                        chatRoomJid += servers.iterator().next().toString();
                    else
                    {
                        chatRoomJid += DEFAULT_CONFERENCE_SERVER;
                    }
                 }

                muc = mucMgr.getMultiUserChat(JidCreate.entityBareFrom(chatRoomJid));
                boolean mucCreated = false;

                try {
                    // Create the room
                    if (!muc.isJoined()) {
                        MultiUserChat.MucCreateConfigFormHandle handle = muc.createOrJoin(Resourcepart.from(nickname));
                        mucCreated = true;
                    }

                } catch (Exception iae) {

                    if (iae.getMessage().contains("Creation failed")) {
                        //some server's don't return the proper 201 create code, so we can just assume the room was created!
                    } else {

                        throw iae;

                    }
                }

                chatGroup = mGroups.get(chatRoomJid);

                if (chatGroup == null) {
                    chatGroup = new ChatGroup(address, subject, this);
                    mGroups.put(chatRoomJid, chatGroup);
                }

                mMUCs.put(chatRoomJid, muc);

                if (mucCreated) {
                    try {
                        Form form = muc.getConfigurationForm();
                        Form submitForm = form.createAnswerForm();

                        for (FormField field : form.getFields()) {
                            if (!(field.getType() == FormField.Type.hidden) && field.getVariable() != null) {
                                submitForm.setDefaultAnswer(field.getVariable());
                            }
                        }

                        // Sets the new owner of the room
                        if (submitForm.getField("muc#roomconfig_roomowners") != null) {
                            List owners = new ArrayList();
                            owners.add(mUser.getAddress().getBareAddress());
                            submitForm.setAnswer("muc#roomconfig_roomowners", owners);
                        }

                        if (submitForm.getField("muc#roomconfig_roomname") != null)
                            submitForm.setAnswer("muc#roomconfig_roomname", subject);

                        if (submitForm.getField("muc#roomconfig_roomdesc") != null)
                            submitForm.setAnswer("muc#roomconfig_roomdesc", subject);

                        if (submitForm.getField("muc#roomconfig_changesubject") != null)
                            submitForm.setAnswer("muc#roomconfig_changesubject", true);

                        if (submitForm.getField("muc#roomconfig_anonymity") != null)
                            submitForm.setAnswer("muc#roomconfig_anonymity", "nonanonymous");

                        if (submitForm.getField("muc#roomconfig_publicroom") != null)
                            submitForm.setAnswer("muc#roomconfig_publicroom", false);

                        if (submitForm.getField("muc#roomconfig_persistentroom") != null)
                            submitForm.setAnswer("muc#roomconfig_persistentroom", true);

                        if (submitForm.getField("muc#roomconfig_whois") != null)
                            submitForm.setAnswer("muc#roomconfig_whois", Arrays.asList("anyone"));

                        //                      if (submitForm.getField("muc#roomconfig_historylength") != null)
                        //                          submitForm.setAnswer("muc#roomconfig_historylength", 0);

                        //                        if (submitForm.getField("muc#maxhistoryfetch") != null)
                        //                          submitForm.setAnswer("muc#maxhistoryfetch", 0);

                        if (submitForm.getField("muc#roomconfig_enablelogging") != null)
                            submitForm.setAnswer("muc#roomconfig_enablelogging", false);

                        //                        if (submitForm.getField("muc#maxhistoryfetch") != null)
                        //                            submitForm.setAnswer("muc#maxhistoryfetch", 0);

                        submitForm.setAnswer(MucConfigFormManager.MUC_ROOMCONFIG_MEMBERSONLY, true);

                        muc.sendConfigurationForm(submitForm);
                        muc.changeSubject(subject);

                    } catch (XMPPException xe) {
                        debug(TAG, "(ignoring) got an error configuring MUC room: " + xe.getLocalizedMessage());

                    }

                }
            }


            addMucListeners(muc);

            return true;

        }

        @Override
        public void deleteChatGroupAsync(ChatGroup group) {

            String chatRoomJid = group.getAddress().getAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);

                try {

                    muc.destroy("", null);

                    mMUCs.remove(chatRoomJid);

                } catch (Exception e) {
                    debug(TAG,"error destroying MUC",e);
                }

            }

        }

        @Override
        protected void addGroupMemberAsync(ChatGroup group, Contact contact) {

         //   inviteUserAsync(group, contact);
        //we already have invite, so... what is this?

        }

        @Override
        protected void removeGroupMemberAsync(ChatGroup group, Contact contact) {


            String chatRoomJid = group.getAddress().getAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);
                try {
                    String reason = "";
                    muc.kickParticipant(Resourcepart.from(contact.getName()),reason);
                  //  muc.kickParticipant(chatRoomJid, contact.getAddress().getBareAddress());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }


        @Override
        public void joinChatGroupAsync(Address address, String subject) {

            String chatRoomJid = address.getBareAddress();
            String[] parts = chatRoomJid.split("@");
            String room = parts[0];
            String server = parts[1];
            String nickname = mUser.getName();//.split("@")[0];

            try {

                // Create a MultiUserChat using a Connection for a room
                MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);
                mucMgr.setAutoJoinOnReconnect(true);
                MultiUserChat muc = mucMgr.getMultiUserChat( JidCreate.entityBareFrom(chatRoomJid));

                if (!muc.isJoined())
                    muc.join(Resourcepart.from(nickname));

                if (TextUtils.isEmpty(subject))
                    subject = room;

                ChatGroup chatGroup = mGroups.get(chatRoomJid);

                if (chatGroup == null) {
                    chatGroup = new ChatGroup(address, subject, this);
                    mGroups.put(chatRoomJid, chatGroup);
                }

                mMUCs.put(chatRoomJid, muc);


                addMucListeners(muc);


            } catch (Exception e) {
                debug(TAG,"error joining MUC",e);
            }

        }

        public void loadMembers (ChatGroup chatGroup) {
            try {

                loadMembers(mMUCs.get(chatGroup.getAddress().getAddress()), chatGroup);

            } catch (Exception e)
            {
                debug(TAG,"Could not load members",e);
            }
        }

        private void loadMembers (MultiUserChat muc, ChatGroup chatGroup) throws SmackException, XMPPException,InterruptedException
        {
            chatGroup.clearMembers();

            XmppAddress xa = null;

            List<Affiliate> mucOwners = muc.getOwners();
            for (Affiliate member : mucOwners)
            {
                xa = new XmppAddress(member.getJid().toString());
                Contact mucContact = mContactListManager.getContact(xa);
                if (mucContact == null)
                    mucContact = new Contact(xa,xa.getResource(), Imps.Contacts.TYPE_NORMAL);
                chatGroup.addMemberAsync(mucContact);
                chatGroup.setOwner(mucContact);
            }

            List<Affiliate> mucMembers = muc.getMembers();

            for (Affiliate member : mucMembers)
            {
                xa = new XmppAddress(member.getJid().toString());
                Contact mucContact = mContactListManager.getContact(xa);
                if (mucContact == null)
                    mucContact = new Contact(xa,xa.getResource(), Imps.Contacts.TYPE_NORMAL);
                chatGroup.addMemberAsync(mucContact);

            }

            List<EntityFullJid> mucOccupant = muc.getOccupants();

            for (EntityFullJid occupant : mucOccupant) {
                Jid jidSource = muc.getOccupant(occupant).getJid();
                if (jidSource != null)
                    xa = new XmppAddress(jidSource.toString());
                else
                    xa = new XmppAddress(occupant.toString());
                Contact mucContact = new Contact(xa,xa.getResource(), Imps.Contacts.TYPE_NORMAL);
                org.jivesoftware.smack.packet.Presence presence = muc.getOccupantPresence(occupant);
                Presence p = new Presence(parsePresence(presence), null, null, null, Presence.CLIENT_TYPE_MOBILE);
                mucContact.setPresence(p);
                chatGroup.addMemberAsync(mucContact);
            }
        }

        private void addMucListeners (MultiUserChat muc)
        {

            muc.addSubjectUpdatedListener(new SubjectUpdatedListener() {

                @Override
                public void subjectUpdated(String subject, EntityFullJid from) {

                    XmppAddress xa = new XmppAddress(from.toString());
                    MultiUserChat muc = mChatGroupManager.getMultiUserChat(xa.getBareAddress());
                    ChatGroup chatGroup = mChatGroupManager.getChatGroup(xa);
                    chatGroup.setName(subject);

                }

            });

            muc.addParticipantStatusListener(new ParticipantStatusListener() {

                @Override
                public void joined(EntityFullJid entityFullJid) {

                    XmppAddress xa = null;
                    MultiUserChat muc = mChatGroupManager.getMultiUserChat(entityFullJid.asBareJid().toString());

                    Jid jidSource = muc.getOccupant(entityFullJid).getJid();
                    if (jidSource != null)
                        xa = new XmppAddress(jidSource.toString());
                    else
                        xa = new XmppAddress(entityFullJid.toString());

                    ChatGroup chatGroup = mChatGroupManager.getChatGroup(xa);
                    Contact mucContact = new Contact(xa, xa.getResource(), Imps.Contacts.TYPE_NORMAL);
                    chatGroup.addMemberAsync(mucContact);
                }

                @Override
                public void left(EntityFullJid entityFullJid) {
                    XmppAddress xa = new XmppAddress(entityFullJid.toString());
                    MultiUserChat muc = mChatGroupManager.getMultiUserChat(xa.getBareAddress());
                    Jid jidSource = muc.getOccupant(entityFullJid).getJid();
                    if (jidSource != null)
                        xa = new XmppAddress(jidSource.toString());
                    else
                        xa = new XmppAddress(entityFullJid.toString());
                    ChatGroup chatGroup = mChatGroupManager.getChatGroup(xa);
                    Contact mucContact = new Contact(xa, xa.getResource(), Imps.Contacts.TYPE_NORMAL);
                    chatGroup.removeMemberAsync(mucContact);

                }

                @Override
                public void kicked(EntityFullJid entityFullJid, Jid jid, String s) {
                    XmppAddress xa = new XmppAddress(entityFullJid.toString());
                    MultiUserChat muc = mChatGroupManager.getMultiUserChat(xa.getBareAddress());
                    Jid jidSource = muc.getOccupant(entityFullJid).getJid();
                    if (jidSource != null)
                        xa = new XmppAddress(jidSource.toString());
                    else
                        xa = new XmppAddress(entityFullJid.toString());
                    ChatGroup chatGroup = mChatGroupManager.getChatGroup(xa);
                    Contact mucContact = new Contact(xa, xa.getResource(), Imps.Contacts.TYPE_NORMAL);
                    chatGroup.removeMemberAsync(mucContact);
                }

                @Override
                public void voiceGranted(EntityFullJid entityFullJid) {

                }

                @Override
                public void voiceRevoked(EntityFullJid entityFullJid) {

                }

                @Override
                public void banned(EntityFullJid entityFullJid, Jid jid, String s) {

                }

                @Override
                public void membershipGranted(EntityFullJid entityFullJid) {

                }

                @Override
                public void membershipRevoked(EntityFullJid entityFullJid) {

                }

                @Override
                public void moderatorGranted(EntityFullJid entityFullJid) {

                }

                @Override
                public void moderatorRevoked(EntityFullJid entityFullJid) {

                }

                @Override
                public void ownershipGranted(EntityFullJid entityFullJid) {

                }

                @Override
                public void ownershipRevoked(EntityFullJid entityFullJid) {

                }

                @Override
                public void adminGranted(EntityFullJid entityFullJid) {

                }

                @Override
                public void adminRevoked(EntityFullJid entityFullJid) {

                }

                @Override
                public void nicknameChanged(EntityFullJid entityFullJid, Resourcepart resourcepart) {

                }



            });

            muc.addParticipantListener(new PresenceListener() {
                @Override
                public void processPresence(org.jivesoftware.smack.packet.Presence presence) {

                    /**
                    XmppAddress xa = new XmppAddress(presence.getFrom().toString());
                    Contact mucContact = new Contact(xa, xa.getResource(), Imps.Contacts.TYPE_NORMAL);
                    Presence p = new Presence(parsePresence(presence), presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT);
                    mucContact.setPresence(p);
                    **/
                }
            });

            muc.addMessageListener(new MessageListener() {
                @Override
                public void processMessage(org.jivesoftware.smack.packet.Message message) {
                    debug(TAG, "receive message: " + message.getFrom() + " to " + message.getTo());


                   // handleMessage(message, false);
                }
            });

        }

        @Override
        public void leaveChatGroupAsync(ChatGroup group) {
            String chatRoomJid = group.getAddress().getBareAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);
                try {
                    muc.leave();
                }
                catch (Exception nce)
                {
                    Log.e(ImApp.LOG_TAG,"not connected error trying to leave group",nce);

                }

                mMUCs.remove(chatRoomJid);

            }

        }

        @Override
        public void inviteUserAsync(final ChatGroup group, final Contact invitee) {

            executeNow(new Runnable () {

                public void run() {
                    String chatRoomJid = group.getAddress().getAddress();

                    if (mMUCs.containsKey(chatRoomJid)) {
                        MultiUserChat muc = mMUCs.get(chatRoomJid);

                        String reason = group.getName(); //no reason for now
                        try {
                            EntityBareJid inviteeJid = JidCreate.entityBareFrom(invitee.getAddress().getAddress());
                            muc.invite(inviteeJid, reason);
                            muc.grantMembership(inviteeJid);
                            group.addMemberAsync(invitee);
                        } catch (Exception nce) {
                            Log.e(ImApp.LOG_TAG, "not connected error trying to add invite", nce);

                        }

                    }
                }
            });

        }

        @Override
        public void acceptInvitationAsync(Invitation invitation) {

            Address addressGroup = invitation.getGroupAddress();

            joinChatGroupAsync (addressGroup,invitation.getReason());

        }

        @Override
        public void rejectInvitationAsync(Invitation invitation) {

            Address addressGroup = invitation.getGroupAddress();

            String reason = ""; // no reason for now

            MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);
            try {

                    mucMgr.decline(JidCreate.entityBareFrom(addressGroup.getAddress()), JidCreate.entityBareFrom(invitation.getSender().getAddress()), reason);

                }
                catch (Exception nce)
                {
                    Log.e(ImApp.LOG_TAG,"not connected error trying to reject invite",nce);
                }
        }

    };

    @Override
    public synchronized ChatSessionManager getChatSessionManager() {

        if (mSessionManager == null)
            mSessionManager = new XmppChatSessionManager();

        return mSessionManager;
    }

    @Override
    public synchronized XmppContactListManager getContactListManager() {

        if (mContactListManager == null)
            mContactListManager = new XmppContactListManager();

        return mContactListManager;
    }

    @Override
    public Contact getLoginUser() {
        return mUser;
    }

    @Override
    public Map<String, String> getSessionContext() {
        // Empty state for now (but must have at least one key)
        return Collections.singletonMap("state", "empty");
    }

    @Override
    public int[] getSupportedPresenceStatus() {
        return new int[] { Presence.AVAILABLE, Presence.AWAY, Presence.IDLE, Presence.OFFLINE,
                           Presence.DO_NOT_DISTURB, };
    }

    @Override
    public boolean isUsingTor() {
        return mUseTor;
    }

    @Override
    public void loginAsync(long accountId, String passwordTemp, long providerId, boolean retry) {

        mAccountId = accountId;
        mPassword = passwordTemp;
        mProviderId = providerId;
        mRetryLogin = retry;

        ContentResolver contentResolver = mContext.getContentResolver();

        if (mPassword == null)
            mPassword = Imps.Account.getPassword(contentResolver, mAccountId);

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return;

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);

        mUser = makeUser(providerSettings, contentResolver);

        providerSettings.close();

        execute(new Runnable() {
            @Override
            public void run() {
                do_login();
            }
        });
    }

    private void loginSync(long accountId, String passwordTemp, long providerId, boolean retry) {

        mAccountId = accountId;
        mPassword = passwordTemp;
        mProviderId = providerId;
        mRetryLogin = retry;

        ContentResolver contentResolver = mContext.getContentResolver();

        if (mPassword == null)
            mPassword = Imps.Account.getPassword(contentResolver, mAccountId);

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return;

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);

        mUser = makeUser(providerSettings, contentResolver);

        providerSettings.close();

        do_login();


    }

    // Runs in executor thread
    private void do_login() {

        if (getState() == LOGGED_IN
                || getState() == SUSPENDED
                || getState() == SUSPENDING ) {
            mNeedReconnect = false;
            return;
        }

        /*
        if (mConnection != null) {
            setState(getState(), new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER,
                    "still trying..."));
            return;
        }*/

        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return; //not going to work

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);


        // providerSettings is closed in initConnection();
        mUsername = Imps.Account.getUserName(contentResolver, mAccountId);

        String defaultStatus = null;

        mNeedReconnect = true;
        setState(LOGGING_IN, null);

        mUserPresence = new Presence(Presence.AVAILABLE, defaultStatus, Presence.CLIENT_TYPE_MOBILE);

        try {
            if (mUsername == null || mUsername.length() == 0)
                throw new Exception("empty username not allowed");

            initConnectionAndLogin(providerSettings, mUsername);

            setState(LOGGED_IN, null);
            debug(TAG, "logged in");
            mNeedReconnect = false;


        } catch (XMPPException e) {
            debug(TAG, "exception thrown on connection",e);

            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());
            mRetryLogin = true; // our default behavior is to retry

            if (mConnection != null && mConnection.isConnected()) {

                if (!mConnection.isAuthenticated())
                {
                    debug(TAG, "not authorized - will not retry");
                    info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "invalid user/password");
                    setState(SUSPENDED, info);
                    mRetryLogin = false;
                    mNeedReconnect = false;
                }

            }

            if (mRetryLogin && getState() != SUSPENDED) {
                debug(TAG, "will retry");
                setState(LOGGING_IN, info);
                maybe_reconnect();

            } else {
               //debug(TAG, "will not retry"); //WE MUST ALWAYS RETRY!
                disconnect();
                disconnected(info);
            }


        } catch (Exception e) {

            debug(TAG, "login failed", e);

            if (getState() != SUSPENDED || getState() != SUSPENDING) {

                mRetryLogin = true;
                mNeedReconnect = true;

                debug(TAG, "will retry");

                ImErrorInfo info = new ImErrorInfo(ImErrorInfo.UNKNOWN_ERROR, "keymanagement exception");
                setState(LOGGING_IN, info);
            }
            else
            {
                mRetryLogin = false;
                mNeedReconnect = true;
            }
        }
        finally {
            providerSettings.close();
            
            if (!cursor.isClosed())
                cursor.close();
        }

    }

    private synchronized Omemo initOmemo (XMPPTCPConnection conn) throws Exception
    {

        if (conn != null && conn.isConnected()) {

            mOmemoInstance = new Omemo(conn, mUserJid);
            mOmemoInstance.getManager().addOmemoMessageListener(new OmemoMessageListener() {
                @Override
                public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, org.jivesoftware.smack.packet.Message message, org.jivesoftware.smack.packet.Message message1, OmemoMessageInformation omemoMessageInformation) {
                    debug(TAG,"omemo key transport received");
                }

                @Override
                public void onOmemoMessageReceived(String body, org.jivesoftware.smack.packet.Message message, org.jivesoftware.smack.packet.Message message1, OmemoMessageInformation omemoMessageInformation) {

                    if (body != null) {
                        debug(TAG, "got inbound message omemo: from:" + message.getFrom() + "=" + message.getBody());
                        message.setBody(body);
                        handleMessage(message, true);
                    } else {
                        debug(TAG, "got empty ibound message omemo: from:" + message.getFrom().toString());
                    }
                }
            });
        }


        return mOmemoInstance;


    }

    private synchronized Omemo getOmemo () throws Exception
    {

        if (mOmemoInstance == null && mConnection != null) {
            initOmemo(mConnection);
        }

        return mOmemoInstance;

    }




    // TODO shouldn't setProxy be handled in Imps/settings?
    public void setProxy(String type, String host, int port) {
        if (type == null) {
            mProxyInfo = null;
        } else {

            ProxyInfo.ProxyType pType = ProxyInfo.ProxyType.valueOf(type);
            String username = null;
            String password = null;

            if (type.equals(TorProxyInfo.PROXY_TYPE) //socks5
                    && host.equals(TorProxyInfo.PROXY_HOST) //127.0.0.1
                    && port == TorProxyInfo.PROXY_PORT) //9050
            {
                //if the proxy is for Orbot/Tor then generate random usr/pwd to isolate Tor streams
                if (rndForTorCircuits == null)
                    rndForTorCircuits = new SecureRandom();

                username = rndForTorCircuits.nextInt(100000)+"";
                password = rndForTorCircuits.nextInt(100000)+"";

            }

            mProxyInfo = new ProxyInfo(pType, host, port, username, password);

        }
    }


    private void initConnectionAndLogin (Imps.ProviderSettings.QueryMap providerSettings,String userName) throws InterruptedException, IOException, SmackException, XMPPException, KeyManagementException, NoSuchAlgorithmException, IllegalStateException, RuntimeException
    {
        Roster.SubscriptionMode subMode = Roster.SubscriptionMode.manual;//Roster.SubscriptionMode.accept_all;//load this from a preference

        Debug.onConnectionStart(); //only activates if Debug TRUE is set, so you can leave this in!

        initConnection(providerSettings, userName);

        //disable compression based on statement by Ge0rg
        // mConfig.setCompressionEnabled(false);

        if (mConnection.isConnected() && mConnection.isSecureConnection())
        {

            mResource = providerSettings.getXmppResource();

            mRoster = Roster.getInstanceFor(mConnection);
            mRoster.setRosterLoadedAtLogin(true);
            mRoster.setSubscriptionMode(subMode);

            mChatManager = ChatManager.getInstanceFor(mConnection);

            mPingManager = PingManager.getInstanceFor(mConnection) ;

            if (mUser == null)
                mUser = makeUser(providerSettings,mContext.getContentResolver());

            mConnection.login(mUsername, mPassword, Resourcepart.from(mResource));

            mStreamHandler.notifyInitialLogin();
            initServiceDiscovery();

            getContactListManager().listenToRoster(mRoster);
            getContactListManager().loadContactListsAsync();

            MultiUserChatManager.getInstanceFor(mConnection).addInvitationListener(new InvitationListener() {

                @Override
                public void invitationReceived(XMPPConnection xmppConnection, MultiUserChat muc, EntityJid entityJid, String reason, String password, org.jivesoftware.smack.packet.Message message, MUCUser.Invite invite) {

                    try {



                        getChatGroupManager().acceptInvitationAsync(invite.getFrom().toString());
                        XmppAddress xa = new XmppAddress(muc.getRoom().toString());

                        mChatGroupManager.joinChatGroupAsync(xa, reason);

                        ChatSession session = mSessionManager.findSession(muc.getRoom());

                        //create a session
                        if (session == null) {
                            ImEntity participant = findOrCreateParticipant(xa.getAddress(), true);

                            if (participant != null)
                                session = mSessionManager.createChatSession(participant, false);

                            if (session != null)
                                ((ChatGroup) session.getParticipant()).setName(reason);
                        }
                    }
                    catch (Exception se)
                    {
                        Log.e(TAG,"error accepting invite",se);
                    }




                }


            });

            execute(new Runnable ()
            {
                public void run ()
                {

                    sendPresencePacket();
                    sendVCard();

                }
            });

        }
        else
        {
            //throw some meaningful error message here
            throw new SmackException("Unable to securely conenct to server");
        }

        

    }

    public void broadcastMigrationIdentity (String newIdentity)
    {

        sendVCard(newIdentity);

        String migrateMessage = mContext.getString(R.string.migrate_message) + ' ' + newIdentity;
        mUserPresence = new Presence(Presence.AVAILABLE, migrateMessage, Presence.CLIENT_TYPE_MOBILE);
        sendPresencePacket();
    }

    @Override
    public String publishFile(String fileName, String mimeType, long fileSize, java.io.InputStream is) {

        UploaderManager uploader = new UploaderManager();
        String result = uploader.doUpload(fileName, mimeType, fileSize, is);

        return result;
    }

    public void sendVCard ()
    {
        sendVCard(null);
    }

    public void sendVCard (String migrateJabberId)
    {

        try {
            String jid = mUser.getAddress().getBareAddress();

            VCardManager vCardManager = VCardManager.getInstanceFor(mConnection);
            VCard vCard = null;

            try {
                vCard = vCardManager.loadVCard(JidCreate.entityBareFrom(jid));
            }
            catch (Exception e){
               // debug(TAG,"error loading vcard",e);
            }

            boolean setAvatar = true;

            if (vCard == null) {
                vCard = new VCard();
                vCard.setJabberId(jid);
                setAvatar = true;
            }
            else if (vCard.getAvatarHash() != null)
            {
                setAvatar = !DatabaseUtils.doesAvatarHashExist(mContext.getContentResolver(),  Imps.Avatars.CONTENT_URI, mUser.getAddress().getBareAddress(), vCard.getAvatarHash());
            }

            vCard.setNickName(mUser.getName());

            //if we have moved to a new account, send it here
            if (migrateJabberId != null)
            {
                vCard.setJabberId(migrateJabberId);
            }
            else
            {
                vCard.setJabberId(mUser.getAddress().getBareAddress());
            }

            if (setAvatar) {
                byte[] avatar = DatabaseUtils.getAvatarBytesFromAddress(mContext.getContentResolver(), mUser.getAddress().getBareAddress());
                if (avatar != null) {
                    vCard.setAvatar(avatar, "image/jpeg");
                }
            }

            if (mConnection != null && mConnection.isConnected() && mConnection.isAuthenticated()) {
                debug(TAG, "Saving VCard for: " + mUser.getAddress().getAddress());
                vCardManager.saveVCard(vCard);
            }
        }
        catch (Exception e)
        {
            debug(TAG,"error saving vcard",e);
        }
    }

    // Runs in executor thread
    private void initConnection(Imps.ProviderSettings.QueryMap providerSettings, String userName) throws InterruptedException, NoSuchAlgorithmException, KeyManagementException, XMPPException, SmackException, IOException  {

        boolean allowPlainAuth = false;//never! // providerSettings.getAllowPlainAuth();
        boolean requireTls = true;// providerSettings.getRequireTls(); //always!
        boolean doDnsSrv = providerSettings.getDoDnsSrv();
       // boolean tlsCertVerify = providerSettings.getTlsCertVerify();

       // boolean useSASL = true;//!allowPlainAuth;
        boolean useTor = providerSettings.getUseTor();
        String domain = providerSettings.getDomain();

        mPriority = providerSettings.getXmppResourcePrio();
        int serverPort = providerSettings.getPort();
        String server = providerSettings.getServer();
        if ("".equals(server))
            server = null;

        if (domain.equals("dukgo.com"))
        {
            doDnsSrv = false;
            server = "dukgo.com";
        }



        /**
         * //need to move this to the new NetCipher BroadcastReceiver API
        try {
            //if Orbot is on and running, we should use it
            if (OrbotHelper.isOrbotInstalled(mContext) && OrbotHelper.isOrbotRunning(mContext)
                    && (server != null && (!doDnsSrv)))
                useTor = true;
        }
        catch (Exception e)
        {
            debug(TAG,"There was an error checking Orbot: " + e.getMessage());
        }*/

        debug(TAG, "TLS required? " + requireTls);

        if (useTor) {
            setProxy(TorProxyInfo.PROXY_TYPE, TorProxyInfo.PROXY_HOST,
                    TorProxyInfo.PROXY_PORT);
        }
        else
        {
            setProxy(null, null, -1);
        }

        if (mProxyInfo == null)
            mProxyInfo = null;

        // If user did not specify a server, and SRV requested then lookup SRV
        if (doDnsSrv) {

            //java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
            //java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");

            debug(TAG, "(DNS SRV) resolving: " + domain);
            List<HostAddress> listHostsFailed = new ArrayList<>();
            List<HostAddress> listHosts = DNSUtil.resolveXMPPServiceDomain(domain, listHostsFailed, ConnectionConfiguration.DnssecMode.disabled);

            if (listHosts.size() > 0) {
                server = listHosts.get(0).getFQDN();
                serverPort = listHosts.get(0).getPort();

                debug(TAG, "(DNS SRV) resolved: " + domain + "=" + server + ":" + serverPort);
            }
        }

        /**
        if (server != null && server.contains("google.com"))
        {
            mUsername = userName + '@' + domain;
        }
        else if (domain.contains("gmail.com"))
        {
            mUsername = userName + '@' + domain;
        }
        else if (mIsGoogleAuth)
        {
            mUsername = userName + '@' + domain;
        }
        else
        {
            mUsername = userName;
        }**/


        if (serverPort == 0) //if serverPort is set to 0 then use 5222 as default
            serverPort = 5222;

        mConfig = XMPPTCPConnectionConfiguration.builder();

        mConfig.setServiceName(JidCreate.domainBareFrom(domain));
        mConfig.setPort(serverPort);

        mConfig.setCompressionEnabled(true);
        mConfig.setConnectTimeout(CONNECT_TIMEOUT);

        // No server requested and SRV lookup wasn't requested or returned nothing - use domain
        if (server == null)
            mConfig.setHost(domain);
        else
            mConfig.setHost(server);

        mConfig.setDebuggerEnabled(Debug.DEBUG_ENABLED);
        SmackConfiguration.DEBUG = Debug.DEBUG_ENABLED;
        SmackConfiguration.setDebuggerFactory(new SmackDebuggerFactory() {
            @Override
            public SmackDebugger create(XMPPConnection xmppConnection, Writer writer, Reader reader) throws IllegalArgumentException {

                return new AndroidDebugger(xmppConnection, writer, reader);

            }
        });

        //mConfig.setSASLAuthenticationEnabled(useSASL);


        // Android has no support for Kerberos or GSSAPI, so disable completely
        SASLAuthentication.unregisterSASLMechanism("KERBEROS_V4");
        SASLAuthentication.unregisterSASLMechanism("GSSAPI");

        /**
        SASLAuthentication.registerSASLMechanism( GTalkOAuth2.NAME, GTalkOAuth2.class );

        if (mIsGoogleAuth) //if using google auth enable sasl
            SASLAuthentication.supportSASLMechanism( GTalkOAuth2.NAME, 0);
        else if (domain.contains("google.com")||domain.contains("gmail.com")) //if not google auth, disable if doing direct google auth
            SASLAuthentication.unsupportSASLMechanism( GTalkOAuth2.NAME);
            */

        if (allowPlainAuth)
            SASLAuthentication.unBlacklistSASLMechanism("PLAIN");

        SASLAuthentication.unBlacklistSASLMechanism("DIGEST-MD5");

        if (mMemTrust == null)
            mMemTrust = new MemorizingTrustManager(mContext);

        if (sslContext == null) {

            sslContext = SSLContext.getInstance(SSLCONTEXT_TYPE);

            secureRandom = new java.security.SecureRandom();
            sslContext.init(null, MemorizingTrustManager.getInstanceList(mContext), secureRandom);

            while (true) {
                try {

                    if (Build.VERSION.SDK_INT >= 20) {

                        sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES_API_20);

                    } else {
                        sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
                    }
                    break;
                } catch (IllegalStateException e) {
                    debug(TAG,"error setting cipher suites; waiting for SSLContext to init...");
                    try { Thread.sleep(1000); } catch (Exception e2){}
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mConfig.setKeystoreType("AndroidCAStore");
                mConfig.setKeystorePath(null);
            } else {
                mConfig.setKeystoreType("BKS");
                String path = System.getProperty("javax.net.ssl.trustStore");
                if (path == null)
                    path = System.getProperty("java.home") + File.separator + "etc"
                            + File.separator + "security" + File.separator
                            + "cacerts.bks";
                mConfig.setKeystorePath(path);
            }

            //wait a second while the ssl context init's
            try { Thread.sleep(1000); } catch (Exception e) {}

        }

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= 16){
            // Enable TLS1.2 and TLS1.1 on supported versions of android
            // http://stackoverflow.com/questions/16531807/android-client-server-on-tls-v1-2

            while (true) {
                try {
                    mConfig.setEnabledSSLProtocols(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"});
                    sslContext.getDefaultSSLParameters().setProtocols(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"});
                    break;
                } catch (IllegalStateException ise) {
                    try { Thread.sleep(1000); } catch (Exception e){}
                }
            }

        }

        if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            mConfig.setEnabledSSLCiphers(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
        }

        mConfig.setCustomSSLContext(sslContext);
        mConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        mConfig.setHostnameVerifier(
                mMemTrust.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));


        mConfig.setSendPresence(true);

        XMPPTCPConnection.setUseStreamManagementDefault(true);

        mConnection = new XMPPTCPConnection(mConfig.build());



        //debug(TAG,"is secure connection? " + mConnection.isSecureConnection());
        //debug(TAG,"is using TLS? " + mConnection.isUsingTLS());

        mConnection.addAsyncStanzaListener(new StanzaListener() {

            @Override
            public void processStanza(Stanza stanza) {

                debug(TAG, "receive message: " + stanza.getFrom() + " to " + stanza.getTo());

                org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) stanza;

                handleMessage(smackMessage, false);

                String msg_xml = smackMessage.toXML().toString();

                try {
                    handleChatState(smackMessage.getFrom().toString(), msg_xml);
                }
                catch (RemoteException re)
                {
                    //no worries
                }
            }
        }, new StanzaTypeFilter(org.jivesoftware.smack.packet.Message.class));

        mConnection.addAsyncStanzaListener(new StanzaListener() {

            @Override
            public void processStanza(Stanza packet) {

                org.jivesoftware.smack.packet.Presence presence = (org.jivesoftware.smack.packet.Presence) packet;
                qPresence.push(presence);

            }
        }, new StanzaTypeFilter(org.jivesoftware.smack.packet.Presence.class));

        if (mTimerPackets != null)
            mTimerPackets.cancel();

        initPacketProcessor();
        
        if (mTimerPresence != null)
            mTimerPresence.cancel();

        initPresenceProcessor ();

        if (mTimerNewContacts != null)
            mTimerNewContacts.cancel();

        initNewContactProcessor();
        
        ConnectionListener connectionListener = new ConnectionListener() {
            /**
             * Called from smack when connect() is fully successful
             *
             * This is called on the executor thread while we are in reconnect()
             */
            @Override
            public void reconnectionSuccessful() {
                if (mStreamHandler == null || !mStreamHandler.isResumePending()) {
                    debug(TAG, "Reconnection success");
                    onReconnectionSuccessful();
                    mRoster = Roster.getInstanceFor(mConnection);
                } else {
                    debug(TAG, "Ignoring reconnection callback due to pending resume");
                }
            }

            @Override
            public void reconnectionFailed(Exception e) {
                // We are not using the reconnection manager
             //   throw new UnsupportedOperationException();
                execute(new Runnable() {

                    public void run() {

                        mNeedReconnect = true;
                        setState(LOGGING_IN,
                                new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network error"));
                        reconnect();

                    }

                });
            }

            @Override
            public void reconnectingIn(int seconds) {
                // // We are not using the reconnection manager
                // throw new UnsupportedOperationException();
            }

            @Override
            public void connectionClosedOnError(final Exception e) {
                /*
                 * This fires when:
                 * - Packet reader or writer detect an error
                 * - Stream compression failed
                 * - TLS fails but is required
                 * - Network error
                 * - We forced a socket shutdown
                 */
                debug(TAG, "reconnect on error: " + e.getMessage());
                if (e.getMessage().contains("conflict")) {


                    execute(new Runnable() {
                        @Override
                        public void run() {
                           // disconnect();
                            disconnected(new ImErrorInfo(ImpsErrorInfo.ALREADY_LOGGED,
                                  "logged in from another location"));
                        }
                    });

                } else if (!mNeedReconnect) {

                    execute(new Runnable() {

                        public void run() {
                            if (getState() == LOGGED_IN)
                            {
                                mNeedReconnect = true;
                                setState(LOGGING_IN,
                                      new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network error"));
                                reconnect();
                            }
                        }

                    });


                }
            }

            @Override
            public void connected(XMPPConnection connection) {
                debug(TAG, "connected");

                try { initOmemo((XMPPTCPConnection)connection); }
                catch (Exception e)
                {
                    debug("OMEMO","There was a problem init'g omemo",e);
                }

            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                debug(TAG, "authenticated: resumed=" + resumed);

                try {

                    OmemoFingerprint of = mOmemoInstance.getManager().getOurFingerprint();
                    if (of != null)
                        debug(TAG,"got our omemo fingerprint: " + of.toString());
                    else
                        debug(TAG,"NO OMEMO FINGERPRINT FOR OUR ACCOUNT!!!");

                    for (ChatSession session : mSessionManager.getSessions())
                    {
                        BareJid jid = JidCreate.bareFrom(session.getParticipant().getAddress().getAddress());

                        if (getOmemo().getManager().contactSupportsOmemo(jid)) {
                            getOmemo().getManager().buildSessionsWith(jid);
                        }
                    }
                }
                catch (NullPointerException e)
                {
                    debug(TAG,"error getting OMEMO devices",e);

                    throw new RuntimeException("no keys, no cheese");
                }
                catch (Exception e)
                {
                    debug(TAG,"error purge OMEMO devices",e);
                }

            }

            @Override
            public void connectionClosed() {

                debug(TAG, "connection closed");

                /*
                 * This can be called in these cases:
                 * - Connection is shutting down
                 *   - because we are calling disconnect
                 *     - in do_logout
                 *
                 * - NOT
                 *   - because server disconnected "normally"
                 *   - we were trying to log in (initConnection), but are failing
                 *   - due to network error
                 *   - due to login failing
                 */

                //if the state is logged in, we should try to reconnect!
                if (getState() == LOGGED_IN)
                {
                    execute(new Runnable() {

                        public void run() {

                            mNeedReconnect = true;
                            setState(LOGGING_IN,
                                    new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network error"));
                            reconnect();

                        }

                    });
                }
            }
        };

        mConnection.addConnectionListener(connectionListener);
        mStreamHandler = new XmppStreamHandler(mConnection, connectionListener);
        Exception xmppConnectException = null;
        AbstractXMPPConnection conn = mConnection.connect();

    //    ReconnectionManager manager = ReconnectionManager.getInstanceFor(mConnection);
     //   manager.enableAutomaticReconnection();
      //  manager.setEnabledPerDefault(true);
      //  manager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY);

    }

    private void handleMessage (org.jivesoftware.smack.packet.Message smackMessage, boolean isOmemo) {

        String body = smackMessage.getBody();
        boolean isGroupMessage = smackMessage.getType() == org.jivesoftware.smack.packet.Message.Type.groupchat;

        if (smackMessage.getError() != null) {
            //  smackMessage.getError().getCode();
            String error = "Error " + smackMessage.getError() + " (" + smackMessage.getError().getCondition() + "): " + smackMessage.getError().getConditionText();
            debug(TAG, error);
            return;
        }


        if (body == null) {
            Collection<org.jivesoftware.smack.packet.Message.Body> mColl = smackMessage.getBodies();
            for (org.jivesoftware.smack.packet.Message.Body bodyPart : mColl) {
                String msg = bodyPart.getMessage();
                if (msg != null) {
                    body = msg;
                    break;
                }
            }

        }

        DeliveryReceipt drIncoming = (DeliveryReceipt) smackMessage.getExtension("received", DeliveryReceipt.NAMESPACE);

        ChatSession session = findOrCreateSession(smackMessage.getFrom().toString(), isGroupMessage);

        if (session != null) //not subscribed so don't do anything
        {

            if (drIncoming != null)
                session.onMessageReceipt(drIncoming.getId());

            if (body != null && session != null) {

                Message rec = new Message(body);

                rec.setTo(new XmppAddress(smackMessage.getTo().toString()));
                rec.setFrom(new XmppAddress(smackMessage.getFrom().toString()));
                rec.setDateTime(new Date());

                rec.setID(smackMessage.getStanzaId());

                if (isOmemo)
                    rec.setType(Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED);
                else
                    rec.setType(Imps.MessageType.INCOMING);

                // Detect if this was said by us, and mark message as outgoing
                if (isGroupMessage) {

                    if (TextUtils.isEmpty(rec.getFrom().getResource()))
                    {
                        return; //do nothing if there is no resource since that is a system message
                    }
                    else if (rec.getFrom().getResource().equals(rec.getTo().getUser())) {
                        try {


                            //rec.setType(Imps.MessageType.OUTGOING);
                            Occupant oc = mChatGroupManager.getMultiUserChat(rec.getFrom().getBareAddress()).getOccupant(JidCreate.entityFullFrom(rec.getFrom().getAddress()));
                            if (oc != null && oc.getJid().equals(mUser.getAddress().getAddress()))
                                return; //do nothing if it is from us

                        }
                        catch (Exception e){
                            debug(TAG,"error parsing address",e);
                        }
                    }

                }

                setPresence(smackMessage.getFrom(),Presence.AVAILABLE);

                boolean good = session.onReceiveMessage(rec);

                if (smackMessage.getExtension("request", DeliveryReceipt.NAMESPACE) != null) {
                    if (good) {
                        debug(TAG, "sending delivery receipt");
                        // got XEP-0184 request, send receipt
                        sendReceipt(smackMessage);
                        session.onReceiptsExpected(true);
                    } else {
                        debug(TAG, "not sending delivery receipt due to processing error");
                    }

                } else {
                    //no request for delivery receipt

                    session.onReceiptsExpected(false);
                }

            }


        }
    }

    private void sendPresencePacket() {        
        qPacket.add(makePresencePacket(mUserPresence));        
    }

    private void sendReceipt(org.jivesoftware.smack.packet.Message msg) {
        debug(TAG, "sending XEP-0184 ack to " + msg.getFrom() + " id=" + msg.getPacketID());
        org.jivesoftware.smack.packet.Message ack = new org.jivesoftware.smack.packet.Message(
                msg.getFrom(), msg.getType());
        ack.addExtension(new DeliveryReceipt(msg.getStanzaId()));
        sendPacket(ack);
    }

    protected int parsePresence(org.jivesoftware.smack.packet.Presence presence) {

        int type = Imps.Presence.AVAILABLE;
        org.jivesoftware.smack.packet.Presence.Mode rmode = presence.getMode();
        org.jivesoftware.smack.packet.Presence.Type rtype = presence.getType();

        if (rmode == org.jivesoftware.smack.packet.Presence.Mode.away
                || rmode == org.jivesoftware.smack.packet.Presence.Mode.xa)
            type = Imps.Presence.AWAY;
        else if (rmode == org.jivesoftware.smack.packet.Presence.Mode.dnd)
            type = Imps.Presence.DO_NOT_DISTURB;
        else if (rtype == org.jivesoftware.smack.packet.Presence.Type.unavailable)
            type = Imps.Presence.OFFLINE;
        else if (rtype ==  org.jivesoftware.smack.packet.Presence.Type.unsubscribed)
            type = Imps.Presence.OFFLINE;

        return type;
    }

    // We must release resources here, because we will not be reused
    void disconnected(ImErrorInfo info) {
        debug(TAG, "disconnected");
        join();
        setState(DISCONNECTED, info);
    }


    @Override
    public void logoutAsync() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                do_logout();
            }
        }).start();

    }

    // Force immediate logout
    public void logout() {
        logoutAsync();
    }

    // Usually runs in executor thread, unless called from logout()
    private void do_logout() {
        setState(LOGGING_OUT, null);
        disconnect();
        disconnected(null);
    }

    // Runs in executor thread
    private void disconnect() {

        if (mConnection != null && mConnection.isConnected()) {
            clearPing();

            try {
                //   mStreamHandler.quickShutdown();
                mConnection.disconnect();
            } catch (Throwable th) {
                // ignore
            }
        }

        //mConnection = null;
        mNeedReconnect = false;
        mRetryLogin = false;
    }

    @Override
    public void reestablishSessionAsync(Map<String, String> sessionContext) {
        execute(new Runnable() {
            @Override
            public void run() {
                if (getState() == SUSPENDED) {
                    debug(TAG, "reestablish");
                    mNeedReconnect = false;
                    setState(LOGGING_IN, null);
                    maybe_reconnect();
                }
            }
        });
    }

    @Override
    public void suspend() {

        if (getState() != DISCONNECTED) {

            setState(SUSPENDED, null);

            // Do not try to reconnect anymore if we were asked to suspend
            mNeedReconnect = false;
            clearPing();

            if (mTimerPackets != null)
                mTimerPackets.cancel();

            execute(new Runnable() {
                @Override
                public void run() {
                    debug(TAG, "suspend");

                    if (mStreamHandler != null)
                        mStreamHandler.quickShutdown();
                }
            });
        }
    }

    private ChatSession findOrCreateSession(String address, boolean groupChat) {

        try {

            ChatSession session = mSessionManager.findSession(JidCreate.bareFrom(address));
            Jid jid = JidCreate.from(address);

            if (jid.hasNoResource())
                return null;

            //create a session if this it not groupchat
            if (session == null && (!groupChat)) {
                ImEntity participant = findOrCreateParticipant(address, groupChat);

                if (participant != null) {
                    session = mSessionManager.createChatSession(participant, false);
                    mContactListManager.refreshPresence(address);

                    qAvatar.put(jid.asBareJid().toString(),"");

                    BareJid bareJid = jid.asBareJid();
                    if (getOmemo().getManager().contactSupportsOmemo(bareJid)) {
                        getOmemo().getManager().requestDeviceListUpdateFor(bareJid);
                        getOmemo().getManager().buildSessionsWith(bareJid);
                    }


                }

            }


            return session;
        }
        catch (Exception e)
        {
            debug(ImApp.LOG_TAG,"error findOrCreateSession",e);
            return null;
        }
    }

    synchronized ImEntity findOrCreateParticipant(String address, boolean isGroupChat) {
        ImEntity participant = null;

        if (isGroupChat) {
            Address xmppAddress = new XmppAddress(address);
            participant = mChatGroupManager.getChatGroup(xmppAddress);

            if (participant == null) {
                try {
                    mChatGroupManager.createChatGroupAsync(address, xmppAddress.getUser(), mUser.getName());
                    participant = mChatGroupManager.getChatGroup(xmppAddress);
                } catch (Exception e) {
                    Log.w(TAG, "unable to join group chat",e);
                    return null;
                }
            }
        } else
        {

           return mContactListManager.getContact(address);

        }

        return participant;
    }

    Contact findOrCreateContact(String address) {
        return (Contact) findOrCreateParticipant(address, false);
    }

    private Contact makeContact(String address) {

        Contact contact = null;

        //load from roster if we don't have the contact
        RosterEntry rEntry = null;

        try {


            if (mConnection != null)
                rEntry = mRoster.getEntry(JidCreate.bareFrom(address));

            if (rEntry != null) {
                XmppAddress xAddress = new XmppAddress(address);

                String name = rEntry.getName();
                if (name == null)
                    name = xAddress.getUser();

                //TODO we should check the type from here
                contact = new Contact(xAddress, name, Imps.Contacts.TYPE_NORMAL);
            } else {
                XmppAddress xAddress = new XmppAddress(address);

                contact = new Contact(xAddress, xAddress.getUser(), Imps.Contacts.TYPE_NORMAL);
            }
        }
        catch (XmppStringprepException xe)
        {
            //nothing return null;
        }

        return contact;
    }

    private final class XmppChatSessionManager extends ChatSessionManager {
        @Override
        public void sendMessageAsync(ChatSession session, Message message) {

            if (getState() != LOGGED_IN) {
                message.setType(Imps.MessageType.QUEUED);
                //can't send for now
                return;
            }

            MultiUserChat muc = null;

            org.jivesoftware.smack.packet.Message msgXmpp = null;

            try {

                Jid jidTo = JidCreate.from(message.getTo().getAddress());

                if (session.getParticipant() instanceof ChatGroup) {
                    muc = ((XmppChatGroupManager)getChatGroupManager()).getMultiUserChat(message.getTo().getAddress());
                    msgXmpp = muc.createMessage();
                } else {
                    msgXmpp = new org.jivesoftware.smack.packet.Message(
                            jidTo, org.jivesoftware.smack.packet.Message.Type.chat);
                }

                if (message.getFrom() == null)
                    msgXmpp.setFrom(JidCreate.from(mUser.getAddress().getAddress()));
                else
                    msgXmpp.setFrom(JidCreate.from(message.getFrom().getAddress()));

                msgXmpp.setBody(message.getBody());

                if (message.getID() != null)
                    msgXmpp.setStanzaId(message.getID());
                else
                    message.setID(msgXmpp.getStanzaId());

                if (muc != null)
                {
                    if (!muc.isJoined())
                        muc.join(Resourcepart.from(mUser.getName()));
                    muc.sendMessage(msgXmpp);
                }
                else {
                    Chat thisChat = mChatManager.createChat(jidTo.asEntityJidIfPossible());

                    //this isn't an OTR message, so let's try OMEMO
                    if (message.getType() == Imps.MessageType.QUEUED) {

                        //if this isn't already OTR encrypted, and the JID can support OMEMO then do it!
                        if (session.canOmemo()) {

                            try {

                                org.jivesoftware.smack.packet.Message msgEncrypted
                                        = getOmemo().getManager().encrypt(jidTo.asBareJid(), msgXmpp.getBody());

                                msgEncrypted.setStanzaId(msgXmpp.getStanzaId());
                                msgEncrypted.addExtension(new DeliveryReceiptRequest());
                                thisChat.sendMessage(msgEncrypted);
                                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);

                            } catch (CryptoFailedException cfe) {
                                debug(TAG, "crypto failed", cfe);
                            } catch (UndecidedOmemoIdentityException uoie) {
                                debug(TAG, "crypto failed", uoie);

                                //if we are connected, then try again
                                if (mConnection != null && mConnection.isConnected()) {
                                    getOmemo().trustOmemoDevice(msgXmpp.getFrom().asBareJid(), null, true);
                                    getOmemo().trustOmemoDevice(jidTo.asBareJid(), null, true);
                                    org.jivesoftware.smack.packet.Message msgEncrypted
                                            = getOmemo().getManager().encrypt(jidTo.asBareJid(), msgXmpp.getBody());
                                    msgEncrypted.addExtension(new DeliveryReceiptRequest());
                                    msgEncrypted.setStanzaId(msgXmpp.getStanzaId());
                                    thisChat.sendMessage(msgEncrypted);
                                    message.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);
                                }
                            }

                            return;
                        } else {
                            message.setType(Imps.MessageType.QUEUED);
                            return;
                        }
                    } else {
                        msgXmpp.addExtension(new DeliveryReceiptRequest());
                        thisChat.sendMessage(msgXmpp);
                        return;
                    }
                }

            }
            catch (Exception xe)
            {
                debug(TAG, "unable to send message",xe);
                message.setType(Imps.MessageType.QUEUED);
            }
        }

        ChatSession findSession(Jid jid) {

            ChatSession result = mSessions.get(jid.toString());

         //   if (result == null)
           //     result = mSessions.get(XmppAddress.stripResource(address));

            return result;
        }

        Collection<ChatSession> getSessions () {
            return mSessions.values();
        }

        @Override
        public ChatSession createChatSession(ImEntity participant, boolean isNewSession) {
            ChatSession session = super.createChatSession(participant,isNewSession);
            mSessions.put(participant.getAddress().getAddress(),session);
            return session;
        }

        @Override
        public boolean resourceSupportsOmemo(Jid jid) {

            try {

                if (getOmemo().resourceSupportsOmemo(jid)) {

                    if (getOmemo().getFingerprints(jid.asBareJid(), false).size() == 0) {
                        getOmemo().getManager().requestDeviceListUpdateFor(jid.asBareJid());
                    //    return false;
                    }

                    return true;
                }

            }
            catch (Exception e)
            {
                debug("OMEMO","There was a problem checking the resource",e);
            }

            return false;
        }
    }

    private void requestPresenceRefresh (String address)
    {
        mContactListManager.refreshPresence(address);
    }

    public class XmppContactListManager extends ContactListManager {

        @Override
        protected void setListNameAsync(final String name, final ContactList list) {
            execute(new Runnable() {
                @Override
                public void run() {
                    do_setListName(name, list);
                }
            });
        }

        // Runs in executor thread
        private void do_setListName(String name, ContactList list) {
            debug(TAG, "set list name");
            //mRoster.getGroup(list.getName()).setName(name);
            try {
                mRoster.getGroup(list.getName()).setName(name);
                notifyContactListNameUpdated(list, name);
            }
            catch (Exception e)
            {}
        }

        @Override
        public String normalizeAddress(String address) {
            return Address.stripResource(address);
        }

        @Override
        public void loadContactListsAsync() {

            execute(new Runnable() {
                @Override
                public void run() {
                    do_loadContactLists();

                }
            });

        }

        // For testing
        /*
        public void loadContactLists() {
            do_loadContactLists();
        }*/

        /**
         * Create new list of contacts from roster entries.
         *
         * Runs in executor thread
         **
         * @return contacts from roster which were not present in skiplist.
         */
        /*
        private Collection<Contact> fillContacts(Collection<RosterEntry> entryIter,
                Set<String> skipList) {

            Roster roster = mConnection.getRoster();

            Collection<Contact> contacts = new ArrayList<Contact>();
            for (RosterEntry entry : entryIter) {

                String address = entry.getUser();
                if (skipList != null && !skipList.add(address))
                    continue;

                String name = entry.getName();
                if (name == null)
                    name = address;

                XmppAddress xaddress = new XmppAddress(address);

                org.jivesoftware.smack.packet.Presence presence = roster.getPresence(address);

                String status = presence.getStatus();
                String resource = null;

                Presence p = new Presence(parsePresence(presence), status,
                        null, null, Presence.CLIENT_TYPE_DEFAULT);

                String from = presence.getFrom();
                if (from != null && from.lastIndexOf("/") > 0) {
                    resource = from.substring(from.lastIndexOf("/") + 1);

                    if (resource.indexOf('.')!=-1)
                        resource = resource.substring(0,resource.indexOf('.'));

                    p.setResource(resource);
                }

                Contact contact = mContactListManager.getContact(xaddress.getBareAddress());

                if (contact == null)
                    contact = new Contact(xaddress, name);

                contact.setPresence(p);

                contacts.add(contact);


            }
            return contacts;
        }
         */

        // Runs in executor thread
        private void do_loadContactLists() {

            if (mConnection == null)
                return;

            debug(TAG, "load contact lists");

            //since we don't show lists anymore, let's just load all entries together

            try {

                mRoster.reloadAndWait();
            }
            catch (Exception e)
            {
                debug(TAG,"error loading roaster",e);
                return;
            }

           // LastActivityManager lam = LastActivityManager.getInstanceFor(mConnection);
            long now = new Date().getTime();

            ContactList cl;

            try {
                cl = getDefaultContactList();
            } catch (ImException e1) {
                debug(TAG,"couldn't read default list");
                cl = null;
            }

            if (cl == null)
            {
                String generalGroupName = mContext.getString(R.string.buddies);

                Collection<Contact> contacts = new ArrayList<Contact>();
                XmppAddress groupAddress = new XmppAddress(generalGroupName);

                cl = new ContactList(groupAddress,generalGroupName, true, contacts, this);
                cl.setDefault(true);
                mDefaultContactList = cl;
                notifyContactListCreated(cl);
            }

            if (mConnection != null) {

                for (RosterEntry rEntry : mRoster.getEntries()) {
                    String address = rEntry.getJid().toString();

                    String name = rEntry.getName();

                    if (mUser.getAddress().getBareAddress().equals(address)) //don't load a roster for yourself
                        continue;

                    Contact contact = null;



                    contact = getContact(address);

                    if (contact == null) {
                        XmppAddress xAddr = new XmppAddress(address);

                        if (name == null || name.length() == 0)
                            name = xAddr.getUser();

                        contact = new Contact(xAddr, name, Imps.Contacts.TYPE_NORMAL);

                    }

                    if (!cl.containsContact(contact)) {
                        try {
                            cl.addExistingContact(contact);
                        } catch (ImException e) {
                            debug(TAG, "could not add contact to list: " + e.getLocalizedMessage());
                        }
                    }


                    int subStatus = Imps.ContactsColumns.SUBSCRIPTION_STATUS_NONE;

                    if (rEntry.isSubscriptionPending())
                      subStatus = Imps.ContactsColumns.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING;

                    rEntry.isApproved();

                    int subType = 0;
                    if (rEntry.getType() == RosterPacket.ItemType.both)
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_BOTH;
                    else if (rEntry.getType() == RosterPacket.ItemType.none)
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_NONE;
                    else if (rEntry.getType() == RosterPacket.ItemType.to) {
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_TO;
                    }
                    else if (rEntry.getType() == RosterPacket.ItemType.from) {
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_FROM;
                    }
                    else if (rEntry.getType() == RosterPacket.ItemType.remove)
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_REMOVE;

                    qPresence.add(mRoster.getPresence(rEntry.getJid()));

                    /**
                    try {
                        LastActivity activity = lam.getLastActivity(JidCreate.from(address));
                        contact.setPresence(new Date(activity.getIdleTime());
                    }
                    catch (Exception e)
                    {
                        Log.e("LastActivity","error getting last activity for: " + address,e);
                    }**/

                    /**
                    try {

                        mContactListManager.getSubscriptionRequestListener().onSubScriptionChanged(contact, mProviderId, mAccountId, subType, subStatus);
                        handlePresenceChanged(mRoster.getPresence(rEntry.getJid()));

                    }
                    catch (RemoteException re)
                    {

                    }**/


                }
            }

            notifyContactListLoaded(cl);
            notifyContactListsLoaded();

        }

     // Runs in executor thread
        public void addContactsToList(Collection<String> addresses) {

            debug(TAG, "add contacts to lists");


            ContactList cl;

            try {
                cl = mContactListManager.getDefaultContactList();
            } catch (ImException e1) {
                debug(TAG,"couldn't read default list");
                cl = null;
            }

            if (cl == null)
            {
                String generalGroupName = mContext.getString(R.string.buddies);

                Collection<Contact> contacts = new ArrayList<Contact>();
                XmppAddress groupAddress = new XmppAddress(generalGroupName);

                cl = new ContactList(groupAddress,generalGroupName, true, contacts, this);

                notifyContactListCreated(cl);
            }

            for (String address : addresses)
            {

                if (mUser.getAddress().getBareAddress().equals(address)) //don't load a roster for yourself
                    continue;

                Contact contact = getContact(address);

                if (contact == null)
                {
                    XmppAddress xAddr = new XmppAddress(address);

                    contact = new Contact(xAddr,xAddr.getUser(), Imps.Contacts.TYPE_NORMAL);

                }

                //org.jivesoftware.smack.packet.Presence p = roster.getPresence(contact.getAddress().getBareAddress());
                //qPresence.push(p);

                if (!cl.containsContact(contact))
                {
                    try {
                        cl.addExistingContact(contact);
                    } catch (ImException e) {
                        debug(TAG,"could not add contact to list: " + e.getLocalizedMessage());
                    }
                }

            }

            notifyContactListLoaded(cl);
            notifyContactListsLoaded();

        }

        public void refreshPresence (String address)
        {

            try {
                org.jivesoftware.smack.packet.Presence presence = mRoster.getPresence(JidCreate.bareFrom(address));
                if (presence != null) {
                    handlePresenceChanged(presence);
                }
            }
            catch (XmppStringprepException xe)
            {
                debug(TAG,"error refreshing presence: " + xe,xe);
            }

        }

        /*
         * iterators through a list of contacts to see if there were any Presence
         * notifications sent before the contact was loaded
         */
        /*
        private void processQueuedPresenceNotifications (Collection<Contact> contacts)
        {

        	Roster roster = mConnection.getRoster();

        	//now iterate through the list of queued up unprocessed presence changes
        	for (Contact contact : contacts)
        	{

        		String address = parseAddressBase(contact.getAddress().getFullName());

        		org.jivesoftware.smack.packet.Presence presence = roster.getPresence(address);

        		if (presence != null)
        		{
        			debug(TAG, "processing queued presence: " + address + " - " + presence.getStatus());

        			unprocdPresence.remove(address);

        			contact.setPresence(new Presence(parsePresence(presence), presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT));

        			Contact[] updatedContact = {contact};
        			notifyContactsPresenceUpdated(updatedContact);	
        		}



        	}
        }*/


        public void listenToRoster(final Roster roster) {

            roster.addRosterListener(rListener);
        }


        RosterListener rListener = new RosterListener() {


            @Override
            public void presenceChanged(org.jivesoftware.smack.packet.Presence presence) {
                
         //       qPresence.push(presence);

            }

            @Override
            public void entriesUpdated(Collection<Jid> addresses) {

                /**
                for (Jid address :addresses)
                {
                    requestPresenceRefresh(address.toString());
                    
                }**/
            }

            @Override
            public void entriesDeleted(Collection<Jid> addresses) {

                ContactList cl;
                try {
                    cl = mContactListManager.getDefaultContactList();

                    for (Jid address : addresses) {
                        Contact contact = new Contact(new XmppAddress(address.toString()),address.toString(), Imps.Contacts.TYPE_NORMAL);
                        mContactListManager.notifyContactListUpdated(cl, ContactListListener.LIST_CONTACT_REMOVED, contact);
                    }
                    

                } catch (ImException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            @Override
            public void entriesAdded(Collection<Jid> addresses) {

                try
                {
                    if (mContactListManager.getState() == LISTS_LOADED)
                    {
                        
                        for (Jid address : addresses)
                        {

                            String addressString = address.toString();

                            Contact contact = getContact(addressString);

                            if (contact == null)
                            {
                                XmppAddress xAddr = new XmppAddress(addressString);
                                contact = new Contact(xAddr,xAddr.getUser(), Imps.Contacts.TYPE_NORMAL);
        
                            }
        
                            try
                            {
                                ContactList cl = mContactListManager.getDefaultContactList();
                                if (!cl.containsContact(contact))
                                    cl.addExistingContact(contact);
                                 
                            }
                            catch (Exception e)
                            {
                                debug(TAG,"could not add contact to list: " + e.getLocalizedMessage());

                            }
                        
        
                        }
                        
                    }
                }
                catch (Exception e)
                {
                    Log.d(TAG,"error adding contacts",e);
                }
            }
        };


        @Override
        protected ImConnection getConnection() {
            return XmppConnection.this;
        }

        @Override
        protected void doRemoveContactFromListAsync(Contact contact, ContactList list) {
            // FIXME synchronize this to executor thread
            if (mConnection == null)
                return;
            
            String address = contact.getAddress().getAddress();

            //otherwise, send unsub message and delete from local contact database
            org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribe);            
            presence.setTo(address);
            sendPacket(presence);
            
            presence = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribed);            
            presence.setTo(address);
            sendPacket(presence);
            
            try {
                RosterEntry entry = mRoster.getEntry(JidCreate.bareFrom(address));
                RosterGroup group = mRoster.getGroup(list.getName());

                if (entry != null) {
                    if (group == null) {
                        debug(TAG, "could not find group " + list.getName() + " in roster");
                        if (mRoster != null)
                            mRoster.removeEntry(entry);

                    } else {
                        group.removeEntry(entry);
                        entry = mRoster.getEntry(JidCreate.bareFrom(address));
                        // Remove from Roster if this is the last group
                        if (entry != null && entry.getGroups().size() <= 1)
                            mRoster.removeEntry(entry);

                    }
                }
            } catch (Exception e) {
                debug(TAG, "remove entry failed: " + e.getMessage());
                throw new RuntimeException(e);
            }


            notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_REMOVED, contact);
        }

        @Override
        protected void doDeleteContactListAsync(ContactList list) {
            // TODO delete contact list
            debug(TAG, "delete contact list " + list.getName());

        }

        @Override
        protected void doCreateContactListAsync(String name, Collection<Contact> contacts,
                boolean isDefault) {
            debug(TAG, "create contact list " + name + " default " + isDefault);
        }

        @Override
        protected void doBlockContactAsync(String address, boolean block) {

            blockContact(address, block);
        }

        @Override
        protected void doAddContactToListAsync(Contact contact, ContactList list, boolean autoSubscribedPresence) throws ImException {
            debug(TAG, "add contact to " + list.getName());

            if (!list.containsContact(contact)) {
                try {
                    list.addExistingContact(contact);
                } catch (ImException e) {
                    debug(TAG, "could not add contact to list: " + e.getLocalizedMessage());
                }
            }

            ChatSession session = findOrCreateSession(contact.getAddress().toString(), false);

            if (session != null)
                session.setSubscribed(autoSubscribedPresence);

            notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);

            Contact[] contacts = {contact};
            notifyContactsPresenceUpdated(contacts);

            if (autoSubscribedPresence)
                qNewContact.push(contact);

        }


        public boolean blockContact(String blockContact, boolean doBlock) {


            PrivacyItem item=new PrivacyItem(PrivacyItem.Type.jid,blockContact, false, 7);
            PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(mConnection);

            if (privacyManager != null) {
                List<PrivacyItem> list = new ArrayList<PrivacyItem>();
                list.add(item);

                try {
                    privacyManager.updatePrivacyList(PRIVACY_LIST_DEFAULT, list);
                    privacyManager.setActiveListName(PRIVACY_LIST_DEFAULT);
                    return true;
                } catch (InterruptedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            return false;
        }

        @Override
        public void declineSubscriptionRequest(Contact contact) {


            try {
                debug(TAG, "decline subscription");
                org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                        org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
                response.setTo(JidCreate.bareFrom(contact.getAddress().getBareAddress()));
                sendPacket(response);

                mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact, mProviderId, mAccountId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            ChatSession session = findOrCreateSession(contact.getAddress().toString(), false);

            if (session != null)
                session.setSubscribed(false);

        }

        @Override
        public void approveSubscriptionRequest(final Contact contact) {


            try {

                BareJid bareJid = JidCreate.bareFrom(contact.getAddress().getBareAddress());
                RosterEntry entry = mRoster.getEntry(bareJid);
                if (entry == null || !entry.canSeeMyPresence())
                {
                    org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                            org.jivesoftware.smack.packet.Presence.Type.subscribed);
                    response.setTo(bareJid);

                    //send now, or queue
                    if (mConnection != null && mConnection.isAuthenticated())
                        mConnection.sendStanza(response);
                    else
                        sendPacket(response);

                }

                if (entry == null || !entry.canSeeHisPresence()) {

                    org.jivesoftware.smack.packet.Presence request = new org.jivesoftware.smack.packet.Presence(
                            org.jivesoftware.smack.packet.Presence.Type.subscribe);
                    request.setTo(bareJid);
                    //send now, or queue
                    if (mConnection != null && mConnection.isAuthenticated())
                        mConnection.sendStanza(request);
                    else
                        sendPacket(request);
                }

                mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact, mProviderId, mAccountId);

                ChatSession session = findOrCreateSession(contact.getAddress().toString(), false);

                if (session != null)
                    session.setSubscribed(true);

                if (entry != null && entry.canSeeHisPresence()) {

                    requestPresenceRefresh(contact.getAddress().getBareAddress());
                    qAvatar.put(contact.getAddress().getAddress(),"");

                    if (getOmemo().getManager().contactSupportsOmemo(bareJid)) {
                        getOmemo().getManager().requestDeviceListUpdateFor(bareJid);
                        getOmemo().getManager().buildSessionsWith(bareJid);
                    }
                }
            }
            catch (Exception e) {
                debug (TAG, "error responding to subscription approval: " + e.getLocalizedMessage());

            }

        }

        @Override
        public Contact[] createTemporaryContacts(String[] addresses) {
            // debug(TAG, "create temporary " + address);

            Contact[] contacts = new Contact[addresses.length];

            int i = 0;

            for (String address : addresses)
            {
                contacts[i++] = makeContact(address);
            }

            notifyContactsPresenceUpdated(contacts);
            return contacts;
        }

        @Override
        protected void doSetContactName(String address, String name) throws ImException {

            // set name
            try {
                RosterEntry entry = mRoster.getEntry(JidCreate.bareFrom(address));
                // confirm entry still exists
                if (entry == null) {
                    return;
                }
                entry.setName(name);
            }
            catch (Exception e)
            {
                throw new ImException(e.toString());
            }
        }
    }

    public void sendHeartbeat(final long heartbeatInterval) {
        // Don't let heartbeats queue up if we have long running tasks - only
        // do the heartbeat if executor is idle.
        boolean success = executeIfIdle(new Runnable() {
            @Override
            public void run() {
                debug(TAG, "heartbeat state = " + getState());
                doHeartbeat(heartbeatInterval);
            }
        });

        if (!success) {
            debug(TAG, "failed to schedule heartbeat state = " + getState());
        }
    }

    // Runs in executor thread
    public void doHeartbeat(long heartbeatInterval) {
        heartbeatSequence++;

        if (getState() == SUSPENDED) {
            debug(TAG, "heartbeat during suspend");
            return;
        }

        if (mConnection == null && mRetryLogin) {
            debug(TAG, "reconnect with login");
            do_login();
            return;
        }

        if (mConnection == null)
            return;

        if (mNeedReconnect) {
            reconnect();
        } else if (!mConnection.isConnected() && getState() == LOGGED_IN) {
            // Smack failed to tell us about a disconnect
            debug(TAG, "reconnect on unreported state change");
            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network disconnected"));
            force_reconnect();
        } else if (getState() == LOGGED_IN) {
            if (PING_ENABLED) {
                // Check ping on every heartbeat.  checkPing() will return true immediately if we already checked.
                if (!mPingSuccess) {
                    debug(TAG, "reconnect on ping failed: " + mUser.getAddress().getAddress());
                    setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network timeout"));
                    maybe_reconnect();
                } else {
                    // Send pings only at intervals configured by the user
                    if (heartbeatSequence >= heartbeatInterval) {
                        heartbeatSequence = 0;
                        debug(TAG, "ping");
                        sendPing();
                    }
                }
            }
        }
    }

    private void clearPing() {
        debug(TAG, "clear ping");
        heartbeatSequence = 0;
    }


    boolean mPingSuccess = true;
    // Runs in executor thread
    private void sendPing() {

        try {

            mPingSuccess = mPingManager.pingMyServer() ;
       ;}
        catch (Exception e)
        {
            mPingSuccess = false;
        }


    }


    @Override
    public void networkTypeChanged() {

        //this should only be called when the network is back online

        super.networkTypeChanged();

        new Thread(new Runnable ()
                {
                    public void run ()
                    {
                        if (mState == SUSPENDED || mState == SUSPENDING) {
                            debug(TAG, "network type changed");
                            mNeedReconnect = true;
                            setState(LOGGING_IN, null);
                            reconnect();
                        }
                    }
                }
        ).start();


    }

    /*
     * Force a shutdown and reconnect, unless we are already reconnecting.
     *
     * Runs in executor thread
     */
    private void force_reconnect() {
        debug(TAG, "force_reconnect mNeedReconnect=" + mNeedReconnect + " state=" + getState()
                + " connection?=" + (mConnection != null));

        if (mConnection == null)
            return;
        if (mNeedReconnect)
            return;

        mNeedReconnect = true;

        try {
            if (mConnection != null && mConnection.isConnected()) {
                mStreamHandler.quickShutdown();
            }
        } catch (Exception e) {

            Log.w(TAG, "problem disconnecting on force_reconnect: " + e.getMessage());
        }

        reconnect();
    }

    /*
     * Reconnect unless we are already in the process of doing so.
     *
     * Runs in executor thread.
     */
    private void maybe_reconnect() {
        debug(TAG, "maybe_reconnect mNeedReconnect=" + mNeedReconnect + " state=" + getState()
                + " connection?=" + (mConnection != null));

        // This is checking whether we are already in the process of reconnecting.  If we are,
        // doHeartbeat will take care of reconnecting.
        if (mNeedReconnect)
            return;

        if (getState() == SUSPENDED)
            return;

        if (mConnection == null)
            return;

        mNeedReconnect = true;
        reconnect();
    }

    /*
     * Retry connecting
     *
     * Runs in executor thread
     */
    private void reconnect() {
        if (getState() == SUSPENDED) {
            debug(TAG, "reconnect during suspend, ignoring");
            return;
        }

        if (mConnection != null) {
            // It is safe to ask mConnection whether it is connected, because either:
            // - We detected an error using ping and called force_reconnect, which did a shutdown
            // - Smack detected an error, so it knows it is not connected
            // so there are no cases where mConnection can be confused about being connected here.
            // The only left over cases are reconnect() being called too many times due to errors
            // reported multiple times or errors reported during a forced reconnect.

            // The analysis above is incorrect in the case where Smack loses connectivity
            // while trying to log in.  This case is handled in a future heartbeat
            // by checking ping responses.
            clearPing();
            if (mConnection != null && mConnection.isAuthenticated()) {
                debug(TAG,"reconnect while already connected, assuming good: " + mConnection);
                mNeedReconnect = false;
                setState(LOGGED_IN, null);
                return;
            }
            debug(TAG, "reconnect");

            try {
                if (mStreamHandler.isResumePossible()) {
                    // Connect without binding, will automatically trigger a resume
                    debug(TAG, "mStreamHandler resume");
                    mConnection.connect();
                    initServiceDiscovery();
                    initPacketProcessor();

                } else {
                    debug(TAG, "reconnection on network change failed: " + mUser.getAddress().getAddress());

                    mConnection = null;
                    mNeedReconnect = true;
                    setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, null));

                    do_login();

                }
            } catch (Exception e) {
                if (mStreamHandler != null)
                    mStreamHandler.quickShutdown();

                mConnection = null;
                debug(TAG, "reconnection attempt failed", e);
                // Smack incorrectly notified us that reconnection was successful, reset in case it fails
                mNeedReconnect = false;
                setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));

                do_login();

            }
        } else {
            mNeedReconnect = false;
            mConnection = null;
            debug(TAG, "reconnection on network change failed");

            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR,
                    "reconnection on network change failed"));

            do_login();

        }
    }

    @Override
    protected void setState(int state, ImErrorInfo error) {
        debug(TAG, "setState to " + state);
        super.setState(state, error);
        
        if (state == LOGGED_IN)
        {
            //update and send new presence packet out
            mUserPresence = new Presence(Presence.AVAILABLE, "", Presence.CLIENT_TYPE_MOBILE);
            sendPresencePacket();
            mChatGroupManager.reconnectAll();
        }
    }    
    
    public void debug(String tag, String msg) {
        //  if (Log.isLoggable(TAG, Log.DEBUG)) {
        if (Debug.DEBUG_ENABLED) {
            Log.d(tag, "" + mGlobalId + " : " + msg);
        }
    }

    public void debug(String tag, String msg, Exception e) {
        if (Debug.DEBUG_ENABLED) {
            Log.e(tag, "" + mGlobalId + " : " + msg, e);
        }
    }

    /*
    @Override
    public void handle(Callback[] arg0) throws IOException {

        for (Callback cb : arg0) {
            debug(TAG, cb.toString());
        }

    }*/

    /*
    public class MySASLDigestMD5Mechanism extends SASLMechanism
    {

        public MySASLDigestMD5Mechanism(SASLAuthentication saslAuthentication)
        {
            super(saslAuthentication);
        }

        protected void authenticate()
            throws IOException, XMPPException
        {
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", hostname, props, this);
            super.authenticate();
        }

        public void authenticate(String username, String host, String password)
            throws IOException, XMPPException
        {
            authenticationId = username;
            this.password = password;
            hostname = host;
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
            super.authenticate();
        }

        public void authenticate(String username, String host, CallbackHandler cbh)
            throws IOException, XMPPException
        {
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
            super.authenticate();
        }

        protected String getName()
        {
            return "DIGEST-MD5";
        }

        public void challengeReceived(String challenge)
            throws IOException
        {
            //StringBuilder stanza = new StringBuilder();
            byte response[];
            if(challenge != null)
                response = sc.evaluateChallenge(Base64.decode(challenge));
            else
                //response = sc.evaluateChallenge(null);
                response = sc.evaluateChallenge(new byte[0]);
            //String authenticationText = "";
            Packet responseStanza;
            //if(response != null)
            //{
                //authenticationText = Base64.encodeBytes(response, 8);
                //if(authenticationText.equals(""))
                    //authenticationText = "=";

                if (response == null){
                    responseStanza = new Response();
                } else {
                    responseStanza = new Response(Base64.encodeBytes(response,Base64.DONT_BREAK_LINES));
                }
            //}
            //stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            //stanza.append(authenticationText);
            //stanza.append("</response>");
            //getSASLAuthentication().send(stanza.toString());
            getSASLAuthentication().send(responseStanza);
        }
    }
     */
    private void initServiceDiscovery() {
        debug(TAG, "init service discovery");
        // register connection features
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);

        if (!sdm.includesFeature(DISCO_FEATURE))
            sdm.addFeature(DISCO_FEATURE);
        if (!sdm.includesFeature(DeliveryReceipt.NAMESPACE))
            sdm.addFeature(DeliveryReceipt.NAMESPACE);

        sdm.addFeature(HttpFileUploadManager.NAMESPACE);

        DeliveryReceiptManager.getInstanceFor(mConnection).dontAutoAddDeliveryReceiptRequests();
        DeliveryReceiptManager.getInstanceFor(mConnection).setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.disabled);

    }


    private void onReconnectionSuccessful() {
        mNeedReconnect = false;
        setState(LOGGED_IN, null);
        
    }


    public static void addProviderManagerExtensions(){

        ProviderManager.addIQProvider("query", "jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
        ProviderManager.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        ProviderManager.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        ProviderManager.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

        // Time
        try {
            ProviderManager.addIQProvider("query", "jabber:iq:time",
                    Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("TestClient",
                    "Can't load class for org.jivesoftware.smackx.packet.Time");
        }
        //Pings
        ProviderManager.addIQProvider("ping","urn:xmpp:ping",new PingProvider());
        // Roster Exchange
       // providerManager.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());
        ProviderManager.addIQProvider("vCard", "vcard-temp", new VCardProvider());
        // Message Events
     //   providerManager.addExtensionProvider("x", "jabber:x:event",
       //         new MessageEventProvider());

        // XHTML
        ProviderManager.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
                new XHTMLExtensionProvider());
        // Group Chat Invitations
        ProviderManager.addExtensionProvider("x", "jabber:x:conference",
                new GroupChatInvitation.Provider());
        // Service Discovery # Items
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/disco#items",
                new DiscoverItemsProvider());
        // Service Discovery # Info
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/disco#info",
                new DiscoverInfoProvider());
        // Data Forms
        ProviderManager.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
        // MUC User
        ProviderManager.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
                new MUCUserProvider());
        // MUC Admin
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
                new MUCAdminProvider());
        // MUC Owner
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
                new MUCOwnerProvider());
        // Delayed Delivery
        //ProviderManager.addExtensionProvider("x", "jabber:x:delay",
          //      new DelayInformationProvider());
        // Version
        try {
            ProviderManager.addIQProvider("query", "jabber:iq:version",
                    Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
            // Not sure what's happening here.
        }
        // VCard
        ProviderManager.addIQProvider("vCard", "vcard-temp", new VCardProvider());
        // Offline Message Requests
        ProviderManager.addIQProvider("offline", "http://jabber.org/protocol/offline",
                new OfflineMessageRequest.Provider());
        // Offline Message Indicator
        ProviderManager.addExtensionProvider("offline", "http://jabber.org/protocol/offline",
                new OfflineMessageInfo.Provider());
        // Last Activity
        ProviderManager.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());

        // User Search
        ProviderManager.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        // SharedGroupsInfo
        ProviderManager.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup",
                new SharedGroupsInfo.Provider());
        // JEP-33: Extended Stanza Addressing
        ProviderManager.addExtensionProvider("addresses", "http://jabber.org/protocol/address",
                new MultipleAddressesProvider());
        // FileTransfer
        ProviderManager.addIQProvider("si", "http://jabber.org/protocol/si",
                new StreamInitiationProvider());


        // Privacy
        ProviderManager.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
        ProviderManager.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        ProviderManager.addExtensionProvider("malformed-action",
                "http://jabber.org/protocol/commands",
                new AdHocCommandDataProvider.MalformedActionError());
        ProviderManager.addExtensionProvider("bad-locale",
                "http://jabber.org/protocol/commands",
                new AdHocCommandDataProvider.BadLocaleError());
        ProviderManager.addExtensionProvider("bad-payload",
                "http://jabber.org/protocol/commands",
                new AdHocCommandDataProvider.BadPayloadError());
        ProviderManager.addExtensionProvider("bad-sessionid",
                "http://jabber.org/protocol/commands",
                new AdHocCommandDataProvider.BadSessionIDError());
        ProviderManager.addExtensionProvider("session-expired",
                "http://jabber.org/protocol/commands",
                new AdHocCommandDataProvider.SessionExpiredError());
        ProviderManager.addIQProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
        //  Offline Message Indicator
        ProviderManager.addExtensionProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/disco#info",
                new DiscoverInfoProvider());
        ProviderManager.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
        // pm.addExtensionProvider("status ","", new XMLPlayerList());
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        ProviderManager.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

        //  Private Data Storage
       // ProviderManager.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());

        //  Time
        /**
        try {
            ProviderManager.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
        }*/

        //  Roster Exchange
//        ProviderManager.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());

        //  Message Events
       // ProviderManager.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());

        //  Chat State
        ChatStateExtensionProvider csep = new ChatStateExtensionProvider();

        ProviderManager.addExtensionProvider("active","http://jabber.org/protocol/chatstates", csep);
        ProviderManager.addExtensionProvider("composing","http://jabber.org/protocol/chatstates",  csep);
        ProviderManager.addExtensionProvider("paused","http://jabber.org/protocol/chatstates",  csep);
        ProviderManager.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates",  csep);
        ProviderManager.addExtensionProvider("gone","http://jabber.org/protocol/chatstates",  csep);


        //  XHTML
//        ProviderManager.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

        //  Group Chat Invitations
      //  ProviderManager.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());

        //  Service Discovery # Items
  //      ProviderManager.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());

        //  Service Discovery # Info
    //    ProviderManager.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

        //  Data Forms
      //  ProviderManager.addExtensionProvider("x","jabber:x:data", new DataFormProvider());

        //  MUC User
       // ProviderManager.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());

        //  MUC Admin
       // ProviderManager.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());

        //  MUC Owner
        //ProviderManager.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());


        //  Delayed Delivery
     //   ProviderManager.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());

        //  Version
        /**
        try {
            ProviderManager.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException err) {
            //  Not sure what's happening here.
        }
        **/

        //  VCard
      //  ProviderManager.addIQProvider("vCard","vcard-temp", new VCardProvider());

        //  Offline Message Requests
        //ProviderManager.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());

        //  Offline Message Indicator
        //ProviderManager.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());

        //  Last Activity
        //ProviderManager.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());

        //  User Search
        //ProviderManager.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());

        //  SharedGroupsInfo
        //ProviderManager.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());

        //  JEP-33: Extended Stanza Addressing
        //ProviderManager.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());

        //   FileTransfer
        //ProviderManager.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());

        //ProviderManager.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());

        //  Privacy

        /**
        ProviderManager.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
        ProviderManager.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        ProviderManager.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        ProviderManager.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        ProviderManager.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        ProviderManager.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        ProviderManager.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
            **/
    }

    class NameSpace {

        public static final String DISCO_INFO = "http://jabber.org/protocol/disco#info";
        public static final String DISCO_ITEMS = "http://jabber.org/protocol/disco#items";
        public static final String IQ_GATEWAY = "jabber:iq:gateway";
        public static final String IQ_GATEWAY_REGISTER = "jabber:iq:gateway:register";
        public static final String IQ_LAST = "jabber:iq:last";
        public static final String IQ_REGISTER = "jabber:iq:register";
        public static final String IQ_REGISTERED = "jabber:iq:registered";
        public static final String IQ_ROSTER = "jabber:iq:roster";
        public static final String IQ_VERSION = "jabber:iq:version";
        public static final String CHATSTATES = "http://jabber.org/protocol/chatstates";
        public static final String XEVENT = "jabber:x:event";
        public static final String XDATA = "jabber:x:data";
        public static final String MUC = "http://jabber.org/protocol/muc";
        public static final String MUC_USER = MUC + "#user";
        public static final String MUC_ADMIN = MUC + "#admin";
        public static final String SPARKNS = "http://www.jivesoftware.com/spark";
        public static final String DELAY = "urn:xmpp:delay";
        public static final String OFFLINE = "http://jabber.org/protocol/offline";
        public static final String X_DELAY = "jabber:x:delay";
        public static final String VCARD_TEMP = "vcard-temp";
        public static final String VCARD_TEMP_X_UPDATE = "vcard-temp:x:update";
        public static final String ATTENTIONNS = "urn:xmpp:attention:0";

    }


    public boolean registerAccount (Imps.ProviderSettings.QueryMap providerSettings, String username, String password, Map<String,String> params) throws Exception
    {

        initConnection(providerSettings, username);

        if (mConnection != null && mConnection.isConnected() && mConnection.isSecureConnection()) {
            org.jivesoftware.smackx.iqregister.AccountManager aMgr = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(mConnection);

            if (aMgr.supportsAccountCreation()) {
                aMgr.createAccount(Localpart.from(username), password, params);

                return true;

            }
        }

        return false;

    }

    public boolean changeServerPassword (long providerId, long accountId, String oldPassword, String newPassword) throws Exception
    {

        boolean result = false;

        try {

            loginSync(accountId, oldPassword, providerId, false);

            if (mConnection != null &&
                    mConnection.isConnected()
                    && mConnection.isSecureConnection()
                    && mConnection.isAuthenticated()) {
                org.jivesoftware.smackx.iqregister.AccountManager aMgr = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(mConnection);
                aMgr.changePassword(newPassword);
                result = true;
                do_logout();
            }
        }
        catch (XMPPException xe)
        {
            result = false;
        }

        return result;

    }

    private void handleChatState (String from, String chatStateXml) throws RemoteException {

        Presence p = null;
        Contact contact = mContactListManager.getContact(from);
        if (contact == null)
            return;

        boolean isTyping = false;

        //handle is-typing, probably some indication on screen
        if (chatStateXml.contains(ChatState.active.toString())) {
            p = new Presence(Presence.AVAILABLE, "", null, null,
                    Presence.CLIENT_TYPE_MOBILE);

        }
        else if (chatStateXml.contains(ChatState.composing.toString())) {
            p = new Presence(Presence.AVAILABLE, "", null, null,
                    Presence.CLIENT_TYPE_MOBILE);

            isTyping = true;

        }
        else if (chatStateXml.contains(ChatState.inactive.toString())||chatStateXml.contains(ChatState.paused.toString())) {

        }
        else if (chatStateXml.contains(ChatState.gone.toString())) {

        }

        IChatSession csa = mSessionManager.getAdapter().getChatSession(from);

        if (csa != null)
            csa.setContactTyping(contact, isTyping);

        if (p != null) {
            String[] presenceParts = from.split("/");
            if (presenceParts.length > 1)
                p.setResource(presenceParts[1]);

            contact.setPresence(p);
            Collection<Contact> contactsUpdate = new ArrayList<Contact>();
            contactsUpdate.add(contact);
            mContactListManager.notifyContactsPresenceUpdated(contactsUpdate.toArray(new Contact[contactsUpdate.size()]));

        }


    }

    @Override
    public void sendTypingStatus (final String to, final boolean isTyping)
    {
        mExecutor.execute(new Runnable() {
            public void run() {
                sendChatState(to, isTyping ? ChatState.composing : ChatState.inactive);
            }
        });
    }

    private void sendChatState (String to, ChatState currentChatState)
    {
        try {
            if (mConnection != null && mConnection.isConnected())
            {
                findOrCreateSession(to, false);
                Chat thisChat = mChatManager.createChat(JidCreate.from(to).asEntityJidIfPossible());
                ChatStateManager.getInstance(mConnection).setCurrentState(currentChatState, thisChat);
            }
        }
        catch (Exception e)
        {
            Log.w(ImApp.LOG_TAG,"error sending chat state: " + e.getMessage());
        }
    }

    private void setPresence (Jid from, int presenceType) {

        Presence p = null;
        Contact contact = mContactListManager.getContact(from.asBareJid().toString());
        if (contact == null)
            return;

        p = new Presence(presenceType, "", null, null,
                Presence.CLIENT_TYPE_MOBILE);

        if (from.hasResource())
            p.setResource(from.getResourceOrEmpty().toString());

        contact.setPresence(p);
        Collection<Contact> contactsUpdate = new ArrayList<Contact>();
        contactsUpdate.add(contact);
        mContactListManager.notifyContactsPresenceUpdated(contactsUpdate.toArray(new Contact[contactsUpdate.size()]));



    }

    private Contact handlePresenceChanged(org.jivesoftware.smack.packet.Presence presence) {

        if (presence == null) //our presence isn't really valid
            return null;

        if (TextUtils.isEmpty(presence.getFrom()))
            return null;

        if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.error)
            return null;

        if (presence.getFrom().toString().startsWith(mUser.getAddress().getBareAddress())) //ignore presence from yourself
              return null;

        XmppAddress xaddress = new XmppAddress(presence.getFrom().toString());

        Presence p = new Presence(parsePresence(presence), presence.getStatus(), null, null,
                Presence.CLIENT_TYPE_MOBILE,null,xaddress.getResource());

        //this is only persisted in memory
        p.setPriority(presence.getPriority());

        // Get presence from the Roster to handle priorities and such
        // TODO: this causes bad network and performance issues
        //   if (presence.getType() == Type.available) //get the latest presence for the highest priority

        if (mContactListManager == null)
            return null; //we may have logged out

        Contact contact = mContactListManager.getContact(xaddress.getBareAddress());
        BareJid jid = presence.getFrom().asBareJid();

        if (presence.getFrom().hasResource())
            p.setResource(presence.getFrom().getResourceOrEmpty().toString());

        if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.subscribe
                ) {
            debug(TAG, "got subscribe request: " + presence.getFrom());

            try {

                ContactList cList = getContactListManager().getDefaultContactList();

                if (contact == null) {
                    XmppAddress xAddr = new XmppAddress(presence.getFrom().toString());
                    contact = new Contact(xAddr, xAddr.getUser(), Imps.Contacts.TYPE_NORMAL);
                    mContactListManager.doAddContactToListAsync(contact, cList, false);
                    contact.setPresence(p);

                    mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(contact, mProviderId, mAccountId);
                }
                else
                {
                    mContactListManager.approveSubscriptionRequest(contact);
                }

            }
            catch (Exception e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }


        }
        else if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.subscribed) {

            debug(TAG, "got subscribed confirmation: " + presence.getFrom());
            try
            {
                if (contact == null) {
                    XmppAddress xAddr = new XmppAddress(presence.getFrom().toString());
                    contact = new Contact(xAddr, xAddr.getUser(), Imps.Contacts.TYPE_NORMAL);
                }

                mContactListManager.doAddContactToListAsync(contact,getContactListManager().getDefaultContactList(),false);
                mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact, mProviderId, mAccountId);

                if (getOmemo().getManager().contactSupportsOmemo(jid)) {
                    getOmemo().getManager().requestDeviceListUpdateFor(jid);
                    getOmemo().getManager().buildSessionsWith(jid);
                }

            }
            catch (Exception e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }
        }
        else if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.unsubscribe) {
            debug(TAG,"got unsubscribe request: " + presence.getFrom());

            //TBD how to handle this
            //     mContactListManager.getSubscriptionRequestListener().onUnSubScriptionRequest(contact);
        }
        else if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.unsubscribed) {
            debug(TAG,"got unsubscribe request: " + presence.getFrom());
            try
            {
                mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact, mProviderId, mAccountId);

            }
            catch (RemoteException e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }

        }

        //this is typical presence, let's get the latest/highest priority
        debug(TAG,"got presence: " + presence.getFrom() + "=" + presence.getType());

        if (contact != null)
        {

            if (contact.getPresence() != null) {
                Presence pOld = contact.getPresence();

                if (pOld == null || pOld.getResource() == null) {
                    contact.setPresence(p);
                } else if (pOld.getResource() != null && pOld.getResource().equals(p.getResource())) //if the same resource as the existing one, then update it
                {
                    contact.setPresence(p);
                } else if (p.getPriority() >= pOld.getPriority()) //if priority is higher, then override
                {
                    contact.setPresence(p);
                }
            }
            else
                contact.setPresence(p);


            ExtensionElement packetExtension=presence.getExtension("x","vcard-temp:x:update");
            if (packetExtension != null) {

                StandardExtensionElement o=(StandardExtensionElement)packetExtension;
                String hash=o.getAttributeValue("photo");
                if (hash != null) {
                    boolean hasMatches = DatabaseUtils.doesAvatarHashExist(mContext.getContentResolver(),  Imps.Avatars.CONTENT_URI, contact.getAddress().getBareAddress(), hash);

                    if (!hasMatches) //we must reload
                        qAvatar.put(contact.getAddress().getAddress(),hash);

                }
                else
                {
                    for (StandardExtensionElement element : o.getElements())
                    {
                        if (element.getElementName().equals("photo"))
                        {
                            hash = element.getText();
                            if (hash != null) {
                                boolean hasMatches = DatabaseUtils.doesAvatarHashExist(mContext.getContentResolver(), Imps.Avatars.CONTENT_URI, contact.getAddress().getBareAddress(), hash);

                                if (!hasMatches) //we must reload
                                    qAvatar.put(contact.getAddress().getAddress(), hash);
                            }

                            break;

                        }
                    }


                }
            }
            else
            {
                boolean hasAvatar = DatabaseUtils.hasAvatarContact(mContext.getContentResolver(),  Imps.Avatars.CONTENT_URI, contact.getAddress().getBareAddress());

                if (!hasAvatar)
                {
                    qAvatar.put(contact.getAddress().getAddress(),"");
                }
            }

        }


        return contact;
    }

    private void initPresenceProcessor ()
    {
        mTimerPresence = new Timer();

        mTimerPresence.schedule(new TimerTask() {

            public void run() {

                if (qPresence.size() > 0)
                {

                    try {
                        ContactList cList = getContactListManager().getDefaultContactList();

                        if (cList == null)
                            return; //not ready yet
                    }
                    catch (Exception e){

                        //not ready yet
                        return;
                    }


                    Map<String, Contact> alUpdate = new HashMap<String, Contact>();
                    
                    org.jivesoftware.smack.packet.Presence p = null;
                    Contact contact = null;

                    final int maxBatch = 20;

                    while (qPresence.peek() != null && alUpdate.size()<maxBatch)
                    {

                        p = qPresence.poll();
                        contact = handlePresenceChanged(p);
                        if (contact != null)
                        {
                            alUpdate.put(contact.getAddress().getBareAddress(),contact);
                        }

                    }

                    if (alUpdate.size() > 0) {
                        loadVCardsAsync();

                        //Log.d(TAG,"XMPP processed presence q=" + alUpdate.size());

                        Collection<Contact> contactsUpdate = alUpdate.values();

                        if (mContactListManager != null)
                            mContactListManager.notifyContactsPresenceUpdated(contactsUpdate.toArray(new Contact[contactsUpdate.size()]));
                    }

                }
                
             }

          }, 500, 500);
    }
    
    Timer mTimerPackets = null;
    
    private void initPacketProcessor ()
    {
        mTimerPackets = new Timer();

        mTimerPackets.scheduleAtFixedRate(new TimerTask() {

            public void run() {

                try
                {
                    org.jivesoftware.smack.packet.Stanza packet = null;
                    
                    if (qPacket.size() > 0)
                        while (qPacket.peek()!=null)
                        {
                            packet = qPacket.poll();
                                    
                            if (mConnection == null || (!mConnection.isConnected())) {
                                debug(TAG, "postponed packet to " + packet.getTo()
                                        + " because we are not connected");
                                postpone(packet);
                                qPacket.push(packet);//return the packet to the stack
                                return;
                            }

                            try {
                                mConnection.sendStanza(packet);
                            } catch (IllegalStateException ex) {
                                postpone(packet);
                               debug(TAG, "postponed packet to " + packet.getTo()
                                        + " because socket is disconnected");
                                qPacket.push(packet);//return the packet to the stack
                                return;
                            }
                        }

                }
                catch (Exception e)
                {
                    Log.e(TAG,"error sending packet",e);
                }


             }

          }, 500, 500);
    }

    Timer mTimerNewContacts = null;

    private void initNewContactProcessor ()
    {
        mTimerNewContacts = new Timer();

        mTimerNewContacts.scheduleAtFixedRate(new TimerTask() {

            public void run() {

                try
                {

                    Contact contact = null;

                    if (qNewContact.size() > 0)
                        while (qNewContact.peek()!=null)
                        {
                            contact = qNewContact.poll();

                            if (mConnection == null || (!mConnection.isConnected())) {
                                debug(TAG, "postponed adding new contact"
                                        + " because we are not connected");

                                qNewContact.push(contact);//return the packet to the stack
                                return;
                            }
                            else
                            {
                                try {

                                    RosterEntry rEntry;

                                    ContactList list = mContactListManager.getDefaultContactList();
                                    String[] groups = new String[] { list.getName() };

                                    BareJid jid = JidCreate.bareFrom(contact.getAddress().getBareAddress());

                                    rEntry = mRoster.getEntry(jid);
                                    RosterGroup rGroup = mRoster.getGroup(list.getName());

                                    if (rGroup == null)
                                    {
                                        if (rEntry == null) {

                                            mRoster.createEntry(jid, contact.getName(), groups);

                                            while ((rEntry = mRoster.getEntry(jid))==null)
                                            {
                                                try { Thread.sleep(500);}catch(Exception e){}
                                            }
                                        }

                                    }
                                    else if (rEntry == null)
                                    {
                                        mRoster.createEntry(jid, contact.getName(), groups);

                                        while ((rEntry = mRoster.getEntry(jid))==null)
                                        {
                                            try { Thread.sleep(500);}catch(Exception e){}
                                        }
                                    }

                                } catch (XMPPException e) {

                                    debug(TAG,"error updating remote roster",e);
                                    qNewContact.push(contact); //try again later

                                } catch (Exception e) {
                                    String msg = "Not logged in to server while updating remote roster";
                                    debug(TAG, msg, e);
                                    qNewContact.push(contact); //try again later

                                }
                            }

                        }

                }
                catch (Exception e)
                {
                    Log.e(TAG,"error sending packet",e);
                }


            }

        }, 500, 500);
    }

    @Override
    public List getFingerprints (String address)
    {
        try {
            if (address.equals(mUser.getAddress().getBareAddress()))
            {
                ArrayList<String> fps = new ArrayList<>();
                fps.add(getOmemo().getManager().getOurFingerprint().toString());
                return fps;
            }
            else {
                 return getOmemo().getFingerprints(JidCreate.bareFrom(address), false);
            }
        } catch (Exception xe) {
            return null;
        }
    }

    private class UploaderManager {

        boolean mIsDiscovered = false;
        HttpFileUploadManager manager;

        public UploaderManager ()
        {

            try {

                manager = HttpFileUploadManager.getInstanceFor(mConnection);
                mIsDiscovered = manager.discoverUploadService();

            }
            catch (Exception e)
            {
                Log.e(TAG,"error discovering upload service",e);
            }
        }

        public String doUpload (String fileName, String mimeType, long fileSize, InputStream is)
        {
            if (!mIsDiscovered)
                return null;

        //    manager.useTlsSettingsFrom(mConnection.getConfiguration());
            UploadService upService = manager.getDefaultUploadService();

            if (upService != null) {
                if (upService.hasMaxFileSizeLimit()) {
                    if (!upService.acceptsFileOfSize(fileSize))
                        return null;
                }

                try {

                 //   String defaultType = "application/octet-stream";
                    Slot upSlot = manager.requestSlot(fileName, fileSize, mimeType);

                    String uploadKey = uploadFile(fileSize, is, upSlot, new UploadProgressListener() {
                        @Override
                        public void onUploadProgress(long l, long l1) {

                            debug(TAG,"upload complete: " + l + "," + l1);
                            //once this is done, send the message
                        }
                    });

                    if (uploadKey != null) {
                        URL resultUrl = upSlot.getGetUrl();
                        String shareUrl = resultUrl.toExternalForm() + "#" + uploadKey;
                        shareUrl = shareUrl.replace("https","aesgcm"); //this indicates it is encrypted
                        return shareUrl;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error getting upload slot", e);

                }
            }

            return null;
        }

        private String uploadFile(long fileSize, InputStream fis, Slot slot, UploadProgressListener listener) throws IOException {

            String result = null;

            if(fileSize >= 2147483647L) {
                throw new IllegalArgumentException("File size " + fileSize + " must be less than " + 2147483647);
            } else {
               // int fileSizeInt = (int)fileSize;
                URL putUrl = slot.getPutUrl();
                HttpURLConnection urlConnection = (HttpURLConnection)putUrl.openConnection();
                urlConnection.setRequestMethod("PUT");
                urlConnection.setUseCaches(false);
                urlConnection.setDoOutput(true);
               // urlConnection.setFixedLengthStreamingMode(fileSizeInt);
                urlConnection.setRequestProperty("Content-Type", "application/octet-stream;");
                Iterator tlsSocketFactory = slot.getHeaders().entrySet().iterator();

                while(tlsSocketFactory.hasNext()) {
                    Map.Entry outputStream = (Map.Entry)tlsSocketFactory.next();
                    urlConnection.setRequestProperty((String)outputStream.getKey(), (String)outputStream.getValue());
                }

                if(urlConnection instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsUrlConn = (HttpsURLConnection)urlConnection;
                    httpsUrlConn.setHostnameVerifier(mMemTrust.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));
                    httpsUrlConn.setSSLSocketFactory(sslContext.getSocketFactory());
                }

                try {
                    OutputStream outputStream2 = urlConnection.getOutputStream();

                    long bytesSend = 0L;
                    if(listener != null) {
                        listener.onUploadProgress(0L, fileSize);
                    }

                    byte[] keyAndIv = secureRandom.generateSeed(48);
                    result = Downloader.bytesToHex(keyAndIv);
                    InputStream cis = Downloader.setupInputStream(fis,keyAndIv);
                    BufferedInputStream inputStream = new BufferedInputStream(cis);
                    byte[] buffer = new byte[4096];

                    int bytesRead;
                    try {
                        while((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream2.write(buffer, 0, bytesRead);
                            bytesSend += (long)bytesRead;
                            if(listener != null) {
                                listener.onUploadProgress(bytesSend, fileSize);
                            }
                        }
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException var34) {
                         //   LOGGER.log(Level.WARNING, "Exception while closing input stream", var34);
                        }

                        try {
                            outputStream2.close();
                        } catch (IOException var33) {
                         //   LOGGER.log(Level.WARNING, "Exception while closing output stream", var33);
                        }

                    }

                    int status = urlConnection.getResponseCode();
                    switch(status) {
                        case 200:
                        case 201:
                        case 204:
                            return result;
                        case 202:
                        case 203:
                        default:
                            throw new IOException("Error response " + status + " from server during file upload: " + urlConnection.getResponseMessage() + ", file size: " + fileSize + ", put URL: " + putUrl);
                    }
                } finally {
                    urlConnection.disconnect();
                }

            }
        }
    }
}
