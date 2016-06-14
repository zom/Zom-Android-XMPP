package org.awesomeapp.messenger.crypto;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.RandomAccessFile;

import org.apache.http.Header;
import org.awesomeapp.messenger.service.IDataListener;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.util.SecureMediaStore;
import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.model.ChatSession;
import org.awesomeapp.messenger.model.DataHandler;
import org.awesomeapp.messenger.model.Message;
import org.awesomeapp.messenger.util.Debug;
import org.awesomeapp.messenger.util.LogCleaner;
import org.awesomeapp.messenger.util.SystemServices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.java.otr4j.session.SessionStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.AbstractSessionOutputBuffer;
import org.apache.http.impl.io.HttpRequestParser;
import org.apache.http.impl.io.HttpRequestWriter;
import org.apache.http.impl.io.HttpResponseParser;
import org.apache.http.impl.io.HttpResponseWriter;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.message.LineFormatter;
import org.apache.http.message.LineParser;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;


public class OtrDataHandler implements DataHandler {

    public static final String URI_PREFIX_OTR_IN_BAND = "otr-in-band:/storage/";

    private static final int MAX_OUTSTANDING = 3;

    private static final int MAX_CHUNK_LENGTH = 32768;
    private static final int REQUEST_CHUNK_LENGTH = 1024*8;

    private static final int MAX_TRANSFER_LENGTH = 1024*1024*10; //10MB max file size

    private static final byte[] EMPTY_BODY = new byte[0];

    private static final String TAG = "Zom.Data";

