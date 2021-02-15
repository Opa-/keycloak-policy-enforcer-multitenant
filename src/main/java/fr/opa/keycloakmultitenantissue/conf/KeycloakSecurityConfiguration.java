package fr.opa.keycloakmultitenantissue.conf;

import fr.opa.keycloakmultitenantissue.conf.properties.MultiTenantConfigurationProperties;
import fr.opa.keycloakmultitenantissue.conf.resolver.MultiTenantTokenBasedKeycloakConfigResolver;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootProperties;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import javax.annotation.PostConstruct;

/**
 * Spring Security configuration with Keycloak.
 * Especially configure method.
 */
@KeycloakConfiguration
@EnableConfigurationProperties(MultiTenantConfigurationProperties.class)
//@PropertySource("classpath:keycloak/policy-enforcer-config.properties")
//@PropertySource("classpath:keycloak/security-constraints.properties")
@PropertySource("classpath:keycloak/keycloak.properties")
public class KeycloakSecurityConfiguration extends KeycloakWebSecurityConfigurerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakSecurityConfiguration.class);
    private final MultiTenantConfigurationProperties multiTenantConfigurationProperties;
    private final KeycloakSpringBootProperties keycloakSpringBootProperties;

    public KeycloakSecurityConfiguration(MultiTenantConfigurationProperties multiTenantConfigurationProperties, KeycloakSpringBootProperties keycloakSpringBootProperties) {
        this.multiTenantConfigurationProperties = multiTenantConfigurationProperties;
        this.keycloakSpringBootProperties = keycloakSpringBootProperties;
    }

    /**
     * Registers the KeycloakAuthenticationProvider with the authentication manager.
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    /**
     * Defines the session authentication strategy.
     */
    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    @Bean(name = "keycloakConfigResolver")
    public KeycloakConfigResolver keycloakSpringBootConfigResolver() {
        return new MultiTenantTokenBasedKeycloakConfigResolver(multiTenantConfigurationProperties, keycloakSpringBootProperties);
    }

    @Override
    @SuppressWarnings("findsecbugs:SPRING_CSRF_PROTECTION_DISABLED")
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http
                // We do not use CSRF protection that is more suitable for MVC pattern. This avoid having a complex security config for swagger.
                .csrf().disable()
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .logout().invalidateHttpSession(true).deleteCookies("JSESSIONID");
    }

    @Configuration
    class MultiTenantProperties {
        /**
         * Check that realms are setup properly for a multi-tenant profile.
         */
        @PostConstruct
        public void multiTenantConfigCheck() {
            LOGGER.info("Keycloak multi-tenant mode enabled");
            if (multiTenantConfigurationProperties.getRealms() == null || multiTenantConfigurationProperties.getRealms().isEmpty()) {
                LOGGER.error("No configuration of realm found.");
            } else if (multiTenantConfigurationProperties.getRealms().size() == 1) {
                LOGGER.warn("Only one configuration of realm found, consider moving away from a Keycloak multi-tenant configuration.");
            }
        }
    }
}
