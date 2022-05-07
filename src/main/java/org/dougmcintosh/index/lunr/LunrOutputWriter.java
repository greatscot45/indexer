package org.dougmcintosh.index.lunr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.google.common.base.Preconditions;
import org.dougmcintosh.index.IndexEntry;
import org.dougmcintosh.index.IndexingException;
import org.dougmcintosh.util.SynchronizedOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

public class LunrOutputWriter extends SynchronizedOutputWriter {
    private static final Logger logger = LoggerFactory.getLogger(LunrOutputWriter.class);
    private static final String TIME_PATTERN = "YYYYMMDDHHmmss";
    private final SequenceWriter sequenceWriter;
    private final File outputFile;

    public LunrOutputWriter(File outputDir, boolean compress, boolean prettyPrint) throws IOException {
        super(outputDir);

        Preconditions.checkNotNull(outputDir, "Output file is null.");
        Preconditions.checkState(outputDir.exists(),
            "Output dir does not exist: " + outputDir.getAbsolutePath());

        this.outputFile = new File(outputDir, timestampedFileName(compress));

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

    private String timestampedFileName(boolean compress) {
        String template = "lunr-%s.json";
        if (compress) {
            template += ".gz";
        }
        return String.format(
            template,
            DateTimeFormatter.ofPattern(TIME_PATTERN).format(LocalDateTime.now()));
    }

    @Override
    public void close() throws IOException {
        if (sequenceWriter != null) {
            sequenceWriter.close();
        }
    }

    @Override
    protected void doWrite(IndexEntry entry) {
        try {
            sequenceWriter.write(entry);
        } catch (IOException e) {
            throw new IndexingException("Unexpected failure writing index entry " + entry, e);
        }
    }
}
