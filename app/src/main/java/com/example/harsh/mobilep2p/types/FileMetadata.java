package com.example.harsh.mobilep2p.types;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileMetadata that = (FileMetadata) o;

        return (fileName.equals(that.fileName)) && (fileSize == that.fileSize);

    }

    @Override
    public int hashCode() {
        int result = fileName.hashCode();
        result = 31 * result + (int) (fileSize ^ (fileSize >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return fileName;
    }
}
