package moustapha.nfsgroup.user.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convertit les rôles Keycloak (realm + client) du JWT en autorités Spring Security (RBAC 2.4.1).
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String resourceClientId;

    public KeycloakRealmRoleConverter(String resourceClientId) {
        this.resourceClientId = resourceClientId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Stream<String> realmRoles = rolesFrom(jwt.getClaimAsMap("realm_access")).map(r -> "ROLE_" + r);

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        Stream<String> clientRoles = Stream.empty();
        if (resourceAccess != null && resourceAccess.get(resourceClientId) instanceof Map<?, ?> client) {
            clientRoles = rolesFrom((Map<String, Object>) client);
        }
        return Stream.concat(realmRoles, clientRoles)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Stream<String> rolesFrom(Map<String, Object> claim) {
        if (claim != null && claim.get("roles") instanceof List<?> roles) {
            return ((List<String>) roles).stream();
        }
        return Stream.empty();
    }
}