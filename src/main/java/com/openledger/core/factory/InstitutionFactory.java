package com.openledger.core.factory;

import com.openledger.institutions.common.InstitutionService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for dynamic institution routing.
 * Provides access to institution-specific services based on institution ID.
 *
 * TODO: Register all institution services when they are implemented
 */
@Component
public class InstitutionFactory {

    private final Map<String, InstitutionService> institutionServices;

    public InstitutionFactory(List<InstitutionService> services) {
        this.institutionServices = new HashMap<>();
        for (InstitutionService service : services) {
            institutionServices.put(service.getInstitutionId().toUpperCase(), service);
        }
    }

    /**
     * Get institution service by ID.
     *
     * @param institutionId Institution identifier (e.g., "BK", "MTN", "EQUITY")
     * @return Optional containing the service if found
     */
    public Optional<InstitutionService> getService(String institutionId) {
        return Optional.ofNullable(institutionServices.get(institutionId.toUpperCase()));
    }

    /**
     * Get institution service by ID, throwing exception if not found.
     *
     * @param institutionId Institution identifier
     * @return The institution service
     * @throws IllegalArgumentException if institution is not supported
     */
    public InstitutionService getServiceOrThrow(String institutionId) {
        return getService(institutionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported institution: " + institutionId));
    }

    /**
     * Check if an institution is supported.
     *
     * @param institutionId Institution identifier
     * @return true if supported
     */
    public boolean isSupported(String institutionId) {
        return institutionServices.containsKey(institutionId.toUpperCase());
    }

    /**
     * Get all supported institution IDs.
     *
     * @return List of institution IDs
     */
    public List<String> getSupportedInstitutions() {
        return List.copyOf(institutionServices.keySet());
    }
}
