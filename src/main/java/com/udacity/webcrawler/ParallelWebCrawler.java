package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final Duration timeout;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;


    @Inject
    public ParallelWebCrawler(
            Clock clock,
            PageParserFactory parserFactory,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @MaxDepth int maxDepth,
            @IgnoredUrls List<Pattern> ignoredUrls) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }
    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }



    @Override
    public CrawlResult crawl(List<String> startingUrls){
        Instant deadline = clock.instant().plus(timeout);

        ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();

        for(String url : startingUrls){
            pool.invoke(new SubCrawl(url, deadline, maxDepth, counts, visitedUrls));
        }
        CrawlResult result;
        if(counts.isEmpty()){
            result = new CrawlResult.Builder()
                    .setWordCounts(counts)
                    .setUrlsVisited(visitedUrls.size())
                    .build();
        }
        else{
            result = new CrawlResult.Builder()
                    .setWordCounts(WordCounts.sort(counts, popularWordCount))
                    .setUrlsVisited(visitedUrls.size())
                    .build();
        }
        return result;
    }


    public class SubCrawl extends RecursiveAction{
        private  final Instant deadline;
        private final ConcurrentMap<String, Integer> counts;
        private final ConcurrentSkipListSet<String> visitedUrls ;

        private final int maxDepth;
        private final String url;
        private final ReentrantLock lock  = new ReentrantLock();

        @Inject
        public SubCrawl(String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls){
            this.url = url;
            this.deadline = deadline;
            this.maxDepth = maxDepth;
            this.counts = counts;
            this.visitedUrls = visitedUrls;
        }


        public Boolean isIgnoredUrl(String url){
            return ignoredUrls.stream().anyMatch(pattern -> pattern.matcher(url).matches());
        }
        public Boolean isDeadlineExceeded(){
            return clock.instant().isAfter(deadline);
        }
        public Boolean isDepthZero(){
            return maxDepth == 0;
        }


        @Override
        protected void compute(){
            //System.out.println("compute");

            if(isDeadlineExceeded() || isDepthZero() || isIgnoredUrl(url)){
                return;
            }
            try {
                lock.lock();
                if(visitedUrls.contains(url)){
                    return;
                }
                else{
                    visitedUrls.add(url);
                }
             } catch(Exception e){
                    e.printStackTrace();
            }finally{
                lock.unlock();
            }

            PageParser.Result result = parserFactory.get(url).parse();
            for(ConcurrentMap.Entry<String, Integer> entry : result.getWordCounts().entrySet()){
                counts.compute(entry.getKey(), (key, value) -> value == null ? entry.getValue() : value + entry.getValue());
            }

            List<String> links = result.getLinks();
            if(links.isEmpty()){
                return;
            }
            List<SubCrawl> subCrawls = new ArrayList<>();
            for(String urlLink : links){
                subCrawls.add(new SubCrawl(urlLink, deadline, maxDepth - 1, counts, visitedUrls));
            }
            invokeAll(subCrawls);
        }
    }
}


 


  
 

