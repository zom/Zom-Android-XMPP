/**
 *
 */
package org.awesomeapp.messenger.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.support.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.awesomeapp.messenger.ImApp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;

/**
 * Copyright (C) 2014 Guardian Project.  All rights reserved.
 *
 * @author liorsaar
 *
 */
public class SecureMediaStore {

    public static final String TAG = SecureMediaStore.class.getName();
    private static String dbFilePath;
    private static final String BLOB_NAME = "media.db";

    public static final int DEFAULT_IMAGE_WIDTH = 1080;


    public static void unmount() {
        VirtualFileSystem.get().unmount();
    }

    public static void list(String parent) {
        File file = new File(parent);
        String[] list = file.list();

        Log.d(TAG, "Dir=" + file.isDirectory() + ";" + file.getAbsolutePath() + ";last=" + new Date(file.lastModified()));

        for (int i = 0 ; i < list.length ; i++) {
            File fileChild = new File(parent,list[i]);
            if (fileChild.isDirectory()) {
                list(fileChild.getAbsolutePath());
            } else {
                Log.d(TAG, "Dir=" + fileChild.isDirectory() + ";" + fileChild.getAbsolutePath()+ ";last=" + new Date(fileChild.lastModified()));
            }
        }
    }

    public static void deleteSession( String sessionId ) throws IOException {
        String dirName = "/" + sessionId;
        File file = new File(dirName);
        // if the session doesnt have any ul/dl files - bail
        if (!file.exists()) {
            return;
        }
        // delete recursive
        delete( dirName );
    }

    private static void delete(String parentName) throws IOException {
        File parent = new File(parentName);
        // if a file or an empty directory - delete it
        if (!parent.isDirectory()  ||  parent.list().length == 0 ) {
        //    Log.e(TAG, "delete:" + parent );
            if (!parent.delete()) {
                throw new IOException("Error deleting " + parent);
            }
            return;
        }
        // directory - recurse
        String[] list = parent.list();
        for (int i = 0 ; i < list.length ; i++) {
            String childName = parentName + "/" + list[i];
            delete( childName );
        }
        delete( parentName );
    }

    private static final String VFS_SCHEME = "vfs";
    private static final String CONTENT_SCHEME = "content";

    public static Uri vfsUri(String filename) {
        return Uri.parse(VFS_SCHEME + ":" + filename);
    }

    public static boolean isVfsUri(Uri uri) {
        return TextUtils.equals(VFS_SCHEME, uri.getScheme());
    }

    public static boolean isContentUri(Uri uri) {
        return TextUtils.equals(CONTENT_SCHEME, uri.getScheme());
    }

    public static boolean isContentUri(String uriString) {
        if (TextUtils.isEmpty(uriString))
            return false;
        else
            return uriString.startsWith(CONTENT_SCHEME + ":/");
    }


    public static boolean isVfsUri(String uriString) {
        if (TextUtils.isEmpty(uriString))
            return false;
        else
            return uriString.startsWith(VFS_SCHEME + ":/");
    }

    public static Bitmap getThumbnailVfs(Uri uri, int thumbnailSize) {
        
        if (!VirtualFileSystem.get().isMounted())
            return null;
        
        File image = new File(uri.getPath());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;

        try {
            FileInputStream fis = new FileInputStream(new File(image.getPath()));
            BitmapFactory.decodeStream(fis, null, options);
        } catch (Exception e) {
            LogCleaner.warn(ImApp.LOG_TAG,"unable to read vfs thumbnail" + e.toString());
            return null;
        }

        if ((options.outWidth == -1) || (options.outHeight == -1))
            return null;

        int originalSize = (options.outHeight > options.outWidth) ? options.outHeight
                : options.outWidth;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / thumbnailSize;

        try {
            FileInputStream fis = new FileInputStream(new File(image.getPath()));
            Bitmap scaledBitmap = BitmapFactory.decodeStream(fis, null, opts);
            return scaledBitmap;
        } catch (FileNotFoundException e) {
            LogCleaner.warn(ImApp.LOG_TAG, "can't find IOcipher file: " + image.getPath());
            return null;
        }
        catch (OutOfMemoryError oe)
        {
            LogCleaner.error(ImApp.LOG_TAG, "out of memory loading thumbnail: " + image.getPath(), oe);

            return null;
        }
    }


    /**
     * Careful! All of the {@code File}s in this method are {@link java.io.File}
     * not {@link info.guardianproject.iocipher.File}s
     *
     * @param context
     * @param key
     * @throws IllegalArgumentException
     */
    public static void init(Context context, byte[] key) throws IllegalArgumentException {
        // there is only one VFS, so if its already mounted, nothing to do
        VirtualFileSystem vfs = VirtualFileSystem.get();

        if (vfs.isMounted()) {
            Log.w(TAG, "VFS " + vfs.getContainerPath() + " is already mounted, so unmount()");
            try
            {
                vfs.unmount();
            }
            catch (Exception e)
            {
                Log.w(TAG, "VFS " + vfs.getContainerPath() + " issues with unmounting: " + e.getMessage());
            }
        }

        Log.w(TAG,"Mounting VFS: " + vfs.getContainerPath());

        dbFilePath = getInternalDbFilePath(context);

        if (!new java.io.File(dbFilePath).exists()) {
            vfs.createNewContainer(dbFilePath, key);
        }

        try {
            vfs.mount(dbFilePath, key);
       //     list("/");
        }
        catch (Exception e)
        {
            Log.w(TAG, "VFS " + vfs.getContainerPath() + " issues with mounting: " + e.getMessage());
        }

    }

