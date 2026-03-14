package dev.nichar.facepalm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import java.util.List;

public class SisuBootstrapper {

    public static Injector createInjector() {
        ClassLoader classLoader = SisuBootstrapper.class.getClassLoader();
        ClassSpace space = new URLClassSpace(classLoader);

        return Guice.createInjector(
            new WireModule(
                new SpaceModule(space, BeanScanning.INDEX),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Log.class).to(SystemStreamLog.class);
                        
                        // Explicitly handle list injections if Sisu doesn't auto-collect them
                        Multibinder<FacepalmScanner.SecretExtractor> extractorBinder = Multibinder.newSetBinder(binder(), FacepalmScanner.SecretExtractor.class);
                        extractorBinder.addBinding().to(FacepalmScanner.RegexSecretExtractor.class);

                        Multibinder<FacepalmScanner.FindingEvaluator> evaluatorBinder = Multibinder.newSetBinder(binder(), FacepalmScanner.FindingEvaluator.class);
                        evaluatorBinder.addBinding().to(FacepalmScanner.PlaceholderSuppressorEvaluator.class);
                        evaluatorBinder.addBinding().to(FacepalmScanner.PublicExposureEvaluator.class);
                        evaluatorBinder.addBinding().to(FacepalmScanner.FileExtensionEvaluator.class);
                        evaluatorBinder.addBinding().to(FacepalmScanner.LocationEvaluator.class);
                        evaluatorBinder.addBinding().to(FacepalmScanner.SurroundingContextEvaluator.class);
                        evaluatorBinder.addBinding().to(FacepalmScanner.EntropyEvaluator.class);

                        Multibinder<FacepalmScanner.FileFindingsPostProcessor> processorBinder = Multibinder.newSetBinder(binder(), FacepalmScanner.FileFindingsPostProcessor.class);
                        processorBinder.addBinding().to(FacepalmScanner.CompositeScoringPostProcessor.class);
                    }
                }
            )
        );
    }
}
