package org.awesomeapp.messenger.ui;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.codec.DecoderException;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.ChatGroup;
import org.awesomeapp.messenger.model.ChatSession;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.service.IImConnection;
import org.awesomeapp.messenger.ui.legacy.DatabaseUtils;
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

    private class GroupMember {
        public String username;
        public String nickname;
        public Drawable avatar;
    }

    private RecyclerView mRecyclerView;
    private ArrayList<GroupMember> mMembers;

    private final static int REQUEST_PICK_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.awesome_activity_group);

        mMembers = new ArrayList<>();
        mName = getIntent().getStringExtra("nickname");
        mAddress = getIntent().getStringExtra("address");
        mProviderId = getIntent().getLongExtra("provider", -1);
        mAccountId = getIntent().getLongExtra("account", -1);
        mLastChatId  = getIntent().getLongExtra("chat", -1);

        mConn = ((ImApp)getApplication()).getConnection(mProviderId,mAccountId);

        mLocalAddress = Imps.Account.getUserName(getContentResolver(), mAccountId);

        mRecyclerView = (RecyclerView) findViewById(R.id.rvRoot);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {

            private static final int VIEW_TYPE_MEMBER = 0;
            private static final int VIEW_TYPE_HEADER = 1;
            private static final int VIEW_TYPE_FOOTER = 2;

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                switch (viewType) {
                    case VIEW_TYPE_HEADER:
                        return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.awesome_activity_group_header, parent, false));
                    case VIEW_TYPE_FOOTER:
                        return new FooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.awesome_activity_group_footer, parent, false));
                }
                return new MemberViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_view, parent, false));
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
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
                    h.actionAddFriends.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(GroupDisplayActivity.this, ContactsPickerActivity.class);
                            startActivityForResult(intent, REQUEST_PICK_CONTACTS);
                        }
                    });
                    h.actionMute.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });
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

                        }
                    });
                } else if (holder instanceof MemberViewHolder) {
                    MemberViewHolder h = (MemberViewHolder) holder;

                    // Reset the padding to match other views in this hierarchy
                    //
                    int padding = getResources().getDimensionPixelOffset(R.dimen.detail_view_padding);
                    h.itemView.setPadding(padding, h.itemView.getPaddingTop(), padding, h.itemView.getPaddingBottom());

                    int idxMember = position - 1;
                    GroupMember member = mMembers.get(idxMember);
                    String nickname = member.nickname;
                    if (TextUtils.isEmpty(nickname)) {
                        nickname = member.username.split("@")[0].split("\\.")[0];
                    } else {
                        nickname = nickname.split("@")[0].split("\\.")[0];
                    }

                    if (member.username.equals(mLocalAddress))
                    {
                        nickname += " (you)";
                    }

                    h.line1.setText(nickname);
                    h.line2.setText(member.username);

                    //h.line2.setText(member.username);
                    if (member.avatar == null) {
                        padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
                        member.avatar = new LetterAvatar(holder.itemView.getContext(), nickname, padding);
                    }
                    h.avatar.setImageDrawable(member.avatar);
                    h.avatar.setVisibility(View.VISIBLE);
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
        });
        updateMembers();
    }

    private void updateMembers() {

        mMembers.clear();

        Thread threadUpdate = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] projection = {Imps.GroupMembers.USERNAME, Imps.GroupMembers.NICKNAME};
                Uri memberUri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, mLastChatId);
                ContentResolver cr = getContentResolver();
                Cursor c = cr.query(memberUri, projection, null, null, null);
                if (c != null) {
                    int colUsername = c.getColumnIndex(Imps.GroupMembers.USERNAME);
                    int colNickname = c.getColumnIndex(Imps.GroupMembers.NICKNAME);
                    while (c.moveToNext()) {
                        GroupMember member = new GroupMember();
                        member.username = c.getString(colUsername);
                        member.nickname = c.getString(colNickname);
                        try {
                            member.avatar = DatabaseUtils.getAvatarFromAddress(cr, member.username, ImApp.SMALL_AVATAR_WIDTH, ImApp.SMALL_AVATAR_HEIGHT);
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        }
                        mMembers.add(member);
                    }
                    c.close();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
            }
        });
        threadUpdate.start();
    }

    public void inviteContacts (ArrayList<String> invitees)
    {
        if (mConn == null)
            return;

        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mAddress);

            for (String invitee : invitees)
                session.inviteContact(invitee);

            updateMembers();
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error inviting contacts to group",e);
        }

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
        final TextView groupAddress;
        final TextView actionShare;
        final TextView actionAddFriends;
        final TextView actionMute;

        HeaderViewHolder(View view) {
            super(view);
            avatar = (ImageView) view.findViewById(R.id.ivAvatar);
            qr = (ImageView) view.findViewById(R.id.qrcode);
            groupName = (TextView) view.findViewById(R.id.tvGroupName);
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

        MemberViewHolder(View view) {
            super(view);
            line1 = (TextView) view.findViewById(R.id.line1);
            line2 = (TextView) view.findViewById(R.id.line2);
            avatar = (ImageView) view.findViewById(R.id.avatar);
        }
    }

    private void changeGroupName (String name)
    {
        try {
            IChatSession session = mConn.getChatSessionManager().getChatSession(mAddress);
            Contact contact = mConn.getContactListManager().getContactByAddress(mAddress);

        }
        catch (Exception e) {}

    }
}
