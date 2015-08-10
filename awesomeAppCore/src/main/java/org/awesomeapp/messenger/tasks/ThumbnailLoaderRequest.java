package org.awesomeapp.messenger.tasks;

import android.content.ContentResolver;
import android.net.Uri;

import org.awesomeapp.messenger.ui.GalleryListItem;

/**
 * Created by n8fr8 on 8/10/15.
 */
public class ThumbnailLoaderRequest {


    public GalleryListItem.ViewHolder mHolder;
    public Uri mUri;
    public ContentResolver mResolver;
}
