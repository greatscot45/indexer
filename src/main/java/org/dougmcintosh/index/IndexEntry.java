package org.dougmcintosh.index;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

public class IndexEntry {
    private static final String PDF_BASEDIR = "pdf/";
    private static final String AUDIO_BASEDIR = "audio/";
    private String audio;
    private String date;
    private String keywords;
    private String pdf;
    private String title;
    private String passage;

    private IndexEntry(String audio, String keywords, String pdf, String date, String title, String passage) {
        Preconditions.checkState(StringUtils.isNotBlank(pdf), "PDF is null/empty.");
        this.audio = audio;
        this.keywords = keywords;
        this.pdf = pdf;
        this.date = date;
        this.title = title;
        this.passage = passage;
    }

    public String getId() {
        return getPdf() + "|" + getAudio() + "|" + getTitle() + "|" + getPassage() + "|" + getDate();
    }

    public String getPdf() {
        return pdf;
    }

    public String getAudio() {
        return audio == null ? "" : audio;
    }

    public String getDate() {
        return date == null ? "" : date;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public String getPassage() {
        return passage == null ? "" : passage;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return pdf;
    }

    public static class Builder {
        private String audio;
        private String keywords;
        private String pdf;
        private String date;
        private String title;
        private String passage;

        public Builder from(IndexEntry entry) {
            Preconditions.checkNotNull(entry, "Cannot create an IndexEntry from null.");
            this.audio = entry.audio;
            this.keywords = entry.keywords;
            this.pdf = entry.pdf;
            this.date = entry.date;
            this.title = entry.title;
            this.passage = entry.passage;
            return this;
        }

        public Builder audio(String audio) {
            this.audio = audio;
            return this;
        }

        public Builder keywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder pdf(String pdf) {
            this.pdf = pdf;
            return this;
        }

        public Builder date(String date) {
            this.date = date;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder passage(String passage) {
            this.passage = passage;
            return this;
        }

        public IndexEntry build() {
            return new IndexEntry(
                audio, keywords, pdf, date, title, passage);
        }
    }
}
