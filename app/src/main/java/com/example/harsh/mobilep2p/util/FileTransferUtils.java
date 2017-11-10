package com.example.harsh.mobilep2p.util;

import android.os.Environment;
import android.util.Log;

import com.example.harsh.mobilep2p.types.TransferRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by Harsh on 10/29/2017.
 */

public class FileTransferUtils {

    private static final int FILE_TRANSFER_PORT = 6579;
    private static final String READ_MODE = "r";
    private static final String WRITE_MODE = "rw";
    private static final int BUFFER_SIZE = 8192;
    private static final String DOWNLOAD_DIRECTORY = "/P2PDownload/";
    private static final String UPLOAD_DIRECTORY = "/Upload/";
    private static final String TAG = "FileTransferUtils";

    public void sendFile(TransferRequest transferRequest) throws IOException {
        Socket socket = null;
        OutputStream outgoingStream = null;
        RandomAccessFile file = null;

        try {
            socket = new Socket(transferRequest.getToIPAddress(), FILE_TRANSFER_PORT);
            Log.d(TAG, "Socket opened");
            outgoingStream = socket.getOutputStream();

            file = new RandomAccessFile(Environment.getExternalStorageDirectory() + UPLOAD_DIRECTORY + transferRequest.getFileName(), READ_MODE);
            file.seek(transferRequest.getStartOffset());

            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesRead = 0;
            while (bytesRead < transferRequest.getSize()) {
                int bytesToRead = getBytesToRead(BUFFER_SIZE, transferRequest.getSize() - bytesRead);
                int length = file.read(buffer, 0, bytesToRead);
                bytesRead += length;
                outgoingStream.write(buffer, 0, length);
            }
            Log.d(TAG, "Bytes Sent:" + bytesRead);
        } finally {
            closeFile(file);
            closeOutputStream(outgoingStream);
            closeSocket(socket);
        }
    }

    public void receiveFile(TransferRequest transferRequest) throws IOException {
        RandomAccessFile file = null;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        InputStream incomingStream = null;

        try {
            file = new RandomAccessFile(Environment.getExternalStorageDirectory() + DOWNLOAD_DIRECTORY + transferRequest.getFileName(), WRITE_MODE);
            FileChannel fileChannel = file.getChannel();
            fileChannel.position(transferRequest.getStartOffset());

            serverSocket = new ServerSocket(FILE_TRANSFER_PORT);
            clientSocket = serverSocket.accept();
            Log.d(TAG, "Client accepted");

            incomingStream = clientSocket.getInputStream();
            long bytesRead = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (bytesRead < transferRequest.getSize()) {
                int length = incomingStream.read(buffer);
                bytesRead += length;
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, length);
                while (byteBuffer.hasRemaining()) {
                    fileChannel.write(byteBuffer);
                }
            }
            Log.d(TAG, "Bytes written:" + bytesRead);
        } finally {
            closeInputStream(incomingStream);
            closeFile(file);
            closeSocket(clientSocket);
            closeServerSocket(serverSocket);
        }
    }

    private int getBytesToRead(int bufferSize, long remaining) {
        if (remaining > bufferSize) {
            return bufferSize;
        } else {
            return (int) remaining;
        }
    }

    private void closeInputStream(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    private void closeOutputStream(OutputStream outputStream) throws IOException {
        if (outputStream != null) {
            outputStream.close();
        }
    }

    private void closeFile(RandomAccessFile file) throws IOException {
        if (file != null) {
            file.close();
        }
    }

    private void closeSocket(Socket socket) throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    private void closeServerSocket(ServerSocket serverSocket) throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}