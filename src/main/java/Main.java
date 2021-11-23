import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.index.qual.NonNegative;

import java.time.*;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Main {

    public static void main(String[] args) {
        MutableClock clock = new MutableClock();

        LoadingCache<String, String> cache = Caffeine.newBuilder()
                .expireAfter(new TheExpiry(1500))
                .ticker(() -> MILLISECONDS.toNanos(clock.millis()))
                .evictionListener((key, value, cause) -> System.out.printf("Evicting because %s%n", cause))
                .removalListener((key, value, cause) -> System.out.printf("Removing because %s%n", cause))
                .buildAsync(key -> clock.instant().toString())
                .synchronous();

        // First time everything should be fine
        System.out.println(await(cache, Instant.EPOCH.toString()));

        // advance the clock to 1s > the old value, not expired
        clock.setSeconds(1);
        System.out.println(await(cache, Instant.EPOCH.toString()));

        // advance the clock to 2s > the new value should be loaded
        clock.setSeconds(2);
        String result = await(cache, Instant.EPOCH.plusSeconds(2).toString());
        System.out.println(result);

        if (!Instant.EPOCH.plusSeconds(2).toString().equals(result)) {
            System.out.println("Reproduced...");
            cache.cleanUp();
            result = await(cache, Instant.EPOCH.plusSeconds(2).toString());
            if (!Instant.EPOCH.plusSeconds(2).toString().equals(result)) {
                System.out.println("Clean up did not work...");
                while (true) {
                    // Wait for debugger to connect.
                    LockSupport.parkNanos(100);
                }
            }
        }
    }

    private static String await(LoadingCache<String, String> cache, String expected) {
        long start = Clock.systemUTC().millis();
        while (!expected.equals(cache.get("")) && Clock.systemUTC().millis() - start < 500) {
            /*
             * Cache is updated through async operations, the exact timing of which varies.
             * For this reason we retry if necessary.
             */
            LockSupport.parkNanos(10);
        }
        System.out.printf("Waited for %sms\n", Clock.systemUTC().millis() - start);
        return cache.get("");
    }

    private static class TheExpiry implements Expiry<String, String> {
        final long ms;

        private TheExpiry(long ms) {
            this.ms = ms;
        }

        @Override
        public long expireAfterCreate(String key, String value, long currentTime) {
            return MILLISECONDS.toNanos(ms);
        }

        @Override
        public long expireAfterUpdate(String key, String value, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(String key, String value, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }
    }

    private static class MutableClock extends Clock {
        Instant instant = Instant.EPOCH;

        public void setSeconds(int seconds) {
            this.instant = Instant.EPOCH.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zoneId) {
            throw new UnsupportedOperationException();
        }
    }
}
