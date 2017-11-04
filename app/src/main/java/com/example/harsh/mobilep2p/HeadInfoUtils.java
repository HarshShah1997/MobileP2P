package com.example.harsh.mobilep2p;
import java.io.Serializable;
import java.util.*;


/**
 * Created by dell on 10/29/2017.
 */

public class HeadInfoUtils implements Serializable {

    private List<FileMetadata> files = new ArrayList<FileMetadata>();

    private Map<String, List<FileMetadata>> nodesContent = new HashMap<>();

    private Map<FileMetadata, List<String>> fileLocations = new HashMap<>();

    public List<FileMetadata> getFiles() {
        return files;
    }

    public Map<String, List<FileMetadata>> getNodesContent() {
        return nodesContent;
    }

    public Map<FileMetadata, List<String>> getFileLocations() {
        return fileLocations;
    }

    public void addFilesList(List<FileMetadata> receivedFileList, String node) {
        for (FileMetadata receivedFile : receivedFileList) {
            addFileInfo(receivedFile, node);
        }
    }

    public void addFileInfo(FileMetadata receivedFile, String node) {
        for (FileMetadata f : files) {
            if (f.getFileName().equals(receivedFile.getFileName()) && f.getFileSize() == receivedFile.getFileSize()) {
                addFileLocations(receivedFile, node);
                addNodesContent(receivedFile, node);
                return;
            }
        }

        files.add(receivedFile);
        addFileLocations(receivedFile,node);
        addNodesContent(receivedFile,node);
    }

    public void addFileLocations(FileMetadata receivedFile, String node){
        if(fileLocations.containsKey(receivedFile) && !fileLocations.get(receivedFile).contains(node)) {
            fileLocations.get(receivedFile).add(node);
        } else {
            List<String> list = new ArrayList<>();
            list.add(node);
            fileLocations.put(receivedFile, list);
        }
    }

    public void addNodesContent(FileMetadata receivedFile, String node){
        if(nodesContent.containsKey(node) && !nodesContent.get(node).contains(receivedFile)){
            nodesContent.get(node).add(receivedFile);
        } else {
            List<FileMetadata> list = new ArrayList<>();
            list.add(receivedFile);
            nodesContent.put(node, list);
        }
    }

    public FileMetadata getFile(String fileName, long fileSize){
        for (FileMetadata f : files){
            if (f.getFileName().equals(fileName) && f.getFileSize() == fileSize) {
                return f;
            }
        }
        throw new IllegalArgumentException("no such file present");
    }

    public List<String> getNodesContainingFile(String fileName, long fileSize){
        FileMetadata f1 = getFile(fileName, fileSize);

        if (fileLocations.containsKey(f1)) {
            return fileLocations.get(f1);
        } else {
            throw new IllegalArgumentException("no such file present at any node");
        }
    }

    public List<FileMetadata> getListOfFiles() {
        return files;
    }

    public void clear() {
        files = new ArrayList<>();
        nodesContent = new HashMap<>();
        fileLocations = new HashMap<>();
    }
}
