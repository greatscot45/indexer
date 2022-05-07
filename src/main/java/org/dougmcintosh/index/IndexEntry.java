package org.dougmcintosh.index;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

public class IndexEntry {
    private String pdf;
    private String audio;
    private String keywords;
    private String rawText;

    private IndexEntry(String pdf, String audio, String keywords, String rawText) {
        Preconditions.checkState(StringUtils.isNotBlank(pdf), "PDF is null/empty.");
        this.pdf = pdf;
        this.audio = audio;
        this.keywords = keywords;
        this.rawText = rawText;
    }

    public String getPdf() {
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
        return pdf;
    }

    public static class Builder {
        private String pdf;
        private String audio;
        private String keywords;
        private String rawText;

        public Builder pdf(String pdf) {
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
