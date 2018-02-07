package org.awesomeapp.messenger.ui.widgets;

import android.net.Uri;

/**
 * Created by n8fr8 on 2/7/18.
 */

public class ShareRequest
{
    public boolean deleteFile = false;
    public boolean resizeImage = false;
    public boolean importContent = false;
    public Uri media;
    public String mimeType;
}
