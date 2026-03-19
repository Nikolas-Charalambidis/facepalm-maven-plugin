package dev.nichar.facepalm;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.MavenVerbose;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;


/**
 * JUnit 5 extension that manages the execution of isolated Maven builds for testing.
 *
 * @author Nikolas Charalambidis
 */
@MavenJupiterExtension
class FacepalmMojoIT {

    /**
     * Triggers a full Maven execution using the 'verify' goal on a clean project sample.
     *
     * @param result Maven execution result
     */
    @MavenTest
    @MavenGoal("verify")
    @MavenVerbose
    void clean_project(final MavenExecutionResult result) {
        // Verifies the build finished without errors and the scanner log matches expected success.
        assertThat(result).isSuccessful();

        assertThat(result).out().info()
            .contains("Discovered 5 files...")
            .contains("Starting scan of 4 files...")
            .contains("SCAN RESULT : SUCCESS")
            .contains("No secrets or sensitive patterns detected. Your secrets are safe.")
            .contains("BUILD SUCCESS");

        final var targetDirectory = result.getMavenProjectResult().getTargetProjectDirectory();
        assertThat(targetDirectory.resolve("target/facepalm-report.html")).exists();
        assertThat(targetDirectory.resolve("target/facepalm-report.sarif")).exists();
    }

    /**
     * Verifies findings are logged without breaking the build when failOnError is false.
     *
     * @param result Maven execution result
     */
    @MavenTest
    @MavenGoal("verify")
    @MavenVerbose
    void dirty_project(final MavenExecutionResult result) {
        // Verifies the build finished without errors and the scanner log matches expected success.
        assertThat(result).isSuccessful();

        assertThat(result).out().warn()
            .contains("[Generic Password Assignment] Score: 69.8 (R:85/C:60) - \uD83D\uDFE1");
        assertThat(result).out().info()
            .contains("  Location: /Users/nikolas/Projects/Personal/facepalm-maven-plugin/target/maven-it/dev/nichar/facepalm/FacepalmMojoIT/dirty_project/project/src/main/resources/application.properties:2");

        assertThat(result).out().info()
            .contains("Discovered 5 files...")
            .contains("Starting scan of 4 files...")
            .contains("SCAN RESULT : WARNINGS")
            .contains("Warnings detected. Review recommended.")
            .contains("BUILD SUCCESS");

        final var targetDirectory = result.getMavenProjectResult().getTargetProjectDirectory();
        assertThat(targetDirectory.resolve("target/facepalm-report.html")).exists();
        assertThat(targetDirectory.resolve("target/facepalm-report.sarif")).exists();
    }

    /**
     * Verifies that the plugin correctly terminates the build when high-risk secrets exist.
     *
     * @param result Maven execution result
     */
    @MavenTest
    @MavenGoal("verify")
    @MavenVerbose
    void dirty_project_fail_on_warnings(final MavenExecutionResult result) {
        // Verifies the build finished with errors and the scanner log matches expected success.
        assertThat(result).isFailure();

        assertThat(result).out().warn()
            .contains("[Generic Password Assignment] Score: 69.8 (R:85/C:60) - \uD83D\uDFE1");
        assertThat(result).out().info()
            .contains("  Location: /Users/nikolas/Projects/Personal/facepalm-maven-plugin/target/maven-it/dev/nichar/facepalm/FacepalmMojoIT/dirty_project_fail_on_warnings/project/src/main/resources/application.properties:2");

        assertThat(result).out().info()
            .contains("Discovered 5 files...")
            .contains("Starting scan of 4 files...")
            .contains("SCAN RESULT : WARNINGS")
            .contains("Warnings detected. Review recommended.")
            .contains("BUILD FAILURE");

        final var targetDirectory = result.getMavenProjectResult().getTargetProjectDirectory();
        assertThat(targetDirectory.resolve("target/facepalm-report.html")).exists();
        assertThat(targetDirectory.resolve("target/facepalm-report.sarif")).exists();
    }
}
