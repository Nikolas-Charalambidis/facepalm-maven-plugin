package dev.nichar.facepalm.engine.extractor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.FacepalmConfig;
import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import dev.nichar.facepalm.pattern.SecretPattern;


/**
 * Scans file content using pre-defined regular expressions to identify potential secrets.
 * It supports both single-line pattern matching for speed and multi-line regex for complex
 * structures like PEM certificates, using SHA-256 hashing to ensure finding uniqueness.
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
                    // Uses capture group 1 if present (the actual secret), otherwise the full match.
                    final var secretValue = m.groupCount() >= 1 ? m.group(1) : m.group();
                    registerFinding(findings, localDedup, context, sp, secretValue, i + 1, rawLine);
                }
            }
        }

        // Multi-Line Block Scanning (e.g., RSA Private Keys).
        for (SecretPattern sp : activePatterns) {
            if (!sp.isMultiLine()) {
                continue;
            }
            // Scans the entire file as one string.
            final var matcher = sp.getPattern().matcher(context.getFullContent());
            while (matcher.find()) {
                final var secretValue = matcher.group();
                // Calculates the exact line number by counting newline characters before the match start
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
     * Constructs a Finding object and adds it to the list if its unique hash hasn't been seen yet.
     *
     * @param findings The list of total findings for the current file.
     * @param dedup Set of existing hashes to prevent duplicates.
     * @param ctx The current file context.
     * @param sp The specific pattern that triggered the match.
     * @param value The extracted secret string.
     * @param lineNum The 1-based line number where the secret begins.
     * @param snippet A preview of the line for reporting purposes.
     */
    private void registerFinding(@Nonnull final List<Finding> findings,
                                 @Nonnull final Set<String> dedup,
                                 @Nonnull final FileContext ctx,
                                 @Nonnull final SecretPattern sp,
                                 @Nonnull final String value,
                                 final int lineNum,
                                 @Nonnull final String snippet) {

        final var hash = hashString(sp.getName() + value + lineNum);
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
     *
     * @param input The combined string of pattern name, value, and line number.
     * @return A 64-character hex string, or the string's hashCode as a fallback.
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
