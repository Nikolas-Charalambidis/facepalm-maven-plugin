package dev.nichar.facepalm;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;


@MavenJupiterExtension
class FacepalmMojoIT {

    @MavenTest
    @MavenGoal("verify")
    void clean_project(MavenExecutionResult result) {
        assertThat(result)
            .isSuccessful()
            .out().info().contains("SCAN SUCCESS");

        assertThat(result.getMavenProjectResult().getTargetProjectDirectory()
            .resolve("target/facepalm-report.html"))
            .exists();
    }

    //@MavenTest
    void dirty_project(MavenExecutionResult result) {
        assertThat(result)
            .isSuccessful() // failOnError=false
            .out().info().contains("SCAN FAILURE").contains("AWS Access Key");
    }

    //@MavenTest
    void dirty_project_fail_on_error(MavenExecutionResult result) {
        assertThat(result).isFailure();
    }
}
