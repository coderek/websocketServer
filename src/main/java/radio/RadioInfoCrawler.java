package radio;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import static java.nio.file.StandardWatchEventKinds.*;


/*
    http://www.smackfu.com/stuff/programming/shoutcast.html
*/

class RadioInfoCrawler extends Thread {
    Station station;
    // TODO clean up queue some time
    List<String> metaQueue;

    RadioInfoCrawler(Station s, List<String> mq) {
        station = s;
        metaQueue = mq;
    }

    public void run() {
        System.out.println("Start thread[" + Thread.currentThread().getId()+"]");

        stream_start:
        while (!station.isTerminated()) {
            System.out.println("================= Starting to listen to "+station.toString());

            HttpURLConnection conn = station.connect();
            if (conn==null) continue;

            // set header that is required for getting meta data
            conn.setDoOutput(true);
            conn.setRequestProperty("Icy-MetaData", "1");

            int i=1;
            int metaint = 0;
            String key = null;
            // getting response header
            while ((key=conn.getHeaderFieldKey(i))!=null) {
                String val = conn.getHeaderField(i);
                if (key.equals("icy-metaint")) {
                    metaint = Integer.parseInt(val);
                }
                i++;
            }

            InputStream in=null;
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                System.out.println("Input is closed restart");
                e.printStackTrace();
                continue;
            }

            message_loop:
            while (true) {
                try {
                    long skipped = in.skip(metaint);
                    assert skipped == metaint;

                    // start reading meta info
                    int len = in.read();
                    assert len != -1;

                    if (len>0) {
                        len = len * 16;

                        byte[] bytes = new byte[len];
                        int cur = 0;
                        while (in.available()>0 && cur < len) {
                            int avail = Math.min(in.available(), len - cur);

                            // non blocking
                            while (avail>0) {
                                bytes[cur] = (byte) in.read();
                                avail--;
                                cur++;
                            }

                            if (Thread.interrupted()) {
                                System.out.println("reading is interrupted");
                                break message_loop;
                            }
                        }

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
        }
        System.out.println("Thread Exited[" + Thread.currentThread().getId()+"], station["+station.name+"]");
    }

    private void process(String s) throws UnsupportedEncodingException {
        String[] segments = s.split(";");

        for (int i=0;i<segments.length;i++) {
            synchronized (metaQueue) {
                metaQueue.add(segments[i]);
                metaQueue.notifyAll();
            }
        }
    }
}
