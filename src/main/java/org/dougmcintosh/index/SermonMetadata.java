package org.dougmcintosh.index;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SermonMetadata {
    private static final Logger logger = LoggerFactory.getLogger(SermonMetadata.class);
    private static final Map<String, IndexEntry.Builder> indexEntries = new LinkedHashMap<>();
    private static volatile boolean initialized = false;

    public static void load(final File sermonMetadataFile) throws IOException {
        final JsonFactory factory = new JsonFactory();
        final ObjectMapper mapper = new ObjectMapper(factory);

        try (final FileInputStream stream = new FileInputStream(sermonMetadataFile)) {
            final JsonNode rootNode = mapper.readTree(stream);
            final Iterator<Map.Entry<String, JsonNode>> fieldsIt = rootNode.fields();
            while (fieldsIt.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldsIt.next();
                String categoryName = entry.getKey();
                JsonNode category = entry.getValue();
                processCategory(categoryName, category);
            }
        }

        logger.info("Completed ingest of external metadata file. Found " +
            indexEntries.size() + " entries with manuscripts.");

        initialized = true;
    }

    public static IndexEntry.Builder entryBuilderForManuscript(final File pdf) {
        Preconditions.checkState(initialized, "SermonMetadata has not been initialized.");
        Preconditions.checkNotNull(pdf, "Provided manuscript file is null.");
        Preconditions.checkState(pdf.isFile(), "Provided manuscript file does not exist: " + pdf.getAbsolutePath());
        final String path = relativePathFromManuscriptFile(pdf);
        final IndexEntry.Builder bldr = indexEntries.get(path);

        if (bldr == null) {
            logger.warn("No entry builder exists for path: {}", path);
        }

        return bldr;
    }

    private static final Pattern REL_FILE_PATTERN = Pattern.compile("(?i).*/?(pdf/.+)");
    private static String relativePathFromManuscriptFile(final File pdf) {
        Preconditions.checkNotNull(pdf, "Manuscript is null.");
        final Matcher matcher = REL_FILE_PATTERN.matcher(pdf.getAbsolutePath());
        String relativePath = null;
        if (matcher.find()) {
            relativePath = matcher.group(1);
            Preconditions.checkState(StringUtils.isNotBlank(relativePath),
                "No relative path could be resolved from \"" + pdf.getAbsolutePath() + "\"");
        }
        return relativePath;
    }

    private static void processCategory(
        final String categoryName,
        final JsonNode node) {

        final Iterator<Map.Entry<String, JsonNode>> fieldsIt = node.fields();
        while (fieldsIt.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldsIt.next();
            JsonNode subCategory = entry.getValue();
            if (entry.getKey().equals("subCategories")) {
                processSubCategories(categoryName, entry.getValue());
            }
        }
    }

    private static void processSubCategories(
        String categoryName,
        final JsonNode node) {

        Iterator<Map.Entry<String, JsonNode>> fieldsIt = node.fields();
        while (fieldsIt.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldsIt.next();
            final String subcategoryName = entry.getKey();
            logger.debug("processing subcategory " + subcategoryName);
            JsonNode subCategory = entry.getValue();
            JsonNode seriesArray = subCategory.get("seriesCollection");
            seriesArray.forEach(series -> {
                processSeries(categoryName, subcategoryName, series);
            });
        }
    }

    private static void processSeries(
        final String categoryName,
        final String subcategoryName,
        final JsonNode node) {
        String seriesCode = node.get("seriesCode").textValue();
        String seriesTitle = node.get("seriesTitle").textValue();
        JsonNode sermons = node.get("sermons");
        Preconditions.checkState(sermons.isArray(), "sermons node is not an array.");
        sermons.forEach(sermon -> {
            final JsonNode pdfNode = sermon.get("pdf");
            final String pdf = pdfNode == null ? "" : pdfNode.asText();
            final JsonNode audioNode = sermon.get("audio");
            final String audio = audioNode == null ? "" : audioNode.asText();
            final JsonNode titleNode = sermon.get("title");
            final String title = titleNode == null ? "" : titleNode.asText();
            final JsonNode dateNode = sermon.get("date");
            final String date = dateNode == null ? "" : dateNode.asText();
            final JsonNode passageNode = sermon.get("passage");
            final String passage = passageNode == null ? "" : passageNode.asText();

            if (pdfNode != null) {
                logger.debug(categoryName + "/" + subcategoryName + "/" + seriesCode + "/" + seriesTitle + "/" +
                    pdf + "/" + audio + "/" + title);

                IndexEntry.Builder builder = IndexEntry.builder()
                    .category(categoryName)
                    .subCategory(subcategoryName)
                    .seriesCode(seriesCode)
                    .seriesTitle(seriesTitle)
                    .pdfRelativePath(pdf)
                    .audio(audio)
                    .sermonTitle(title)
                    .passage(passage);

                if (!date.isEmpty()) {
                    builder.date(LocalDate.parse(date));
                }

                IndexEntry.Builder replaced = indexEntries.put(pdf, builder);
                Preconditions.checkState(replaced == null, "Duplicate pdf entry: " + pdf);
            }
        });
    }
}
