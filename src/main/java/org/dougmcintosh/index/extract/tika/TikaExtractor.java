package org.dougmcintosh.index.extract.tika;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.dougmcintosh.index.IndexingException;
import org.dougmcintosh.index.extract.ExtractResult;
import org.dougmcintosh.index.extract.StaticPatternExtractFilter;
import org.dougmcintosh.index.lucene.CustomAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class TikaExtractor {
    private static final Logger logger = LoggerFactory.getLogger(TikaExtractor.class);

    public static Optional<ExtractResult> extract(File sourceFile)
        throws IndexingException {

        Optional<ExtractResult> optResult = Optional.empty();

        try (final InputStream stream = new FileInputStream(sourceFile)) {
            final String rawText = extractRawText(stream);

            if (StringUtils.isNotBlank(rawText)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Extracted " + rawText.getBytes(StandardCharsets.UTF_8).length +
                        " bytes of text from " + sourceFile.getAbsolutePath());
                }
                return Optional.of(ExtractResult.of(sourceFile, rawText));
            } else {
                logger.warn("No text extracted from file {}", sourceFile.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new IndexingException(e, sourceFile);
        }

        return optResult;
    }

    public static Optional<ExtractResult> extractAndTokenize(File sourceFile, int minTokenLength)
        throws IndexingException {

        Optional<ExtractResult> optResult = extract(sourceFile);

        if (optResult.isPresent()) {
            final ExtractResult extractResult = optResult.get();
            extractResult.addTokens(CustomAnalyzer.tokenize(sourceFile, extractResult.getText(), minTokenLength));
        }

        return optResult;
    }

    private static String extractRawText(InputStream stream) throws TikaException, IOException, SAXException {
        final BodyContentHandler handler = new BodyContentHandler(-1 /* disable write limit */);
        final AutoDetectParser parser = new AutoDetectParser();
        parser.parse(stream, handler, new Metadata());
        String text = handler.toString().replaceAll("[\\n\\r\\t]", " ");
        return StaticPatternExtractFilter.filter(text);
    }

    private TikaExtractor() {
    }

}
