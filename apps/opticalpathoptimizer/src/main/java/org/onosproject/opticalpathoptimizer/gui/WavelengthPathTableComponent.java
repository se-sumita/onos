package org.onosproject.opticalpathoptimizer.gui;

import com.google.common.collect.ImmutableList;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wavelength-path Table-View application component.
 */
@Component(immediate = true)
public class WavelengthPathTableComponent {

    private static final String VIEW_ID = "wavelengthPaths";
    private static final String VIEW_TEXT = "Wavelength Paths";
    private static final String RESOURCE_PATH = "gui/wavelengthPaths";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiView(UiView.Category.NETWORK, VIEW_ID, VIEW_TEXT)
    );

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new WavelengthPathTableMessageHandler()
            );

    // Application UI extension
    protected UiExtension extension =
            new UiExtension.Builder(getClass().getClassLoader(), uiViews)
                    .resourcePath(RESOURCE_PATH)
                    .messageHandlerFactory(messageHandlerFactory)
                    .ui2()
                    .build();

    @Activate
    protected void activate() {
        uiExtensionService.register(extension);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
        log.info("Stopped");
    }
}
