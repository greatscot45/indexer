package org.dougmcintosh.index;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.dougmcintosh.index.extract.ExtractResult;
import org.dougmcintosh.index.extract.tika.TikaExtractor;
import org.dougmcintosh.index.lucene.CustomAnalyzer;
import org.dougmcintosh.index.lunr.LunrOutputWriter;
import org.dougmcintosh.util.SynchronizedOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class WorkerFactory implements Closeable {
    protected final IndexerArgs args;

    private WorkerFactory(IndexerArgs args) {
        this.args = Preconditions.checkNotNull(args, "IndexerArgs cannot be null.");
        CustomAnalyzer.initializeStopWords(args.getStopwordsFile());
    }

    public static WorkerFactory of(IndexerArgs args) throws IOException {
        return args.getIndexType() == IndexerArgs.IndexType.LUCENE ?
            new LuceneWorkerFactory(args) : new LunrWorkerFactory(args);
    }

    public abstract Worker newWorker(File sourceFile);

    public static class LuceneWorkerFactory extends WorkerFactory {
        private final IndexWriter indexWriter;

        private LuceneWorkerFactory(IndexerArgs args) throws IOException {
            super(args);
            IndexWriterConfig cfg = new IndexWriterConfig(CustomAnalyzer.from(args.getMinTokenLength()));
            Directory index = FSDirectory.open(Paths.get(args.getOutputdir().toURI()));
            this.indexWriter = new IndexWriter(index, cfg);
        }

        @Override
        public Worker newWorker(File sourceFile) {
            return new LuceneWorker(sourceFile);
        }

        @Override
        public void close() throws IOException {

        }
    }

    public static class LunrWorkerFactory extends WorkerFactory {
        private final SynchronizedOutputWriter lunrWriter;

        private LunrWorkerFactory(IndexerArgs args) throws IOException {
            super(args);
            this.lunrWriter = new LunrOutputWriter(
                args.getOutputdir(), args.isCompressed(), args.isPrettyPrint());
        }

        @Override
        public Worker newWorker(File sourceFile) {
            return new LunrWorker(lunrWriter, sourceFile);
        }

        @Override
        public void close() throws IOException {
            if (this.lunrWriter != null) {
                this.lunrWriter.close();
            }
        }
    }

    private abstract static class Worker implements Runnable {
        protected static final Logger logger = LoggerFactory.getLogger(Worker.class);
        protected final File sourceFile;
        protected final Stopwatch stopwatch;

        Worker(File sourceFile) {
            this.sourceFile = Preconditions.checkNotNull(sourceFile, "Source file is null.");
            this.stopwatch = Stopwatch.createUnstarted();
        }

        protected abstract Optional<ExtractResult> extract();

        @Override
        public void run() {
            logger.info("Processing source file {}", sourceFile.getAbsolutePath());

            Optional<ExtractResult> extractOpt = extract();

            if (extractOpt.isPresent()) {
                stopwatch.start();
                final ExtractResult extraction = extractOpt.get();

                processIndexEntry(IndexEntry.builder()
                    .audio(sourceFile.getName().replaceAll("(?i)\\.pdf$", ".mp3"))
                    .pdf(sourceFile.getName())
                    .keywords(extraction.tokenString())
                    .rawText(extraction.getText())
                    .build());

                if (logger.isTraceEnabled()) {
                    logger.trace("Indexed {} in {} ms.", sourceFile.getAbsolutePath(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
                }
            }
        }

        protected abstract void processIndexEntry(IndexEntry extractOpt);
    }

    private static class LuceneWorker extends Worker {
        LuceneWorker(File sourceFile) {
            super(sourceFile);
        }

        @Override
        protected Optional<ExtractResult> extract() {
            return TikaExtractor.extract(sourceFile);
        }

        @Override
        protected void processIndexEntry(IndexEntry extractOpt) {

        }
    }

    private class LunrWorker extends Worker {
        private final SynchronizedOutputWriter writer;

        LunrWorker(SynchronizedOutputWriter writer, File sourceFile) {
            super(sourceFile);
            this.writer = Preconditions.checkNotNull(writer, "Writer is null.");
        }

        @Override
        protected Optional<ExtractResult> extract() {
            return TikaExtractor.extractAndTokenize(sourceFile, args.getMinTokenLength());
        }

        @Override
        protected void processIndexEntry(IndexEntry entry) {
            writer.write(entry);
        }
    }
}
