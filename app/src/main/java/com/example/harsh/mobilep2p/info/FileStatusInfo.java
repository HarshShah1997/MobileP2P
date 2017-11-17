package com.example.harsh.mobilep2p.info;

import android.widget.LinearLayout;
import android.widget.TableRow;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Harsh on 11-Nov-17.
 */

public class FileStatusInfo {

    private Map<String, String> fileStatuses = new HashMap<>();
    private Map<String, LinearLayout> tableRowMap = new HashMap<>();

    public String getFileStatus(String fileName, long fileSize) {
        return fileStatuses.get(generateString(fileName, fileSize));
    }

    public void setFileStatus(String fileName, long fileSize, String status) {
        fileStatuses.put(generateString(fileName, fileSize), status);
    }

    public LinearLayout getFileRow(String fileName, long fileSize) {
        return tableRowMap.get(generateString(fileName, fileSize));
    }

    public void setFileRow(String fileName, long fileSize, LinearLayout row) {
        tableRowMap.put(generateString(fileName, fileSize), row);
    }

    private String generateString(String fileName, long fileSize) {
        return fileName + fileSize;
    }
}
