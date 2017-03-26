package websocket;
import java.util.concurrent.*;
import java.io.*;
import radio.*;
import static websocket.WebSocketConnection.MetaWrapper;

public class Worker extends Thread {

    OutputStream out;
    MetaWrapper wrapper;
    public Worker(MetaWrapper w, OutputStream o) {
        out = o;
        wrapper = w;
    }

    public void run() {
        System.out.println("worker started");
        String line=null;

        while (!Thread.interrupted()) {
            try {
                while (wrapper.hasNext()) {
                    line = wrapper.next();
                    System.out.println(line);
                    WebSocketConnection.sendString(line, out);
                }

               synchronized (wrapper.metaQueue) {
                   wrapper.metaQueue.wait();
               }
               System.out.println("New meta info is available");
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (IOException e) {
                System.out.println("OutputStream is closed.");
                break;
            }
        }
        System.out.println("Worker thread exited.");
    }
}
