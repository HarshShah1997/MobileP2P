package com.example.harsh.mobilep2p.util;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Utils to handle requests and responses from the socket
 * Created by Harsh on 22-Jul-17.
 */

public class SocketUtils {

    private static final int BUFFER_SIZE = 1024;

    private static final String TAG = "SocketUtils";

    /**
     * Returns the input stream of the message from the socket
     *
     * @param clientSocket - Socket of the client
     * @return InputStream to read the request of the client
     * @throws IOException - Throws IOException if there is any error in getting the stream from socket
     */
    public InputStream receive(Socket clientSocket) throws IOException {
        if (clientSocket == null) {
            throw new IllegalArgumentException("Socket of client is null");
        }
        InputStream requestFromClient = clientSocket.getInputStream();
        Log.i(TAG, String.format("Getting Input Stream from Socket: %s", clientSocket));
        return requestFromClient;
    }

    /**
     * Sends the data to the socket
     *
     * @param clientSocket - Socket of the client
     * @param response     - InputStream of the data
     * @throws IOException - Throws IOException if there is any error in getting outputstream
     */
    public void send(Socket clientSocket, InputStream response) throws IOException {
        if (clientSocket == null) {
            throw new IllegalArgumentException("Client socket is null");
        } else if (response == null) {
            throw new IllegalArgumentException("InputStream of response is null");
        }
        OutputStream clientOutputStream = null;
        clientOutputStream = clientSocket.getOutputStream();
        Log.i(TAG, String.format("Getting Output Stream from socket: %s", clientSocket));
        copy(response, clientOutputStream);
        Log.i(TAG, String.format("Successfully sent data to socket: %s", clientSocket));
    }

    // Reads from input stream and writes to output stream
    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = inputStream.read(buffer);
        int offset = 0;
        while (bytesRead != -1) {
            outputStream.write(buffer, offset, bytesRead);
            if (inputStream.available() > 0) {
                bytesRead = inputStream.read(buffer);
            } else {
                break;
            }
        }
    }

    /**
     * Closes the socket
     * @param socket - socket to be closed
     * @throws IOException
     */
    public void closeSocket(Socket socket) throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    /**
     * Closes the server socket
     * @param serverSocket Server Socket to be closed
     * @throws IOException
     */
    public void closeServerSocket(ServerSocket serverSocket) throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    // For debugging purposes
    private void printBuffer(byte[] buffer, int bytesRead) {
        for (int i = 0; i < bytesRead; i++) {
            System.out.print((char) buffer[i]);
        }
        System.out.println("");
    }
}