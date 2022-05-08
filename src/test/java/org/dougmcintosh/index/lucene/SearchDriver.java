package org.dougmcintosh.index.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;

public class SearchDriver {
    private static final String QUERY = "sermon on the mount";

    public static void main(String[] args) {
        String index = "/Users/jon/dev/projects/mcintosh/indexer/build/lucene";
        try {
            DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
            IndexSearcher searcher = new IndexSearcher(reader);
            CustomAnalyzer.initializeStopWords(new File(
                "/Users/jon/dev/projects/mcintosh/indexer/var/conf/stopwords.txt"));
            Analyzer analyzer = CustomAnalyzer.from(3);
            QueryParser parser = new QueryParser("contents", analyzer);
            Query query = parser.parse(QUERY);

            System.out.println("searching for " + QUERY);

            final TopDocs results = searcher.search(query, 25);
            final ScoreDoc[] hits = results.scoreDocs;

            for (ScoreDoc hit : hits) {
                final Document doc = searcher.doc(hit.doc);
                final String title = doc.get("pdf");
                System.out.println("hit on query \"" + QUERY + "\" in doc \"" + title + "\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
