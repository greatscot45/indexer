package org.dougmcintosh.index;

import com.google.common.base.Preconditions;

import java.io.File;
import java.time.LocalDate;

public class IndexEntry {
    private String category;
    private String subCategory;
    private String seriesCode;
    private String seriesTitle;
    private String sermonTitle;
    private String passage;
    private LocalDate sermonDate;
    private File pdf;
    private String pdfRelativePath;
    private String audio;
    private String keywords;
    private String rawText;

    private IndexEntry(
        final String category,
        final String subCategory,
        final String seriesCode,
        final String seriesTitle,
        final String sermonTitle,
        final String passage,
        final LocalDate sermonDate,
        final File pdf,
        final String pdfRelativePath,
        final String audio,
        final String keywords,
        final String rawText) {
        Preconditions.checkNotNull(pdf, "PDF file is null.");
        Preconditions.checkState(pdf.exists(), "PDF does not exist: %s", pdf.getAbsolutePath());
        this.category = category;
        this.subCategory = subCategory;
        this.seriesCode = seriesCode;
        this.seriesTitle = seriesTitle;
        this.sermonTitle = sermonTitle;
        this.passage = passage;
        this.sermonDate = sermonDate;
        this.pdf = pdf;
        this.pdfRelativePath = pdfRelativePath;
        this.audio = audio;
        this.keywords = keywords;
        this.rawText = rawText;
    }

    public String getCategory() {
        return category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public String getSeriesCode() {
        return seriesCode;
    }

    public String getSeriesTitle() {
        return seriesTitle;
    }

    public String getSermonTitle() {
        return sermonTitle;
    }

    public String getPassage() {
        return passage;
    }

    public LocalDate getSermonDate() {
        return sermonDate;
    }

    public File getPdf() {
        return pdf;
    }

    public String getPdfRelativePath() {
        return pdfRelativePath;
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
        private String category;
        private String subCategory;
        private String seriesCode;
        private String seriesTitle;
        private String sermonTitle;
        private String passage;
        private LocalDate date;
        private File pdfFile;
        private String pdfRelativePath;
        private String audio;
        private String keywords;
        private String rawText;

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder subCategory(String subCategory) {
            this.subCategory = subCategory;
            return this;
        }

        public Builder seriesCode(String seriesCode) {
            this.seriesCode = seriesCode;
            return this;
        }

        public Builder seriesTitle(String seriesTitle) {
            this.seriesTitle = seriesTitle;
            return this;
        }

        public Builder sermonTitle(String sermonTitle) {
            this.sermonTitle = sermonTitle;
            return this;
        }

        public Builder passage(String passage) {
            this.passage = passage;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder pdfFile(File pdfFile) {
            this.pdfFile = pdfFile;
            return this;
        }

        public Builder pdfRelativePath(String pdfRelativePath) {
            this.pdfRelativePath = pdfRelativePath;
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
            return new IndexEntry(
                category, subCategory, seriesCode, seriesTitle, sermonTitle, passage, date,
                pdfFile, pdfRelativePath, audio, keywords, rawText);
        }
    }
}
