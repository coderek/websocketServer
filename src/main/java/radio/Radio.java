package radio;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

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

    // station to current playing infos
    private Map<Station, Observable<String>> stations;

    private final String[] stationList = {
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
        startStations();
        logger.info("Radio started.");
    }

    public Optional<Observable<String>> getObservableStream(String freq) {
        if (!Pattern.matches("[\\d\\.]+", freq)) return Optional.empty();

        for (Station s: stations.keySet()) {
            if (s.name.indexOf(freq)!=-1) {
                return Optional.of(stations.get(s));
            }
        }
        return Optional.empty();
    }

    private void startStations() {
        for (int i=0;i<stationList.length;i+=2) {
            Station s = new Station(stationList[i], stationList[i+1]);
            stations.put(s, Observable.create((sub)->{
                if (!s.getInited())
                    new RadioInfoCrawler(s, sub).start();
            }));
        }
    }

    public static void main(String... args) {
        Radio r = new Radio();
        Logger logger = Logger.getLogger("derek");

        Optional<Observable<String>> o = r.getObservableStream("93.3");
        if (o.isPresent()) {
            Disposable d = o.get().subscribe(
                    System.out::println,
                    System.out::println,
                    System.out::println
            );
            logger.info("Number of active threads:" + java.lang.Thread.activeCount());
            d.dispose();
            o.get().subscribe(
                    System.out::println,
                    System.out::println,
                    System.out::println
            );
            logger.info("Number of active threads:" + java.lang.Thread.activeCount());

        }
    }
}
