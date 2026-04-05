# Reporting

Facepalm provides integrated reporting to help you visualize and track potential secrets in your codebase. Reports are decoupled from the core build-breaking scanner to keep your CI/CD pipelines fast and focused.

## How to Generate Reports

```xml
<reporting>
    <plugins>
        <plugin>
            <groupId>dev.nichar</groupId>
            <artifactId>facepalm-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
        </plugin>
    </plugins>
</reporting>
```

### 2. Run the Site Goal

To generate the reports, run:

```bash
mvn site
```

The reports will be available in the `target/site/` directory.

> **Note:** The reporting goal relies on data collected during the scan phase. Ensure you have run `mvn facepalm:scan` (or a full `mvn verify`) at least once before generating the site.
