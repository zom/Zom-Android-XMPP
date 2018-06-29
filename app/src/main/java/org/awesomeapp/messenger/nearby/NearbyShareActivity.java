package org.awesomeapp.messenger.nearby;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import org.apache.commons.io.IOUtils;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.tasks.AddContactAsyncTask;
import org.awesomeapp.messenger.ui.ContactListItem;
import org.awesomeapp.messenger.ui.ContactViewHolder;
import org.awesomeapp.messenger.util.GlideUtils;
import org.awesomeapp.messenger.util.SecureMediaStore;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import eu.siacs.conversations.Downloader;
import im.zom.messenger.R;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;

public class NearbyShareActivity extends ConnectionsActivity {

    /**
     * This service id lets us find other nearby devices that are interested in the same thing. Our
     * sample does exactly one thing, so we hardcode the ID.
     */
    private static final String SERVICE_ID =
            "org.awesomeapp.messenger.nearby.share.SERVICE_ID";

    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    private String mName = null;
    private File mFile = null;

    private RecyclerView mList;
    private ImageView mView;

    private HashMap<String,Contact> contactList = new HashMap<>();

    private State mState = State.UNKNOWN;

    private ImApp mApp;

    private HashMap<String,Endpoint> mUserEndpoint = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (ImApp)getApplication();

        setContentView(R.layout.activity_nearby);

        mList = findViewById(R.id.nearbyList);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setItemAnimator(new DefaultItemAnimator());

        mView = (ImageView) findViewById(R.id.nearbyIcon);

        NearbyShareListRecyclerViewAdapter adapter = new NearbyShareListRecyclerViewAdapter(this,new ArrayList<Contact>(contactList.values()));
        mList.setAdapter(adapter);

        mName = getIntent().getStringExtra("name");
        if (TextUtils.isEmpty(mName))
        {
            mName = mApp.getDefaultUsername();
        }

        if (getIntent().getData() != null)
            mFile = new File(getIntent().getData().getPath());

