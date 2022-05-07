package org.dougmcintosh.index.lucene;

import org.dougmcintosh.index.IndexEntry;
import org.dougmcintosh.index.IndexingException;
import org.dougmcintosh.util.SynchronizedOutputWriter;

import java.io.File;
import java.io.IOException;

public class LuceneOutputWriter extends SynchronizedOutputWriter {
    public LuceneOutputWriter(File outputDir) {
        super(outputDir);
    }

    @Override
    protected void doWrite(IndexEntry entry) throws IndexingException {

    }

    @Override
    public void close() throws IOException {

    }
}
