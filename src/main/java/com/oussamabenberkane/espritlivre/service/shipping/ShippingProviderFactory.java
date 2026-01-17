package com.oussamabenberkane.espritlivre.service.shipping;

import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Factory to get the appropriate shipping provider service based on the ShippingProvider enum.
 */
@Component
public class ShippingProviderFactory {

    private final YalidineService yalidineService;

    public ShippingProviderFactory(YalidineService yalidineService) {
        this.yalidineService = yalidineService;
    }

    /**
     * Get the shipping provider service for the given provider.
     *
     * @param provider the shipping provider enum value
     * @return an Optional containing the service if available, or empty if not implemented
     */
    public Optional<ShippingProviderService> getService(ShippingProvider provider) {
        if (provider == null) {
            return Optional.empty();
        }

        return switch (provider) {
            case YALIDINE -> Optional.of(yalidineService);
            case ZR -> Optional.empty(); // Not yet implemented
        };
    }
}
