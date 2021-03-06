package com.focusings.focusingsworld.ImageAndTextList;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

public class AsyncImageLoader {
    private HashMap<String, SoftReference<Drawable>> imageCache;
    
    public AsyncImageLoader() {
    	imageCache= new HashMap<String, SoftReference<Drawable>>();
    }
 
    public Drawable loadDrawable(final String imageUrl, final ImageCallback imageCallback) {
    	final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                imageCallback.imageLoaded((Drawable) message.obj, imageUrl);
            }
        };
    	
    	if (imageCache.containsKey(imageUrl)) {
            SoftReference<Drawable> softReference = imageCache.get(imageUrl);
            Drawable drawable = softReference.get();
            if (drawable != null) {
                return drawable;
            }
        }else{
	        new Thread() {
	            @Override
	            public void run() {
	                Drawable drawable = loadImageFromUrl(imageUrl);
	                imageCache.put(imageUrl, new SoftReference<Drawable>(drawable));
	                Message message = handler.obtainMessage(0, drawable);
	                handler.sendMessage(message);
	            }
	        }.start();
        }
        return null;
    }
 
    public static Drawable loadImageFromUrl(String url) {
    	InputStream inputStream;
        try {
            inputStream = new URL(url).openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Drawable.createFromStream(inputStream, "src");
    }
 
    public interface ImageCallback {
        public void imageLoaded(Drawable imageDrawable, String imageUrl);
    }
}

