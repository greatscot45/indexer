package org.dougmcintosh.index;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkManager implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(WorkManager.class);
    private final ExecutorService threadPool;
    private final WorkerFactory workerFactory;
    private final AtomicBoolean failureDetected;

    public WorkManager(int workers, WorkerFactory workerFactory) {
        Preconditions.checkState(workers >= 1, "Workers must be >= 1.");
        this.failureDetected = new AtomicBoolean(false);
        this.workerFactory = Preconditions.checkNotNull(workerFactory, "WorkerFactory is null.");
        this.threadPool = Executors.newFixedThreadPool(workers, new ThreadFactory() {
            private final AtomicInteger workerIdx = new AtomicInteger(0);
            private final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Metrics.failure();
                    failureDetected.set(true);

                    if (e instanceof IndexingException ex) {
                        if (ex.getTarget() != null) {
                            logger.error("Uncaught exception while indexing {}", ex.getTarget().getAbsolutePath(), ex);
                            return;
                        }
                    }
                    logger.error("Uncaught exception in worker {}", t.getName(), e);
                }
            };

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, String.format("index-worker-%d", workerIdx.incrementAndGet()));
                t.setUncaughtExceptionHandler(exceptionHandler);
                return t;
            }
        });
    }

    public void queueWork(File work) {
        Metrics.fileSeen();
        logger.debug("Queueing file {}", work.getAbsolutePath());
        threadPool.execute(workerFactory.newWorker(work));
    }

    @Override
    public void close() throws IOException {
        if (!threadPool.isShutdown()) {
            threadPool.shutdown();

            logger.info("Awaiting thread pool shutdown.");

            try {
                if (!threadPool.awaitTermination(1l, TimeUnit.HOURS)) {
                    throw new IndexingException("Thread pool timed out before all workers completed.");
                }
            } catch (InterruptedException e) {
                logger.error("Thread pool interrupted while awaiting worker completion.", e);
                Thread.currentThread().interrupt();
                throw new IndexingException(e);
            } finally {
                this.workerFactory.close();
            }

            if (failureDetected.get()) {
                throw new IndexingException("One or more worker threads failed. Check logs for details.");
            }

            logger.info("Thread pool shutdown complete.");
        }
    }
}
