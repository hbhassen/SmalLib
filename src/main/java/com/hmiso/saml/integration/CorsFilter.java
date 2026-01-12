package com.hmiso.saml.integration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CorsFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsFilter.class);

    private volatile CorsConfiguration corsConfiguration;

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.info("CorsFilter init start");
        if (filterConfig == null) {
            LOGGER.info("CorsFilter init end (no config)");
            return;
        }
        Object config = filterConfig.getServletContext().getAttribute(SamlAppConfiguration.CONFIG_CONTEXT_KEY);
        if (config instanceof SamlAppConfiguration) {
            this.corsConfiguration = ((SamlAppConfiguration) config).getCorsConfiguration();
        }
        LOGGER.info("CorsFilter init end");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();
        LOGGER.info("CorsFilter doFilter start method={} path={}", method, path);

        try {
            CorsConfiguration corsConfig = resolveCorsConfiguration(httpRequest);
            if (corsConfig == null || !corsConfig.isEnabled()) {
                chain.doFilter(request, response);
                return;
            }

            String origin = httpRequest.getHeader("Origin");
            if (origin != null && isAllowedOrigin(origin, corsConfig)) {
                httpResponse.setHeader("Access-Control-Allow-Origin", origin);
                httpResponse.setHeader("Vary", "Origin");
                if (corsConfig.isAllowCredentials()) {
                    httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
                }
                setCsvHeader(httpResponse, "Access-Control-Allow-Methods", corsConfig.getAllowedMethods());
                setCsvHeader(httpResponse, "Access-Control-Allow-Headers", corsConfig.getAllowedHeaders());
                setCsvHeader(httpResponse, "Access-Control-Expose-Headers", corsConfig.getExposeHeaders());
            }

            if ("OPTIONS".equalsIgnoreCase(method)) {
                httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            chain.doFilter(request, response);
        } finally {
            LOGGER.info("CorsFilter doFilter end method={} path={}", method, path);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("CorsFilter destroy start");
        LOGGER.info("CorsFilter destroy end");
    }

    private CorsConfiguration resolveCorsConfiguration(HttpServletRequest request) {
        CorsConfiguration local = corsConfiguration;
        if (local != null) {
            return local;
        }
        Object config = request.getServletContext().getAttribute(SamlAppConfiguration.CONFIG_CONTEXT_KEY);
        if (config instanceof SamlAppConfiguration) {
            local = ((SamlAppConfiguration) config).getCorsConfiguration();
            corsConfiguration = local;
        }
        return local;
    }

    private boolean isAllowedOrigin(String origin, CorsConfiguration config) {
        List<String> allowed = config.getAllowedOrigins();
        if (allowed.isEmpty()) {
            return false;
        }
        for (String value : allowed) {
            if (origin.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void setCsvHeader(HttpServletResponse response, String name, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        String joined = String.join(",", values);
        response.setHeader(name, joined);
    }
}
