package org.awesomeapp.messenger.ui.stickers;

import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.awesomeapp.messenger.ImApp;

import im.zom.messenger.R;


public class StickerGridAdapter extends BaseAdapter
{
    private Context mContext;       
    ArrayList<Sticker> mEmoji;
    
    public StickerGridAdapter(Context c, ArrayList<Sticker> emoji)
    {
          mContext = c;
          mEmoji = emoji;
          
    }
    
    
    public int getCount() 
    {
          return mEmoji.size();
    }
    public Object getItem(int position)
    {
          return mEmoji.get(position);
    }
    public long getItemId(int position)
    {
          return position;
    }

    public View getView(int position,View convertView,ViewGroup parent)
    {
       
        ImageView i = null ;

        if (convertView != null && convertView instanceof ImageView)
        	i = (ImageView)convertView;
        else
        	i = new ImageView(mContext);
       
  	  try
  	  {
  		  
  		  InputStream  is = mEmoji.get(position).res.getAssets().open(mEmoji.get(position).assetUri.getPath());
          final BitmapFactory.Options options = new BitmapFactory.Options();
          options.inSampleSize = 2;
    	  Bitmap bmp = BitmapFactory.decodeStream(is,null,options);
    	  
          i = new ImageView(mContext);
          i.setLayoutParams(new GridView.LayoutParams(256,256));
          i.setScaleType(ImageView.ScaleType.FIT_CENTER);
          i.setImageBitmap(bmp);


  	  }
  	  catch (Exception e)
  	  {
  		  Log.e("grid","problem rendering grid",e);
  	  }
  	  
         return i;
    }               
}