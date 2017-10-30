package com.example.harsh.mobilep2p;

import java.util.ArrayList;

/**
 * Created by dell on 10/29/2017.
 */

public class FileMetadata {

    private String filename;

    private long filesize;

    public void setFilename(String filename) {

        this.filename = filename;
    }

    public void setFilesize(long filesize) {

        this.filesize = filesize;
    }

    public String getFilename() {

        return filename;
    }

    public long getFilesize() {

        return filesize;
    }
}
