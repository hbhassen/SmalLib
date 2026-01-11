package com.hmiso.examples.demo2;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ApiNavigationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isNavigationRequest(httpRequest)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.getWriter().write("{\"error\":\"browser_navigation_not_allowed\"}");
            return;
        }

        chain.doFilter(request, response);
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
