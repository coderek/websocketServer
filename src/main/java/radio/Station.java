package radio;

import io.reactivex.Observable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Station {
    private Logger logger = Logger.getLogger("derek");
    private String name = null;
    private String url = null;
    private boolean terminated = false;
    private boolean paused = false;
    public Station(String n, String u) {
        name = n;
        url = u;

        Observable.interval(0, 2, TimeUnit.HOURS).subscribe(t-> {
            logger.info("Pausing "+ getName());
            setPaused(true);
        });
    }

    public void start() {
        terminated = false;
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public String toString() {
        return name;
    }

    public HttpURLConnection connect() {
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            return conn;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Station url can't connect.", e);
        }
        return null;
    }

}
