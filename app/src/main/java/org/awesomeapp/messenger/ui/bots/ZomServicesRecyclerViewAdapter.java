package org.awesomeapp.messenger.ui.bots;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import im.zom.messenger.R;

/**
 * Created by N-Pex on 2018-02-12.
 */

public class ZomServicesRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Object> mValues;

    public interface ServiceItemCallback {
        void onBotClicked(String jid, String nickname);
    }

    public ServiceItemCallback listener;

    public ZomServicesRecyclerViewAdapter(Context context, ServiceItemCallback listener) {
        super();
        this.listener = listener;
        mValues = new ArrayList<>();

        TypedArray ta = context.getResources().obtainTypedArray(R.array.bots);
        for (int i = 0; i < ta.length(); i++) {
            int resIdBot = ta.getResourceId(i, -1);
            if (resIdBot >= 0) {
                TypedArray botArray = context.getResources().obtainTypedArray(resIdBot);
                int resIdName = botArray.getResourceId(0, 0);
                String nickname = botArray.getString(0);
                String jid = botArray.getString(1);
                int resIdDescription = botArray.getResourceId(2, 0);
                int resIdImage = botArray.getResourceId(3, 0);

                BotEntry botEntry = new BotEntry(resIdName, nickname, jid, resIdDescription, resIdImage);
                mValues.add(botEntry);
                botArray.recycle();
            }
        }
        ta.recycle();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bot_item, parent, false);
        return new ViewHolderBot(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        Context context = holder.itemView.getContext();

        if (mValues.get(position) instanceof BotEntry) {
            final BotEntry e = (BotEntry)mValues.get(position);

            ViewHolderBot viewHolder = (ViewHolderBot) holder;
            viewHolder.image.setImageResource(e.resIdImage);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            int rounding = view.getContext().getResources().getDimensionPixelSize(R.dimen.bot_image_rounding);
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), rounding);
                        }
                    }
                };
                viewHolder.image.setOutlineProvider(viewOutlineProvider);
                viewHolder.image.setClipToOutline(true);
            }
            viewHolder.name.setText(e.resIdName);
            viewHolder.description.setText(e.resIdDescription);
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBotClicked(e.jid,e.nickname);
                }
            });
        }
    }

    private void onBotClicked(String jid, String nickname) {
        if (listener != null) {
            listener.onBotClicked(jid,nickname);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }


    public class ViewHolderBot extends RecyclerView.ViewHolder {
        public final ImageView image;
        public final TextView name;
        public final TextView description;

        public ViewHolderBot(View view) {
            super(view);
            image = (ImageView) view.findViewById(R.id.image);
            name = (TextView) view.findViewById(R.id.name);
            description = (TextView) view.findViewById(R.id.description);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + name.getText() + "'";
        }
    }

    protected class BotEntry {
        public int resIdName;
        public String nickname;
        public String jid;
        public int resIdDescription;
        public int resIdImage;
        public BotEntry(int resIdName, String nickname, String jid, int resIdDescription, int resIdImage) {
            this.resIdName = resIdName;
            this.nickname = nickname;
            this.jid = jid;
            this.resIdDescription = resIdDescription;
            this.resIdImage = resIdImage;
        }
    }
}