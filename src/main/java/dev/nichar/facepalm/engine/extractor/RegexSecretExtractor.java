package dev.nichar.facepalm.engine.extractor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import jakarta.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import dev.nichar.facepalm.pattern.SecretPattern;


/**
 * Scans file content using regular expressions to identify potential secrets.
 * Supports both single-line matching and multi-line blocks like PEM certificates.
 */
@Named
@Singleton
public class RegexSecretExtractor implements SecretExtractor {

    @Inject
    private FacepalmConfig config;

    @Override
    public List<Finding> extract(@Nonnull final FileContext context) {
        final List<Finding> findings = new ArrayList<>();
        final Set<String> localDedup = new HashSet<>();
        final List<SecretPattern> activePatterns = config.getPatterns().getOverrides();

        // Single-Line Scanning.
        for (int i = 0; i < context.getLines().size(); i++) {
            final var rawLine = context.getLines().get(i);
            final var normalizedLine = rawLine.replace("\"", "").replace("'", "");

            for (final var sp : activePatterns) {
                if (sp.isMultiLine()) {
                    // Skip multi-line patterns to avoid redundant processing here.
                    continue;
                }
                final var m = sp.getPattern().matcher(normalizedLine);
                while (m.find()) {
                    // Extract capture group 1 if present; otherwise the full match.
                    final var secretValue = m.groupCount() >= 1 ? m.group(1) : m.group();
                    registerFinding(findings, localDedup, context, sp, secretValue, i + 1, rawLine);
                }
            }
        }

        // Multi-Line Block Scanning (e.g., PEM certificates).
        for (SecretPattern sp : activePatterns) {
            if (!sp.isMultiLine()) {
                continue;
            }
            // Scans the entire file as one string.
            final var matcher = sp.getPattern().matcher(context.getFullContent());
            while (matcher.find()) {
                final var secretValue = matcher.group();
                // Map character offset to 1-based line number.
                final var lineNum = (int) context.getFullContent()
                    .substring(0, matcher.start())
                    .chars()
                    .filter(ch -> ch == '\n')
                    .count() + 1;
                registerFinding(findings, localDedup, context, sp, secretValue, lineNum, "[MULTI-LINE BLOCK]");
            }
        }
        return findings;
    }

    /**
     * Constructs and registers a finding if its unique hash hasn't been seen yet.
     */
    private void registerFinding(@Nonnull final List<Finding> findings,
                                 @Nonnull final Set<String> dedup,
                                 @Nonnull final FileContext ctx,
                                 @Nonnull final SecretPattern sp,
                                 @Nonnull final String value,
                                 final int lineNum,
                                 @Nonnull final String snippet) {

        // Deduplication logic: If the SAME secret appears multiple times in the SAME file on DIFFERENT lines.
        // It's treated as one finding, but we track all its occurrences.
        final var hash = hashString(sp.getName() + value);
        final var existing = findings.stream().filter(f -> f.getDeduplicationHash().equals(hash)).findFirst();

        if (existing.isPresent()) {
            // Already found this secret in this file; ignore additional occurrences
            // One finding per file for the same secret.
            return;
        }

        if (dedup.add(hash)) {
            final var f = Finding.builder()
                .patternName(sp.getName())
                .deduplicationHash(hash)
                .secretValue(value)
                .lineNumber(lineNum)
                .context(ctx)
                .contextSnippet(snippet.trim())
                .riskScore(sp.getBaseRisk())
                .confidenceScore(sp.getBaseConfidence())
                .build();
            f.log("Base Pattern Match", 0, 0);
            findings.add(f);
        }
    }

    /**
     * Generates an SHA-256 hex string to uniquely identify a finding.
     */
    private String hashString(@Nonnull final String input) {
        try {
            final var messageDigest = MessageDigest.getInstance("SHA-256");
            final var hash = messageDigest.digest(input.getBytes());
            final var hexString = new StringBuilder();
            for (final var b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