    public static boolean isMounted ()
    {
        return VirtualFileSystem.get().isMounted();
    }

    /**
     * get the internal storage path for the chat media file storage file.
     */
    public static String getInternalDbFilePath(Context c) {
        return c.getFilesDir() + "/" + BLOB_NAME;
    }

    /**
     * Copy device content into vfs.
     * All imported content is stored under /SESSION_NAME/
     * The original full path is retained to facilitate browsing
     * The session content can be deleted when the session is over
     * @param sourceFile
     * @return vfs uri
     * @throws IOException
     */
    public static Uri importContent(String sessionId, java.io.File sourceFile) throws IOException {
        //list("/");
        String targetPath = "/" + sessionId + "/upload/" + UUID.randomUUID().toString() + "/" + sourceFile.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        targetPath = createUniqueFilename(targetPath);
        copyToVfs( sourceFile, targetPath );
        //list("/");
        return vfsUri(targetPath);
    }
    
    /**
     * Copy device content into vfs.
     * All imported content is stored under /SESSION_NAME/
     * The original full path is retained to facilitate browsing
     * The session content can be deleted when the session is over
     * @param sessionId
     * @return vfs uri
     * @throws IOException
     */
    public static Uri importContent(String sessionId, String fileName, InputStream sourceStream) throws IOException {
        //list("/");
        String targetPath = "/" + sessionId + "/upload/" + UUID.randomUUID().toString() + '/' + fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        targetPath = createUniqueFilename(targetPath);
        copyToVfs( sourceStream, targetPath );
        //list("/");
        return vfsUri(targetPath);
    }

    /**
     * Copy device content into vfs.
     * All imported content is stored under /SESSION_NAME/
     * The original full path is retained to facilitate browsing
     * The session content can be deleted when the session is over
     * @param sessionId
     * @return vfs uri
     * @throws IOException
     */
    public static Uri createContentPath(String sessionId, String fileName) throws IOException {
        //list("/");
        String targetPath = "/" + sessionId + "/upload/" + UUID.randomUUID().toString() + '/' + fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        targetPath = createUniqueFilename(targetPath);
        mkdirs( targetPath );

        //list("/");
        return vfsUri(targetPath);
    }

    /**
     * Resize an image to an efficient size for sending via OTRDATA, then copy
     * that resized version into vfs. All imported content is stored under
     * /SESSION_NAME/ The original full path is retained to facilitate browsing
     * The session content can be deleted when the session is over
     *
     * @param sessionId
     * @return vfs uri
     * @throws IOException
     */
    public static Uri resizeAndImportImage(Context context, String sessionId, Uri uri, String mimeType)
            throws IOException {

        String originalImagePath = uri.getPath();
        String targetPath = "/" + sessionId + "/upload/" + UUID.randomUUID().toString() + "/image";
        boolean savePNG = false;

        if (originalImagePath.endsWith(".png") || (mimeType != null && mimeType.contains("png"))
                || originalImagePath.endsWith(".gif") || (mimeType != null && mimeType.contains("gif"))
                ) {
            savePNG = true;
            targetPath += ".png";
        }
        else
        {
            targetPath += ".jpg";


        }

        //load lower-res bitmap
        Bitmap bmp = getThumbnailFile(context, uri, DEFAULT_IMAGE_WIDTH);

        File file = new File(targetPath);
        file.getParentFile().mkdirs();
        FileOutputStream out = new info.guardianproject.iocipher.FileOutputStream(file);
        
        if (savePNG)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        else
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);

        out.flush();
        out.close();        
        bmp.recycle();        

