package org.dougmcintosh.index.extract;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ExtractResult {
    private File sourceFile;
    private String text;
    private Set<String> tokens = new LinkedHashSet<>();
    private Joiner joiner = Joiner.on(" ");

    private ExtractResult(File sourceFile, String text) {
        this.sourceFile = sourceFile;
        this.text = text;
    }

    public static ExtractResult of(File sourceFile, String text) {
        Preconditions.checkNotNull(sourceFile, "Source file is null.");
        Preconditions.checkState(StringUtils.isNotBlank(text), "Extract text is null or empty.");
        return new ExtractResult(sourceFile, text);
    }

    public boolean addToken(String token) {
        return this.tokens.add(token);
    }

    public String tokenString() {
        return joiner.join(tokens);
    }
}
