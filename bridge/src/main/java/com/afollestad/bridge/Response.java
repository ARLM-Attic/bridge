package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Response implements AsResults {

    private final String mUrl;
    private final byte[] mData;
    private final int mCode;
    private final String mMessage;
    private Bitmap mBitmapCache;
    private Map<String, List<String>> mHeaders;

    protected Response(byte[] data, String url, HttpURLConnection conn) throws IOException {
        mData = data;
        mUrl = url;
        mCode = conn.getResponseCode();
        mMessage = conn.getResponseMessage();
        mHeaders = conn.getHeaderFields();
    }

    public String url() {
        return mUrl;
    }

    public int code() {
        return mCode;
    }

    public String phrase() {
        return mMessage;
    }

    public String header(String name) {
        return mHeaders.get(name).get(0);
    }

    public List<String> headerList(String name) {
        return mHeaders.get(name);
    }

    public int contentLength() {
        String contentLength = header("Content-Length");
        if (contentLength == null) return -1;
        return Integer.parseInt(contentLength);
    }

    public String contentType() {
        return header("Content-Type");
    }

    public boolean isSuccess() {
        return mCode >= 200 && mCode < 300;
    }

    public Response throwIfNotSuccess() throws BridgeException {
        if (!isSuccess())
            throw new BridgeException(this, "Response status code was not successful.", BridgeException.REASON_RESPONSE_UNSUCCESSFUL);
        return this;
    }

    public byte[] asBytes() {
        return mData;
    }

    public String asString() {
        try {
            final byte[] bytes = asBytes();
            if (bytes == null || bytes.length == 0) return null;
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public Spanned asHtml() {
        final String content = asString();
        if (content == null)
            return null;
        return Html.fromHtml(content);
    }

    @Override
    public Bitmap asBitmap() {
        if (mBitmapCache == null) {
            final InputStream is = new ByteArrayInputStream(asBytes());
            mBitmapCache = BitmapFactory.decodeStream(is);
            Util.closeQuietly(is);
        }
        return mBitmapCache;
    }

    public JSONObject asJsonObject() throws BridgeException {
        final String content = asString();
        if (content == null)
            throw new BridgeException(this, "No content was returned in this response.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        try {
            return new JSONObject(content);
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    public JSONArray asJsonArray() throws BridgeException {
        final String content = asString();
        if (content == null)
            throw new BridgeException(this, "No content was returned in this response.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        try {
            return new JSONArray(content);
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    public void asFile(File destination) throws BridgeException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(destination);
            os.write(asBytes());
            os.flush();
        } catch (IOException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_IOERROR);
        } finally {
            Util.closeQuietly(os);
        }
    }

    @Override
    public String toString() {
        return String.format("%s, %d %s, %d bytes", mUrl, mCode, mMessage, mData != null ? mData.length : 0);
    }
}
