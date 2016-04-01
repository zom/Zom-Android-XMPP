package org.awesomeapp.messenger.tasks;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.request.target.ViewTarget;

import java.io.InputStream;

/**
 * Created by n8fr8 on 12/17/15.
 */
public class GlideVFSModule implements GlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        //ViewTarget.setTagId(1234);//R.id.glide_tag_id);

        //builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);


    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(info.guardianproject.iocipher.FileInputStream.class, InputStream.class, new GlideVFSLoader.Factory());

    }

}

