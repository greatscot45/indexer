package org.dougmcintosh.index.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.dougmcintosh.index.IndexEntry;
import org.dougmcintosh.index.IndexingException;
import org.dougmcintosh.util.SynchronizedOutputWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class LuceneOutputWriter extends SynchronizedOutputWriter {
    private final IndexWriter indexWriter;

    public LuceneOutputWriter(File outputDir, int minTokenLength) throws IOException {
        super(outputDir);
        final IndexWriterConfig cfg = new IndexWriterConfig(CustomAnalyzer.from(minTokenLength));
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        final Directory index = FSDirectory.open(Paths.get(outputDir.toURI()));
        this.indexWriter = new IndexWriter(index, cfg);
    }

    @Override
    protected void doWrite(IndexEntry entry) throws IndexingException {
        try (final FileInputStream fis = new FileInputStream(entry.getPdf())) {
            final Document doc = new Document();
            doc.add(new StringField("pdf", entry.getPdf().getName(), Field.Store.YES));
            doc.add(new StringField("audio", entry.getAudio(), Field.Store.YES));
            doc.add(new TextField("contents", entry.getRawText(), Field.Store.YES));
            indexWriter.addDocument(doc);
        } catch (IOException e) {
            throw new IndexingException("Error while writing index entry for " + entry, e);
        }
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
    }
}