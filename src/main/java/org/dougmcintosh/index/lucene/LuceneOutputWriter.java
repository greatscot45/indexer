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
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;

public class LuceneOutputWriter extends SynchronizedOutputWriter {
    public static final String FLD_CATEGORY = "category";
    public static final String FLD_SUBCATEGORY = "subcategory";
    public static final String FLD_SERIES_CODE = "seriesCode";
    public static final String FLD_SERIES_TITLE = "seriesTitle";
    public static final String FLD_SERMON_TITLE = "sermonTitle";
    public static final String FLD_SERMON_DATE = "sermonDate";
    public static final String FLD_SERMON_MANUSCRIPT = "sermonManuscript";
    public static final String FLD_SERMON_AUDIO = "sermonAudio";
    public static final String FLD_SERMON_PASSAGE = "sermonPassage";
    public static final String FLD_SERMON_TEXT = "sermonText";
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
        final Document doc = new Document();
        doc.add(new StringField(FLD_CATEGORY, entry.getCategory(), Field.Store.YES));
        doc.add(new StringField(FLD_SUBCATEGORY, entry.getSubCategory(), Field.Store.YES));
        doc.add(new StringField(FLD_SERIES_CODE, entry.getSeriesCode(), Field.Store.YES));
        doc.add(new StringField(FLD_SERIES_TITLE, entry.getSeriesTitle(), Field.Store.YES));
        doc.add(new StringField(FLD_SERMON_TITLE, entry.getSermonTitle(), Field.Store.YES));
        doc.add(new StringField(FLD_SERMON_MANUSCRIPT, entry.getPdfRelativePath(), Field.Store.YES));
        doc.add(new StringField(FLD_SERMON_AUDIO, entry.getAudio(), Field.Store.YES));
        doc.add(new StringField(FLD_SERMON_PASSAGE, entry.getPassage(), Field.Store.YES));
        doc.add(new TextField(FLD_SERMON_TEXT, entry.getRawText(), Field.Store.YES));

        final LocalDate sermonDate = entry.getSermonDate();
        if (sermonDate != null) {
            doc.add(new StringField(FLD_SERMON_DATE, sermonDate.toString(), Field.Store.YES));
        } else {
            doc.add(new StringField(FLD_SERMON_DATE, "1970-01-01", Field.Store.YES));
        }

        try {
            indexWriter.addDocument(doc);
        } catch (IOException e) {
            throw new IndexingException(e);
        }
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
    }
}