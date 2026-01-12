package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SamlServerSessionFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SamlServerSessionFilter.class);

    private volatile SamlAuthenticationFilterConfig config;
    private final SamlSessionHelper sessionHelper = new SamlSessionHelper();

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.info("SamlServerSessionFilter init start");
        if (filterConfig == null) {
            LOGGER.info("SamlServerSessionFilter init end (no config)");
            return;
        }
        Object value = filterConfig.getServletContext().getAttribute(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY);
        if (value instanceof SamlAuthenticationFilterConfig) {
            this.config = (SamlAuthenticationFilterConfig) value;
        }
        LOGGER.info("SamlServerSessionFilter init end");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();
        LOGGER.info("SamlServerSessionFilter doFilter start method={} path={}", method, path);

        try {
            SamlAuthenticationFilterConfig localConfig = resolveConfig(httpRequest);
            if (localConfig == null) {
                chain.doFilter(request, response);
                return;
            }
            if (!isProtected(path, localConfig.getProtectedPaths())) {
                chain.doFilter(request, response);
                return;
            }

            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                chain.doFilter(request, response);
                return;
            }

            Optional<SamlPrincipal> principal = sessionHelper.retrievePrincipalFromSession(
                    session, localConfig.getSessionAttributeKey());
            if (principal.isEmpty()) {
                chain.doFilter(request, response);
                return;
            }

            SamlServerSession serverSession = resolveServerSession(session, localConfig);
            if (serverSession == null || !principal.get().getNameId().equals(serverSession.getPrincipal().getNameId())) {
                sessionHelper.invalidateSession(session);
            }

            chain.doFilter(request, response);
        } finally {
            LOGGER.info("SamlServerSessionFilter doFilter end method={} path={}", method, path);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("SamlServerSessionFilter destroy start");
        LOGGER.info("SamlServerSessionFilter destroy end");
    }

    private SamlAuthenticationFilterConfig resolveConfig(HttpServletRequest request) {
        SamlAuthenticationFilterConfig local = config;
        if (local != null) {
            return local;
        }
        Object value = request.getServletContext().getAttribute(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY);
        if (value instanceof SamlAuthenticationFilterConfig) {
            local = (SamlAuthenticationFilterConfig) value;
            config = local;
        }
        return local;
    }

    private SamlServerSession resolveServerSession(HttpSession session, SamlAuthenticationFilterConfig config) {
        if (session == null || config.getServerSessionRegistry() == null) {
            return null;
        }
        Object attribute = session.getAttribute(config.getServerSessionAttributeKey());
        if (!(attribute instanceof String sessionId)) {
            return null;
        }
        return config.getServerSessionRegistry().getSession(sessionId);
    }

    private boolean isProtected(String requestUri, List<String> protectedPaths) {
        if (requestUri == null || protectedPaths == null) {
            return false;
        }
        return protectedPaths.stream().anyMatch(path -> matches(requestUri, path));
    }

    private boolean matches(String requestUri, String path) {
        if (path.endsWith("/*")) {
            String base = path.substring(0, path.length() - 2);
            return requestUri.startsWith(base);
        }
        return requestUri.equals(path);
    }
}
