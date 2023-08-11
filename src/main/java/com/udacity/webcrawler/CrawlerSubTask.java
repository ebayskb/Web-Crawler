package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
//import org.xml.sax.helpers.ParserFactory;

import javax.inject.Inject;
import java.net.URL;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public class CrawlerSubTask extends RecursiveAction {

    private String url;
    private Instant deadline;
    int maxDepth;
    ConcurrentSkipListSet<String> visitedUrls;
    ConcurrentMap<String, Integer> counts;

    Clock clock;

    List<Pattern> ignoredUrls;

    //@Inject
    PageParserFactory parserFactory;

    public CrawlerSubTask(Clock clock, String url, Instant deadline, int maxDepth, ConcurrentSkipListSet<String> visitedUrls, ConcurrentMap<String, Integer> counts, List<Pattern> ignoredUrls, PageParserFactory pageParserFactory ) {
        this.clock = clock;
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.visitedUrls = visitedUrls;
        this.counts = counts;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = pageParserFactory;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);
        PageParser.Result result = parserFactory.get(url).parse();
        for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.put(e.getKey(), counts.containsKey(e.getKey()) ? counts.get(e.getKey())+e.getValue():e.getValue());
        }
        List<CrawlerSubTask> crawlerSubTaskList = new ArrayList<>();
        for (String link : result.getLinks()) {
            CrawlerSubTask crawlerSubTask = new CrawlerSubTask(clock, link, deadline, maxDepth - 1, visitedUrls, counts, ignoredUrls, parserFactory);
            crawlerSubTaskList.add(crawlerSubTask);
            //crawlInternal(link, deadline, maxDepth - 1, counts, visitedUrls);
        }
        invokeAll(crawlerSubTaskList);
    }
}
