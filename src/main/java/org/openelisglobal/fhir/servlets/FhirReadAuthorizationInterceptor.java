package org.openelisglobal.fhir.servlets;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.PolicyEnum;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resource-type read grants from the existing role/module permission registry.
 */
public class FhirReadAuthorizationInterceptor extends AuthorizationInterceptor {

    private static final Map<String, List<String>> RESOURCES = Map.of("fhir.read.results",
            List.of("Observation", "DiagnosticReport", "Specimen", "ServiceRequest"), "fhir.read.patients",
            List.of("Patient"), "fhir.read.reference", List.of("Practitioner", "Organization", "Location", "Device"));

    public FhirReadAuthorizationInterceptor() {
        super(PolicyEnum.DENY);
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails requestDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        RuleBuilder rules = new RuleBuilder();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return rules.denyAll().build();
        }
        Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        RESOURCES.forEach((permission, resources) -> {
            if (authorities.contains(permission)) {
                for (String resource : resources) {
                    rules.allow().read().resourcesOfType(resource).withAnyId();
                }
            }
        });
        if (RESOURCES.keySet().stream().anyMatch(authorities::contains)) {
            rules.allow().metadata();
        }
        return rules.denyAll().build();
    }
}
