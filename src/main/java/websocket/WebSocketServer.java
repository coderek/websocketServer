package websocket;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import radio.*;

public class WebSocketServer {

    public static void main(String[] args) {
        Radio radio = new Radio();

        // start socket server
        WebSocketServer server = new WebSocketServer();
        server.startServer(4201, radio);
    }

    public void startServer(int port, Radio radio) {
        try (
            ServerSocket serverSocket = new ServerSocket(port);
        ) {
            while (true) {
                new WebSocketConnection(serverSocket.accept(), radio).start();
                System.out.println("Number of active threads:" + java.lang.Thread.activeCount());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
