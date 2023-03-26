package org.dougmcintosh.index;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.dougmcintosh.index.extract.ExtractResult;
import org.dougmcintosh.index.extract.tika.TikaExtractor;
import org.dougmcintosh.index.lucene.CustomAnalyzer;
import org.dougmcintosh.index.lucene.LuceneOutputWriter;
import org.dougmcintosh.index.lunr.LunrOutputWriter;
import org.dougmcintosh.util.SynchronizedOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
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
        private final LuceneOutputWriter luceneWriter;

        private LuceneWorkerFactory(IndexerArgs args) throws IOException {
            super(args);
            this.luceneWriter = new LuceneOutputWriter(args.getOutputdir(), args.getMinTokenLength());
        }

        @Override
        public Worker newWorker(File sourceFile) {
            return new LuceneWorker(luceneWriter, sourceFile);
        }

        @Override
        public void close() throws IOException {
            if (this.luceneWriter != null) {
                luceneWriter.close();
            }
        }
    }

    public static class LunrWorkerFactory extends WorkerFactory {
        private final LunrOutputWriter lunrWriter;

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
        private final SynchronizedOutputWriter writer;
        protected final File sourceFile;
        protected final Stopwatch stopwatch;

        Worker(SynchronizedOutputWriter writer, File sourceFile) {
            this.writer = Preconditions.checkNotNull(writer, "Output writer is null.");
            this.sourceFile = Preconditions.checkNotNull(sourceFile, "Source file is null.");
            this.stopwatch = Stopwatch.createUnstarted();
        }

        protected abstract Optional<ExtractResult> extract();

        @Override
        public void run() {
            final String path = sourceFile.getAbsolutePath();
            logger.info("Processing source file {}", path);

            Optional<ExtractResult> extractOpt = extract();

            if (extractOpt.isPresent()) {
                stopwatch.start();
                final ExtractResult extraction = extractOpt.get();
                final IndexEntry.Builder entryBldr = SermonMetadata.entryBuilderForManuscript(sourceFile);

                if (entryBldr != null) {
                    writer.write(
                        entryBldr.pdfFile(sourceFile)
                            .keywords(extraction.tokenString())
                            .rawText(extraction.getText())
                            .build());

                    if (logger.isTraceEnabled()) {
                        logger.trace("Indexed {} in {} ms.", sourceFile.getAbsolutePath(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }
                } else {
                    logger.error("No index entry was built for file {}.", path);
                }
            }
        }
    }

    private static class LuceneWorker extends Worker {
        LuceneWorker(LuceneOutputWriter luceneWriter, File sourceFile) {
            super(luceneWriter, sourceFile);
        }

        @Override
        protected Optional<ExtractResult> extract() {
            return TikaExtractor.extract(sourceFile);
        }
    }

    private class LunrWorker extends Worker {
        LunrWorker(SynchronizedOutputWriter writer, File sourceFile) {
            super(writer, sourceFile);
        }

        @Override
        protected Optional<ExtractResult> extract() {
            return TikaExtractor.extractAndTokenize(sourceFile, args.getMinTokenLength());
        }
    }
}
