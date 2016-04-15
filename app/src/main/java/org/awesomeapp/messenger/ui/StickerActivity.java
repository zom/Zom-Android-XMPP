/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.awesomeapp.messenger.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.ui.stickers.Sticker;
import org.awesomeapp.messenger.ui.stickers.StickerGroup;
import org.awesomeapp.messenger.ui.stickers.StickerManager;
import org.awesomeapp.messenger.ui.stickers.StickerPagerAdapter;
import org.awesomeapp.messenger.ui.stickers.StickerSelectListener;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.util.SystemServices;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import im.zom.messenger.R;
import info.guardianproject.iocipher.FileInputStream;

public class StickerActivity extends AppCompatActivity {


    private ImApp mApp;
    private ViewPager mStickerPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.awesome_activity_stickers);
        setTitle(R.string.action_stickers);
        Intent intent = getIntent();
        mApp = (ImApp)getApplication();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mStickerPager = (ViewPager)findViewById(R.id.stickerPager);


        initStickers();
    }

    private void initStickers ()
    {
        if (ConversationView.sStickerManager == null)
        {
            ConversationView.sStickerManager = StickerManager.getInstance(this);

            try {

                String basePath = "stickers/olo_and_shimi_1";
                AssetManager aMan = getAssets();
                String[] filelist = aMan.list(basePath);

                String category = "Olo & Shimi";

                for (int i = 0; i < filelist.length; i++) {
                    Sticker sticker = new Sticker();
                    sticker.name = filelist[i];
                    sticker.category = category;
                    sticker.assetUri = Uri.parse(basePath + '/' +  filelist[i]);
                    sticker.res = getResources();
                    sticker.emoticon =  filelist[i];

                    ConversationView.sStickerManager.addPattern(sticker.emoticon, sticker);
                    ConversationView.sStickerManager.addEmojiToCategory(category, sticker);
                }

                basePath = "stickers/zomkyi";
                filelist = aMan.list(basePath);

                category = "Zomkyi";

                for (int i = 0; i < filelist.length; i++) {
                    Sticker sticker = new Sticker();
                    sticker.name = filelist[i];
                    sticker.category = category;
                    sticker.assetUri = Uri.parse(basePath + '/' +  filelist[i]);
                    sticker.res = getResources();
                    sticker.emoticon =  filelist[i];

                    ConversationView.sStickerManager.addPattern(sticker.emoticon, sticker);
                    ConversationView.sStickerManager.addEmojiToCategory(category, sticker);
                }

                basePath = "stickers/topgyal";
                filelist = aMan.list(basePath);

                category = "Topgyal";

                for (int i = 0; i < filelist.length; i++) {
                    Sticker sticker = new Sticker();
                    sticker.name = filelist[i];
                    sticker.category = category;
                    sticker.assetUri = Uri.parse(basePath + '/' +  filelist[i]);
                    sticker.res = getResources();
                    sticker.emoticon =  filelist[i];

                    ConversationView.sStickerManager.addPattern(sticker.emoticon, sticker);
                    ConversationView.sStickerManager.addEmojiToCategory(category, sticker);
                }

                basePath = "stickers/sindu";
                filelist = aMan.list(basePath);

                category = "Sindu";

                for (int i = 0; i < filelist.length; i++) {
                    Sticker sticker = new Sticker();
                    sticker.name = filelist[i];
                    sticker.category = category;
                    sticker.assetUri = Uri.parse(basePath + '/' +  filelist[i]);
                    sticker.res = getResources();
                    sticker.emoticon =  filelist[i];

                    ConversationView.sStickerManager.addPattern(sticker.emoticon, sticker);
                    ConversationView.sStickerManager.addEmojiToCategory(category, sticker);
                }

                basePath = "stickers/losar";
                filelist = aMan.list(basePath);

                category = "Losar";

                for (int i = 0; i < filelist.length; i++) {
                    Sticker sticker = new Sticker();
                    sticker.name = filelist[i];
                    sticker.category = category;
                    sticker.assetUri = Uri.parse(basePath + '/' +  filelist[i]);
                    sticker.res = getResources();
                    sticker.emoticon =  filelist[i];

                    ConversationView.sStickerManager.addPattern(sticker.emoticon, sticker);
                    ConversationView.sStickerManager.addEmojiToCategory(category, sticker);
                }

            }
            catch (Exception fe)
            {
                Log.e(ImApp.LOG_TAG,"could not load emoji definition",fe);
            }

        }



        Collection<StickerGroup> emojiGroups = ConversationView.sStickerManager.getEmojiGroups();

        if (emojiGroups.size() > 0)
        {
            StickerPagerAdapter emojiPagerAdapter = new StickerPagerAdapter(this, new ArrayList<StickerGroup>(emojiGroups),
                    new StickerSelectListener() {
                        @Override
                        public void onStickerSelected(Sticker s) {

                           exportAsset(s.assetUri,s.name);

                        }
                    });

            mStickerPager.setAdapter(emojiPagerAdapter);

        }


    }

    private void exportAsset (Uri mediaUri, String name)
    {
        try {

            String mimeType = "image/png";
            java.io.File exportPath = new File(Environment.getExternalStorageDirectory(),name + ".png");

            java.io.InputStream fis = getResources().getAssets().open(mediaUri.getPath());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(exportPath, false);

            IOUtils.copyLarge(fis, fos);

            fos.close();
            fis.close();

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportPath));
            shareIntent.setType(mimeType);
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.export_media)));
        } catch (IOException e) {
            Toast.makeText(this, "Export Failed " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      //  getMenuInflater().inflate(R.menu.menu_conversation_detail, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);



    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

}
