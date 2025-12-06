package com.hmiso.examples.demo2;

import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.integration.SamlAuthenticationFilterConfig;
import com.hmiso.saml.integration.SamlAuthenticationFilterHelper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@WebFilter("/*")
public class SamlJakartaFilter implements Filter {

    static final String CONFIG_KEY = "demo2.saml.config";
    static final String HELPER_KEY = "demo2.saml.helper";

    @Override
    public void init(FilterConfig filterConfig) {
        // Pas d'initialisation spécifique : la configuration est fournie par SamlBootstrapListener
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        SamlAuthenticationFilterConfig config = (SamlAuthenticationFilterConfig) request.getServletContext()
                .getAttribute(CONFIG_KEY);
        SamlAuthenticationFilterHelper helper = (SamlAuthenticationFilterHelper) request.getServletContext()
                .getAttribute(HELPER_KEY);

        if (config == null || helper == null) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        if (path.equals(config.getAcsPath()) && "POST".equalsIgnoreCase(httpRequest.getMethod())) {
            helper.handleAcsRequest(httpRequest, httpResponse);
            return;
        }
        if (path.equals(config.getSloPath())) {
            helper.handleSloRequest(httpRequest, httpResponse);
            return;
        }
        if (path.startsWith("/saml/error")) {
            chain.doFilter(request, response);
            return;
        }

        Optional<BindingMessage> redirect = helper.shouldRedirectToIdP(httpRequest, httpResponse);
        if (redirect.isPresent()) {
            BindingMessage message = redirect.get();
            if (message.getBindingType() == BindingType.HTTP_REDIRECT) {
                String target = message.getDestination() + "?SAMLRequest=" + urlEncode(message.getPayload());
                if (message.getRelayState() != null) {
                    target += "&RelayState=" + urlEncode(message.getRelayState());
                }
                httpResponse.sendRedirect(target);
            } else {
                renderPost(httpResponse, message);
            }
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private void renderPost(HttpServletResponse response, BindingMessage message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        String relayStateInput = message.getRelayState() == null ? "" :
                "<input type=\"hidden\" name=\"RelayState\" value=\"" + message.getRelayState() + "\" />";
        String html = """
                <html>
                  <body onload=\"document.forms[0].submit()\">
                    <form method=\"post\" action=\"%s\">\n
                      <input type=\"hidden\" name=\"SAMLRequest\" value=\"%s\" />\n
                      %s
                    </form>
                  </body>
                </html>
                """.formatted(message.getDestination(), message.getPayload(), relayStateInput);
        response.getWriter().write(html);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