        return vfsUri(targetPath);
    }

    public static InputStream openInputStream (Context context, Uri uri) throws FileNotFoundException {
        InputStream is;

        if (uri.getScheme() != null && uri.getScheme().equals("vfs"))
            is = new info.guardianproject.iocipher.FileInputStream(uri.getPath());
        else
            is = context.getContentResolver().openInputStream(uri);


        return is;
    }
    public static Bitmap getThumbnailFile(Context context, Uri uri, int thumbnailSize) throws IOException {

        InputStream is = openInputStream(context, uri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;
        
        BitmapFactory.decodeStream(is, null, options);
        
        if ((options.outWidth == -1) || (options.outHeight == -1))
            return null;

        int originalSize = (options.outHeight > options.outWidth) ? options.outHeight
                : options.outWidth;

        is.close();
        is = openInputStream(context, uri);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(options, thumbnailSize, thumbnailSize);

        Bitmap scaledBitmap = BitmapFactory.decodeStream(is, null, opts);
        is.close();

        InputStream isEx = openInputStream(context,uri);
        ExifInterface exif = new ExifInterface(isEx);
        int orientationType = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int orientationD  = 0;
        if (orientationType == ExifInterface.ORIENTATION_ROTATE_90)
            orientationD = 90;
        else if (orientationType == ExifInterface.ORIENTATION_ROTATE_180)
            orientationD = 180;
        else if (orientationType == ExifInterface.ORIENTATION_ROTATE_270)
            orientationD = 270;

        if (orientationD != 0)
            scaledBitmap = rotateBitmap(scaledBitmap, orientationD);

        isEx.close();

        return scaledBitmap;
    }

    public static void exportAll(String sessionId ) throws IOException {
    }

    public static void exportContent(String mimeType, Uri mediaUri, java.io.File exportPath) throws IOException {
        String sourcePath = mediaUri.getPath();

        copyToExternal( sourcePath, exportPath);
    }

    public static java.io.File exportPath(String mimeType, Uri mediaUri) {
        java.io.File targetFilename;
        if (mimeType.startsWith("image")) {
            targetFilename = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),mediaUri.getLastPathSegment());
        } else if (mimeType.startsWith("audio")) {
            targetFilename = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),mediaUri.getLastPathSegment());
        } else {
            targetFilename = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),mediaUri.getLastPathSegment());
        }
        java.io.File targetUniqueFilename = createUniqueFilenameExternal(targetFilename);
        return targetFilename;
    }

    public static void copyToVfs(String sourcePath, String targetPath) throws IOException {
       copyToVfs(new java.io.File(sourcePath), targetPath);
    }

    public static void copyToVfs(java.io.File sourceFile, String targetPath) throws IOException {
        // create the target directories tree
        mkdirs( targetPath );
        // copy
        java.io.InputStream fis = null;
        fis  = new java.io.FileInputStream(sourceFile);

        FileOutputStream fos = new FileOutputStream(new File(targetPath), false);

        IOUtils.copyLarge(fis, fos);

        fos.close();
        fis.close();
    }


    public static void copyToVfs(InputStream sourceIS, String targetPath) throws IOException {
        // create the target directories tree
        mkdirs( targetPath );
        // copy
        FileOutputStream fos = new FileOutputStream(new File(targetPath), false);

        IOUtils.copyLarge(sourceIS, fos);

        fos.close();
        sourceIS.close();
    }


    public static void copyToVfs(byte buf[], String targetPath) throws IOException {
        File file = new File(targetPath);
        FileOutputStream out = new FileOutputStream(file);
        out.write(buf);
        out.close();
    }
    

    public static void copyToExternal(String sourcePath, java.io.File targetPath) throws IOException {
        // copy
        FileInputStream fis = new FileInputStream(new File(sourcePath));
        java.io.FileOutputStream fos = new java.io.FileOutputStream(targetPath, false);

        IOUtils.copyLarge(fis, fos);

        fos.close();
        fis.close();
    }

    private static void mkdirs(String targetPath) throws IOException {
        File targetFile = new File(targetPath);
        if (!targetFile.exists()) {
            File dirFile = targetFile.getParentFile();
            if (!dirFile.exists()) {
                boolean created = dirFile.mkdirs();
                if (!created) {
                    throw new IOException("Error creating " + targetPath);
                }
            }
        }
    }

    public static boolean exists(String path) {
        return new File(path).exists();
    }

    public static boolean sessionExists(String sessionId) {
        return exists( "/" + sessionId );
    }

    private static String createUniqueFilename( String filename ) {

        if (!exists(filename)) {
            return filename;
        }
        int count = 1;
        String uniqueName;
        File file;
        do {
            uniqueName = formatUnique(filename, count++);
            file = new File(uniqueName);
        } while(file.exists());

        return uniqueName;
    }

    private static String formatUnique(String filename, int counter) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot != -1)
        {
            String name = filename.substring(0,lastDot);
            String ext = filename.substring(lastDot);
            return name + "-" + counter + "." + ext;
        }
        else
        {
            return filename + counter;
        }
    }

    public static String getDownloadFilename(String sessionId, String filenameFromUrl) {
        String filename = "/" + sessionId + "/download/" + filenameFromUrl.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String uniqueFilename = createUniqueFilename(filename);
        return uniqueFilename;
    }

    private static java.io.File createUniqueFilenameExternal(java.io.File filename ) {
        if (!filename.exists()) {
            return filename;
        }
        int count = 1;
        String uniqueName;
        java.io.File file;
        do {
            uniqueName = formatUnique(filename.getName(), count++);
            file = new java.io.File(filename.getParentFile(),uniqueName);
        } while(file.exists());

        return file;
    }

    public static int getImageOrientation(String imagePath) {
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int rotate) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotatedBitmap;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
