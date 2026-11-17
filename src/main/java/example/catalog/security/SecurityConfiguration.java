package example.catalog.security;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    AuthenticationManager ldapAuthenticationManager(BaseLdapPathContextSource contextSource) {
        var groups = new DefaultLdapAuthoritiesPopulator(contextSource, "ou=groups");
        groups.setGroupSearchFilter("(member={0})");
        groups.setRolePrefix("");
        groups.setConvertToUpperCase(false);
        Map<String, GrantedAuthority> roles = Map.of(
                "ROLE_USER", new SimpleGrantedAuthority("ROLE_USER"),
                "ROLE_ADMIN", new SimpleGrantedAuthority("ROLE_ADMIN"));

        var factory = new LdapBindAuthenticationManagerFactory(contextSource);
        factory.setUserSearchBase("ou=people");
        factory.setUserSearchFilter("(uid={0})");
        factory.setLdapAuthoritiesPopulator((user, username) -> {
            List<GrantedAuthority> authorities = groups.getGrantedAuthorities(user, username).stream()
                    .map(group -> roles.get(group.getAuthority().toUpperCase(Locale.ROOT)))
                    .filter(Objects::nonNull)
                    .toList();
            if (authorities.isEmpty()) {
                throw new org.springframework.security.authentication.BadCredentialsException("No application role");
            }
            return authorities;
        });
        return factory.createAuthenticationManager();
    }

    @Bean
    SecurityFilterChain web(HttpSecurity http, AuthenticationManager manager) throws Exception {
        return http.authenticationManager(manager)
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers("/login", "/app.css", "/error").permitAll()
                        .requestMatchers("/admin/users", "/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/", "/products", "/products/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().denyAll())
                .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/products", true))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }
}
