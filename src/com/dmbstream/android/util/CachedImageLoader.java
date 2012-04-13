package com.dmbstream.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.dmbstream.android.helpers.StreamHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

public class CachedImageLoader {
    
    private static final String TAG = CachedImageLoader.class.getSimpleName();
	private MemoryCache memoryCache=new MemoryCache();
    private FileCache fileCache;
    private DisplayMetrics metrics;
    
    public CachedImageLoader(Context context, DisplayMetrics displayMetrics){
        fileCache=new FileCache(context);
        metrics = displayMetrics;
    }

    public Bitmap getImage(String url) {
    	Log.d(TAG, "getImage: " + url);
    	
    	Bitmap bitmap = memoryCache.get(url);
    	
    	if (bitmap == null) {
    		bitmap = getBitmap(url);
    		memoryCache.put(url, bitmap);
    	}
    	
    	return bitmap;
    }
    
    private Bitmap getBitmap(String url) 
    {
    	Log.d(TAG, "getBitmap from file cache: " + url);
        File f=fileCache.getFile(url);
        
        //from SD cache
        Bitmap bitmap = getBitmapFromFile(f);
        if(bitmap != null)
            return bitmap;
        
        //from web
        try {
        	Log.d(TAG, "getBitmap from web: " + url);
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            InputStream is = conn.getInputStream();
            if (!f.exists() && f.canWrite()) {
            	Log.d(TAG, "File doesn't exist, creating");
            	f.createNewFile();
            	Log.d(TAG, "Done");
            }
            if (f.exists()) {
                OutputStream os = new FileOutputStream(f);
                StreamHelper.CopyStream(is, os);
                os.close();

            	bitmap = getBitmapFromFile(f);
            }
            else {
            	Log.d(TAG, "File still doesn't exist, just return bitmap from stream");
            	bitmap = getBitmapFromStream(is);
            }
            
            return bitmap;
        } catch (Exception ex){
        	Log.d(TAG, "Error getBitmap from web: " + ex);
           return null;
        }
    }
    
    private Bitmap getBitmapFromFile(File file) {
        try {
			return getBitmapFromStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
		}
		return null;
    }
    private Bitmap getBitmapFromStream(InputStream stream) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inTargetDensity = metrics.densityDpi;
        o.inDensity = DisplayMetrics.DENSITY_LOW;
		return BitmapFactory.decodeStream(stream,null,o);
    }

    public void clearCache() {
    	Log.d(TAG, "clearCache");
    	if (memoryCache != null)
    		memoryCache.clear();
    	if (fileCache != null)
    		fileCache.clear();
    }
}
