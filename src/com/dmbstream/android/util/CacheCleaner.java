package com.dmbstream.android.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.os.StatFs;
import com.dmbstream.android.service.DownloadFile;
import com.dmbstream.android.service.IDownloadService;

/**
 * @author Sindre Mehus
 * @version $Id: CacheCleaner.java 2303 2011-06-29 12:12:24Z sindre_mehus $
 */
public class CacheCleaner {

    private static final String TAG = CacheCleaner.class.getSimpleName();
    private static final double MAX_FILE_SYSTEM_USAGE = 0.95;

    private final Context context;
    private final IDownloadService downloadService;

    public CacheCleaner(Context context, IDownloadService downloadService) {
        this.context = context;
        this.downloadService = downloadService;
    }

    public void clean() {

        Log.i(TAG, "Starting cache cleaning.");

        if (downloadService == null) {
            Log.e(TAG, "DownloadService not set. Aborting cache cleaning.");
            return;
        }

        try {

            List<File> files = new ArrayList<File>();
            List<File> dirs = new ArrayList<File>();

            findCandidatesForDeletion(FileUtil.getMusicDirectory(context), files, dirs);
            sortByAscendingModificationTime(files);

            Set<File> undeletable = findUndeletableFiles();

            deleteFiles(files, undeletable);
            deleteEmptyDirs(dirs, undeletable);
            Log.i(TAG, "Completed cache cleaning.");

        } catch (RuntimeException x) {
            Log.e(TAG, "Error in cache cleaning.", x);
        }
    }

    private void deleteEmptyDirs(List<File> dirs, Set<File> undeletable) {
        for (File dir : dirs) {
            if (undeletable.contains(dir)) {
                continue;
            }

            File[] children = dir.listFiles();

            // Delete empty directory and associated album artwork.
            if (children.length == 0) {
                Util.delete(dir);
            }
        }
    }

    private void deleteFiles(List<File> files, Set<File> undeletable) {

        if (files.isEmpty()) {
            return;
        }

        long cacheSizeBytes = Util.getCacheSizeMB(context) * 1024L * 1024L;

        long bytesUsedBySubsonic = 0L;
        for (File file : files) {
            bytesUsedBySubsonic += file.length();
        }

        // Ensure that file system is not more than 95% full.
        StatFs stat = new StatFs(files.get(0).getPath());
        long bytesTotalFs = (long) stat.getBlockCount() * (long) stat.getBlockSize();
        long bytesAvailableFs = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        long bytesUsedFs = bytesTotalFs - bytesAvailableFs;
        long minFsAvailability = Math.round(MAX_FILE_SYSTEM_USAGE * (double) bytesTotalFs);

        long bytesToDeleteCacheLimit = Math.max(bytesUsedBySubsonic - cacheSizeBytes, 0L);
        long bytesToDeleteFsLimit = Math.max(bytesUsedFs - minFsAvailability, 0L);
        long bytesToDelete = Math.max(bytesToDeleteCacheLimit, bytesToDeleteFsLimit);

        Log.i(TAG, "File system       : " + Util.formatBytes(bytesAvailableFs) + " of " + Util.formatBytes(bytesTotalFs) + " available");
        Log.i(TAG, "Cache limit       : " + Util.formatBytes(cacheSizeBytes));
        Log.i(TAG, "Cache size before : " + Util.formatBytes(bytesUsedBySubsonic));
        Log.i(TAG, "Minimum to delete : " + Util.formatBytes(bytesToDelete));

        long bytesDeleted = 0L;
        for (File file : files) {

            if (bytesToDelete > bytesDeleted || file.getName().endsWith(".partial") || file.getName().contains(".partial.")) {
                if (!undeletable.contains(file)) {
                    long size = file.length();
                    if (Util.delete(file)) {
                        bytesDeleted += size;
                    }
                }
            }
        }

        Log.i(TAG, "Deleted           : " + Util.formatBytes(bytesDeleted));
        Log.i(TAG, "Cache size after  : " + Util.formatBytes(bytesUsedBySubsonic - bytesDeleted));
    }

    private void findCandidatesForDeletion(File file, List<File> files, List<File> dirs) {
        if (file.isFile()) {
            String name = file.getName();
            boolean isCacheFile = name.endsWith(".partial") || name.contains(".partial.") || name.endsWith(".complete") || name.contains(".complete.");
            if (isCacheFile) {
                files.add(file);
            }
        } else {
            // Depth-first
            for (File child : FileUtil.listFiles(file)) {
                findCandidatesForDeletion(child, files, dirs);
            }
            dirs.add(file);
        }
    }

    private void sortByAscendingModificationTime(List<File> files) {
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                if (a.lastModified() < b.lastModified()) {
                    return -1;
                }
                if (a.lastModified() > b.lastModified()) {
                    return 1;
                }
                return 0;
            }
        });
    }

    private Set<File> findUndeletableFiles() {
        Set<File> undeletable = new HashSet<File>(5);

        for (DownloadFile downloadFile : downloadService.getDownloads()) {
            undeletable.add(downloadFile.getPartialFile());
            undeletable.add(downloadFile.getCompleteFile());
        }

        undeletable.add(FileUtil.getMusicDirectory(context));
        return undeletable;
    }
}
