package radio;

import io.reactivex.subjects.Subject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Logger;
/*
    http://www.smackfu.com/stuff/programming/shoutcast.html
*/

class RadioInfoCrawler implements Runnable {
    private Logger logger = Logger.getLogger("derek");

    private final Station station;
    private Subject<Message> emitter;

    RadioInfoCrawler(Station s, Subject<Message> _emitter) {
        station = s;
        emitter = _emitter;
    }

    public void run() {
        logger.info("Start thread[" + Thread.currentThread().getId() + "]");
        while (!station.isTerminated()) {
            if (station.isPaused()) {
                logger.info("Station " + station.getName() + " is paused.");
            }
            try {
                logger.info("Waiting to start");
                synchronized (station) {
                    station.wait();
                }
                startStreamming();
            } catch (InterruptedException e) {
            }
        }
        logger.info("Thread Exited[" + Thread.currentThread().getId() + "], station[" + station.getName() + "]");
    }

    private void startStreamming() {
        logger.info("================= Start Streaming from " + station.toString());
        InputStream in = null;

        HttpURLConnection conn = station.connect();
        if (conn == null) return;

        // set header that is required for getting meta data
        conn.setDoOutput(true);
        conn.setRequestProperty("Icy-MetaData", "1");

        int i = 1;
        int metaInt = 0;
        String key = null;
        // getting response header
        while ((key = conn.getHeaderFieldKey(i)) != null) {
            String val = conn.getHeaderField(i);
            if (key.equals("icy-metaint")) {
                metaInt = Integer.parseInt(val);
            }
            i++;
        }
        logger.info("meta int: " + metaInt);

        try {
            in = conn.getInputStream();
        } catch (IOException e) {
            logger.warning("Input is closed unexpectedly.");
            e.printStackTrace();
            return;
        }

        while (!station.isPaused() && !station.isTerminated()) {
            try {
                long skipped = in.skip(metaInt);
                assert skipped == metaInt;

                // start reading meta info
                int len = in.read();
                assert len != -1;

                if (len > 0) {
                    len = len * 16;

                    byte[] bytes = new byte[len];
                    int cur = 0;
                    while (in.available() > 0 && cur < len) {
                        int avail = Math.min(in.available(), len - cur);

                        // non blocking
                        while (avail > 0) {
                            bytes[cur] = (byte) in.read();
                            avail--;
                            cur++;
                        }
                    }

                    if (cur < len) {
                        in.read(bytes, cur, len - cur);
                    }

                    assert len == cur: "Supposed to read " + len + " bytes. Actually read " + cur + " bytes";

                    String text = new String(bytes).trim();
                    // maybe all spaces
                    if (text.isEmpty()) continue;
                    process(text);
                }
            } catch (UnsupportedEncodingException e) {
                break;
            } catch (IOException e) {
                break;
            }
        }

        try {
            in.close();
        } catch (IOException e) {

        }
        conn.disconnect();
    }

    private void process(String s) throws UnsupportedEncodingException {
        String[] segments = s.split(";");
        Message msg = null;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("StreamTitle")) {
                if (msg != null) emitter.onNext(msg);
                msg = new Message(station.getName());
                msg.setTitle(segments[i]);
            }
            if (segments[i].startsWith("StreamUrl")) {
                msg.setBody(segments[i]);
            }
            System.out.println("["+ LocalDateTime.now() + "]" +segments[i]);
        }
        if (msg!=null) emitter.onNext(msg);
    }
}