    private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);
    private static HttpParams params = new BasicHttpParams();
    private static HttpRequestFactory requestFactory = new MyHttpRequestFactory();
    private static HttpResponseFactory responseFactory = new DefaultHttpResponseFactory();

    private LineParser lineParser = new BasicLineParser(PROTOCOL_VERSION);
    private LineFormatter lineFormatter = new BasicLineFormatter();
    private ChatSession mChatSession;
    private long mChatId;

    private IDataListener mDataListener;
    private SessionStatus mOtrStatus;

    HashMap<String, Offer> offerCache = new HashMap<>();//CacheBuilder.newBuilder().maximumSize(100).build();
    HashMap<String, Request> requestCache =  new HashMap<>();//CacheBuilder.newBuilder().maximumSize(100).build();
    HashMap<String, Transfer> transferCache =  new HashMap<>();//CacheBuilder.newBuilder().maximumSize(100).build();

    public OtrDataHandler(ChatSession chatSession) {
        this.mChatSession = chatSession;
    }

    public void setChatId(long chatId) {
        this.mChatId = chatId;
    }

    public void onOtrStatusChanged(SessionStatus status) {
        mOtrStatus = status;
        if (status == SessionStatus.ENCRYPTED) {
            retryRequests();
        }
    }

    private synchronized void retryRequests() {
        // Resend all unfilled requests
        Collection<Request> requests = new ArrayList<Request>(requestCache.values());

        for (Request request: requests) {
            if (!request.isSeen())
                sendRequest(request);
        }
    }

    public void setDataListener (IDataListener dataListener)
    {
        mDataListener = dataListener;
    }

    public static class MyHttpRequestFactory implements HttpRequestFactory {
        public MyHttpRequestFactory() {
            super();
        }

        public HttpRequest newHttpRequest(final RequestLine requestline)
                throws MethodNotSupportedException {
            if (requestline == null) {
                throw new IllegalArgumentException("Request line may not be null");
            }
            //String method = requestline.getMethod();
            return new BasicHttpRequest(requestline);
        }

        public HttpRequest newHttpRequest(final String method, final String uri)
                throws MethodNotSupportedException {
            return new BasicHttpRequest(method, uri);
        }
    }

    static class MemorySessionInputBuffer extends AbstractSessionInputBuffer {
        public MemorySessionInputBuffer(byte[] value) {
            init(new ByteArrayInputStream(value), 1000, params);
        }

        @Override
        public boolean isDataAvailable(int timeout) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    static class MemorySessionOutputBuffer extends AbstractSessionOutputBuffer {
        ByteArrayOutputStream outputStream;
        public MemorySessionOutputBuffer() {
            outputStream = new ByteArrayOutputStream(1000);
            init(outputStream, 1000, params);
        }

        public byte[] getOutput() {
            return outputStream.toByteArray();
        }
    }

    public synchronized void onIncomingRequest(Address requestThem, Address requestUs, byte[] value) {
        //Log.e( TAG, "onIncomingRequest:" + requestThem);

        SessionInputBuffer inBuf = new MemorySessionInputBuffer(value);
        HttpRequestParser parser = new HttpRequestParser(inBuf, lineParser, requestFactory, params);
        HttpRequest req;

        try {
            req = (HttpRequest)parser.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            e.printStackTrace();
            return;
        }

        String requestMethod = req.getRequestLine().getMethod();
        String uid = req.getFirstHeader("Request-Id").getValue();
        String url = req.getRequestLine().getUri();

        if (requestMethod.equals("OFFER")) {
            debug("incoming OFFER " + url);
            for (Header header :req.getAllHeaders())
            {
                debug("incoming header: " + header.getName() + "=" + header.getValue());
            }

            if (!url.startsWith(URI_PREFIX_OTR_IN_BAND)) {
                debug("Unknown url scheme " + url);
                sendResponse(requestUs, requestThem, 400, "Unknown scheme", uid, EMPTY_BODY);
                return;
            }

            if (!req.containsHeader("File-Length"))
            {
                sendResponse(requestUs, requestThem, 400, "File-Length must be supplied", uid, EMPTY_BODY);
                return;
            }

            int length = Integer.parseInt(req.getFirstHeader("File-Length").getValue());
            if (!req.containsHeader("File-Hash-SHA1"))
            {
                sendResponse(requestUs, requestThem, 400, "File-Hash-SHA1 must be supplied", uid, EMPTY_BODY);
                return;
            }

            sendResponse(requestUs, requestThem, 200, "OK", uid, EMPTY_BODY);

            String sum = req.getFirstHeader("File-Hash-SHA1").getValue();
            String type = null;
            if (req.containsHeader("Mime-Type")) {
                type = req.getFirstHeader("Mime-Type").getValue();
            }

            debug("Incoming sha1sum " + sum);

            Transfer transfer;
            try {
                transfer = new VfsTransfer(url, type, length, requestUs, requestThem, sum);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            transferCache.put(url, transfer);

            // Handle offer

            // TODO ask user to confirm we want this
            boolean accept = false;

            if (mDataListener != null)
            {
                try {
                    mDataListener.onTransferRequested(url, requestThem.getAddress(),requestUs.getAddress(),transfer.url);

                    //callback is now async, via "acceptTransfer" method
                 //   if (accept)
                   //     transfer.perform();

                } catch (RemoteException e) {
                    LogCleaner.error(ImApp.LOG_TAG, "error approving OTRDATA transfer request", e);
                }

            }

        } else if (requestMethod.equals("GET") && url.startsWith(URI_PREFIX_OTR_IN_BAND)) {
            debug("incoming GET " + url);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int reqEnd;

            try {
                Offer offer = offerCache.get(url);
                if (offer == null) {
                    sendResponse(requestUs,  requestThem,400, "No such offer made", uid, EMPTY_BODY);
                    return;
                }

                offer.seen(); // in case we don't see a response to underlying request, but peer still proceeds

                if (!req.containsHeader("Range"))
                {
                    sendResponse(requestUs, requestThem, 400, "Range must start with bytes=", uid, EMPTY_BODY);
                    return;
                }
                String rangeHeader = req.getFirstHeader("Range").getValue();
                String[] spec = rangeHeader.split("=");
                if (spec.length != 2 || !spec[0].equals("bytes"))
                {
                    sendResponse(requestUs, requestThem, 400, "Range must start with bytes=", uid, EMPTY_BODY);
                    return;
                }
                String[] startEnd = spec[1].split("-");
                if (startEnd.length != 2)
                {
                    sendResponse(requestUs, requestThem, 400, "Range must be START-END", uid, EMPTY_BODY);
                    return;
                }

                int start = Integer.parseInt(startEnd[0]);
                int end = Integer.parseInt(startEnd[1]);
                if (end - start + 1 > MAX_CHUNK_LENGTH) {
                    sendResponse(requestUs, requestThem, 400, "Range must be at most " + MAX_CHUNK_LENGTH, uid, EMPTY_BODY);
                    return;
                }


                File fileGet = new File(offer.getUri());
                long fileLength = -1;

                if (fileGet.exists()) {
                    fileLength = fileGet.length();
                    FileInputStream is = new FileInputStream(fileGet);
                    readIntoByteBuffer(byteBuffer, is, start, end);
                    is.close();

                }
                else
                {
                    java.io.File fileGetExtern = new java.io.File(offer.getUri());
                    if (fileGetExtern.exists()) {
                        fileLength = fileGetExtern.length();
                        java.io.FileInputStream is = new java.io.FileInputStream(fileGetExtern);
                        readIntoByteBuffer(byteBuffer, is, start, end);
                        is.close();
                    }
                }

                if (mDataListener != null && fileLength != -1)
                {
                    float percent = ((float)end) / ((float)fileLength);

                    mDataListener.onTransferProgress(true, offer.getId(), requestThem.getAddress(), offer.getUri(),
                        percent);

                    String mimeType = null;
                    if (req.getFirstHeader("Mime-Type") != null)
                        mimeType = req.getFirstHeader("Mime-Type").getValue();

                    mDataListener.onTransferComplete(true, offer.getId(), requestThem.getAddress(), offer.getUri(), mimeType, offer.getUri());
                
                }

            } catch (UnsupportedEncodingException e) {
            //    throw new RuntimeException(e);
                sendResponse(requestUs,  requestThem,400, "Unsupported encoding", uid, EMPTY_BODY);
                return;
            } catch (IOException e) {
                //throw new RuntimeException(e);
                sendResponse(requestUs,requestThem, 400,  "IOException", uid, EMPTY_BODY);
                return;
            } catch (NumberFormatException e) {
                sendResponse(requestUs, requestThem,400,  "Range is not numeric", uid, EMPTY_BODY);
                return;
            } catch (Exception e) {
                sendResponse(requestUs, requestThem,500,  "Unknown error", uid, EMPTY_BODY);
                return;
            }

            byte[] body = byteBuffer.toByteArray();
         //   debug("Sent sha1 is " + sha1sum(body));
            sendResponse(requestUs, requestThem, 200, "OK", uid, body);


        } else {
            debug("Unknown method / url " + requestMethod + " " + url);
            sendResponse(requestUs, requestThem, 400, "OK", uid, EMPTY_BODY);
        }
    }

    public void acceptTransfer (String url, String address)
    {
        Transfer transfer = transferCache.get(url);
        if (transfer != null)
        {
            transfer.perform();

        }

    }

    private static void readIntoByteBuffer(ByteArrayOutputStream byteBuffer, InputStream is, int start, int end)
            throws IOException {
        //Log.e( TAG, "readIntoByteBuffer:" + (end-start));
        if (start != is.skip(start)) {
            return;
        }
        int size = end - start + 1;
        int buffersize = 1024;
        byte[] buffer = new byte[buffersize];

        int len = 0;
        while((len = is.read(buffer)) != -1){
            if (len > size) {
                len = size;
            }
            byteBuffer.write(buffer, 0, len);
            size -= len;
        }
    }

    private static void readIntoByteBuffer(ByteArrayOutputStream byteBuffer, SessionInputBuffer sib)
            throws IOException {
        //Log.e( TAG, "readIntoByteBuffer:");
        int buffersize = 1024;
        byte[] buffer = new byte[buffersize];

        int len = 0;
        while((len = sib.read(buffer)) != -1){
            byteBuffer.write(buffer, 0, len);
        }
    }

    private void sendResponse(Address us, Address them, int code, String statusString, String uid, byte[] body) {
        MemorySessionOutputBuffer outBuf = new MemorySessionOutputBuffer();
        HttpMessageWriter writer = new HttpResponseWriter(outBuf, lineFormatter, params);
        HttpMessage response = new BasicHttpResponse(new BasicStatusLine(PROTOCOL_VERSION, code, statusString));
        response.addHeader("Request-Id", uid);
        try {
            writer.write(response);
            outBuf.write(body);
            outBuf.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        }
        byte[] data = outBuf.getOutput();
        Message message = new Message("");
        message.setFrom(us);
        message.setTo(them);
        debug("send response " + statusString + " for " + uid);
        mChatSession.sendDataAsync(message, true, data);
    }

    public void onIncomingResponse(Address from, Address to, byte[] value) {
        //Log.e( TAG, "onIncomingResponse:" + value.length);
        SessionInputBuffer buffer = new MemorySessionInputBuffer(value);
        HttpResponseParser parser = new HttpResponseParser(buffer, lineParser, responseFactory, params);
        HttpResponse res;
        try {
            res = (HttpResponse) parser.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            e.printStackTrace();
            return;
        }

        String uid = res.getFirstHeader("Request-Id").getValue();
        Request request = requestCache.get(uid);
        if (request == null) {
            debug("Unknown request ID " + uid);
            return;
        }

        if (request.isSeen()) {
            debug("Already seen request ID " + uid);
            return;
        }

        request.seen();
        int statusCode = res.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            debug("got status " + statusCode + ": " + res.getStatusLine().getReasonPhrase());
            // TODO handle error
            return;
        }

        // TODO handle success
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            readIntoByteBuffer(byteBuffer, buffer);
         //   debug("Received sha1 @" + request.start + " is " + sha1sum(byteBuffer.toByteArray()));
            if (request.method.equals("GET")) {
                Transfer transfer = transferCache.get(request.url);
                if (transfer == null) {
                    debug("Transfer expired for url " + request.url);
                    return;
                }
                transfer.chunkReceived(request, byteBuffer.toByteArray());
                if (transfer.isDone()) {
                    //Log.e( TAG, "onIncomingResponse: isDone");
                    debug("Transfer complete for " + request.url);
                    String filename = transfer.closeFile();
                    Uri vfsUri = SecureMediaStore.vfsUri(filename);
                    if (transfer.checkSum()) {

                        //Log.e( TAG, "onIncomingResponse: writing");
                        if (mDataListener != null)
                            mDataListener.onTransferComplete(
                                    false,
                                    null,
                                from.getAddress(),
                                transfer.url,
                                transfer.type,
                                vfsUri.toString());
                    } else {
                        if (mDataListener != null)
                            mDataListener.onTransferFailed(
                                    false,
                                    null,
                                    to.getAddress(),
                                transfer.url,
                                "checksum");
                        debug( "Wrong checksum for file");
                    }
                } else {
                    if (mDataListener != null)
                        mDataListener.onTransferProgress(true, null, to.getAddress(), transfer.url,
                            ((float)transfer.chunksReceived) / transfer.chunks);
                    transfer.perform();
                    debug("Progress " + transfer.chunksReceived + " / " + transfer.chunks);
                }
            }
        } catch (IOException e) {
            debug("Could not read line from response");
        } catch (RemoteException e) {
            debug("Could not read remote exception");
        }

    }

    private String getFilenameFromUrl(String url) {
        String[] path = url.split("/");
        String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);
        return sanitizedPath;
    }

    /**
    private File writeDataToStorage (String url, byte[] data)
    {
        debug( "writeDataToStorage:" + url + " " + data.length);

        String[] path = url.split("/");
        String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);

        File fileDownloadsDir = new File(Environment.DIRECTORY_DOWNLOADS);
        fileDownloadsDir.mkdirs();

        info.guardianproject.iocipher.File file = new info.guardianproject.iocipher.File(fileDownloadsDir, sanitizedPath);
        debug( "writeDataToStorage:" + file.getAbsolutePath() );

        try {
            OutputStream output = (new info.guardianproject.iocipher.FileOutputStream(file));
            output.write(data);
            output.flush();
            output.close();
            return file;
        } catch (IOException e) {
            OtrDebugLogger.log("error writing file", e);
            return null;
        }
    }*/

    @Override
    public void offerData(String id, Address us, Address them, String localUri, Map<String, String> headers) throws IOException {

        // TODO stash localUri and intended recipient


        long length = -1;
        String hash = null;

        File fileLocal = new File(localUri);

        if (fileLocal.exists()) {
            length = fileLocal.length();
            if (length > MAX_TRANSFER_LENGTH) {
                throw new IOException("Length too large: " + length);
            }
            FileInputStream is = new FileInputStream(fileLocal);
            hash = sha1sum(is);
            is.close();
        }
        else
        {
            //it is not in the encrypted store
            java.io.File fileExtern = new java.io.File(localUri);
            length = fileExtern.length();
            if (length > MAX_TRANSFER_LENGTH) {
                throw new IOException("Length too large: " + length);
            }
            java.io.FileInputStream is = new java.io.FileInputStream(fileExtern);
            hash = sha1sum(is);
            is.close();
        }

        if (headers == null)
            headers = new HashMap<>();

        headers.put("File-Name", fileLocal.getName());
        headers.put("File-Length", String.valueOf(length));
        headers.put("File-Hash-SHA1", hash);

        if (!headers.containsKey("Mime-Type")) {
            String mimeType = SystemServices.getMimeType(localUri);
            headers.put("Mime-Type", mimeType);
        }

        /**
         * 0 = {BufferedHeader@7083} "File-Name: B3C1B9F7-81EB-454A-AB5B-2B3B645453C6.m4a"
         1 = {BufferedHeader@7084} "Mime-Type: audio/x-m4a"
         2 = {BufferedHeader@7085} "File-Length: 0"
         3 = {BufferedHeader@7086} "Request-Id: 92C2FF9A-7C1E-42BA-A5DA-F2D445BCF1A5"
         */

        String[] paths = localUri.split("/");
        String url = URI_PREFIX_OTR_IN_BAND + SystemServices.sanitize(paths[paths.length - 1]);
        Request request = new Request("OFFER", us, them, url, headers);
        offerCache.put(url, new Offer(id, localUri, request));
        sendRequest(request);

    }

    public Request performGetData(Address us, Address them, String url, Map<String, String> headers, int start, int end) {
        String rangeSpec = "bytes=" + start + "-" + end;
        headers.put("Range", rangeSpec);
        Request request = new Request("GET", us, them, url, start, end, headers, EMPTY_BODY);

        sendRequest(request);
        return request;
    }

    static class Offer {
        private String mId;
        private String mUri;
        private Request request;

        public Offer(String id, String uri, Request request) {
            this.mId = id;
            this.mUri = uri;
            this.request = request;
        }

        public String getUri() {
            return mUri;
        }

        public String getId() {
            return mId;
        }

        public Request getRequest() {
            return request;
        }

        public void seen() {
            request.seen();
        }

        public boolean isSeen () { return request.isSeen(); }
    }

    static class Request {

        public Request(String method, Address us, Address them, String url, int start, int end, Map<String, String> headers, byte[] body) {
            this.method = method;
            this.url = url;
            this.start = start;
            this.end = end;
            this.us = us;
            this.them = them;
            this.headers = headers;
            this.body = body;
        }

        public Request(String method, Address us, Address them, String url, Map<String, String> headers) {
            this(method, us, them, url, -1, -1, headers, null);
        }

        public String method;
        public String url;
        public int start;
        public int end;
        public byte[] data;
        public boolean seen = false;
        public Address us;
        public Address them;
        public Map<String, String> headers;
        public byte[] body;

        public boolean isSeen() {
            return seen;
        }

        public void seen() {
            seen = true;
        }
    }

    public class Transfer {
        public final String TAG = Transfer.class.getSimpleName();
        public String url;
        public String type;
        public int chunks = 0;
        public int chunksReceived = 0;
        private int length = 0;
        private int current = 0;
        private Address us;
        private Address them;
        protected Set<Request> outstanding;
        private byte[] buffer;
        protected String sum;

        public Transfer(String url, String type, int length, Address us, Address them, String sum) {
            this.url = url;
            this.type = type;
            this.length = length;
            this.us = us;
            this.them = them;
            this.sum = sum;

            //Log.e(TAG, "url:"+url + " type:"+ type + " length:"+length) ;

            if (length > MAX_TRANSFER_LENGTH || length <= 0) {
                throw new RuntimeException("Invalid transfer size " + length);
            }
            chunks = ((length - 1) / REQUEST_CHUNK_LENGTH) + 1;
            buffer = new byte[length];
            outstanding = new HashSet<Request>();
        }

        public boolean checkSum() {
            return sum.equals(sha1sum(buffer));
        }

        public synchronized boolean perform() {

            // TODO global throttle rather than this local hack

            int performIdx = 0;

            while (current < length && outstanding.size() < MAX_OUTSTANDING) {

                Map<String, String> headers = new HashMap<>();
                int end = Math.min(length, current + REQUEST_CHUNK_LENGTH)-1;

                Request request= performGetData(us, them, url, headers, current, end);
                outstanding.add(request);

                current = end + 1;

            //    debug("current: " + current);

            }

            return true;
        }

        public boolean isDone() {
            //Log.e( TAG, "isDone:" + chunksReceived + " " + chunks);
            return chunksReceived == chunks;
        }

        public void chunkReceived(Request request, byte[] bs) {
            //Log.e( TAG, "chunkReceived:" + bs.length);
            chunksReceived++;
            System.arraycopy(bs, 0, buffer, request.start, bs.length);
            outstanding.remove(request);
        }

        public String getSum() {
            return sum;
        }

        public String closeFile() throws IOException
        {
            return url;
        }

    }

    public class VfsTransfer extends Transfer {
        String localFilename;
        private RandomAccessFile raf;

        public VfsTransfer(String url, String type, int length, Address us, Address them, String sum) throws FileNotFoundException {
            super(url, type, length, us, them, sum);
        }

        @Override
        public void chunkReceived(Request request, byte[] bs) {

            if (raf == null) {
                if (!perform()) //initialize file
                    return;
            }


            try {
                raf.seek(request.start);
                raf.write(bs);
                debug("chunkReceived: " + request.start + "-" + request.end);
                chunksReceived++;
            } catch (Exception e) {
                debug("chunkReceived failure: " + request.start + "-" + request.end + ": " + e.toString());

            }
            outstanding.remove(request);

        }

        @Override
        public boolean checkSum() {
            try {
                File file = new File(localFilename);
                return sum.equals( checkSum(file.getAbsolutePath()) );
            } catch (IOException e) {
                debug("checksum IOException");
                return false;
            }
        }

        @Override
        public synchronized boolean perform() {
            try {
                if (raf == null) {
                    raf = openFile(url);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return super.perform();
        }

        private RandomAccessFile openFile(String url) throws FileNotFoundException {
            debug( "openFile: url " + url) ;
            String sessionId = ""+ mChatId;
            String filename = getFilenameFromUrl(url);
            localFilename = SecureMediaStore.getDownloadFilename(sessionId, filename);
            debug( "openFile: localFilename " + localFilename) ;
            info.guardianproject.iocipher.RandomAccessFile ras = new info.guardianproject.iocipher.RandomAccessFile(localFilename, "rw");
            return ras;
        }

        public String closeFile() throws IOException {
            //Log.e(TAG, "closeFile") ;
            raf.close();
            File file = new File(localFilename);
            String newPath = file.getCanonicalPath();
            if(true) return newPath;

            newPath = newPath.substring(0,newPath.length()-4); // remove the .tmp
            //Log.e(TAG, "vfsCloseFile: rename " + newPath) ;
            File newPathFile = new File(newPath);
            boolean success = file.renameTo(newPathFile);
            if (!success) {
                throw new IOException("Rename error " + newPath );
            }
            return newPath;
        }

        private String checkSum(String filename) throws IOException {
            FileInputStream fis = new FileInputStream(new File(filename));
            String sum = sha1sum(fis);
            fis.close();
            return sum;
        }
    }

    private void sendRequest(Request request) {
        MemorySessionOutputBuffer outBuf = new MemorySessionOutputBuffer();
        HttpMessageWriter writer = new HttpRequestWriter(outBuf, lineFormatter, params);
        HttpMessage req = new BasicHttpRequest(request.method, request.url, PROTOCOL_VERSION);
        String uid = UUID.randomUUID().toString();
        req.addHeader("Request-Id", uid);
        if (request.headers != null) {
            for (Entry<String, String> entry : request.headers.entrySet()) {
                req.addHeader(entry.getKey(), entry.getValue());
            }
        }

        try {
            writer.write(req);
            outBuf.write(request.body);
            outBuf.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        }
        byte[] data = outBuf.getOutput();
        Message message = new Message("");
        message.setFrom(request.us);
        message.setTo(request.them);

        if (req.containsHeader("Range"))
            debug("send request " + request.method + " " + request.url + " " + req.getFirstHeader("Range"));
        else
            debug("send request " + request.method + " " + request.url);
        requestCache.put(uid, request);
        mChatSession.sendDataAsync(message, false, data);
    }

    private static String hexChr(int b) {
        return Integer.toHexString(b & 0xF);
    }

    private static String toHex(int b) {
        return hexChr((b & 0xF0) >> 4) + hexChr(b & 0x0F);
    }

    private String sha1sum(byte[] bytes) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(bytes, 0, bytes.length);
        byte[] sha1sum = digest.digest();
        String display = "";
        for(byte b : sha1sum)
            display += toHex(b);
        return display;
    }

    private String sha1sum(java.io.InputStream is) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");

            DigestInputStream dig = new DigestInputStream(is, digest);
            IOUtils.copy( dig, new NullOutputStream() );

            byte[] sha1sum = digest.digest();
            String display = "";
            for(byte b : sha1sum)
                display += toHex(b);
            return display;
        }
        catch (Exception npe)
        {
            Log.e(ImApp.LOG_TAG,"unable to hash file",npe);
            return null;
        }

    }


    private void debug (String msg)
    {
        if (Debug.DEBUG_ENABLED)
            Log.d("OTRDATA",msg);
    }
}
