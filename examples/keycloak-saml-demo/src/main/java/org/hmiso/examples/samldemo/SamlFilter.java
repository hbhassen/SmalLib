package org.hmiso.examples.samldemo;

import org.hmiso.saml.binding.BindingMessage;
import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.integration.SamlAuthenticationFilterConfig;
import org.hmiso.saml.integration.SamlAuthenticationFilterHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class SamlFilter extends OncePerRequestFilter {
    private final SamlAuthenticationFilterConfig config;
    private final SamlAuthenticationFilterHelper helper;

    public SamlFilter(SamlAuthenticationFilterConfig config, SamlAuthenticationFilterHelper helper) {
        this.config = config;
        this.helper = helper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/saml/error") || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.equals(config.getAcsPath()) && "POST".equalsIgnoreCase(request.getMethod())) {
            helper.handleAcsRequest(request, response);
            return;
        }
        if (path.equals(config.getSloPath())) {
            helper.handleSloRequest(request, response);
            return;
        }

        Optional<BindingMessage> redirect = helper.shouldRedirectToIdP(request, response);
        if (redirect.isPresent()) {
            BindingMessage message = redirect.get();
            if (message.getBindingType() == BindingType.HTTP_REDIRECT) {
                String target = message.getDestination() + "?SAMLRequest=" +
                        urlEncode(message.getPayload());
                if (message.getRelayState() != null) {
                    target += "&RelayState=" + urlEncode(message.getRelayState());
                }
                response.sendRedirect(target);
            } else {
                renderPost(response, message);
            }
            return;
        }

        filterChain.doFilter(request, response);
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
