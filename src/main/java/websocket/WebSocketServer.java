package websocket;

import radio.Radio;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.*;

/**
 * TODO
 *
 * 1. add route to each station
 * 2. add option to mute talkings
 * 3. add option to shuffle stations
 */


public class WebSocketServer {
    private Logger logger = Logger.getLogger("derek");

    public static void main(String[] args) {
        startRadio();
    }

    private Executor executor = Executors.newCachedThreadPool();

    public static void startRadio() {
        try {
            Handler fileHandler = new FileHandler("%t/radio.log");
            fileHandler.setFormatter(new SimpleFormatter());

            Logger.getLogger("derek").addHandler(fileHandler);

            Radio radio = Radio.getInstance();
            // start socket server
            WebSocketServer server = new WebSocketServer();
            server.startServer(4201, radio);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startServer(int port, Radio radio) {
        boolean isDebugging = System.getenv().getOrDefault("debugging", "false").equals("true");
        if (!isDebugging)
            logger.setUseParentHandlers(false);

        try (
                ServerSocket serverSocket = new ServerSocket(port);
        ) {
            while (true) {
                executor.execute(new WebSocketConnection(serverSocket.accept(), radio));
                logger.info("Number of active threads:" + java.lang.Thread.activeCount());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create socket", e);
        }
    }

}
