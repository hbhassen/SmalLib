package com.hmiso.saml.integration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ApiNavigationFilter implements Filter {
    private volatile Boolean blockBrowserNavigation;

    @Override
    public void init(FilterConfig filterConfig) {
        if (filterConfig == null) {
            return;
        }
        Object config = filterConfig.getServletContext().getAttribute(SamlAppConfiguration.CONFIG_CONTEXT_KEY);
        if (config instanceof SamlAppConfiguration) {
            blockBrowserNavigation = ((SamlAppConfiguration) config).isBlockBrowserNavigation();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Boolean enabled = resolveBlockNavigation(httpRequest);
        if (Boolean.TRUE.equals(enabled) && isNavigationRequest(httpRequest)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            httpResponse.getWriter().write("{\"error\":\"browser_navigation_not_allowed\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private Boolean resolveBlockNavigation(HttpServletRequest request) {
        Boolean local = blockBrowserNavigation;
        if (local != null) {
            return local;
        }
        Object config = request.getServletContext().getAttribute(SamlAppConfiguration.CONFIG_CONTEXT_KEY);
        if (config instanceof SamlAppConfiguration) {
            local = ((SamlAppConfiguration) config).isBlockBrowserNavigation();
            blockBrowserNavigation = local;
        }
        return local;
    }

    private boolean isNavigationRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }

        String fetchMode = request.getHeader("Sec-Fetch-Mode");
        if (fetchMode != null && "navigate".equalsIgnoreCase(fetchMode)) {
            return true;
        }

        String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase().contains("text/html");
    }
}
