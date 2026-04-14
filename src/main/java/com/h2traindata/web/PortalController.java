package com.h2traindata.web;

import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.web.portal.PortalPageRenderer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalController {

    private final ProviderRegistry providerRegistry;
    private final PortalPageRenderer portalPageRenderer;

    public PortalController(ProviderRegistry providerRegistry,
                            PortalPageRenderer portalPageRenderer) {
        this.providerRegistry = providerRegistry;
        this.portalPageRenderer = portalPageRenderer;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return portalPageRenderer.render(providerRegistry.registeredProviderIds());
    }
}
