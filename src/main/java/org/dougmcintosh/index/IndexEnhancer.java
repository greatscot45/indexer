package org.dougmcintosh.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.dougmcintosh.index.enhance.SermonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IndexEnhancer {
    private static final Logger logger = LoggerFactory.getLogger(IndexEnhancer.class);
    public static final IndexEnhancer NO_OP = new IndexEnhancer();
    private File enhanceFile;
    private Map<String, SermonData> sermonDataMap = new ConcurrentHashMap<>();

    private IndexEnhancer() {
    }

    public IndexEnhancer(final File enhanceFile) {
        this.enhanceFile = Preconditions.checkNotNull(enhanceFile, "Enhance file is null.");
        parse(this.enhanceFile);
    }

    public IndexEntry enhance(final IndexEntry entry) {
        if (enhanceFile == null) {
            return entry;
        }

        IndexEntry result = entry;

        if (sermonDataMap.containsKey(entry.getPdf())) {
            SermonData sd = sermonDataMap.get(entry.getPdf());
            result = IndexEntry.builder()
                .from(entry)
                .audio(sd.getAudio())
                .pdf(sd.getPdf())
                .date(sd.getDate())
                .title(sd.getTitle())
                .passage(sd.getPassage())
                .build();
        }
        else {
            throw new IllegalStateException("no sermon data found for " + entry.getPdf());
        }
        return result;
    }

    private void parse(final File enhanceFile) {
        logger.info("Parsing index enhancer file {}", enhanceFile.getAbsolutePath());
        try {
            Map<String, ?> m = new ObjectMapper().readValue(enhanceFile, Map.class);
            recurse(m);
//            System.out.println("sermon data size: " + sermonDataMap.size());
//            sermonDataMap.values().forEach(e -> System.out.println(e.toString()));
//            sermonDataMap.keySet().forEach(e -> System.out.println(e));
        } catch (IOException e) {
            throw new IndexingException("Failed to parse enhance file.", e);
        }
    }

    private void recurse(Map<String,?> m) {
        m.entrySet().forEach(e -> {
            if (e.getValue() instanceof Map) {
                recurse((Map<String,?>)e.getValue());
            }
            if (e.getValue() instanceof List) {
                List<?> list = (List<?>) e.getValue();
                list.forEach(item -> {
                    if (item instanceof Map) {
                        recurse((Map<String,?>)item);
                    }
                });
            }
            if (e.getKey().equals("sermons")) {
                List<Map<String,String>> list = (List)e.getValue();
                list.forEach(v -> {
                    if (v.containsKey(SermonData.PDF)) {
                        SermonData sd = SermonData.from(v);
                        final String name = sd.getPdfName();
                        if (sermonDataMap.containsKey(name)) {
                            throw new IllegalStateException(name + " is already mapped.");
                        }
                        sermonDataMap.put(sd.getPdfName(), sd);
                    } else {
                        System.out.println("no pdf for " + v);
                    }
                });
            }
        });
    }

    public static void main(String[] args) {
//        Pattern NAME_PATTERN = Pattern.compile(".*/(.+)");
//        String name = "pdf/1John/1John01.pdf";
//        Matcher m = NAME_PATTERN.matcher(name);
//        if(m.find()) {
//            System.out.println(m.group(1));
//        }
        new IndexEnhancer(new File("/Users/joncrater/dev/projects/mcintosh/site-v1/src/data/allSermonData.json"));
    }

}
