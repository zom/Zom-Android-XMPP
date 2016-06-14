package org.awesomeapp.messenger.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by n8fr8 on 3/29/16.
 */
public class ContactViewHolder extends RecyclerView.ViewHolder
{
    public ContactViewHolder(ContactListItem view) {
        super(view);
        mView = view;
    }

    public ContactListItem mView;
    public String mAddress;
    public String mNickname;
    public long mProviderId;
    public long mAccountId;

    public View mContainer;
    public TextView mLine1;
    public TextView mLine2;
    //public TextView mStatusText;
    public ImageView mAvatar;
    //public ImageView mStatusIcon;
    public ImageView mMediaThumb;

    public View mSubBox;
    public Button mButtonSubApprove;
    public Button mButtonSubDecline;
}
