package conc;

import java.util.List;

/** LeetCode 1242-style: crawl cùng hostname, mỗi URL một lần, đa luồng. */
public class WebCrawler {
    public interface HtmlParser {
        List<String> getUrls(String url);
    }

    public List<String> crawl(String startUrl, HtmlParser parser) {
        throw new UnsupportedOperationException("TODO WebCrawler.crawl");
    }
}
