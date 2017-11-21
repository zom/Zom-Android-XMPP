package org.awesomeapp.messenger.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Date;

import info.guardianproject.iocipher.FileInputStream;

/**
 * Created by n8fr8 on 12/17/15.
 */


public class GlideVFSLoader implements ModelLoader<info.guardianproject.iocipher.FileInputStream, InputStream> {

    public GlideVFSLoader() {
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(info.guardianproject.iocipher.FileInputStream model, int width, int height, Options options) {
        return new LoadData<>(new GlideVFSCacheKey(model), new VFSDataFetcher(model));
    }

    @Override
    public boolean handles(info.guardianproject.iocipher.FileInputStream model) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<info.guardianproject.iocipher.FileInputStream, InputStream> {
        @Override
        public ModelLoader<FileInputStream, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new GlideVFSLoader();
        }

        @Override
        public void teardown() {
        }
    }
}

class GlideVFSCacheKey implements Key {
    FileInputStream fis;

    public GlideVFSCacheKey(FileInputStream fis) {
        this.fis = fis;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
      //  Log.d(getClass().getName(),"updateDiskCacheKey=" + messageDigest.toString());
    }

    @Override
    public int hashCode() {
        try {
            int hashCode = fis.getFD().hashCode();
         //   Log.d("GlideKey","HashCode="+hashCode);
            return hashCode;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return ((GlideVFSCacheKey) obj).hashCode() == (hashCode());
    }
}

class VFSDataFetcher implements DataFetcher<InputStream> {
    private InputStream vfsFileStream;

    public VFSDataFetcher(info.guardianproject.iocipher.FileInputStream vfsFileStream) {
        // explode model fields so that they can't be modified (finals in OBBFile are optional)
        this.vfsFileStream = vfsFileStream;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        callback.onDataReady(vfsFileStream);
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

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}