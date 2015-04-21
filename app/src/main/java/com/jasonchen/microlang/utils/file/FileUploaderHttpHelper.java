package com.jasonchen.microlang.utils.file;

/**
 * jasonchen
 * 2015/04/10
 */
public class FileUploaderHttpHelper {

    public static interface ProgressListener {
        public void transferred(long data);
        public void waitServerResponse();
        public void completed();
    }
}
