package com.example.harsh.mobilep2p.info;

import android.widget.LinearLayout;

import com.example.harsh.mobilep2p.types.FileMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Harsh on 11-Nov-17.
 */

public class FileStatusInfo {

    private Map<FileMetadata, String> fileStatuses = new HashMap<>();
    private Map<FileMetadata, LinearLayout> tableRowMap = new HashMap<>();

    public String getFileStatus(FileMetadata file) {
        return fileStatuses.get(file);
    }

    public void setFileStatus(FileMetadata file, String status) {
        fileStatuses.put(file, status);
    }

    public LinearLayout getFileRow(FileMetadata file) {
        return tableRowMap.get(file);
    }

    public void setFileRow(FileMetadata file, LinearLayout row) {
        tableRowMap.put(file, row);
    }
}
