package org.awesomeapp.messenger.util;

/**
 * Found here: https://gist.github.com/chalup/4201307da02b9cfe4f40
 */

import android.content.ContentResolver;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashSet;

public class MultiUriCursorWrapper extends CursorWrapper {

    public MultiUriCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    protected boolean mClosed;
    protected ContentResolver mContentResolver;

    private final LinkedHashSet<Uri> mNotifyUris = new LinkedHashSet<Uri>();

    private final Object mSelfObserverLock = new Object();
    private ContentObserver mSelfObserver;
    private boolean mSelfObserverRegistered;

    private final LinkedHashSet<Uri> mChangedByUris = new LinkedHashSet<Uri>();

    private final ContentObservable mContentObservable = new ContentObservable();

    @Override
    public void deactivate() {
        onDeactivateOrClose();
        super.deactivate();
    }

    protected void onDeactivateOrClose() {
        if (mSelfObserver != null) {
            mContentResolver.unregisterContentObserver(mSelfObserver);
            mSelfObserverRegistered = false;
        }
    }

    @Override
    public boolean requery() {
        if (mSelfObserver != null && !mSelfObserverRegistered) {
            for (Uri notifyUri : mNotifyUris) {
                mContentResolver.registerContentObserver(notifyUri, true, mSelfObserver);
            }
            mSelfObserverRegistered = true;
        }

        boolean success = super.requery();
        if (success) {
            mChangedByUris.clear();
        }
        return success;
    }

    @Override
    public boolean isClosed() {
        return super.isClosed() && mClosed;
    }

    @Override
    public void close() {
        super.close();

        mClosed = true;
        mContentObservable.unregisterAll();
        onDeactivateOrClose();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void registerContentObserver(ContentObserver observer) {
        mContentObservable.registerObserver(observer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            for (Uri changedByUri : mChangedByUris) {
                observer.dispatchChange(false, changedByUri);
            }
        } else {
            if (!mChangedByUris.isEmpty()) {
                observer.dispatchChange(false);
            }
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        // cursor will unregister all observers when it close
        if (!mClosed) {
            mContentObservable.unregisterObserver(observer);
        }
    }

    @SuppressWarnings("deprecation")
    private void onChange(boolean selfChange, Uri uri) {
        synchronized (mSelfObserverLock) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mContentObservable.dispatchChange(selfChange, uri);
            } else {
                mContentObservable.dispatchChange(selfChange);
            }

            if (selfChange) {
                for (Uri notifyUri : mNotifyUris) {
                    mContentResolver.notifyChange(notifyUri, mSelfObserver);
                }
            }

            if (!selfChange) {
                mChangedByUris.add(uri);
            }
        }
    }

    public MultiUriCursorWrapper withNotificationUris(ContentResolver cr, Iterable<Uri> uris) {
        synchronized (mSelfObserverLock) {
            //Iterables.addAll(mNotifyUris, uris);
            for (Uri uri : uris)
                mNotifyUris.add(uri);

            mContentResolver = cr;
            if (mSelfObserver == null) {
                mSelfObserver = new SelfContentObserver(this);
            }

            for (Uri uri : uris) {
                mContentResolver.registerContentObserver(uri, true, mSelfObserver);
            }

            mSelfObserverRegistered = true;
        }

        return this;
    }

    public MultiUriCursorWrapper withNotificationUri(ContentResolver cr, Uri uri) {
        return withNotificationUris(cr, Collections.singletonList(uri));
    }

    public LinkedHashSet<Uri> getNotificationUris() {
        return mNotifyUris;
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri notifyUri) {
        withNotificationUri(cr, notifyUri);
    }

    @Override
    public Uri getNotificationUri() {
        synchronized (mSelfObserverLock) {
            return mNotifyUris.iterator().next();
        }
    }

    @Override
    protected void finalize() {
        try {
            super.finalize();

            if (mSelfObserver != null && mSelfObserverRegistered) {
                mContentResolver.unregisterContentObserver(mSelfObserver);
            }

            if (!mClosed) close();
        } catch (Throwable e) {
            // ignored
        }
    }

    private static class SelfContentObserver extends ContentObserver {
        WeakReference<MultiUriCursorWrapper> mCursor;

        public SelfContentObserver(MultiUriCursorWrapper cursor) {
            super(null);
            mCursor = new WeakReference<MultiUriCursorWrapper>(cursor);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            MultiUriCursorWrapper cursor = mCursor.get();
            if (cursor != null) {
                cursor.onChange(false, uri);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    }
}