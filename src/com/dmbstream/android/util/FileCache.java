package com.dmbstream.android.util;

import java.io.File;
import android.content.Context;

public class FileCache {
    
    private File cacheDir;
    
    public FileCache(Context context){
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
        	File parentDir = new File(android.os.Environment.getExternalStorageDirectory(),"dmbstream");
        	if (!parentDir.exists() && parentDir.canWrite())
        		parentDir.mkdirs();
            cacheDir=new File(parentDir,"ImageCache");
        }
        else {
            cacheDir=context.getCacheDir();
        }
        if(!cacheDir.exists() && cacheDir.canWrite())
            cacheDir.mkdirs();
    }
    
    public File getFile(String url){
        //I identify images by hashcode. Not a perfect solution...
        String filename=String.valueOf(url.hashCode());
        File f = new File(cacheDir, filename);
        return f;
    }
    
    public void clear(){
    	if (!cacheDir.exists() || !cacheDir.canWrite())
    		return;
        File[] files=cacheDir.listFiles();
        for(File f:files)
            f.delete();
    }
}