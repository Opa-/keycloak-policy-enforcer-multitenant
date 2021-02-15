package fr.opa.keycloakmultitenantissue.conf.properties;

import org.keycloak.adapters.springboot.KeycloakSpringBootProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Simple POJO to store Keycloak configuration with multiple realms.
 * See:     - https://www.keycloak.org/docs/latest/securing_apps/index.html#_multi_tenancy
 * - https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config-typesafe-configuration-properties
 * - https://www.baeldung.com/configuration-properties-in-spring-boot
 */
@ConfigurationProperties(prefix = "keycloak-multitenant", ignoreUnknownFields = false)
public class MultiTenantConfigurationProperties {
    private List<KeycloakSpringBootProperties> realms;

    public List<KeycloakSpringBootProperties> getRealms() {
        return realms;
    }

    public void setRealms(List<KeycloakSpringBootProperties> realms) {
        this.realms = realms;
    }
}
