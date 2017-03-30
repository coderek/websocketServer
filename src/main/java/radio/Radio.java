package radio;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/*
 * @author coderek, @date 19/03/17 1:12 AM
 */

public class Radio {
    private Logger logger = Logger.getLogger("derek");
    /**
     * Radio Class
     */

    private volatile RadioInfoCrawler crawler = null;

    // station to current playing infos
    private Map<Station, List<String>> stations;

    final String[] stationList = {
            "Ria 89.7", "http://mediacorp.rastream.com/897fm",
            "Gold 90.5 FM", "http://mediacorp.rastream.com/905fm",
            "91.3 Hot FM", "http://sph.rastream.com/913fm",
            "Kiss 92.0", "http://sph.rastream.com/sph-kiss92",
            "Symphony 92.4", "http://mediacorp.rastream.com/924fm",
            "Yes 93.3", "http://mediacorp.rastream.com/933fm",
            "93.8 Live", "http://mediacorp.rastream.com/938fm",
            "Warna 94.2", "http://mediacorp.rastream.com/942fm",
            "Class 95.0", "http://mediacorp.rastream.com/950fm",
            "95.8 Capital", "http://mediacorp.rastream.com/958fm",
            "XFM 96.3", "http://mediacorp.rastream.com/963fm",
            "Love 97.2", "http://mediacorp.rastream.com/972fm",
            "98.7 FM", "http://mediacorp.rastream.com/987fm",
            "Lush 99.5", "http://mediacorp.rastream.com/995fm",
            "UFM 100.3", "http://sph.rastream.com/1003fm",
    };


    public Radio() {
        // only radio can access this map
        stations = new HashMap<>();
        logger.info("Radio started.");
    }

    public void playStation(String freq) {
        if (!Pattern.matches("[\\d\\.]+", freq)) return;
        // short list
        for (int i = 0; i < stationList.length; i += 2) {
            if (stationList[i].indexOf(freq) != -1) {
                playStation(stationList[i], stationList[i + 1]);
                return;
            }
        }
    }

    public List<String> get(String freq) {
        List<String> ret = new ArrayList<>();
        ;
        if (!Pattern.matches("[\\d\\.]+", freq)) return ret;
        playStation(freq);
        // short list
        for (Station s : stations.keySet()) {
            if (s.name.indexOf(freq) != -1) return stations.get(s);
        }
        return ret;
    }

    private void playStation(int n) {
        if (n < 0 || 2 * n >= stationList.length) return;
        String stationName = stationList[n * 2];
        String stationUrl = stationList[n * 2 + 1];
        playStation(stationName, stationUrl);
    }

    private void playStation(String n, String u) {
        Station s = new Station(n, u);
        if (!stations.containsKey(s)) {
            logger.info("Station not init yet, init now");
            List<String> syncedList = Collections.synchronizedList(new ArrayList<String>());
            stations.put(s, syncedList);
            new RadioInfoCrawler(s, stations.get(s)).start();
        }
    }

    public static void main(String[] args) {
        Radio radio = new Radio();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {

        }
        radio.playStation(2);
    }
}
