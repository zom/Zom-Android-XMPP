package org.awesomeapp.messenger.tasks;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.request.target.ViewTarget;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import im.zom.messenger.R;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;

/**
 * Created by n8fr8 on 12/17/15.
 */


public class GlideVFSLoader implements StreamModelLoader<info.guardianproject.iocipher.FileInputStream> {

    private final Context context;
    public GlideVFSLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override public DataFetcher<InputStream> getResourceFetcher(final info.guardianproject.iocipher.FileInputStream model, int width, int height) {
        return new VFSDataFetcher(context, model);
    }

    public static class Factory implements ModelLoaderFactory<info.guardianproject.iocipher.FileInputStream, InputStream> {
        @Override public ModelLoader<FileInputStream, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new GlideVFSLoader(context);
        }
        @Override public void teardown() {


        }
    }
}


class VFSDataFetcher implements DataFetcher<InputStream> {
    private final Context context;
    private InputStream vfsFileStream;

    public VFSDataFetcher(Context context, info.guardianproject.iocipher.FileInputStream vfsFileStream) {
        // explode model fields so that they can't be modified (finals in OBBFile are optional)
        this.context = context;
        this.vfsFileStream = vfsFileStream;
    }

    @Override public InputStream loadData(Priority priority) throws Exception {
        return vfsFileStream;
    }
    @Override public void cleanup() {
        try {
            if (vfsFileStream != null) {
                vfsFileStream.close();
            }
        } catch (IOException e) {
            Log.w("VFSDataFetcher", "Cannot clean up after stream", e);
        }
    }
    @Override public String getId() {
        return context.getPackageName() + "@" + new Date().getTime() + Math.random();
    }
    @Override public void cancel() {
        // do nothing
        try {
            if (vfsFileStream != null) {
                vfsFileStream.close();
            }
        } catch (IOException e) {
            Log.w("VFSDataFetcher", "Cannot clean up after stream", e);
        }
    }
}