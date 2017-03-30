package websocket;

import radio.Radio;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.*;

public class WebSocketServer {
    private Logger logger = Logger.getLogger("derek");

    public static void main(String[] args) {
        startRadio();
    }

    public static void startRadio() {
        try {
            Handler fileHandler = new FileHandler("%t/radio.log");
            fileHandler.setFormatter(new SimpleFormatter());

            Logger.getLogger("derek").addHandler(fileHandler);

            Radio radio = new Radio();
            // start socket server
            WebSocketServer server = new WebSocketServer();
            server.startServer(4201, radio);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startServer(int port, Radio radio) {
        logger.setUseParentHandlers(false);
        try (
                ServerSocket serverSocket = new ServerSocket(port);
        ) {
            while (true) {
                new WebSocketConnection(serverSocket.accept(), radio).start();
                logger.info("Number of active threads:" + java.lang.Thread.activeCount());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create socket", e);
        }
    }

}
