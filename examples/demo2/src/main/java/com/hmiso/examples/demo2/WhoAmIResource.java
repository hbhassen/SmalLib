package com.hmiso.examples.demo2;

import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.integration.SamlAppConfiguration;
import com.hmiso.saml.integration.SamlAuthenticationFilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/whoami")
public class WhoAmIResource {

    @Context
    private HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response whoAmI() {
        SamlPrincipal principal = extractPrincipal();
        if (principal == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Utilisateur non authentifie"))
                    .build();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("nameId", principal.getNameId());
        payload.put("sessionIndex", principal.getSessionIndex());
        payload.put("attributes", principal.getAttributes());
        payload.put("attributes", "correct");
        return Response.ok(payload).build();
    }

    private SamlPrincipal extractPrincipal() {
        if (request == null) {
            return null;
        }
        var session = request.getSession(false);
        if (session == null) {
            return null;
        }
        String attributeKey = resolveSessionAttributeKey();
        Object attribute = session.getAttribute(attributeKey);
        if (attribute instanceof SamlPrincipal principal) {
            return principal;
        }
        return null;
    }

    private String resolveSessionAttributeKey() {
        if (request == null) {
            return SamlAppConfiguration.DEFAULT_SESSION_ATTRIBUTE_KEY;
        }
        Object config = request.getServletContext()
                .getAttribute(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY);
        if (config instanceof SamlAuthenticationFilterConfig filterConfig) {
            return filterConfig.getSessionAttributeKey();
        }
        return SamlAppConfiguration.DEFAULT_SESSION_ATTRIBUTE_KEY;
    }
}
