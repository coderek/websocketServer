/**
 * Created by coderek on 29/03/17.
 */

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

public class PlayReactive {

    static class TestThread extends Thread {

        ObservableEmitter<Integer> obs;


        TestThread(ObservableEmitter<Integer> _obs) {
            obs = _obs;
        }

        public void run() {
            for (int i = 0; i < 10; i++) {
                obs.onNext(i);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {

                }
            }
        }
    }


    public static void main(String... args) {
        Observable.<Integer>create((sub) -> new TestThread(sub).start()).subscribe(
                (i) -> System.out.println(i),
                (e) -> e.printStackTrace(),
                () -> System.out.println("Done")
        );
    }
}
