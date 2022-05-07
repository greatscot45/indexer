package org.dougmcintosh.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.google.common.base.Preconditions;
import org.dougmcintosh.index.IndexEntry;
import org.dougmcintosh.index.IndexingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class providing for proper locking of a single writer via a {@link ReentrantLock}.
 */
public class SynchronizedOutputWriter implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedOutputWriter.class);
    private static final String TIME_PATTERN = "YYYYMMDDHHmmss";
    private final SequenceWriter sequenceWriter;
    private final ReentrantLock writeLock;
    private final File outputFile;

    public SynchronizedOutputWriter(File outputDir, boolean compress, boolean prettyPrint) throws IOException {
        Preconditions.checkNotNull(outputDir, "Output file is null.");
        Preconditions.checkState(outputDir.exists(),
            "Output dir does not exist: " + outputDir.getAbsolutePath());

        this.outputFile = new File(outputDir, timestampedFileName(compress));
        this.writeLock = new ReentrantLock();

        final ObjectWriter jsonWriter = prettyPrint ?
                new ObjectMapper().writer().withDefaultPrettyPrinter() :
                new ObjectMapper().writer();

        final OutputStream outStream = compress ?
            new GZIPOutputStream(new FileOutputStream(outputFile)) :
            new FileOutputStream(outputFile);

        // strange api call; nothing is written but creates a SequenceWriter
        sequenceWriter = jsonWriter.writeValues(outStream);
        sequenceWriter.init(true /* wrap in array */);

        logger.info("Initialized output writer on {}", outputDir.getAbsolutePath());
    }

    public void write(final IndexEntry entry) throws IndexingException {
        try {
            writeLock.lock();
            logger.debug("Attempting to write index entry for \"{}\"", entry);
            sequenceWriter.write(entry);
        } catch (Exception e) {
            logger.error("Worker threw exception.", e);
        } finally {
            writeLock.unlock();
        }
    }

    public String getOutputFilePath() {
        return outputFile == null ? "null" : outputFile.getAbsolutePath();
    }

    @Override
    public void close() throws IOException {
        if (sequenceWriter != null) {
            sequenceWriter.close();
        }
    }

    private String timestampedFileName(boolean compress) {
        String template = "lunr-%s.json";
        if (compress) {
            template += ".gz";
        }
        return String.format(
                template,
                DateTimeFormatter.ofPattern(TIME_PATTERN).format(LocalDateTime.now()));
    }
}
