package dev.nichar.facepalm;

import java.io.File;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nichar.facepalm.report.FindingReport;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import javax.annotation.Nonnull;

@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE)
public class FacepalmReportMojo extends AbstractMavenReport {

    @Override
    public String getOutputName() {
        return "facepalm-report";
    }

    @Override
    public String getName(@Nonnull final Locale locale) {
        return "Facepalm Report";
    }

    @Override
    public String getDescription(@Nonnull final Locale locale) {
        return "Scan results for leaked secrets.";
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        // Output directory is injected via Maven: @Parameter(defaultValue = "${project.reporting.outputDirectory}", readonly = true)
        getLog().info("### " + outputDirectory.getAbsolutePath().toString());
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
            // Read the JSON back into a List of Finding objects
            findings = mapper.readValue(
                resultsFile,
                mapper.getTypeFactory().constructCollectionType(List.class, FindingReport.class)
            );
        } catch (Exception e) {
            throw new MavenReportException("Failed to read facepalm-results.json. Is the file corrupted?", e);
        }

        // Render the HTML using the Doxia Sink
        renderReport(findings);
    }

    private void renderReport(List<FindingReport> findings) {
        Sink sink = getSink();

        // 1. Document Setup
        sink.head();
        sink.title();
        sink.text("Facepalm Security Report");
        sink.title_();
        sink.head_();

        sink.body();

        // 2. Header Section
        sink.section1();
        sink.sectionTitle1();
        sink.text("Facepalm Security Scan Results");
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text("The following potential secrets and security risks were identified during the verify phase.");
        sink.paragraph_();

        // 3. Summary Statistics
        long errors = findings.stream().filter(f -> "ERROR".equals(f.getFinalSeverity())).count();
        long warnings = findings.stream().filter(f -> "WARNING".equals(f.getFinalSeverity())).count();

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

        // 4. Findings Table
        if (findings.isEmpty()) {
            sink.paragraph();
            sink.italic();
            sink.text("No security findings detected. Your codebase looks clean!");
            sink.italic_();
            sink.paragraph_();
        } else {
            sink.table();

            // Table Header
            sink.tableRow();
            sink.tableHeaderCell(); sink.text("Severity"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Score"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Location"); sink.tableHeaderCell_();
            sink.tableHeaderCell(); sink.text("Pattern / Secret"); sink.tableHeaderCell_();
            sink.tableRow_();

            // Table Rows
            for (FindingReport finding : findings) {
                sink.tableRow();

                // Severity Column
                sink.tableCell();
                if ("ERROR".equals(finding.getFinalSeverity())) {
                    sink.bold(); sink.text("HIGH"); sink.bold_();
                } else {
                    sink.text(finding.getFinalSeverity());
                }
                sink.tableCell_();

                // Score Column
                sink.tableCell();
                sink.text(String.format("%.1f", finding.getFinalScore()));
                sink.tableCell_();

                // Location Column (File + Line)
                sink.tableCell();
                sink.italic();
                sink.text(finding.getFileAbsolutePath() + " (Line: " + finding.getLineNumber() + ")");
                sink.italic_();
                sink.tableCell_();

                // Pattern and Masked Secret
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
     * Renders a fallback report if the JSON data is missing.
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
