package websocket;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.security.*;
import java.util.regex.*;
import java.net.*;
import java.nio.file.*;
import radio.*;

public class WebSocketConnection extends Thread implements RadioListener {
    private final String concat = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private String currentRadioName = null;

    private Socket sock = null;
    private OutputStream out;
    private Radio radio;
    private Worker worker = null;
    private MetaWrapper wrapper = null;

    public WebSocketConnection(Socket _sock, Radio _radio) {
        System.out.println("openning WebSocketConnection");
        sock = _sock;
        radio = _radio;
        wrapper = new MetaWrapper();
    }

    public void run() {
        try (
            InputStream in = sock.getInputStream();
            OutputStream out = sock.getOutputStream();
        ) {
            this.out = out;
            readConnection(in, out);
            System.out.println("Closing WebSocketConnection");
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void readConnection(InputStream in, OutputStream out) {
        Scanner scanner = new Scanner(in, "UTF-8").useDelimiter("\\r\\n\\r\\n");
        String data = scanner.next();
        Matcher get = Pattern.compile("^GET").matcher(data);

        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();
            String clientKey = match.group(1);

            doHandShake(clientKey, out);

            initListeningThread(out);
            int b = -1;
            List<Byte> bytes = new ArrayList<>();

            try {
                // message loop
                while ((b=in.read())!=-1) {
                    // one message by one message
                    // check first byte
                    boolean fin = b > 127;
                    int opcode = b & 0b1111;

                    switch (opcode) {
                        case 1:
                            // text
                            int maskAndLen = in.read();
                            int diff = maskAndLen & 0b1111111;
                            int len = 0;
                            if (diff <= 125 && diff >=0) {
                                len = diff;
                            } else if (diff == 126) {
                                len = in.read() * 256 + in.read();
                            } else if (diff == 127) {
                                for (int i=0;i<8;i++) {
                                    len = len * 256 + in.read();
                                }
                            }
                            System.out.println("String len: "+len);

                            if (maskAndLen <= 127) {
                                System.err.print("Client message is not masked");
                                bytes.clear();
                                continue;
                            }

                            byte[] mask = new byte[4];
                            for (int i=0;i<4;i++) {
                                mask[i] = (byte) in.read();
                            }

                            // decode xor
                            System.out.println("Decoding string");
                            for (int i=0;i<len;i++) {
                                int tByte = in.read();
                                bytes.add((byte)(tByte ^ mask[i & 0x3]));
                            }
                            break;
                        case 9: // Ping
                            pong(out);
                            continue;
                        case 8: // Close
                            System.out.println("Thread quit");
                            return;
                        default:
                            continue;
                    }

                    if (fin) {
                       byte[] strBytes = new byte[bytes.size()];
                       for (int i=0;i<bytes.size();i++) {
                           strBytes[i]= bytes.get(i);
                       }
                       processMessage(new String(strBytes), out);
                       bytes.clear();
                    }
                }
                System.out.println("before exit, just read "+b);
            } catch (IOException e) {
                System.out.println("Probably OutputStream is closed.");
                e.printStackTrace();
            }
        }

        System.out.println("Connection exited["+Thread.currentThread()+"]");
    }

    void initListeningThread(OutputStream out) {
        if (worker!=null) {
            worker.interrupt();
        }
        worker = new Worker(wrapper, out);
        worker.start();
    }

    void doHandShake(String clientKey, OutputStream out) {
        String key = clientKey + concat;
        // TODO should check origin

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(key.getBytes("UTF-8"));
            Base64.Encoder encoder = Base64.getEncoder();
            String digest = new String(encoder.encode(bytes));

            String response = String.format("HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade:websocket\r\n"+
                    "Connection: Upgrade\r\n"+
                    "Sec-WebSocket-Accept: %s\r\n\r\n", digest);
            out.write(response.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void pong(OutputStream out) throws IOException {
        byte[] response = new byte[1];
        response[0] = ((byte) 128) | 0xA; // Pong
        send(response, out);
    }

    void processMessage(String message, OutputStream out) {
        System.out.println("Got message: " + message);
        /*
         * Message consists of type and data, separated by |
         *
         * Types of message
         * 1. CHANGE_STATION (change station)
         */

        String[] parts = message.split("\\|", 2);
        if (parts.length!=2) return;
        switch (parts[0].trim().toUpperCase()) {
            case "CHANGE_STATION":
                System.out.println(parts[1]);
                // frequency string e.g. "93.3"
                String freq = parts[1];
                wrapper.resetQueue(radio.get(freq));
                initListeningThread(out);
                break;
        }
    }

    static void send(byte[] bytes, OutputStream out) throws IOException {
        synchronized(out) {    
            out.write(bytes);
            out.flush();
        }

    }

    static void sendString(String s, OutputStream out) throws IOException {
        System.out.println("sending " + s);

        byte[] bytes = null;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        long len = bytes.length;

        byte[] response;
        int dataStart = 2;
        if (len >=0 && len <=125) {
            response = new byte[bytes.length+2];
            response[1] = (byte) len;
        } else if (len < (1<<17)) {
            response = new byte[bytes.length+4];
            response[1] = (byte) 126;
            response[2] = (byte) (len >>> 8);
            response[3] = (byte) len;
            dataStart = 4;
        } else {
            response = new byte[bytes.length+10];
            response[1] = (byte) 127;
            for (int i=9;i>=2;i++) {
                response[i] = (byte) len;
                len >>>=8;
            }
            dataStart = 10;
        }
        response[0] = (byte) 0x81;
        System.out.println("bytes length: "+bytes.length);
        System.arraycopy(bytes, 0, response, dataStart, bytes.length);
        send(response, out);
    }


    static class MetaWrapper {
        List<String> metaQueue = new ArrayList<>();
        int cur = 0;

        boolean hasNext() {
            return cur < metaQueue.size();
        }

        String next() {
            return metaQueue.get(cur++);
        }

        void resetQueue(List<String> q) {
            synchronized(metaQueue ) {
                metaQueue.notify();
            }
            metaQueue = q;
            cur = Math.max(0, q.size() - 1);
        }
    }
}
