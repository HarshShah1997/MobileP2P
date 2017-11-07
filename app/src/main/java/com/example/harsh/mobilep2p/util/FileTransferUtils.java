package com.example.harsh.mobilep2p.util;

import com.example.harsh.mobilep2p.types.TransferRequest;

import java.io.File;
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
    private static final String WRITE_MODE = "w";
    private static final int BUFFER_SIZE = 4096;

    public void sendFile(TransferRequest transferRequest) throws IOException {

        Socket socket = new Socket(transferRequest.getToIPAddress(), FILE_TRANSFER_PORT);
        OutputStream outgoingStream = socket.getOutputStream();

        RandomAccessFile file = new RandomAccessFile(transferRequest.getFileName(), READ_MODE);
        file.seek(transferRequest.getStartOffset());

        byte[] buffer = new byte[BUFFER_SIZE];
        long bytesRead = 0;
        while (bytesRead < transferRequest.getSize()) {
            bytesRead += file.read(buffer);
            outgoingStream.write(buffer);
        }
        file.close();
        outgoingStream.close();
        socket.close();
    }

    public void receiveFile(TransferRequest transferRequest) throws IOException {
        RandomAccessFile file = new RandomAccessFile(transferRequest.getFileName(), WRITE_MODE);
        FileChannel fileChannel = file.getChannel();
        fileChannel.position(transferRequest.getStartOffset());

        ServerSocket serverSocket = new ServerSocket(FILE_TRANSFER_PORT);
        Socket clientSocket = serverSocket.accept();

        InputStream incomingStream = clientSocket.getInputStream();
        long bytesRead = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        while (bytesRead < transferRequest.getSize()) {
            bytesRead += incomingStream.read(buffer);
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            while (byteBuffer.hasRemaining()) {
                fileChannel.write(byteBuffer);
            }
        }
        incomingStream.close();
        fileChannel.close();
        clientSocket.close();
        serverSocket.close();
    }
}
