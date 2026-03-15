package dev.nichar.facepalm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;


public class FacepalmCLI {

    // Only inject stateless services
    @Inject private FacepalmScanner.FacepalmContext context;
    @Inject private FacepalmScanner.FacepalmRunner runner;

    public static void main(String[] args) {
        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();

        Injector injector = Guice.createInjector(
            new WireModule(
                new SpaceModule(new URLClassSpace(FacepalmCLI.class.getClassLoader()), BeanScanning.CACHE),
                new CliModule()
            )
        );

        FacepalmCLI cli = injector.getInstance(FacepalmCLI.class);
        cli.run(root);
    }

    private void run(Path root) {
        try {
            // Instantiate your config (normally you'd parse CLI args into this object)
            FacepalmConfig cliConfig = new FacepalmConfig();
            context.set(cliConfig);

            // Execute the stateless runner
            runner.run(root, Paths.get("target"), "1.0.0-CLI");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            context.clear();
        }
    }

    public static class CliModule extends AbstractModule {
        @Override
        protected void configure() {
            final var plexusLogger = new ConsoleLogger(Logger.LEVEL_DEBUG, "facepalm-cli");
            bind(Logger.class).toInstance(plexusLogger);

            // Note: Since you use DefaultLog, ensure you have the maven-plugin-api dependency available
            // at runtime for the CLI. If not, fallback to SystemStreamLog.
            bind(Log.class).toInstance(new org.apache.maven.monitor.logging.DefaultLog(plexusLogger));
        }
    }
}
