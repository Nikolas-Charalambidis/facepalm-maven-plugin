package dev.nichar.facepalm;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nichar.facepalm.report.FindingReport;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import jakarta.annotation.Nonnull;

/**
 * Maven Site reporting Mojo that generates an integrated security scan report.
 * Consumes the JSON findings produced during the 'scan' phase.
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE)
public class FacepalmReportMojo extends AbstractMavenReport {

    /**
     * Unique identifier for the report in the Maven Site navigation.
     */
    @Override
    public String getOutputName() {
        return "facepalm-report";
    }

    /**
     * Localized name of the report for the Site index.
     */
    @Override
    public String getName(@Nonnull final Locale locale) {
        return "Facepalm Report";
    }

    /**
     * Localized description of the report's purpose.
     */
    @Override
    public String getDescription(@Nonnull final Locale locale) {
        return "Scan results for leaked secrets.";
    }

    /**
     * Core execution logic for rendering the Maven Site report.
     * Deserializes findings from the target directory and transforms them into Doxia Sink elements.
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        // Output directory is injected via Maven: @Parameter(defaultValue = "${project.reporting.outputDirectory}", readonly = true)
        getLog().debug("Facepalm report generation output directory: " + outputDirectory.getAbsolutePath());
        
        // Resolve findings relative to the reporting output directory (usually target/site)
        File resultsFile = outputDirectory.toPath().resolve("..").resolve("facepalm-findings.json").normalize().toFile();

        if (!resultsFile.exists()) {
            getLog().warn("Facepalm results not found at " + resultsFile.getAbsolutePath() +
                ". Please run 'mvn verify' (or 'mvn facepalm:scan') before generating the site report.");
            renderEmptyReport();
            return;
        }

        List<FindingReport> findings;
        try {
            final var mapper = new ObjectMapper();
            // Deserializes findings from the machine-readable JSON format.
            findings = mapper.readValue(
                resultsFile,
                mapper.getTypeFactory().constructCollectionType(List.class, FindingReport.class)
            );
        } catch (Exception e) {
            throw new MavenReportException("Failed to read facepalm-results.json. Is the file corrupted?", e);
        }

        // Generate the HTML content using Maven's Doxia Sink API.
        renderReport(findings);
    }

    /**
     * Translates a list of findings into structured HTML via the Doxia Sink API.
     */
    private void renderReport(List<FindingReport> findings) {
        final var sink = getSink();

        // Sort the findings.
        findings.sort(Comparator.comparing(FindingReport::getFinalScore, Comparator.reverseOrder()));

        // Document Head Section
        sink.head();
        sink.title();
        sink.text("Facepalm Security Report");
        sink.title_();
        sink.head_();

        sink.body();

        // Page Header
        sink.section1();
        sink.sectionTitle1();
        sink.text("Facepalm Security Scan Results");
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text("The following potential secrets and security risks were identified during the verify phase.");
        sink.paragraph_();

        // High-level Summary Breakdown
        final long errors = findings.stream().filter(fr -> "ERROR".equals(fr.getFinalSeverity())).count();
        final long warnings = findings.stream().filter(fr -> "WARNING".equals(fr.getFinalSeverity())).count();

        sink.list();
        sink.listItem();
        sink.bold(); sink.text("Critical Findings: "); sink.bold_();
        sink.text(String.valueOf(errors));
        sink.listItem_();
        sink.listItem();
        sink.bold(); sink.text("Warnings: "); sink.bold_();
        sink.text(String.valueOf(warnings));
        sink.listItem_();
        sink.list_();

        sink.horizontalRule();

        // Detailed Findings Tabular Data
        if (findings.isEmpty()) {
            sink.paragraph();
            sink.italic();
            sink.text("No security findings detected. Your codebase looks clean!");
            sink.italic_();
            sink.paragraph_();
        } else {
            sink.table();

            // Render Table Header
            sink.tableRow();
            sink.tableHeaderCell(); sink.text("Severity"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Total Score"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Risk Score"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Confidence Score"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Location"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Pattern / Secret"); sink.tableHeaderCell_();
            sink.tableRow_();

            // Populate Table Rows


            for (FindingReport finding : findings) {
                sink.tableRow();

                // Severity Column with High-risk emphasis
                sink.tableCell();
                if ("ERROR".equals(finding.getFinalSeverity())) {
                    sink.bold(); sink.text("HIGH"); sink.bold_();
                } else {
                    sink.text(finding.getFinalSeverity());
                }
                sink.tableCell_();

                // Final Score Column
                sink.tableCell();
                sink.bold();
                sink.text(String.format("%.1f", finding.getFinalScore()));
                sink.bold_();
                sink.tableCell_();

                // Risk Score Column
                sink.tableCell();
                sink.text(String.valueOf(finding.getRiskScore()));
                sink.tableCell_();

                // Confidence Score Column
                sink.tableCell();
                sink.text(String.valueOf(finding.getConfidenceScore()));
                sink.tableCell_();

                // File Path and Line Number
                sink.tableCell();
                sink.italic();
                sink.text(finding.getFileAbsolutePath() + " (Line: " + finding.getLineNumber() + ")");
                sink.italic_();
                sink.tableCell_();

                // Masked Secret and Pattern Identification
                sink.tableCell();
                sink.text(finding.getPatternName() + ": ");
                sink.monospaced();
                sink.text(finding.getMaskedSecret());
                sink.monospaced_();
                sink.tableCell_();

                sink.tableRow_();
            }
            sink.table_();
        }

        sink.section1_();
        sink.body_();

        sink.flush();
        sink.close();
    }
    
    /**
     * Renders a placeholder message if the scanner has not been executed yet.
     */
    private void renderEmptyReport() {
        org.apache.maven.doxia.sink.Sink sink = getSink();
        sink.head();
        sink.title();
        sink.text("Facepalm Scan Results");
        sink.title_();
        sink.head_();

        sink.body();
        sink.section1();
        sink.sectionTitle1();
        sink.text("Facepalm Security Report");
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text("No scan data available. You must run the facepalm:scan goal prior to site generation to see results here.");
        sink.paragraph_();

        sink.section1_();
        sink.body_();
        sink.flush();
        sink.close();
    }
}
