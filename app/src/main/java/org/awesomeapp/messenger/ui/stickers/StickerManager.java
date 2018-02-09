package org.awesomeapp.messenger.ui.stickers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;

import im.zom.messenger.R;

public class StickerManager {
	
	
	private static StickerManager mInstance = null;

	private Map<Pattern, Sticker> emoticons = new HashMap<Pattern, Sticker>();
	private LinkedHashMap<String, StickerGroup> categories = new LinkedHashMap<String, StickerGroup>();
	
	private Context mContext;
	
	private final static String PLUGIN_CONSTANT = "im.zom.emoji.STICKER_PACK";
	
	private StickerManager(Context context)
	{
		mContext = context;
	}
	
	public void addJsonPlugins () throws IOException
	{
		PackageManager packageManager = mContext.getPackageManager();
		Intent stickerIntent = new Intent(PLUGIN_CONSTANT);
		List<ResolveInfo> stickerPack = packageManager.queryIntentActivities(stickerIntent, 0);
		
		for (ResolveInfo ri : stickerPack)
		{
			
			try {
				Resources res = packageManager.getResourcesForApplication(ri.activityInfo.applicationInfo);
				
				String[] files = res.getAssets().list("");
				
				for (String file : files)
				{
					if (file.endsWith(".json"))
						addJsonDefinitions(file,file.substring(0,file.length()-5),"png",res);
				}
				
			} catch (NameNotFoundException e) {
				Log.e("emoji","unable to find application for emoji plugin");
			}
		}
		
	}
	
	public void addJsonDefinitions (String assetPathJson, String basePath, String fileExt) throws IOException
	{
		addJsonDefinitions (assetPathJson, basePath, fileExt, mContext.getResources());
	}
	
	public void addJsonDefinitions (String assetPathJson, String basePath, String fileExt, Resources res) throws IOException
	{

		/*
		Gson gson = new Gson();
		
		Reader reader = new InputStreamReader(res.getAssets().open(assetPathJson));
		
		Type collectionType = new TypeToken<ArrayList<Emoji>>(){}.getType();
		Collection<Emoji> emojis = gson.fromJson(reader, collectionType );
		
		for (Emoji emoji : emojis)
		{
			emoji.assetPath = basePath + '/' + emoji.name + '.' + fileExt;
			emoji.res = res;
			
			try
			{
				res.getAssets().open(emoji.assetPath);
				
				addPattern(':' + emoji.name + ':', emoji);
				
				if (emoji.moji != null)
					addPattern(emoji.moji, emoji);
				
				if (emoji.emoticon != null)
					addPattern(emoji.emoticon, emoji);

				
				if (emoji.category != null)
					addEmojiToCategory (emoji.category, emoji);
			}
			catch (FileNotFoundException fe)
			{
				//should not be added as a valid emoji
			}
		}*/
		
		
	}
	
	public Collection<StickerGroup> getEmojiGroups ()
	{
		return categories.values();
	}
	
	public String getAssetPath (Sticker emoji)
	{
		return emoji.name;
	}
	
	public synchronized void addEmojiToCategory (String category, Sticker emoji)
	{
		StickerGroup emojiGroup = categories.get(category);
		
		if (emojiGroup == null)
		{
			emojiGroup = new StickerGroup();
			emojiGroup.category = category;
			emojiGroup.emojis = new ArrayList<Sticker>();
		}
		
		emojiGroup.emojis.add(emoji);
		
		categories.put(category, emojiGroup);
	}
	
	public static synchronized StickerManager getInstance (Activity context)
	{       
		
		if (mInstance == null) {
			mInstance = new StickerManager(context);
			mInstance.initStickers(context);
		}
		
		return mInstance;
	}

	
	public void addPattern(String pattern, Sticker resource) {
		  
		emoticons.put(Pattern.compile(pattern,Pattern.LITERAL), resource);
		
	}

	public void addPattern(char charPattern, Sticker resource) {
		  
		emoticons.put(Pattern.compile(charPattern+"",Pattern.UNICODE_CASE), resource);
	}
	
	
	public boolean addEmoji(Context context, Spannable spannable) throws IOException {
		boolean hasChanges = false;
		for (Entry<Pattern, Sticker> entry : emoticons.entrySet())
		{
			Matcher matcher = entry.getKey().matcher(spannable);
			while (matcher.find()) {
				boolean set = true;
				for (ImageSpan span : spannable.getSpans(matcher.start(),
				        matcher.end(), ImageSpan.class))
					
				    if (spannable.getSpanStart(span) >= matcher.start()
				            && spannable.getSpanEnd(span) <= matcher.end())
				        spannable.removeSpan(span);
				    else {
				        set = false;
				        break;
				    }
				if (set) {
				    hasChanges = true;
				    
				    Sticker emoji = entry.getValue();
				    spannable.setSpan(new ImageSpan(context, BitmapFactory.decodeStream(emoji.res.getAssets().open(emoji.assetUri.getPath()))),
				            matcher.start(), matcher.end(),
				            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		}
		return hasChanges;
	}

	private void initStickers (Activity activity) {

		try {

			AssetManager aMan = activity.getAssets();

            String basePath = "stickers/zomkyi";
            String category = activity.getString(R.string.stickers_zomkyi);
            loadStickers(activity,category,basePath,aMan.list(basePath));

            basePath = "stickers/expressions";
            category = activity.getString(R.string.stickers_expressions);
            loadStickers(activity,category,basePath,aMan.list(basePath));

            basePath = "stickers/olo and shimi";
			category = activity.getString(R.string.stickers_olo_and_shimi);
			loadStickers(activity,category,basePath,aMan.list(basePath));

            basePath = "stickers/losar";
            category = activity.getString(R.string.stickers_losar);
            loadStickers(activity,category,basePath,aMan.list(basePath));

            basePath = "stickers/sindu";
            category = activity.getString(R.string.stickers_sindu);
            loadStickers(activity,category,basePath,aMan.list(basePath));

            basePath = "stickers/pema";
			category = activity.getString(R.string.stickers_pema);
			loadStickers(activity,category,basePath,aMan.list(basePath));

			basePath = "stickers/topgyal";
			category = activity.getString(R.string.stickers_topgyal);
			loadStickers(activity,category,basePath,aMan.list(basePath));

            basePath = "stickers/buddhist";
            category = activity.getString(R.string.stickers_buddhist);
            loadStickers(activity,category,basePath,aMan.list(basePath));

			basePath = "stickers/tiboji";
			category = activity.getString(R.string.stickers_tiboji);
			loadStickers(activity,category,basePath,aMan.list(basePath));



		} catch (Exception fe) {
			Log.e(ImApp.LOG_TAG, "could not load emoji definition", fe);
		}

	}

	private void loadStickers (Activity activity, String category, String basePath, String[] filelist)
	{
		for (int i = 0; i < filelist.length; i++) {
			Sticker sticker = new Sticker();
			sticker.name = filelist[i];
			sticker.category = category;
			sticker.assetUri = Uri.parse(basePath + '/' + filelist[i]);
			sticker.res = activity.getResources();
			sticker.emoticon = filelist[i];

			addPattern(sticker.emoticon, sticker);
			addEmojiToCategory(category, sticker);
		}
	}
}
