package org.openelisglobal.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.GenericValidator;
import org.jasypt.util.text.AES256TextEncryptor;
import org.jasypt.util.text.TextEncryptor;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.openelisglobal.security.KeystoreUtil.KeyCertPair;
import org.openelisglobal.security.login.BasicAuthFilter;
import org.openelisglobal.security.login.CustomAuthenticationFailureHandler;
import org.openelisglobal.security.login.CustomFormAuthenticationSuccessHandler;
import org.openelisglobal.security.login.CustomSSOAuthenticationSuccessHandler;
import org.openelisglobal.spring.util.SpringContext;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.AssertionToken;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.multipart.support.MultipartFilter;

@EnableWebSecurity
public class SecurityConfig {

    // TODO should we move these to the properties files?
    // pages that have special security constraints
    public static final String[] OPEN_PAGES = { "/pluginServlet/**", "/ChangePasswordLogin",
            "/UpdateLoginChangePassword", "/health/**", "/rest/open-configuration-properties", "/docs/UserManual" };
    public static final String[] LOGIN_PAGES = { "/LoginPage", "/ValidateLogin", "/session" };

    public static final String[] AUTH_OPEN_PAGES = { "/Home", "/Dashboard", "/Logout", "/MasterListsPage",
            "/analyzer/runAction/**" };
    public static final String[] RESOURCE_PAGES = { "/fontawesome-free-5.13.1-web/**", "/select2/**", "/css/**",
            "/favicon/**", "/images/**", "/documentation/**", "/scripts/**", "/jsp/**" };
    // public static final String[] HTTP_BASIC_SERVLET_PAGES = {
    // "/pluginServlet/**",
    // "/importAnalyzer", "/fhir/**" };
    public static final String[] REST_CONTROLLERS = { "/Provider/**", "/rest/**" };
    // public static final String[] CLIENT_CERTIFICATE_PAGES = {};

    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval';"
            + " connect-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline';"
            + " frame-src *.openlmis.org 'self'; object-src 'self';";