        setState(State.SEARCHING);

    }

    private void ignoreContact (Contact contact)
    {
        contactList.remove(contact);
        refreshList();
    }

    private void confirmContact (Contact contact)
    {
        Endpoint endpoint = mUserEndpoint.get(contact.getAddress().getBareAddress());
        if (endpoint != null) {
            connectToEndpoint(endpoint);
            contact.setSubscriptionStatus(Imps.ContactsColumns.SUBSCRIPTION_TYPE_BOTH);
        }
        else
        {
            contactList.remove(contact);
        }

        refreshList();
    }

    private void removeContact (String username) {
        contactList.remove(username);
        refreshList();
    }

    private void addContact (String username)
    {
        if (!contactList.containsKey(username)) {
            Contact contact = new Contact(new XmppAddress(username));
            contact.setName(username);
            contact.setPresence(new Presence());
            contact.setSubscriptionType(Imps.ContactsColumns.SUBSCRIPTION_TYPE_FROM);
            contact.setSubscriptionStatus(Imps.ContactsColumns.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING);
            contactList.put(contact.getAddress().getAddress(), contact);
            refreshList();
        }
    }

    private void refreshList ()
    {
        NearbyShareListRecyclerViewAdapter adapter = new NearbyShareListRecyclerViewAdapter(this,new ArrayList<Contact>(contactList.values()));
        mList.setAdapter(adapter);
    }

    private void sendFile (String endpointId) throws FileNotFoundException {
        if (mFile != null)
        {
            sendFile(mFile, endpointId);
        }
    }

    private void sendFile (File file, String endpointId) throws FileNotFoundException {

        Payload payload = Payload.fromStream(new FileInputStream(file));
        send(payload,endpointId);


    }

    /** {@see ConnectionsActivity#onReceive(Endpoint, Payload)} */
    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.STREAM) {

            String name = endpoint.getName();;

            Toast.makeText(this,"Got Payload!",Toast.LENGTH_SHORT).show();

            ImApp app = (ImApp)getApplication();
            IChatSession session = app.getChatSession(app.getDefaultProviderId(),app.getDefaultAccountId(),name);

            try {

                if (session != null) {
                    long mContactId = session.getId();

                    Downloader dl = new Downloader();

                    String msgId = "nb" + new Date().getTime();
                    String mimeType = "image/jpeg";

                    File fileDownload = dl.openSecureStorageFile(mContactId + "", msgId + ".jpg");
                    OutputStream storageStream = new info.guardianproject.iocipher.FileOutputStream(fileDownload);
                    IOUtils.copy(payload.asStream().asInputStream(),storageStream);
                    int type = Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED;
                    String result = SecureMediaStore.vfsUri(fileDownload.getAbsolutePath()).toString();

                    Uri messageUri = Imps.insertMessageInDb(getContentResolver(),
                            false, mContactId,
                            true, name,
                            result, System.currentTimeMillis(), type,
                            0, msgId, mimeType);

                    session.setLastMessage(result);

                    Uri mediaUri = Uri.parse("vfs://" + fileDownload.getAbsolutePath());
                    GlideUtils.loadImageFromUri(this, Uri.parse(result), mView);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onBackPressed() {
        if (getState() == State.CONNECTED) {
            setState(State.SEARCHING);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        stopDiscovering();

        mUserEndpoint.put(endpoint.getName(),endpoint);

        addContact(endpoint.getName());


    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // A connection to another device has been initiated! We'll use the auth token, which is the
        // same on both devices, to pick a color to use when we're connected. This way, users can
        // visually see which device they connected with.
        //  mConnectedColor = COLORS[connectionInfo.getAuthenticationToken().hashCode() % COLORS.length];
        acceptConnection(endpoint);
        addContact(endpoint.getName());
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {

        setState(State.CONNECTED);

        try {
            sendFile(endpoint.getId());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {

        setState(State.SEARCHING);

        removeContact(endpoint.getName());

    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (getState() == State.SEARCHING) {
            startDiscovering();
        }

        removeContact(endpoint.getName());
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state) {
        if (mState == state) {
            logW("State set to " + state + " but already in that state");
            return;
        }

        logD("State set to " + state);
        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state);
    }

    /** @return The current state. */
    private State getState() {
        return mState;
    }

    /**
     * State has changed.
     *
     * @param oldState The previous state we were in. Clean up anything related to this state.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private void onStateChanged(State oldState, State newState) {

        // Update Nearby Connections to the new state.
        switch (newState) {
            case SEARCHING:
                disconnectFromAllEndpoints();
                startDiscovering();
                startAdvertising();
                break;
            case CONNECTED:
                stopDiscovering();
                stopAdvertising();
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;
            default:
                // no-op
                break;
        }

        // Update the UI.
        switch (oldState) {
            case UNKNOWN:
                // Unknown is our initial state. Whatever state we move to,
                // we're transitioning forwards.
            //    transitionForward(oldState, newState);
                break;
            case SEARCHING:
                switch (newState) {
                    case UNKNOWN:
                      //  transitionBackward(oldState, newState);
                        break;
                    case CONNECTED:
                      //  transitionForward(oldState, newState);
                        break;
                    default:
                        // no-op
                        break;
                }
                break;
            case CONNECTED:
                // Connected is our final state. Whatever new state we move to,
                // we're transitioning backwards.
              //  transitionBackward(oldState, newState);
                break;
        }
    }

    @Override
    protected String getName() {
        return mName;
    }

    @Override
    protected String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    protected Strategy getStrategy() {
        return STRATEGY;
    }

    /** States that the UI goes through. */
    public enum State {
        UNKNOWN,
        SEARCHING,
        CONNECTED
    }


    public class NearbyShareListRecyclerViewAdapter
            extends RecyclerView.Adapter<ContactViewHolder> {

        private final TypedValue mTypedValue = new TypedValue();
        private int mBackground;
        private Context mContext;
        private  ArrayList<Contact> mItemList;

        public NearbyShareListRecyclerViewAdapter(Context context,  ArrayList<Contact> itemList) {
            super();

            mItemList = itemList;
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
            mBackground = mTypedValue.resourceId;
            mContext = context;

        }


        @Override
        public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


            ContactListItem view = (ContactListItem) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.contact_view, parent, false);
            view.setShowPresence(false);
            view.setBackgroundResource(mBackground);

            ContactViewHolder holder = view.getViewHolder();
            if (holder == null) {
                holder = new ContactViewHolder(view);
                view.applyStyleColors(holder);

                view.setViewHolder(holder);
            }

            return holder;

        }

        @Override
        public void onBindViewHolder(@NonNull ContactViewHolder viewHolder, final int position) {

            final Contact contact = mItemList.get(position);

            viewHolder.mContactId =  -1;
            viewHolder.mAddress =  contact.getAddress().getBareAddress();
            viewHolder.mType = Imps.ContactsColumns.TYPE_NORMAL;

            String nickname = contact.getName();

            viewHolder.mNickname = nickname;

            if (viewHolder.itemView instanceof ContactListItem) {
                ((ContactListItem)viewHolder.itemView).bind(viewHolder, contact, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        confirmContact(contact);

                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ignoreContact(contact);
                    }
                });
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {



                }
            });

        }

        @Override
        public int getItemCount() {
            return mItemList.size();
        }


    }
}
