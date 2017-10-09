package org.awesomeapp.messenger.ui;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.codec.DecoderException;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.MainActivity;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.plugin.xmpp.XmppAddress;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatListener;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IContactListManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
import org.awesomeapp.messenger.ui.legacy.adapter.ChatListenerAdapter;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;
import org.awesomeapp.messenger.ui.qr.QrShareAsyncTask;
import org.awesomeapp.messenger.ui.widgets.GroupAvatar;
import org.awesomeapp.messenger.ui.widgets.LetterAvatar;

import java.util.ArrayList;

import im.zom.messenger.R;

public class GroupDisplayActivity extends BaseActivity {

    private String mName = null;
    private String mAddress = null;
    private long mProviderId = -1;
    private long mAccountId = -1;
    private long mLastChatId = -1;
    private String mLocalAddress = null;

    private IImConnection mConn;
    private IChatSession mSession;
    private Contact mGroupOwner;
    private boolean mIsOwner = false;
    private Thread mThreadUpdate;

    private class GroupMemberDisplay {
        public String username;
        public String nickname;
        public String role;
        public String affiliation;
        public Drawable avatar;
        public boolean online = false;
    }

    private RecyclerView mRecyclerView;
    private ArrayList<GroupMemberDisplay> mMembers;

    private final static int REQUEST_PICK_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.awesome_activity_group);

        mName = getIntent().getStringExtra("nickname");
        mAddress = getIntent().getStringExtra("address");
        mProviderId = getIntent().getLongExtra("provider", -1);
        mAccountId = getIntent().getLongExtra("account", -1);
        mLastChatId = getIntent().getLongExtra("chat", -1);

        Cursor cursor = getContentResolver().query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return; //not going to work

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, getContentResolver(), mProviderId, false, null);

        mMembers = new ArrayList<>();
        mConn = ((ImApp) getApplication()).getConnection(mProviderId, mAccountId);
        mLocalAddress = Imps.Account.getUserName(getContentResolver(), mAccountId) + '@' + providerSettings.getDomain();

        providerSettings.close();

        try {
            mSession = mConn.getChatSessionManager().getChatSession(mAddress);
            if (mSession != null) {
                mGroupOwner = mSession.getGroupChatOwner();
                if (mGroupOwner != null)
                    mIsOwner = mGroupOwner.getAddress().getBareAddress().equals(mLocalAddress);
            }
        }
        catch (RemoteException e){}

        mRecyclerView = (RecyclerView) findViewById(R.id.rvRoot);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {

            private static final int VIEW_TYPE_MEMBER = 0;
            private static final int VIEW_TYPE_HEADER = 1;
            private static final int VIEW_TYPE_FOOTER = 2;

            private int colorTextPrimary = 0xff000000;

            public RecyclerView.Adapter init() {
                TypedValue out = new TypedValue();
                getTheme().resolveAttribute(R.attr.contactTextPrimary, out, true);
                colorTextPrimary = out.data;
                return this;
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                switch (viewType) {
                    case VIEW_TYPE_HEADER:
                        return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.awesome_activity_group_header, parent, false));
                    case VIEW_TYPE_FOOTER:
                        return new FooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.awesome_activity_group_footer, parent, false));
                }
                return new MemberViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.group_member_view, parent, false));
            }

            @Override
            public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
                if (holder instanceof HeaderViewHolder) {
                    HeaderViewHolder h = (HeaderViewHolder)holder;
                    GroupAvatar avatar = new GroupAvatar(mAddress.split("@")[0]);
                    avatar.setRounded(false);
                    h.avatar.setImageDrawable(avatar);

                    h.qr.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String inviteString;
                            try {
                                inviteString = OnboardingManager.generateInviteLink(GroupDisplayActivity.this, mAddress, "", mName);
                                OnboardingManager.inviteScan(GroupDisplayActivity.this, inviteString);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    h.groupName.setText(mName);
                    h.groupAddress.setText(mAddress);

                    h.actionShare.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                String inviteLink = OnboardingManager.generateInviteLink(GroupDisplayActivity.this, mAddress, "", mName);
                                new QrShareAsyncTask(GroupDisplayActivity.this).execute(inviteLink, mName);
                            } catch (Exception e) {
                                Log.e(ImApp.LOG_TAG, "couldn't generate QR code", e);
                            }
                        }
                    });

                    if (!mIsOwner)
                        h.actionAddFriends.setVisibility(View.GONE);
                    else {
                        h.actionAddFriends.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(GroupDisplayActivity.this, ContactsPickerActivity.class);
                                ArrayList<String> usernames = new ArrayList<>(mMembers.size());
                                for (GroupMemberDisplay member : mMembers) {
                                    usernames.add(member.username);
                                }
                                intent.putExtra(ContactsPickerActivity.EXTRA_EXCLUDED_CONTACTS, usernames);
                                startActivityForResult(intent, REQUEST_PICK_CONTACTS);
                            }
                        });
                    }
                    h.actionMute.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            setMuted(!isMuted());
                            mRecyclerView.getAdapter().notifyItemChanged(holder.getAdapterPosition());
                        }
                    });
                    boolean muted = isMuted();
                   h.actionMute.setText(muted ? R.string.turn_notifications_on : R.string.turn_notifications_off);
             //       h.actionMute.setText(muted ? "turn on" : " t")
                    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(h.actionMute,
                            muted ? R.drawable.ic_notifications_active_black_24dp : R.drawable.ic_notifications_off_black_24dp,
                            0, 0, 0);

                    if (!mIsOwner)
                        h.editGroupName.setVisibility(View.GONE);
                    else {
                        h.editGroupName.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                editGroupSubject();
                            }
                        });
                    }
                } else if (holder instanceof FooterViewHolder) {
                    FooterViewHolder h = (FooterViewHolder)holder;

                    // Tint the "leave" text and drawable(s)
                    int colorAccent = ResourcesCompat.getColor(getResources(), R.color.holo_orange_light, getTheme());
                    for (Drawable d : h.actionLeave.getCompoundDrawables()) {
                        if (d != null) {
                            DrawableCompat.setTint(d, colorAccent);
                        }
                    }
                    h.actionLeave.setTextColor(colorAccent);
                    h.actionLeave.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmLeaveGroup();
                        }
                    });
                } else if (holder instanceof MemberViewHolder) {
                    MemberViewHolder h = (MemberViewHolder) holder;

                    // Reset the padding to match other views in this hierarchy
                    //
                    int padding = getResources().getDimensionPixelOffset(R.dimen.detail_view_padding);
                    h.itemView.setPadding(padding, h.itemView.getPaddingTop(), padding, h.itemView.getPaddingBottom());

                    int idxMember = position - 1;
                    final GroupMemberDisplay member = mMembers.get(idxMember);

                    String nickname = member.nickname;
                    if (TextUtils.isEmpty(nickname)) {
                        nickname = member.username.split("@")[0].split("\\.")[0];
                    } else {
                        nickname = nickname.split("@")[0].split("\\.")[0];
                    }

                    h.line1.setText(nickname);

                    boolean hasRoleNone = TextUtils.isEmpty(member.role) || "none".equalsIgnoreCase(member.role);
                    h.line1.setTextColor(hasRoleNone ? Color.GRAY : colorTextPrimary);

                    h.line2.setText(member.username);
                    if (member.affiliation.contentEquals("owner") || member.affiliation.contentEquals("admin")) {
                        h.avatarCrown.setVisibility(View.VISIBLE);
                    } else {
                        h.avatarCrown.setVisibility(View.GONE);
                    }

                    /**
                    if (!member.online)
                    {
                        h.line1.setEnabled(false);
                        h.line2.setEnabled(false);
                        h.avatar.setBackgroundColor(getResources().getColor(R.color.holo_grey_light));
                     }**/

                    //h.line2.setText(member.username);
                    if (member.avatar == null) {
                        padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
                        member.avatar = new LetterAvatar(holder.itemView.getContext(), nickname, padding);
                    }
                    h.avatar.setImageDrawable(member.avatar);
                    h.avatar.setVisibility(View.VISIBLE);
                    h.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showMemberInfo(member);
                        }
                    });
                }
            }

            @Override
            public int getItemCount() {
                return 2 + mMembers.size();
            }

            @Override
            public int getItemViewType(int position) {
                if (position == 0)
                    return VIEW_TYPE_HEADER;
                else if (position == getItemCount() - 1)
                    return VIEW_TYPE_FOOTER;
                return VIEW_TYPE_MEMBER;
            }
        }.init());
        updateMembers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSession != null) {
            try {
                mSession.registerChatListener(mChatListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        updateMembers();
    }

    @Override
    protected void onPause() {
        if (mSession != null) {
            try {
                mSession.unregisterChatListener(mChatListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.onPause();
    }

    private void updateMembers() {
        if (mThreadUpdate != null) {
            mThreadUpdate.interrupt();
            mThreadUpdate = null;
        }
        mThreadUpdate = new Thread(new Runnable() {
            @Override
            public void run() {

                final ArrayList<GroupMemberDisplay> members = new ArrayList<>();

                IContactListManager contactManager = null;

                try {
                    if (mConn != null) {
                        contactManager = mConn.getContactListManager();
                    }
                }
                catch (RemoteException re){}

                String[] projection = {Imps.GroupMembers.USERNAME, Imps.GroupMembers.NICKNAME, Imps.GroupMembers.ROLE, Imps.GroupMembers.AFFILIATION};
                Uri memberUri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, mLastChatId);
                ContentResolver cr = getContentResolver();
                Cursor c = cr.query(memberUri, projection, null, null, null);
                if (c != null) {
                    int colUsername = c.getColumnIndex(Imps.GroupMembers.USERNAME);
                    int colNickname = c.getColumnIndex(Imps.GroupMembers.NICKNAME);
                    int colRole = c.getColumnIndex(Imps.GroupMembers.ROLE);
                    int colAffiliation = c.getColumnIndex(Imps.GroupMembers.AFFILIATION);

                    android.database.DatabaseUtils.dumpCursor(c);

                    while (c.moveToNext()) {
                        GroupMemberDisplay member = new GroupMemberDisplay();
                        member.username = new XmppAddress(c.getString(colUsername)).getBareAddress();
                        member.nickname = c.getString(colNickname);
                        member.role = c.getString(colRole);
                        member.affiliation = c.getString(colAffiliation);
                        try {
                            member.avatar = DatabaseUtils.getAvatarFromAddress(cr, member.username, ImApp.SMALL_AVATAR_WIDTH, ImApp.SMALL_AVATAR_HEIGHT);
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        }

                        /**
                        try {
                            if (contactManager != null) {
                                Contact contact = contactManager.getContactByAddress(member.username);
                                if (contact != null)
                                    member.online = contact.getPresence().isOnline();
                            }
                        }
                        catch (RemoteException re){}**/

                        members.add(member);
                    }
                    c.close();
                }
                if (!Thread.currentThread().isInterrupted()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMembers = members;
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        mThreadUpdate.start();
    }

    public void inviteContacts (ArrayList<String> invitees)
    {
        if (mConn == null)
            return;

        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mAddress);

            for (String invitee : invitees) {
                session.inviteContact(invitee);
                GroupMemberDisplay member = new GroupMemberDisplay();
                XmppAddress address = new XmppAddress(invitee);
                member.username = address.getBareAddress();
                member.nickname = address.getUser();
                try {
                    member.avatar = DatabaseUtils.getAvatarFromAddress(getContentResolver(), member.username, ImApp.SMALL_AVATAR_WIDTH, ImApp.SMALL_AVATAR_HEIGHT);
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
                mMembers.add(member);
            }

            mRecyclerView.getAdapter().notifyDataSetChanged();

        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error inviting contacts to group",e);
        }

    }

    public void showMemberInfo(GroupMemberDisplay member) {
        Intent intent = new Intent(this, ContactDisplayActivity.class);
        intent.putExtra("address", member.username);
        intent.putExtra("nickname", member.nickname);
        intent.putExtra("provider", mProviderId);
        intent.putExtra("account", mAccountId);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_PICK_CONTACTS) {

                ArrayList<String> invitees = new ArrayList<String>();

                String username = resultIntent.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (username != null)
                    invitees.add(username);
                else
                    invitees = resultIntent.getStringArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAMES);

                inviteContacts(invitees);

            }
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final ImageView qr;
        final TextView groupName;
        final View editGroupName;
        final TextView groupAddress;
        final TextView actionShare;
        final TextView actionAddFriends;
        final TextView actionMute;

        HeaderViewHolder(View view) {
            super(view);
            avatar = (ImageView) view.findViewById(R.id.ivAvatar);
            qr = (ImageView) view.findViewById(R.id.qrcode);
            groupName = (TextView) view.findViewById(R.id.tvGroupName);
            editGroupName = view.findViewById(R.id.edit_group_subject);
            groupAddress = (TextView) view.findViewById(R.id.tvGroupAddress);
            actionShare = (TextView) view.findViewById(R.id.tvActionShare);
            actionAddFriends = (TextView) view.findViewById(R.id.tvActionAddFriends);
            actionMute = (TextView) view.findViewById(R.id.tvActionMute);
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        final TextView actionLeave;

        FooterViewHolder(View view) {
            super(view);
            actionLeave = (TextView) view.findViewById(R.id.tvActionLeave);
        }
    }

    private class MemberViewHolder extends RecyclerView.ViewHolder {
        final TextView line1;
        final TextView line2;
        final ImageView avatar;
        final ImageView avatarCrown;

        MemberViewHolder(View view) {
            super(view);
            line1 = (TextView) view.findViewById(R.id.line1);
            line2 = (TextView) view.findViewById(R.id.line2);
            avatar = (ImageView) view.findViewById(R.id.avatar);
            avatarCrown = (ImageView) view.findViewById(R.id.avatarCrown);
        }
    }

    private void editGroupSubject() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setText(mName);
        alert.setView(input);
        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String newSubject = input.getText().toString();
                changeGroupSubject(newSubject);

                // Update the UI
                mName = newSubject;
                mRecyclerView.getAdapter().notifyItemChanged(0);
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    private void changeGroupSubject (String subject)
    {
        try {
            IChatSession session = mConn.getChatSessionManager().getChatSession(mAddress);
            session.setGroupChatSubject(subject);
        }
        catch (Exception e) {}
    }

    boolean isMuted() {
        int type = Imps.ChatsColumns.CHAT_TYPE_ACTIVE;

        String[] projection = {Imps.Chats.CHAT_TYPE};
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mLastChatId);
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(chatUri, projection, null, null, null);
        if (c != null) {
            int colType = c.getColumnIndex(Imps.Chats.CHAT_TYPE);
            if (colType != -1 && c.moveToFirst()) {
                type = c.getInt(colType);
            }
            c.close();
        }
        return type == Imps.ChatsColumns.CHAT_TYPE_MUTED;
    }

    public void setMuted(boolean muted) {
        int type = muted ? Imps.ChatsColumns.CHAT_TYPE_MUTED : Imps.ChatsColumns.CHAT_TYPE_ACTIVE;

        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mLastChatId);
        ContentValues values = new ContentValues();
        values.put(Imps.Chats.CHAT_TYPE,type);
        getContentResolver().update(chatUri,values,Imps.Chats.CONTACT_ID + "=" + mLastChatId,null);
    }

    private void confirmLeaveGroup ()
    {
        new android.support.v7.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.action_leave))
                .setMessage(getString(R.string.confirm_leave_group))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        leaveGroup();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void leaveGroup ()
    {
        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mAddress);

            if (session == null)
                session = manager.createChatSession(mAddress,true);

            if (session != null) {
                session.leave();

                //clear the stack and go back to the main activity
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error leaving group",e);
        }
    }

    private final IChatListener mChatListener = new ChatListenerAdapter() {
        @Override
        public void onContactJoined(IChatSession ses, Contact contact) {
            super.onContactJoined(ses, contact);
            updateMembers();
        }

        @Override
        public void onContactLeft(IChatSession ses, Contact contact) {
            super.onContactLeft(ses, contact);
            updateMembers();
        }

        @Override
        public void onContactChanged(IChatSession ses, Contact contact) {
            super.onContactChanged(ses, contact);
            updateMembers();
        }
    };
}
