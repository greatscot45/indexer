package org.dougmcintosh.util;

import com.google.common.base.Preconditions;
import org.dougmcintosh.index.IndexEntry;
import org.dougmcintosh.index.IndexingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility class providing for proper locking of a single writer via a {@link ReentrantLock}.
 */
public abstract class SynchronizedOutputWriter implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedOutputWriter.class);
    private final ReentrantLock writeLock;
    protected final File outputDir;

    protected SynchronizedOutputWriter(File outputDir) {
        Preconditions.checkNotNull(outputDir, "Output file is null.");
        Preconditions.checkState(outputDir.exists(),
            "Output dir does not exist: " + outputDir.getAbsolutePath());

        this.outputDir = outputDir;
        this.writeLock = new ReentrantLock();

        logger.info("Initialized output writer in directory {}", outputDir.getAbsolutePath());
    }

    public void write(final IndexEntry entry) {
        try {
            writeLock.lock();
            logger.debug("Attempting to write index entry for \"{}\"", entry);
            doWrite(entry);
        } catch (Exception e) {
            logger.error("Worker threw exception.", e);
        } finally {
            writeLock.unlock();
        }
    }

    protected abstract void doWrite(final IndexEntry entry) throws IndexingException;
}
