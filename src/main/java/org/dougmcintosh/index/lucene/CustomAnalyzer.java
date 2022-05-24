package org.dougmcintosh.index.lucene;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.dougmcintosh.index.IndexingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomAnalyzer extends StopwordAnalyzerBase {
    private static final Logger logger = LoggerFactory.getLogger(CustomAnalyzer.class);

    /**
     * Default maximum allowed token length
     */
    public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

    /**
     * Default minimum allowed token length
     */
    public static final int DEFAULT_MIN_TOKEN_LENGTH = 5;

    private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

    private int minTokenLength = DEFAULT_MIN_TOKEN_LENGTH;

    private static volatile CharArraySet stopWords;

    public static CustomAnalyzer from(int minTokenLength) {
        Preconditions.checkNotNull(stopWords, "Stop words have not been initialized.");
        return new CustomAnalyzer(stopWords, minTokenLength);
    }

    /**
     * Initialize the shared stop words. This method must be called prior to tokenize().
     * @param stopwordsFile
     */
    public static synchronized void initializeStopWords(final File stopwordsFile) {
        Preconditions.checkState(stopWords == null, "Stop words have already been initialized.");

        stopWords = new CharArraySet(16, true);
        stopWords.addAll(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

        if (stopwordsFile != null) {
            logger.info("Initializing stop words from {}", stopwordsFile.getAbsolutePath());

            try {
                Set<String> stopWordsSet = new HashSet<>(Files.readLines(stopwordsFile, StandardCharsets.UTF_8));
                stopWords.addAll(stopWordsSet);
            } catch (IOException e) {
                throw new IndexingException("Error reading stop words file: " + stopwordsFile.getAbsolutePath());
            }
        }
    }

    public static Collection<String> tokenize(File sourceFile, String rawText, int minTokenLength) {
        Preconditions.checkNotNull(sourceFile, "Source file argument is null.");
        Preconditions.checkState(StringUtils.isNotBlank(rawText), "Cannot tokenize null/empty text string.");

        final Collection<String> result = new HashSet<>();
        try (final CustomAnalyzer analyzer = CustomAnalyzer.from(minTokenLength);
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
     * Builds an analyzer with the given stop words.
     *
     * @param stopWords stop words
     */
    private CustomAnalyzer(CharArraySet stopWords) {
        super(stopWords);
    }

    /**
     * Builds an analyzer with no stop words.
     */
    private CustomAnalyzer() {
        this(CharArraySet.EMPTY_SET);
    }

    /**
     * Builds an analyzer with the stop words from the given reader.
     *
     * @param stopwords Reader to read stop words from
     * @see WordlistLoader#getWordSet(Reader)
     */
    private CustomAnalyzer(Reader stopwords) throws IOException {
        this(loadStopwordSet(stopwords));
    }

    private CustomAnalyzer(CharArraySet stopWords, int minTokenLength) {
        this(stopWords);
        Preconditions.checkState(minTokenLength >= 1, "Min token length must be >= 1.");
        setMinTokenLength(minTokenLength);
    }

    /**
     * Set the max allowed token length.  Tokens larger than this will be chopped
     * up at this token length and emitted as multiple tokens.  If you need to
     * skip such large tokens, you could increase this max length, and then
     * use {@code LengthFilter} to remove long tokens.  The default is
     * {@link CustomAnalyzer#DEFAULT_MAX_TOKEN_LENGTH}.
     */
    public void setMaxTokenLength(int length) {
        maxTokenLength = length;
    }

    /**
     * Set the minimum allowed token length.  Tokens shorter than this will be
     * ignored. The default is
     * {@link CustomAnalyzer#DEFAULT_MIN_TOKEN_LENGTH}.
     */
    public void setMinTokenLength(int length) {
        minTokenLength = length;
    }

    /**
     * Returns the current maximum token length
     *
     * @see #setMaxTokenLength
     */
    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(final String fieldName) {
        final StandardTokenizer src = new StandardTokenizer();
        src.setMaxTokenLength(maxTokenLength);
        TokenStream tokenStream = new LowerCaseFilter(src);
        tokenStream = new StopFilter(tokenStream, stopwords);
        tokenStream = new LengthFilter(tokenStream, minTokenLength, maxTokenLength);
        return new TokenStreamComponents(r -> {
            src.setMaxTokenLength(CustomAnalyzer.this.maxTokenLength);
            src.setReader(r);
        }, tokenStream);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }
}
