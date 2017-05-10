package radio;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/*
 * @author coderek, @date 19/03/17 1:12 AM
 */

public class Radio {
    private Logger logger = Logger.getLogger("derek");
    private Subject<Message> broadcaster;
    private final String[][] stationList = {
            {"Ria 89.7", "http://mediacorp.rastream.com/897fm"},
            {"Gold 90.5 FM", "http://mediacorp.rastream.com/905fm"},
            {"91.3 Hot FM", "http://sph.rastream.com/913fm"},
            {"Kiss 92.0", "http://sph.rastream.com/sph-kiss92"},
            {"Symphony 92.4", "http://mediacorp.rastream.com/924fm"},
            {"Yes 93.3", "http://mediacorp.rastream.com/933fm"},
            {"93.8 Live", "http://mediacorp.rastream.com/938fm"},
            {"Warna 94.2", "http://mediacorp.rastream.com/942fm"},
            {"Class 95.0", "http://mediacorp.rastream.com/950fm"},
            {"95.8 Capital", "http://mediacorp.rastream.com/958fm"},
            {"XFM 96.3", "http://mediacorp.rastream.com/963fm"},
            {"Love 97.2", "http://mediacorp.rastream.com/972fm"},
            {"98.7 FM", "http://mediacorp.rastream.com/987fm"},
            {"Lush 99.5", "http://mediacorp.rastream.com/995fm"},
            {"UFM 100.3", "http://sph.rastream.com/1003fm"},
    };
    private Station[] stations = new Station[stationList.length];
    private Executor executor = Executors.newFixedThreadPool(stationList.length);
    private static Radio instance = null;

    public static Radio getInstance() {
        if (instance == null) {
            instance = new Radio();
        }
        return instance;
    }

    private Radio() {
        // only radio can access this map
        broadcaster = BehaviorSubject.<Message>create().toSerialized();
        initializeStations();
        logger.info("Radio started.");
    }

    public Optional<Observable<String>> getObservableStream(String freq) {
        if (!Pattern.matches("[\\d\\.]+", freq)) return Optional.empty();

        for (Station s: stations) {
            if (s.getName().indexOf(freq)!=-1) {
                logger.info("Getting observable stream for: " + s.getName());
                s.start();
                synchronized (s) {
                    s.notify();
                }
                return Optional.of(broadcaster
                        .filter(m-> m.getStationName().contains(freq))
                        .flatMap(m-> Observable.just(m.getTitle(), m.getBody()))
                );
            }
        }
        return Optional.empty();
    }

    private void initializeStations() {
        for (int i=0;i<stationList.length;i++) {
            Station s = new Station(stationList[i][0], stationList[i][1]);
            stations[i] = s;
            executor.execute(new RadioInfoCrawler(s, broadcaster));
        }
    }

    public static void main(String... args) {
        Radio r = Radio.getInstance();
        r.getObservableStream("93.3").get().subscribe(System.out::println);
    }
}
