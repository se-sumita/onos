package org.onosproject.opticalpathoptimizer.gui;

import com.google.common.collect.ImmutableList;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Wavelength-path Topology-View application component.
 */
@Component(immediate = true)
public class WavelengthPathTopovComponent {

    private static final ClassLoader CL = WavelengthPathTopovComponent.class.getClassLoader();
    private static final String VIEW_ID = "wavelengthPathTopov";
    private static final String RESOURCE_PATH = "gui/wavelengthPathTopov";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiViewHidden(VIEW_ID)
    );

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new WavelengthPathTopovMessageHandler()
            );

    // Factory for UI topology overlays
    private final UiTopoOverlayFactory topoOverlayFactory =
            () -> ImmutableList.of(
                    new WavelengthPathTopovOverlay()
            );

    // Application UI extension
    protected UiExtension extension =
            new UiExtension.Builder(CL, uiViews)
                    .resourcePath(RESOURCE_PATH)
                    .messageHandlerFactory(messageHandlerFactory)
                    .topoOverlayFactory(topoOverlayFactory)
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
