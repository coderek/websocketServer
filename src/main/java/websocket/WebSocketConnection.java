package websocket;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import radio.Radio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketConnection extends Thread {
    private Logger logger = Logger.getLogger("derek");
    private final String concat = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private String currentRadioName = null;

    private Socket sock = null;
    private OutputStream out;
    private Radio radio;
    private Disposable dis = null;

    public WebSocketConnection(Socket _sock, Radio _radio) {
        logger.info("openning WebSocketConnection");
        sock = _sock;
        radio = _radio;
    }

    public void run() {
        try (
                InputStream in = sock.getInputStream();
                OutputStream out = sock.getOutputStream();
        ) {
            this.out = out;
            readConnection(in, out);
            logger.info("Closing WebSocketConnection");
            sock.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "WebSocketConnection closed unexpectedly.", e);
        }
    }

    private void readConnection(InputStream in, OutputStream out) {
        Scanner scanner = new Scanner(in, "UTF-8").useDelimiter("\\r\\n\\r\\n");
        String data = scanner.next();
        Matcher get = Pattern.compile("^GET").matcher(data);

        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();
            String clientKey = match.group(1);

            doHandShake(clientKey, out);

            int b = -1;
            List<Byte> bytes = new ArrayList<>();

            try {
                // message loop
                while ((b = in.read()) != -1) {
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
                            if (diff <= 125 && diff >= 0) {
                                len = diff;
                            } else if (diff == 126) {
                                len = in.read() * 256 + in.read();
                            } else if (diff == 127) {
                                for (int i = 0; i < 8; i++) {
                                    len = len * 256 + in.read();
                                }
                            }
                            logger.info("String len: " + len);

                            if (maskAndLen <= 127) {
                                System.err.print("Client message is not masked");
                                bytes.clear();
                                continue;
                            }

                            byte[] mask = new byte[4];
                            for (int i = 0; i < 4; i++) {
                                mask[i] = (byte) in.read();
                            }

                            // decode xor
                            logger.info("Decoding string");
                            for (int i = 0; i < len; i++) {
                                int tByte = in.read();
                                bytes.add((byte) (tByte ^ mask[i & 0x3]));
                            }
                            break;
                        case 9: // Ping
                            pong(out);
                            continue;
                        case 8: // Close
                            logger.info("Thread quit");
                            return;
                        default:
                            continue;
                    }

                    if (fin) {
                        byte[] strBytes = new byte[bytes.size()];
                        for (int i = 0; i < bytes.size(); i++) {
                            strBytes[i] = bytes.get(i);
                        }
                        processMessage(new String(strBytes), out);
                        bytes.clear();
                    }
                }
                logger.info("before exit, just read " + b);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Probably OutputStream is closed.", e);
            }
        }

        logger.info("Connection exited[" + Thread.currentThread() + "]");
    }

    void initListeningThread(Observable<String> obs, OutputStream out) {
        if (dis!=null)
            dis.dispose();

        dis = obs.subscribe(
                (msg)-> WebSocketConnection.sendString(msg, out),
                (e)-> e.printStackTrace(),
                ()->{
                    System.out.println("Observable is closed");
                    dis.dispose();
                    dis=null;
                }
        );
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
                    "Upgrade:websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: %s\r\n\r\n", digest);
            out.write(response.getBytes("UTF-8"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Handshake failed.", e);
        }
    }

    void pong(OutputStream out) throws IOException {
        byte[] response = new byte[1];
        response[0] = ((byte) 128) | 0xA; // Pong
        send(response, out);
    }

    void processMessage(String message, OutputStream out) {
        logger.info("Got message: " + message);
        /*
         * Message consists of type and data, separated by |
         *
         * Types of message
         * 1. CHANGE_STATION (change station)
         */

        String[] parts = message.split("\\|", 2);
        if (parts.length != 2) return;
        switch (parts[0].trim().toUpperCase()) {
            case "CHANGE_STATION":
                logger.info(parts[1]);
                // frequency string e.g. "93.3"
                String freq = parts[1];
                Optional<Observable<String>> obs = radio.getObservableStream(freq);
                if (obs.isPresent()) {
                    initListeningThread(obs.get(), out);
                }
                break;
        }
    }

    static void send(byte[] bytes, OutputStream out) throws IOException {
        synchronized (out) {
            out.write(bytes);
            out.flush();
        }

    }

    static void sendString(String s, OutputStream out) throws IOException {
        Logger logger = Logger.getLogger("derek");
        logger.info("sending " + s);

        byte[] bytes = null;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, "Can't get bytes.", e);
            return;
        }
        long len = bytes.length;

        byte[] response;
        int dataStart = 2;
        if (len >= 0 && len <= 125) {
            response = new byte[bytes.length + 2];
            response[1] = (byte) len;
        } else if (len < (1 << 17)) {
            response = new byte[bytes.length + 4];
            response[1] = (byte) 126;
            response[2] = (byte) (len >>> 8);
            response[3] = (byte) len;
            dataStart = 4;
        } else {
            response = new byte[bytes.length + 10];
            response[1] = (byte) 127;
            for (int i = 9; i >= 2; i++) {
                response[i] = (byte) len;
                len >>>= 8;
            }
            dataStart = 10;
        }
        response[0] = (byte) 0x81;
        logger.info("bytes length: " + bytes.length);
        System.arraycopy(bytes, 0, response, dataStart, bytes.length);
        send(response, out);
    }

}
