package com.example.harsh.mobilep2p;

import java.io.Serializable;

/**
 * Created by dell on 10/29/2017.
 */

public class FileMetadata implements Serializable {

    private String fileName;

    private long fileSize;

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public void setFileSize(long fileSize) {

        this.fileSize = fileSize;
    }

    public String getFileName() {

        return fileName;
    }

    public long getFileSize() {

        return fileSize;
    }

    @Override
    public boolean equals(Object second) {
        FileMetadata otherFile = (FileMetadata) second;
        if (this.getFileName().equals(otherFile.getFileName()) && this.getFileSize() == otherFile.getFileSize()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return fileName;
    }
}