    @Value("${encryption.general.password:dev}")
    private String encryptionPassword;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    public void configureGlobalSecurity(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Configuration
    @Order(1)
    public static class openSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            CharacterEncodingFilter filter = new CharacterEncodingFilter();
            filter.setEncoding("UTF-8");
            filter.setForceEncoding(true);
            http.addFilterBefore(filter, CsrfFilter.class);
            MultipartFilter multipartFilter = new MultipartFilter();
            multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
            http.addFilterBefore(multipartFilter, CsrfFilter.class);

            // for all requests going to open pages, use this security configuration
            http.requestMatchers().antMatchers(OPEN_PAGES).and().authorizeRequests().anyRequest().permitAll().and()
                    // disable csrf as it is not needed for open pages
                    .csrf().disable().headers().frameOptions().sameOrigin()
                    .contentSecurityPolicy(CONTENT_SECURITY_POLICY);
        }
    }

    @Configuration
    @Order(2)
    @ConditionalOnProperty(property = "org.itech.login.basic", havingValue = "true", matchIfMissing = true)
    public static class httpBasicServletSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            CharacterEncodingFilter filter = new CharacterEncodingFilter();
            filter.setEncoding("UTF-8");
            filter.setForceEncoding(true);
            http.addFilterBefore(filter, CsrfFilter.class);
            MultipartFilter multipartFilter = new MultipartFilter();
            multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
            http.addFilterBefore(multipartFilter, CsrfFilter.class);
            // for all requests going to a http basic page, use this security configuration
            http.requestMatcher(new BasicAuthRequestedMatcher()).authorizeRequests().anyRequest()
                    // ensure they are authenticated
                    .authenticated().and()
                    // ensure they authenticate with http basic
                    .httpBasic().and()
                    // disable csrf as it is not needed for httpBasic
                    .csrf().disable() //
                    .addFilterAt(SpringContext.getBean(BasicAuthFilter.class), BasicAuthenticationFilter.class)
                    // add security headers
                    .headers().frameOptions().sameOrigin().contentSecurityPolicy(CONTENT_SECURITY_POLICY);
        }
    }

    @Configuration
    @Order(3)
    @ConditionalOnProperty(property = "org.itech.login.saml", havingValue = "true")
    public static class samlSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Value("${org.itech.login.saml.registrationId:keycloak}")
        private String registrationId;

        @Value("${org.itech.login.saml.entityId:OpenELIS-Global_saml}")
        private String entityId;

        @Value("${server.ssl.key-store}")
        private Resource keyStore;

        @Value("${server.ssl.key-store-password}")
        private String keyStorePassword;

        @Value("${org.itech.login.saml.metadatalocation:}")
        private String metadata;

        @Value("${org.itech.login.saml.idpEntityId:}")
        private String idpEntityId;

        @Value("${org.itech.login.saml.webSSOEndpoint:}")
        private String webSSOEndpoint;

        @Value("${org.itech.login.saml.idpVerificationCertificateLocation:/run/secrets/samlIDP.crt}")
        private String idpVerificationCertificateLocation;

        @Bean("samlRelyingPartyRegistrationRepository")
        public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() throws KeyStoreException,
                CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
            RelyingPartyRegistration relyingPartyRegistration;
            final String acsUrlTemplate = "{baseUrl}/login/saml2/sso/{registrationId}";

            KeyStore keystore = KeystoreUtil.readKeyStoreFile(keyStore, keyStorePassword.toCharArray());
            KeyCertPair keyCert = KeystoreUtil.getKeyCertFromKeystore(keystore, keyStorePassword.toCharArray());
            Saml2X509Credential credential = Saml2X509Credential.signing(keyCert.getKey(),
                    (X509Certificate) keyCert.getCert());
            if (GenericValidator.isBlankOrNull(metadata)) {
                Saml2X509Credential idpVerificationCertificate;
                try (InputStream pub = new FileInputStream(new File(idpVerificationCertificateLocation))) {
                    X509Certificate c = (X509Certificate) CertificateFactory.getInstance("X.509")
                            .generateCertificate(pub);
                    idpVerificationCertificate = new Saml2X509Credential(c, Saml2X509CredentialType.VERIFICATION);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                relyingPartyRegistration = RelyingPartyRegistration.withRegistrationId(registrationId) //
                        .assertionConsumerServiceLocation(acsUrlTemplate) //
                        .signingX509Credentials(e -> e.add(credential)) //
                        .assertingPartyDetails(config -> config.entityId(idpEntityId) //
                                .singleSignOnServiceLocation(webSSOEndpoint) //
                                .singleLogoutServiceLocation(webSSOEndpoint) //
                                .wantAuthnRequestsSigned(true) //
                                .verificationX509Credentials(c -> c.add(idpVerificationCertificate))) //
                        .entityId(entityId) //
                        .build();
            } else {
                relyingPartyRegistration = RelyingPartyRegistrations.fromMetadataLocation(metadata) //
                        .registrationId(registrationId) //
                        .assertionConsumerServiceLocation(acsUrlTemplate) //
                        .signingX509Credentials(e -> e.add(credential)) //
                        .entityId(entityId) //
                        .build();
            }

            // SAML configuration
            // Mapping this application to one or more Identity Providers
            return new InMemoryRelyingPartyRegistrationRepository(relyingPartyRegistration);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            CharacterEncodingFilter filter = new CharacterEncodingFilter();
            filter.setEncoding("UTF-8");
            filter.setForceEncoding(true);
            http.addFilterBefore(filter, CsrfFilter.class);
            MultipartFilter multipartFilter = new MultipartFilter();
            multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
            http.addFilterBefore(multipartFilter, CsrfFilter.class);
            OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
            Converter<ResponseToken, Saml2Authentication> delegate = OpenSaml4AuthenticationProvider
                    .createDefaultResponseAuthenticationConverter();
            authenticationProvider
                    .setAssertionValidator(OpenSaml4AuthenticationProvider.createDefaultAssertionValidator());
            authenticationProvider.setResponseAuthenticationConverter(responseToken -> {

                Saml2Authentication authentication = delegate.convert(responseToken);
                Assertion assertion = responseToken.getResponse().getAssertions().get(0);
                AuthenticatedPrincipal principal = (AuthenticatedPrincipal) authentication.getPrincipal();
                Collection<GrantedAuthority> authorities = new KeycloakAuthoritiesExtractor().convert(assertion);

                return new Saml2Authentication(principal, authentication.getSaml2Response(), authorities);
            });
            Converter<AssertionToken, Saml2ResponseValidatorResult> validator = OpenSaml4AuthenticationProvider
                    .createDefaultAssertionValidator();
            authenticationProvider.setAssertionValidator(validator);
            http.requestMatcher(new SamlRequestedMatcher()).authorizeRequests().anyRequest().authenticated().and()
                    .saml2Logout().logoutUrl("/Logout").and().saml2Login()
                    .authenticationManager(new ProviderManager(authenticationProvider))
                    .failureHandler(customAuthenticationFailureHandler())
                    .successHandler(customAuthenticationSuccessHandler())
                    .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository());
        }

        @Bean("samlAuthenticationSuccessHandler")
        public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
            return new CustomSSOAuthenticationSuccessHandler();
        }

        @Bean("samlAuthenticationFailureHandler")
        public AuthenticationFailureHandler customAuthenticationFailureHandler() {
            return new CustomAuthenticationFailureHandler();
        }
    }

    @Configuration
    @Order(4)
    @ConditionalOnProperty(property = "org.itech.login.oauth", havingValue = "true")
    public static class openidSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Value("${org.itech.login.oauth.config:}")
        private String config;

        @Value("${org.itech.login.oauth.clientID:OpenELIS-Global_oauth}")

        private String clientID;

        @Value("${org.itech.login.oauth.clientSecret:}")
        private String clientSecret;

        private ClientRegistration clientRegistrationFromConfig(String config) {
            return ClientRegistrations.fromOidcIssuerLocation(config).clientId(clientID).clientSecret(clientSecret)
                    .build();
        }

        @Bean("oauthClientRegistrationRepository")
        public ClientRegistrationRepository clientRegistrationRepository() {
            List<ClientRegistration> registrations = new ArrayList<>();

            if (!GenericValidator.isBlankOrNull(config)) {
                registrations.add(clientRegistrationFromConfig(config));
            }
            return new InMemoryClientRegistrationRepository(registrations);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            CharacterEncodingFilter filter = new CharacterEncodingFilter();
            filter.setEncoding("UTF-8");
            filter.setForceEncoding(true);
            http.addFilterBefore(filter, CsrfFilter.class);
            MultipartFilter multipartFilter = new MultipartFilter();
            multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
            http.addFilterBefore(multipartFilter, CsrfFilter.class);
            // for all requests going to a http basic page, use this security configuration
            http.requestMatcher(new OAuthRequestedMatcher()).authorizeRequests().anyRequest()
                    // ensure they are authenticated
                    .authenticated().and() //
                    .oauth2Login().clientRegistrationRepository(clientRegistrationRepository()) //
                    .authorizedClientService(authorizedClientService())
                    .failureHandler(customAuthenticationFailureHandler())
                    .successHandler(customAuthenticationSuccessHandler()).and()
                    .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler()))
                    // add security headers
                    .headers().frameOptions().sameOrigin().contentSecurityPolicy(CONTENT_SECURITY_POLICY);
        }

        private LogoutSuccessHandler oidcLogoutSuccessHandler() {
            OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(
                    clientRegistrationRepository());
            return oidcLogoutSuccessHandler;
        }

        @Bean
        public OAuth2AuthorizedClientService authorizedClientService() {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository());
        }

        @Bean("oauthAuthenticationSuccessHandler")
        public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
            return new CustomFormAuthenticationSuccessHandler();
        }

        @Bean("oauthAuthenticationFailureHandler")
        public AuthenticationFailureHandler customAuthenticationFailureHandler() {
            return new CustomAuthenticationFailureHandler();
        }
    }

    @Configuration
    @Order(5)
    @ConditionalOnProperty(property = "org.itech.login.certificate", havingValue = "true")
    public static class clientCertificateSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            CharacterEncodingFilter filter = new CharacterEncodingFilter();
            filter.setEncoding("UTF-8");
            filter.setForceEncoding(true);
            http.addFilterBefore(filter, CsrfFilter.class);

            http.requestMatcher(new CertificateAuthRequestedMatcher()).authorizeRequests().anyRequest()
                    // ensure they are authenticated
                    .authenticated().and().x509().subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                    .userDetailsService(SpringContext.getBean(UserDetailsService.class)).and()
                    // disable csrf as it is not needed for httpBasic
                    .csrf().disable();
        }
    }

    @Configuration
    @ConditionalOnProperty(property = "org.itech.login.form", havingValue = "true", matchIfMissing = true)
    public static class defaultSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            CharacterEncodingFilter filter = new CharacterEncodingFilter();
            filter.setEncoding("UTF-8");
            filter.setForceEncoding(true);
            http.addFilterBefore(filter, CsrfFilter.class);
            MultipartFilter multipartFilter = new MultipartFilter();
            multipartFilter.setServletContext(SpringContext.getBean(ServletContext.class));
            http.addFilterBefore(multipartFilter, CsrfFilter.class);
            http.authorizeRequests()
                    // allow all users to access these pages no matter authentication status
                    .antMatchers(LOGIN_PAGES).permitAll().antMatchers(RESOURCE_PAGES).permitAll()
                    // ensure all other requests are authenticated
                    .anyRequest().authenticated().and()
                    // setup login redirection and logic
                    .formLogin().loginPage("/LoginPage").permitAll().loginProcessingUrl("/ValidateLogin")
                    .usernameParameter("loginName").passwordParameter("password")
                    .failureHandler(customAuthenticationFailureHandler())
                    .successHandler(customAuthenticationSuccessHandler()).and()
                    // setup logout
                    .logout().logoutUrl("/Logout").logoutSuccessUrl("/LoginPage").invalidateHttpSession(true).and()
                    .sessionManagement().invalidSessionUrl("/LoginPage").sessionFixation().migrateSession().and().csrf()
                    .ignoringAntMatchers("/ValidateLogin").and()
                    // add security headers
                    .headers().frameOptions().sameOrigin().contentSecurityPolicy(CONTENT_SECURITY_POLICY);
        }

        @Bean
        public AuthenticationFailureHandler customAuthenticationFailureHandler() {
            return new CustomAuthenticationFailureHandler();
        }

        @Bean
        @Primary
        public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
            return new CustomFormAuthenticationSuccessHandler();
        }
    }

    // @Bean
    // public static UserDetailsService allowAllUserDetailsService() {
    // return new UserDetailsService() {
    // @Override
    // public UserDetails loadUserByUsername(String username) {
    // return new User("falseIdol", "", new ArrayList<>());
    // }
    // };
    // }

    // @Bean
    // @ConditionalOnProperty(property = "org.itech.authProvider.useADLDAP",
    // havingValue = "true")
    // public AuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
    // LdapAuthenticationProvider adProvider = new LdapAuthenticationProvider(new
    // LdapAuthenticator()
    //
    // return adProvider;
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public TextEncryptor textEncryptor() {
        AES256TextEncryptor textEncryptor = new AES256TextEncryptor();
        textEncryptor.setPassword(encryptionPassword);
        return textEncryptor;
    }

    // @Bean
    // public AuthenticationEventPublisher authenticationEventPublisher
    // (ApplicationEventPublisher applicationEventPublisher) {
    // return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    // }

    private static class OAuthRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean haveOauth2Token = (auth != null) && auth.startsWith("Bearer");
            boolean useOauth = haveOauth2Token || "true".equals(request.getParameter("useOAUTH"))
                    || request.getRequestURI().contains("oauth");
            return useOauth;
        }
    }

    private static class SamlRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean useSAML = (auth != null) && auth.startsWith("SAML")
                    || "true".equals(request.getParameter("useSAML")) || request.getRequestURI().contains("saml2");
            return useSAML;
        }
    }

    private static class BasicAuthRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean haveBasicAuth = (auth != null) && auth.startsWith("Basic");
            return haveBasicAuth;
        }
    }

    private static class CertificateAuthRequestedMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            boolean haveCertificateAuth = (auth != null) && auth.startsWith("Cert");
            return haveCertificateAuth;
        }
    }

    private static class KeycloakAuthoritiesExtractor {

        // TODO should we use authority AND Role? (Spring Concepts)
        public Collection<GrantedAuthority> convert(Assertion assertion) {
            Collection<GrantedAuthority> authorties = new ArrayList<>();
            for (AttributeStatement statement : assertion.getAttributeStatements()) {
                for (Attribute attr : statement.getAttributes()) {
                    if ("Role".equals(attr.getName())) {
                        for (XMLObject attributeValue : attr.getAttributeValues()) {
                            String value = ((XSString) attributeValue).getValue();
                            if (value != null && value.startsWith("oeg-")) {
                                authorties.add(new SimpleGrantedAuthority(value));
                            }

                        }
                    }
                }
            }
            return authorties;
        }
    }
}
