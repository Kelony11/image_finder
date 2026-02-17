package com.eulerity.hackathon.imagefinder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/*
- BFS by depth
- Each depth layer is fetched in parallel
- Main thread tracks visited to avoid re-crawling (looping)
- Same-domain only
*/

public class ImageCrawler {

    private static final int POLITE_DELAY_MS = 150;
    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 120;
    private static final int THREADS = 8;

    public Set<String> crawl(String startUrl) {
        Set<String> images = new HashSet<>();
        Set<String> seen = new HashSet<>();

        URI start;
        try {
            start = cleanUri(startUrl);
        } catch (Exception e) {
            return images;
        }

        String rootHost = start.getHost();
        if (rootHost == null) return images;

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CompletionService<PageResult> ecs = new ExecutorCompletionService<>(pool);

        try {
            List<String> frontier = new ArrayList<>();
            frontier.add(start.toString());
            seen.add(start.toString());

            for (int depth = 0; depth <= MAX_DEPTH; depth++) {
                if (frontier.isEmpty()) break;
                if (seen.size() >= MAX_PAGES) break;

                // Submit all pages at this depth (parallel)
                int submitted = 0;
                for (String url : frontier) {
                    if (seen.size() >= MAX_PAGES) break;
                    ecs.submit(() -> fetchPage(url, rootHost));
                    submitted++;
                }

                // Collect results and build next layer
                List<String> nextFrontier = new ArrayList<>();

                for (int i = 0; i < submitted; i++) {
                    PageResult r;
                    try {
                        r = ecs.take().get();
                    } catch (Exception e) {
                        continue;
                    }

                    images.addAll(r.images);

                    for (String link : r.links) {
                        if (seen.size() >= MAX_PAGES) break;
                        if (!seen.contains(link)) {
                            seen.add(link);
                            nextFrontier.add(link);
                        }
                    }
                }

                // Move to the next depth layer
                frontier = nextFrontier;
            }
        } finally {
            // Graceful shutdown (still simple)
            pool.shutdown();
            try {
                pool.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                pool.shutdownNow();
            }
        }

        return images;
    }

    private boolean isHttp(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    // Normalize URL: remove fragments (#...), keep query
    private URI cleanUri(String input) throws Exception {
        URI uri = new URI(input.trim());
        return new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                null
        ).normalize();
    }

    private static class PageResult {
        final Set<String> images = new HashSet<>();
        final Set<String> links = new HashSet<>();
    }

    // Worker: fetch one page, extract image URLs & same-domain links.
    private PageResult fetchPage(String url, String rootHost) {
        PageResult out = new PageResult();

        try {
            Thread.sleep(POLITE_DELAY_MS);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; ImageFinder/1.0)")
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // Images
            for (Element img : doc.select("img[src]")) {
                String src = img.absUrl("src");
                if (isHttp(src)) out.images.add(src);
            }

            // Icons (favicon, etc.)
            for (Element icon : doc.select("link[rel~=(?i)icon][href]")) {
                String href = icon.absUrl("href");
                if (isHttp(href)) out.images.add(href);
            }

            // Links (same domain only)
            for (Element a : doc.select("a[href]")) {
                String abs = a.absUrl("href");
                if (!isHttp(abs)) continue;

                URI u;
                try {
                    u = cleanUri(abs);
                } catch (Exception e) {
                    continue;
                }

                if (u.getHost() == null) continue;
                if (!u.getHost().equalsIgnoreCase(rootHost)) continue;

                // Skip obvious non-HTML files
                String path = (u.getPath() == null) ? "" : u.getPath().toLowerCase();
                if (path.endsWith(".pdf") || path.endsWith(".zip")) continue;

                out.links.add(u.toString());
            }

        } catch (Exception ignored) {
            // If a page fails, return empty results and keep crawling
        }

        return out;
    }
}
