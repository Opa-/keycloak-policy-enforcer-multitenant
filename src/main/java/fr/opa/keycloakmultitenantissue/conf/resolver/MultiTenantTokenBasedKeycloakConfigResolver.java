package fr.opa.keycloakmultitenantissue.conf.resolver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.opa.keycloakmultitenantissue.conf.properties.MultiTenantConfigurationProperties;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.springboot.KeycloakSpringBootProperties;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom {@link KeycloakConfigResolver} handling Multi-Tenancy - see https://www.keycloak.org/docs/latest/securing_apps/index.html#_multi_tenancy
 */
public class MultiTenantTokenBasedKeycloakConfigResolver implements KeycloakConfigResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTenantTokenBasedKeycloakConfigResolver.class);

    private final ConcurrentHashMap<String, KeycloakDeployment> cache = new ConcurrentHashMap<>();
    private final MultiTenantConfigurationProperties multiTenantConfigurationProperties;
    private final KeycloakSpringBootProperties keycloakSpringBootProperties;

    public MultiTenantTokenBasedKeycloakConfigResolver(MultiTenantConfigurationProperties multiTenantConfigurationProperties,
                                                       KeycloakSpringBootProperties keycloakSpringBootProperties) {
        this.multiTenantConfigurationProperties = multiTenantConfigurationProperties;
        this.keycloakSpringBootProperties = keycloakSpringBootProperties;
    }

    /**
     * Resolve the Keycloak configuration to use depending on the path of the request.
     * Resolve happen in 3 steps:
     * - Try to get the realm from the request's header token if any is present in the request.
     * - Try to get the realm from the {@link KeycloakAuthenticationToken} if any is present in the request.
     * - Returns default realm if none of the above worked.
     *
     * @param request An instance of {@link OIDCHttpFacade.Request}.
     * @return An instance of {@link KeycloakDeployment}.
     */
    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        String issuer = getIssuer(request);
        LOGGER.info("Issuer: {}", issuer);
        if (issuer != null) {
            if (!cache.contains(issuer)) {
                cache.put(issuer, Objects.requireNonNull(resolveHeaderBased(request)));
            }
        } else {
            issuer = "default";
            if (!cache.contains("default")) {
                cache.put("default", resolveDefault());
            }
        }
        return cache.get(issuer);
    }

    private String getIssuer(OIDCHttpFacade.Request request) {
        String token = getKeycloakToken(request);
        if (token == null) return null;
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getIssuer();
    }

    /**
     * Resolve which Realm to use based on the Keycloak token content.
     *
     * @return An instance of {@link KeycloakDeployment} or null if no {@link KeycloakAuthenticationToken} is present.
     */
    private KeycloakDeployment resolveHeaderBased(OIDCHttpFacade.Request request) {
        String tokenString = getKeycloakToken(request);
        if (tokenString != null) {
            DecodedJWT jwt = JWT.decode(tokenString);
            String issuerFromToken = jwt.getIssuer();
            LOGGER.info("Found issuer [{}] in JWT.", issuerFromToken);

            // Search in all declared KeycloakProperties which contains the right Realm
            for (KeycloakSpringBootProperties resolvedKeycloakSpringBootProperties : multiTenantConfigurationProperties.getRealms()) {
                KeycloakSpringBootProperties currentConfig = new KeycloakSpringBootProperties();
                BeanUtils.copyProperties(resolvedKeycloakSpringBootProperties, currentConfig);
                String authServerUrl = currentConfig.getAuthServerUrl();
                String realm = currentConfig.getRealm();
                String issuerFromConfig = URI.create(authServerUrl + "/realms/" + realm).normalize().toString();
                LOGGER.info("Checking JWT issuer [{}] against [{}].", issuerFromToken, issuerFromConfig);
                if (issuerFromConfig.equals(issuerFromToken)) {
//                    currentConfig.setSecurityConstraints(keycloakSpringBootProperties.getSecurityConstraints());
//                    currentConfig.setPolicyEnforcerConfig(keycloakSpringBootProperties.getPolicyEnforcerConfig());
                    LOGGER.info("Policy Enforcer set for config. Returning config for realm [{}] and issuer [{}]", realm, issuerFromConfig);
                    LOGGER.info("Realm resolved by HEADER: {}", realm);
                    return KeycloakDeploymentBuilder.build(currentConfig);
                }
            }
        } else {
            LOGGER.info("JWT is null");
        }
        return null;
    }

    /**
     * Resolve default realm. Usually set to customer realm.
     *
     * @return An instance of {@link KeycloakDeployment}.
     */
    private KeycloakDeployment resolveDefault() {
        KeycloakSpringBootProperties currentConfig = new KeycloakSpringBootProperties();
        BeanUtils.copyProperties(multiTenantConfigurationProperties.getRealms().get(0), currentConfig);
//        currentConfig.setSecurityConstraints(keycloakSpringBootProperties.getSecurityConstraints());
//        currentConfig.setPolicyEnforcerConfig(keycloakSpringBootProperties.getPolicyEnforcerConfig());
        return KeycloakDeploymentBuilder.build(currentConfig);
    }

    /**
     * This method has been shamelessly copy/paste from
     * https://github.com/keycloak/keycloak/blob/7f1de02/adapters/oidc/adapter-core/src/main/java/org/keycloak/adapters/BearerTokenRequestAuthenticator.java
     * in order to parse the authentication token from the header.
     * <p>
     * As the token is Keycloak's and the code is Keycloak's, this should prevent any issue with a custom implementation.
     *
     * @return A properly cleaned up Keycloak token
     */
    private String getKeycloakToken(OIDCHttpFacade.Request request) {
        List<String> authHeaders = request.getHeaders("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            LOGGER.info("No Authorization header.");
            return null;
        }

        String tokenString = null;
        for (String authHeader : authHeaders) {
            String[] split = authHeader.trim().split("\\s+");
            if (split.length != 2) continue;
            if (split[0].equalsIgnoreCase("Bearer")) {
                tokenString = split[1];
                LOGGER.info("Found {} values in authorization header, selecting the first value for Bearer.", authHeaders.size());
                break;
            }
        }

        return tokenString;
    }
}
