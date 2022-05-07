package org.dougmcintosh.index.extract.lucene;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.dougmcintosh.index.IndexingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public final class LuceneWrapper {
    private static final Logger logger = LoggerFactory.getLogger(LuceneWrapper.class);
    private static volatile CharArraySet stopWords;

    public static Collection<String> tokenize(File sourceFile, String rawText, int minTokenLength) {
        Preconditions.checkNotNull(sourceFile, "Source file argument is null.");
        Preconditions.checkState(StringUtils.isNotBlank(rawText), "Cannot tokenize null/empty text string.");

        final Collection<String> result = new HashSet<>();
        try (final CustomAnalyzer analyzer = new CustomAnalyzer(stopWords, minTokenLength);
             final TokenStream tokenStream = analyzer.tokenStream("text",
                 new InputStreamReader(
                     new ByteArrayInputStream(rawText.getBytes(StandardCharsets.UTF_8))))) {

            try {
                tokenStream.reset();
                while (tokenStream.incrementToken()) {
                    result.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
                }
            } finally {
                tokenStream.end();
            }
        } catch (IOException e) {
            final String absPath = sourceFile.getAbsolutePath();
            logger.error("Exception tokenizing extract result for file {}", absPath, e);
        }
        return result;
    }

    /**
     * Initialize the shared stop words. This method must be called prior to tokenize().
     * @param stopwordsFile
     */
    public static synchronized void initializeStopWords(File stopwordsFile) {
        Preconditions.checkState(stopWords == null, "Stop words have already been initialized.");

        stopWords = new CharArraySet(16, true);
        stopWords.addAll(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

        if (stopwordsFile != null) {
            logger.info("Initializing stop words from {}", stopwordsFile.getAbsolutePath());

            final List<String> stopWordsList;
            try {
                stopWordsList = Files.readLines(stopwordsFile, StandardCharsets.UTF_8);
                stopWords.addAll(stopWordsList);
            } catch (IOException e) {
                throw new IndexingException("Error reading stop words file: " + stopwordsFile.getAbsolutePath());
            }
        }
    }

    private LuceneWrapper() {}
}
