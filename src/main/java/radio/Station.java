package radio;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class Station {
    public String name=null;
    public String url=null; 
    private boolean terminated = false;
    private BlockingQueue<String> queue = new ArrayBlockingQueue<String>(20);

    public Station() {}

    public Station(String n, String u) {
        name = n;
        url = u;
    }

    public void terminate() {
        synchronized (this) {
            terminated = true;
        }
    }

    public boolean isTerminated() {
        synchronized(this) {
            return terminated;
        }
    } 
    public String toString() {
        return name;
    }

    public synchronized void listenTo(String name, String url) {
        this.name = name;
        this.url = url;
        System.out.println("Listening to "+this.name+" "+this.url);
        notifyAll();
    }

    public HttpURLConnection connect() {
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void setCurrentPlaying(String[] properties) {    
        try {
            queue.put(String.join("|", properties));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public BlockingQueue<String> getQueue() {
        return queue;
    }

    public boolean equals(Object o) {
        Station st = (Station) o;
        return st.url.equals(url);
    }

    public int hashCode() {
        return 1;
    }
}
