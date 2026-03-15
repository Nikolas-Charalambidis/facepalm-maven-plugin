package dev.nichar.facepalm;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
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
    void clean_project(final MavenExecutionResult result) {
        // Verifies the build finished without errors and the scanner log matches expected success.
        assertThat(result)
            .isSuccessful()
            .out().info().contains("SCAN SUCCESS");

        // Confirms the HTML report was actually generated in the target directory of the test project.
        assertThat(result.getMavenProjectResult().getTargetProjectDirectory()
            .resolve("target/facepalm-report.html"))
            .exists();
    }

    /**
     * Verifies findings are logged without breaking the build when failOnError is false.
     *
     * @param result Maven execution result
     */
    //@MavenTest
    void dirty_project(final MavenExecutionResult result) {
        // Build should succeed if failOnError is false, but log finding details.
        assertThat(result)
            .isSuccessful()
            .out().info().contains("SCAN FAILURE").contains("AWS Access Key");
    }

    /**
     * Verifies that the plugin correctly terminates the build when high-risk secrets exist.
     *
     * @param result Maven execution result
     */
    //@MavenTest
    void dirty_project_fail_on_error(final MavenExecutionResult result) {
        // Assert that the Maven process exited with a non-zero status code.
        assertThat(result).isFailure();
    }
}
