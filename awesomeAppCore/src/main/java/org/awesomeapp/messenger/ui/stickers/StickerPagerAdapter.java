package org.awesomeapp.messenger.ui.stickers;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import java.util.List;

import info.guardianproject.otr.app.im.R;

public class StickerPagerAdapter extends PagerAdapter {

	   
	   StickerGridAdapter[] gias;
	   List<StickerGroup> mEmojiGroups;
	   
	   Context mContext;
        StickerSelectListener mListener;

	   public StickerPagerAdapter(Context context, List<StickerGroup> emojiGroups, StickerSelectListener listener)
	   {
		   super();
		   
		   mContext = context;
		   
		   mEmojiGroups = emojiGroups;
		   gias = new StickerGridAdapter[mEmojiGroups.size()];

           mListener = listener;

	   }
	   
	   @Override
	   public Object instantiateItem(View collection, int position) {
		
		   gias[position] = new StickerGridAdapter(mContext,mEmojiGroups.get(position).emojis);
			
		   LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		   GridView imagegrid = (GridView) inflater.inflate(R.layout.stickergrid, null);

		
			imagegrid.setAdapter(gias[position]);
			 
			 imagegrid.setOnItemClickListener(new OnItemClickListener () {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
							long arg3) {
						
						
						GridView gv = ((GridView)arg0);
						
						Sticker t = (Sticker)((StickerGridAdapter)gv.getAdapter()).getItem(arg2);

                        if (mListener != null)
                            mListener.onStickerSelected(t);

						
					}
					  
				  });     
			 
		        ((ViewPager)collection).addView(imagegrid);

		        
			 return imagegrid;
     	 
				  
	   }
	   
		@Override
		public int getCount() {
			return mEmojiGroups.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
		@Override
		 public void destroyItem(ViewGroup collection, int position, Object arg2) {
		     ((ViewPager) collection).removeView((ViewGroup) arg2);}


		 @Override
		 public Parcelable saveState() {
		     return null;}


		@Override
		public void startUpdate(ViewGroup collection) {}

		@Override
		public void finishUpdate(ViewGroup collection) {}
		

        @Override
        public CharSequence getPageTitle(int position) {
           
        	return mEmojiGroups.get(position).category;
        	
        	
        }
	   
}