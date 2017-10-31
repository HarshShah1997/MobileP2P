package com.example.harsh.mobilep2p;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by dell on 10/29/2017.
 */

public class FileMetadata implements Serializable {

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

    @Override
    public boolean equals(Object second) {
        FileMetadata otherFile = (FileMetadata) second;
        if (this.getFilename().equals(otherFile.getFilename()) && this.getFilesize() == otherFile.getFilesize()) {
            return true;
        } else {
            return false;
        }
    }
}
