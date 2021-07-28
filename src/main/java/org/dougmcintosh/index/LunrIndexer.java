package org.dougmcintosh.index;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.dougmcintosh.index.crawl.Crawler;
import org.dougmcintosh.index.extract.lucene.LuceneWrapper;
import org.dougmcintosh.util.SynchronizedOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class LunrIndexer {
    private static final Logger logger = LoggerFactory.getLogger(LunrIndexer.class);
    private static final Pattern FILE_PATTERN = Pattern.compile("(?i).+\\.pdf$");
    private final IndexerArgs args;
    private final Stopwatch stopwatch;
    private SynchronizedOutputWriter writer;

    private LunrIndexer(IndexerArgs args) {
        this.args = Preconditions.checkNotNull(args);
        this.stopwatch = Stopwatch.createUnstarted();
    }

    public static LunrIndexer with(IndexerArgs args) {
        return new LunrIndexer(args);
    }

    public void index() throws IOException {
        logger.info("Starting index.");
        stopwatch.start();

        LuceneWrapper.initializeStopWords(args.getStopwordsFile());

        this.writer = new SynchronizedOutputWriter(
                args.getOutputdir(), args.isCompressed(), args.isPrettyPrint());

        final WorkerFactory workerFactory = new WorkerFactory(writer, args.getMinTokenLength());

        try (final WorkManager workMgr = new WorkManager(args.getWorkers(), workerFactory)) {
            Crawler.builder()
                    .directories(args.getInputdirs())
                    .filter(file -> file.isDirectory() || file.isFile() && FILE_PATTERN.matcher(file.getName()).matches())
                    .workManager(workMgr)
                    .recurse(args.isRecurse())
                    .build()
                    .crawl();
        } finally {
            logger.info("Index completed in {} seconds. Processed {} files with {} failure(s). Index written to {}.",
                    stopwatch.elapsed(TimeUnit.SECONDS),
                    Metrics.getFilesSeen(),
                    Metrics.getFailures(),
                    this.writer.getOutputFilePath());
        }
    }
}
