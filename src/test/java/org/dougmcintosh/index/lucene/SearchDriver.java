package org.dougmcintosh.index.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;

import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_CATEGORY;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERIES_CODE;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERIES_TITLE;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERMON_AUDIO;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERMON_DATE;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERMON_MANUSCRIPT;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERMON_PASSAGE;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERMON_TEXT;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SERMON_TITLE;
import static org.dougmcintosh.index.lucene.LuceneOutputWriter.FLD_SUBCATEGORY;

public class SearchDriver {
    private static final String QUERY = "\"sermon on the mount\"";
    private static final String index = "/Users/jon/dev/projects/mcintosh/lucene-index";

    public static void main(String[] args) {
        try {
            //simpleSearch();
            highlightSearch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void highlightSearch() throws Exception {
        try (FSDirectory dir = FSDirectory.open(Paths.get(index))) {
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            CustomAnalyzer.initializeStopWords(new File(
                "/Users/jon/dev/projects/mcintosh/indexer/var/conf/stopwords.txt"));
            Analyzer analyzer = CustomAnalyzer.from(3);
            QueryParser parser = new QueryParser(FLD_SERMON_TEXT, analyzer);
            Query query = parser.parse(QUERY);

            System.out.println("searching for " + QUERY);

            final TopDocs hits = searcher.search(query, 25);
            final Formatter formatter = new SimpleHTMLFormatter("<span class=\"foo\">", "</span>");
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 25);
            highlighter.setTextFragmenter(fragmenter);

            for (ScoreDoc hit : hits.scoreDocs) {
                final Document doc = searcher.doc(hit.doc);
                final String title = doc.get(FLD_SERMON_MANUSCRIPT);
                System.out.println("hit on query \"" + QUERY + "\" in doc \"" + title + "\"");
                System.out.println(FLD_CATEGORY + "=" + doc.get(FLD_CATEGORY));
                System.out.println(FLD_SUBCATEGORY + "=" + doc.get(FLD_SUBCATEGORY));
                System.out.println(FLD_SERIES_CODE + "=" + doc.get(FLD_SERIES_CODE));
                System.out.println(FLD_SERIES_TITLE + "=" + doc.get(FLD_SERIES_TITLE));
                System.out.println(FLD_SERMON_TITLE + "=" + doc.get(FLD_SERMON_TITLE));
                System.out.println(FLD_SERMON_DATE + "=" + doc.get(FLD_SERMON_DATE));
                System.out.println(FLD_SERMON_MANUSCRIPT + "=" + doc.get(FLD_SERMON_MANUSCRIPT));
                System.out.println(FLD_SERMON_AUDIO + "=" + doc.get(FLD_SERMON_AUDIO));
                System.out.println(FLD_SERMON_PASSAGE + "=" + doc.get(FLD_SERMON_PASSAGE));
                final String contents = doc.get(LuceneOutputWriter.FLD_SERMON_TEXT);
                TokenStream stream = TokenSources.getAnyTokenStream(reader, hit.doc, LuceneOutputWriter.FLD_SERMON_TEXT, analyzer);

                String[] frags = highlighter.getBestFragments(stream, contents, 10);
                for (String frag : frags) {
                    System.out.println("=======================");
                    System.out.println(frag);
                }
            }
        }
    }

    private static void simpleSearch() throws Exception {
        try (FSDirectory dir = FSDirectory.open(Paths.get(index))) {
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            CustomAnalyzer.initializeStopWords(new File(
                "/Users/jon/dev/projects/mcintosh/indexer/var/conf/stopwords.txt"));
            Analyzer analyzer = CustomAnalyzer.from(3);
            QueryParser parser = new QueryParser(LuceneOutputWriter.FLD_SERMON_TEXT, analyzer);
            Query query = parser.parse(QUERY);

            System.out.println("searching for " + QUERY);

            final TopDocs results = searcher.search(query, 25);
            final ScoreDoc[] hits = results.scoreDocs;

            for (ScoreDoc hit : hits) {
                final Document doc = searcher.doc(hit.doc);
                final String title = doc.get(FLD_SERMON_TITLE);
                System.out.println("hit on query \"" + QUERY + "\" in doc \"" + title + "\"");
                System.out.println(FLD_CATEGORY + "=" + doc.get(FLD_CATEGORY));
                System.out.println(FLD_SUBCATEGORY + "=" + doc.get(FLD_SUBCATEGORY));
                System.out.println(FLD_SERIES_CODE + "=" + doc.get(FLD_SERIES_CODE));
                System.out.println(FLD_SERIES_TITLE + "=" + doc.get(FLD_SERIES_TITLE));
                System.out.println(FLD_SERMON_TITLE + "=" + doc.get(FLD_SERMON_TITLE));
                System.out.println(FLD_SERMON_DATE + "=" + doc.get(FLD_SERMON_DATE));
                System.out.println(FLD_SERMON_MANUSCRIPT + "=" + doc.get(FLD_SERMON_MANUSCRIPT));
                System.out.println(FLD_SERMON_AUDIO + "=" + doc.get(FLD_SERMON_AUDIO));
                System.out.println(FLD_SERMON_PASSAGE + "=" + doc.get(FLD_SERMON_PASSAGE));
            }
        }
    }
}
