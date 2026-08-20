package conc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WebCrawler {
    public interface HtmlParser {
        List<String> getUrls(String url);
    }

    public List<String> crawl(String startUrl, HtmlParser parser) {
        String host = hostOf(startUrl);
        Set<String> seen = ConcurrentHashMap.newKeySet();
        seen.add(startUrl);
        ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<>();
        q.add(startUrl);
        AtomicInteger pending = new AtomicInteger(1);
        CountDownLatch done = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                try {
                    String url = q.poll();
                    if (url == null) {
                        return;
                    }
                    for (String next : parser.getUrls(url)) {
                        if (!host.equals(hostOf(next))) {
                            continue;
                        }
                        if (seen.add(next)) {
                            q.add(next);
                            pending.incrementAndGet();
                            pool.execute(this);
                        }
                    }
                } finally {
                    if (pending.decrementAndGet() == 0) {
                        done.countDown();
                    }
                }
            }
        };
        pool.execute(worker);
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        return new ArrayList<>(seen);
    }

    static String hostOf(String url) {
        int scheme = url.indexOf("://");
        int start = scheme >= 0 ? scheme + 3 : 0;
        int slash = url.indexOf('/', start);
        return slash < 0 ? url.substring(start) : url.substring(start, slash);
    }
}
