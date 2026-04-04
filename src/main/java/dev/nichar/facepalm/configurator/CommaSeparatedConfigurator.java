package dev.nichar.facepalm.configurator;


import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;


/**
 * Custom Maven configurator that enables injecting comma-separated strings into {@link java.util.Set} parameters.
 * It registers a specialized converter to handle flat XML values and CLI properties as unique, trimmed sets.
 */
@Component(role = ComponentConfigurator.class, hint = CommaSeparatedConfigurator.COMMA_SEPARATED_CONFIGURATOR)
public class CommaSeparatedConfigurator extends BasicComponentConfigurator {

    /**
     * A unique string identifier used to link this custom configuration logic to a specific Mojo.
     */
    public static final String COMMA_SEPARATED_CONFIGURATOR = "comma-separated-configurator";

    @Override
    public void configureComponent(final Object component,
                                   final PlexusConfiguration configuration,
                                   final ExpressionEvaluator evaluator,
                                   final ClassRealm realm,
                                   final ConfigurationListener listener) throws ComponentConfigurationException {

        // Register the converter that handles comma-separated strings for Set fields.
        converterLookup.registerConverter(new CommaSeparatedSetConverter());

        // Delegate to the base class to perform the actual injection using the updated lookup table
        super.configureComponent(component, configuration, evaluator, realm, listener);
    }
}
