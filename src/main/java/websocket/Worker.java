package websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static websocket.WebSocketConnection.MetaWrapper;

public class Worker extends Thread {
    private Logger logger = Logger.getLogger("derek");

    OutputStream out;
    MetaWrapper wrapper;

    public Worker(MetaWrapper w, OutputStream o) {
        out = o;
        wrapper = w;
    }

    public void run() {
        logger.info("worker started");
        String line = null;

        while (!Thread.interrupted()) {
            try {
                while (wrapper.hasNext()) {
                    line = wrapper.next();
                    logger.info(line);
                    WebSocketConnection.sendString(line, out);
                }

                synchronized (wrapper.metaQueue) {
                    wrapper.metaQueue.wait();
                }
                logger.info("New meta info is available");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                logger.log(Level.WARNING, "OutputStream is closed.", e);
                break;
            }
        }
        logger.info("Worker thread exited.");
    }
}
