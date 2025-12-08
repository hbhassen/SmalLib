package org.hmiso.examples.demo2;

import org.hmiso.saml.api.SamlPrincipal;
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
                    .entity(Map.of("error", "Utilisateur non authentifié"))
                    .build();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("nameId", principal.getNameId());
        payload.put("sessionIndex", principal.getSessionIndex());
        payload.put("attributes", principal.getAttributes());
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
        Object attribute = session.getAttribute(SamlDemo2Configuration.SESSION_ATTRIBUTE_KEY);
        if (attribute instanceof SamlPrincipal principal) {
            return principal;
        }
        return null;
    }
}
