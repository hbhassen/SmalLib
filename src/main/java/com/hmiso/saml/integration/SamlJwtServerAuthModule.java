package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SamlJwtServerAuthModule implements ServerAuthModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(SamlJwtServerAuthModule.class);
    private static final Class<?>[] SUPPORTED_MESSAGE_TYPES = new Class<?>[] {
            HttpServletRequest.class,
            HttpServletResponse.class
    };

    private CallbackHandler callbackHandler;

    @Override
    public void initialize(MessagePolicy requestPolicy,
                           MessagePolicy responsePolicy,
                           CallbackHandler handler,
                           Map options) throws AuthException {
        this.callbackHandler = handler;
        String handlerName = handler != null ? handler.getClass().getName() : "null";
        LOGGER.info("SamlJwtServerAuthModule initialize handler={}", handlerName);
    }

    @Override
    public Class<?>[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {
        Object requestMessage = messageInfo != null ? messageInfo.getRequestMessage() : null;
        HttpServletRequest request = requestMessage instanceof HttpServletRequest
                ? (HttpServletRequest) requestMessage
                : null;
        String method = request != null ? request.getMethod() : "unknown";
        String path = request != null ? request.getRequestURI() : "unknown";
        String outcome = "unknown";

        LOGGER.info("SamlJwtServerAuthModule validateRequest start method={} path={}", method, path);
        try {
            if (messageInfo == null) {
                outcome = "no_message_info";
                return AuthStatus.SUCCESS;
            }
            if (request == null) {
                outcome = "unsupported_request";
                return AuthStatus.SUCCESS;
            }

            String token = extractBearerToken(request.getHeader("Authorization"));
            if (token == null) {
                outcome = "missing_token";
                return AuthStatus.SUCCESS;
            }

            SamlAuthenticationFilterConfig config = resolveConfig(request);
            if (config == null || config.getJwtService() == null || config.getServerSessionRegistry() == null) {
                outcome = "missing_config";
                return AuthStatus.SUCCESS;
            }

            SamlJwtService.JwtClaims claims;
            try {
                claims = config.getJwtService().validate(token);
            } catch (Exception ex) {
                outcome = "jwt_invalid";
                return AuthStatus.SUCCESS;
            }

            SamlServerSession serverSession = config.getServerSessionRegistry().getSession(claims.getSessionId());
            if (serverSession == null) {
                outcome = "session_not_found";
                return AuthStatus.SUCCESS;
            }
            SamlPrincipal principal = serverSession.getPrincipal();
            if (principal == null) {
                outcome = "principal_missing";
                return AuthStatus.SUCCESS;
            }

            HttpSession session = request.getSession(true);
            session.setAttribute(config.getSessionAttributeKey(), principal);
            session.setAttribute(config.getServerSessionAttributeKey(), serverSession.getSessionId());

            if (clientSubject != null) {
                clientSubject.getPublicCredentials().add(principal);
            }

            Set<String> roles = normalizeRoles(resolveRoles(claims, principal));
            Subject targetSubject = clientSubject != null ? clientSubject : new Subject();
            try {
                LOGGER.info("SamlJwtServerAuthModule validateRequest end method={} path={} principal={}",
                        method,
                        path,
                        principal.getNameId());
                LOGGER.info("SamlJwtServerAuthModule roles resolved count={} roles={}",
                        roles.size(),
                        roles);
                if (tryElytronIdentityCallback(principal.getNameId(), roles)) {
                    outcome = "authenticated";
                    return AuthStatus.SUCCESS;
                }
                handleCallbacks(targetSubject, principal.getNameId(), roles);
            } catch (AuthException ex) {
                outcome = "callback_error";
                String handlerName = callbackHandler != null ? callbackHandler.getClass().getName() : "null";
                LOGGER.warn("SamlJwtServerAuthModule callback_error handler={} subjectNull={} rolesCount={}",
                        handlerName,
                        clientSubject == null,
                        roles != null ? roles.size() : 0,
                        ex);
                throw ex;
            }

            outcome = "authenticated";
            return AuthStatus.SUCCESS;
        } finally {
            LOGGER.info("SamlJwtServerAuthModule validateRequest end method={} path={} outcome={}",
                    method,
                    path,
                    outcome);
        }
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
        if (subject == null) {
            return;
        }
        subject.getPrincipals().clear();
        subject.getPublicCredentials().clear();
        subject.getPrivateCredentials().clear();
    }

    private SamlAuthenticationFilterConfig resolveConfig(HttpServletRequest request) {
        Object value = request.getServletContext().getAttribute(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY);
        if (value instanceof SamlAuthenticationFilterConfig) {
            return (SamlAuthenticationFilterConfig) value;
        }
        return null;
    }

    private String extractBearerToken(String header) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.length() < 7) {
            return null;
        }
        if (!trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = trimmed.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private Set<String> resolveRoles(SamlJwtService.JwtClaims claims, SamlPrincipal principal) {
        Set<String> roles = new HashSet<>();
        if (claims != null) {
            roles.addAll(claims.getRoles());
        }
        if (!roles.isEmpty()) {
            return roles;
        }
        return extractRoles(principal);
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String role : roles) {
            if (role == null) {
                continue;
            }
            String trimmed = role.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private Set<String> extractRoles(SamlPrincipal principal) {
        Set<String> roles = new HashSet<>();
        if (principal == null || principal.getAttributes() == null) {
            return roles;
        }
        Object value = principal.getAttributes().get("Role");
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
        } else if (value instanceof String) {
            String text = ((String) value).trim();
            if (!text.isEmpty()) {
                roles.add(text);
            }
        }
        return roles;
    }

    private void handleCallbacks(Subject subject, String nameId, Set<String> roles) throws AuthException {
        if (callbackHandler == null) {
            return;
        }
        Exception lastException = null;
        try {
            callbackHandler.handle(buildCallbacks(subject, nameId, roles, true));
            return;
        } catch (UnsupportedCallbackException | IOException ex) {
            lastException = ex;
        }
        try {
            callbackHandler.handle(buildCallbacks(subject, nameId, roles, false));
            return;
        } catch (UnsupportedCallbackException | IOException ex) {
            lastException = ex;
        }
        if (lastException != null) {
            throw new AuthException("JASPIC callback failure: " + lastException.getMessage());
        }
    }

    private Callback[] buildCallbacks(Subject subject,
                                      String nameId,
                                      Set<String> roles,
                                      boolean includeGroups) {
        boolean hasRoles = includeGroups && roles != null && !roles.isEmpty();
        CallerPrincipalCallback callerCallback = new CallerPrincipalCallback(subject, nameId);
        if (!hasRoles) {
            return new Callback[] { callerCallback };
        }
        return new Callback[] {
                callerCallback,
                new GroupPrincipalCallback(subject, roles.toArray(new String[0]))
        };
    }

    private boolean tryElytronIdentityCallback(String nameId, Set<String> roles) {
        if (callbackHandler == null || nameId == null || nameId.isBlank()) {
            return false;
        }
        try {
            Class<?> securityDomainClass = Class.forName("org.wildfly.security.auth.server.SecurityDomain");
            Object domain = securityDomainClass.getMethod("getCurrent").invoke(null);
            if (domain == null) {
                return false;
            }
            Object identity = securityDomainClass.getMethod("createAdHocIdentity", String.class)
                    .invoke(domain, nameId);
            Object identityWithRoles = identity;
            if (roles != null && !roles.isEmpty()) {
                Class<?> rolesClass = Class.forName("org.wildfly.security.authz.Roles");
                Object rolesValue = rolesClass.getMethod("fromSet", Set.class).invoke(null, roles);
                Class<?> roleMapperClass = Class.forName("org.wildfly.security.authz.RoleMapper");
                Object roleMapper = roleMapperClass.getMethod("constant", rolesClass).invoke(null, rolesValue);
                identityWithRoles = identity.getClass()
                        .getMethod("withDefaultRoleMapper", roleMapperClass)
                        .invoke(identity, roleMapper);
            }
            Class<?> identityClass = Class.forName("org.wildfly.security.auth.server.SecurityIdentity");
            Class<?> callbackClass = Class.forName("org.wildfly.security.auth.callback.SecurityIdentityCallback");
            Object callback = callbackClass.getConstructor().newInstance();
            callbackClass.getMethod("setSecurityIdentity", identityClass).invoke(callback, identityWithRoles);
            callbackHandler.handle(new Callback[] { (Callback) callback });
            LOGGER.info("SamlJwtServerAuthModule Elytron identity callback applied principal={}", nameId);
            return true;
        } catch (ClassNotFoundException ex) {
            LOGGER.warn("SamlJwtServerAuthModule Elytron classes unavailable (add module dependency)", ex);
            return false;
        } catch (Exception ex) {
            LOGGER.warn("SamlJwtServerAuthModule Elytron identity callback failed", ex);
            return false;
        }
    }
}
