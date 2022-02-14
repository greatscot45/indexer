package org.dougmcintosh.index.enhance;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SermonData {
    private static final Pattern NAME_PATTERN = Pattern.compile(".*/(.+)");
    public static final String PDF = "pdf";
    private static final String AUDIO = "audio";
    private static final String DATE = "date";
    private static final String PASSAGE = "passage";
    private static final String TITLE = "title";

    private String audio;
    private String date;
    private String passage;
    private String pdf;
    private String title;

    private SermonData(
        final String audio,
        final String date,
        final String passage,
        final String pdf,
        final String title) {
        this.audio = audio;
        this.date = date;
        this.passage = passage;
        this.pdf = pdf;
        this.title = title;

        if (this.pdf == null) {
            throw new RuntimeException("no pdf for: audio: " + audio + "; date: " + date + "; passage: " + passage + "; title: " + title);
        }
    }

    public static SermonData from(Map<String, String> m) {
        return new SermonData(
            m.getOrDefault(AUDIO, ""),
            m.getOrDefault(DATE, ""),
            m.getOrDefault(PASSAGE, ""),
            m.getOrDefault(PDF, null),
            m.getOrDefault(TITLE, "")
        );
    }

    public String getAudio() {
        return audio;
    }

    public String getDate() {
        return date;
    }

    public String getPassage() {
        return passage;
    }

    public String getPdf() {
        return pdf;
    }

    public String getTitle() {
        return title;
    }

    public String getPdfName() {
        final Matcher matcher = NAME_PATTERN.matcher(getPdf());
        if(matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("could not distill pdf name for " + getPdf());
    }

    @Override
    public String toString() {
        return getPdf() + "|" + getAudio() + "|" + getTitle() + "|" + getPassage() + "|" + getDate();
    }
}
