package dev.nichar.facepalm.engine.extractor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dev.nichar.facepalm.engine.FileContext;
import dev.nichar.facepalm.engine.Finding;
import dev.nichar.facepalm.pattern.SecretPattern;


@Named
@Singleton
public class RegexSecretExtractor implements SecretExtractor {
    @Inject
    private dev.nichar.facepalm.FacepalmConfig config;

    @Override
    public List<Finding> extract(FileContext context) {
        List<Finding> findings = new ArrayList<>();
        Set<String> localDedup = new HashSet<>();
        List<SecretPattern> activePatterns = config.getPatterns().getOverrides();

        for (int i = 0; i < context.getLines().size(); i++) {
            String rawLine = context.getLines().get(i);
            String normalizedLine = rawLine.replace("\"", "").replace("'", "");

            for (SecretPattern sp : activePatterns) {
                if (sp.isMultiLine()) {
                    continue;
                }
                Matcher m = sp.getPattern().matcher(normalizedLine);
                while (m.find()) {
                    String secretValue = m.groupCount() >= 1 ? m.group(1) : m.group();
                    registerFinding(findings, localDedup, context, sp, secretValue, i + 1, rawLine);
                }
            }
        }

        for (SecretPattern sp : activePatterns) {
            if (!sp.isMultiLine()) {
                continue;
            }
            Matcher m = sp.getPattern().matcher(context.getFullContent());
            while (m.find()) {
                String secretValue = m.group();
                int lineNum = (int) context.getFullContent().substring(0, m.start())
                    .chars().filter(ch -> ch == '\n').count() + 1;
                registerFinding(findings, localDedup, context, sp, secretValue, lineNum, "[MULTI-LINE BLOCK]");
            }
        }
        return findings;
    }

    private void registerFinding(List<Finding> findings, Set<String> dedup, FileContext ctx,
                                 SecretPattern sp, String value, int lineNum, String snippet) {
        String hash = hashString(sp.getName() + value + lineNum);
        if (dedup.add(hash)) {
            Finding f = Finding.builder()
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

    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
