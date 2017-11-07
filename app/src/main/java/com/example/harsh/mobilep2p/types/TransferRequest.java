package com.example.harsh.mobilep2p.types;

import java.io.Serializable;

/**
 * Created by Harsh on 07-Nov-17.
 */

public class TransferRequest implements Serializable {

    private String fileName;

    private String fromIPAddress;

    private String toIPAddress;

    private long startOffset;

    private long size;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFromIPAddress() {
        return fromIPAddress;
    }

    public void setFromIPAddress(String fromIPAddress) {
        this.fromIPAddress = fromIPAddress;
    }

    public String getToIPAddress() {
        return toIPAddress;
    }

    public void setToIPAddress(String toIPAddress) {
        this.toIPAddress = toIPAddress;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
