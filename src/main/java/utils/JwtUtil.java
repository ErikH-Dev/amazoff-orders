package utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JwtUtil {
    private static final Logger LOG = Logger.getLogger(JwtUtil.class);

    @Inject
    JsonWebToken jwt;

    public String getCurrentKeycloakUserId() {
        if (jwt == null) {
            throw new SecurityException("No JWT token found");
        }
        
        String keycloakUserId = jwt.getSubject();
        LOG.debugf("Extracted Keycloak user ID from JWT: %s", keycloakUserId);
        return keycloakUserId;
    }
}