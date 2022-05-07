package org.dougmcintosh.index;

import com.google.common.base.Preconditions;

import java.io.File;

public class IndexEntry {
    private File pdf;
    private String audio;
    private String keywords;
    private String rawText;

    private IndexEntry(File pdf, String audio, String keywords, String rawText) {
        Preconditions.checkNotNull(pdf, "PDF file is null.");
        Preconditions.checkState(pdf.exists(), "PDF does not exist: %s", pdf.getAbsolutePath());
        this.pdf = pdf;
        this.audio = audio;
        this.keywords = keywords;
        this.rawText = rawText;
    }

    public File getPdf() {
        return pdf;
    }

    public String getAudio() {
        return audio;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getRawText() {
        return rawText;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return pdf.getAbsolutePath();
    }

    public static class Builder {
        private File pdf;
        private String audio;
        private String keywords;
        private String rawText;

        public Builder pdf(File pdf) {
            this.pdf = pdf;
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

        public Builder rawText(String rawText) {
            this.rawText = rawText;
            return this;
        }

        public IndexEntry build() {
            return new IndexEntry(pdf, audio, keywords, rawText);
        }
    }
}
