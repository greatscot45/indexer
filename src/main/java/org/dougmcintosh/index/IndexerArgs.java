package org.dougmcintosh.index;

import com.google.common.base.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexerArgs {
    private static final int DEFAULT_WORKERS = 6;
    private static final int DEFAULT_MIN_TOKEN_LENGTH = 5;
    private Set<File> inputdirs;
    private File outputdir;
    private File stopwordsFile;
    private Optional<File> optEnhanceFile;
    private final boolean recurse;
    private final int workers;
    private final int minTokenLength;
    private boolean compress;
    private boolean prettyPrint;

    private IndexerArgs(Set<String> inputDirPaths,
                        String outputdirPath,
                        Optional<String> stopwordsPath,
                        Optional<String> enhanceFilePath,
                        boolean recurse,
                        Optional<Integer> workers,
                        Optional<Integer> minTokenLength,
                        boolean compress,
                        boolean prettyPrint) {
        Preconditions.checkState(CollectionUtils.isNotEmpty(inputDirPaths), "Input dir paths is null/empty.");
        Preconditions.checkState(StringUtils.isNotBlank(outputdirPath), "outputdirPath is null/blank.");

        initInputDirs(inputDirPaths);
        initOutputDir(outputdirPath);
        initStopWordsFile(stopwordsPath);
        initEnhanceFile(enhanceFilePath);

        this.recurse = recurse;
        this.minTokenLength = minTokenLength.orElse(DEFAULT_MIN_TOKEN_LENGTH);
        this.compress = compress;
        this.prettyPrint = prettyPrint;
        this.workers = workers.orElse(DEFAULT_WORKERS);
        Preconditions.checkState(this.workers >= 1, "Workers must be >= 1.");
    }

    private void initInputDirs(Set<String> inputDirPaths) {
        this.inputdirs = inputDirPaths.stream().map(
            s -> new File(s)).collect(Collectors.toCollection(LinkedHashSet::new));

        this.inputdirs.stream().forEach(d ->
            Preconditions.checkState(d.isDirectory(),
                "Input directory does not exist or isn't a directory: " + d.getAbsolutePath()));
    }

    private void initOutputDir(String outputdirPath) {
        this.outputdir = new File(outputdirPath);
        Preconditions.checkState(outputdir.isDirectory(),
            "Output directory does not exist or isn't a directory: " + outputdir.getAbsolutePath());
    }

    private void initStopWordsFile(Optional<String> stopwordsPath) {
        if (stopwordsPath.isPresent()) {
            this.stopwordsFile = new File(stopwordsPath.get());
            Preconditions.checkState(stopwordsFile.isFile(), "Stopwords path doesn't exist or isn't a file.;");
        }
    }

    private void initEnhanceFile(Optional<String> enhancePath) {
        this.optEnhanceFile = Optional.empty();

        if (enhancePath.isPresent()) {
            final File f = new File(enhancePath.get());
            this.optEnhanceFile = Optional.of(f);
            Preconditions.checkState(f.isFile(),"Enhance file doesn't exist or isn't a file.;");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<File> getInputdirs() {
        return inputdirs;
    }

    public File getOutputdir() {
        return outputdir;
    }

    public File getStopwordsFile() {
        return stopwordsFile;
    }

    public Optional<File> getEnhanceFile() {
        return optEnhanceFile;
    }
    public boolean isRecurse() {
        return recurse;
    }

    public int getWorkers() {
        return workers;
    }

    public int getMinTokenLength() {
        return minTokenLength;
    }

    public boolean isCompressed() {
        return compress;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public static class Builder {
        private Set<String> inputdirPaths;
        private String outputdirPath;
        private Optional<String> stopwordsPath = Optional.empty();
        private Optional<String> enhancePath = Optional.empty();
        private boolean recurse = true;
        private Optional<Integer> workers = Optional.empty();
        private Optional<Integer> minTokenLength = Optional.empty();
        private boolean compress = true;
        private boolean prettyPrint = false;

        public Builder inputdirPaths(String[] inputdirPaths) {
            if (ArrayUtils.isNotEmpty(inputdirPaths)) {
                this.inputdirPaths = new LinkedHashSet<>(Arrays.asList(inputdirPaths));
            }
            return this;
        }

        public Builder outputdirPath(String outputdirPath) {
            this.outputdirPath = outputdirPath;
            return this;
        }

        public Builder stopwordsPath(Optional<String> stopwordsPath) {
            this.stopwordsPath = stopwordsPath;
            return this;
        }

        public Builder enhancePath(Optional<String> enhancePath) {
            this.enhancePath = enhancePath;
            return this;
        }

        public Builder recurse(boolean flag) {
            this.recurse = flag;
            return this;
        }

        public Builder workers(Optional<Integer> workers) {
            this.workers = workers;
            return this;
        }

        public Builder minTokenLength(Optional<Integer> minTokenLength) {
            this.minTokenLength = minTokenLength;
            return this;
        }

        public Builder compress(boolean flag) {
            this.compress = flag;
            return this;
        }

        public Builder prettyPrint(boolean flag) {
            this.prettyPrint = flag;
            return this;
        }

        public IndexerArgs build() {
            return new IndexerArgs(
                inputdirPaths, outputdirPath, stopwordsPath, enhancePath,
                recurse, workers, minTokenLength, compress, prettyPrint);
        }
    }
}
