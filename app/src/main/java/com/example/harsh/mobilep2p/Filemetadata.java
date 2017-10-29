package com.example.harsh.mobilep2p;

import java.util.ArrayList;

/**
 * Created by dell on 10/29/2017.
 */

public class Filemetadata {
   private String filename;
    private int filesize;
  // private ArrayList<String>nodeadresses =new ArrayList<String>();

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFilesize(int filesize) {
        this.filesize = filesize;
    }

    public String getFilename() {

        return filename;
    }

    public int getFilesize() {
        return filesize;
    }
}
