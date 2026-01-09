package com.hmiso.saml.integration;

import com.hmiso.saml.TestConfigurations;
import com.hmiso.saml.api.SamlServiceProvider;
import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.binding.RelayStateStore;
import com.hmiso.saml.config.BindingType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SamlJakartaFilterTest {

    @Test
    void passesThroughWhenMissingConfig() throws Exception {
        SamlJakartaFilter filter = new SamlJakartaFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        ServletContext context = mock(ServletContext.class);
        Map<String, Object> attributes = new HashMap<>();

        when(request.getServletContext()).thenReturn(context);
        when(context.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void handlesAcsPost() throws Exception {
        SamlJakartaFilter filter = new SamlJakartaFilter();
        SamlAuthenticationFilterConfig config = buildConfig();
        SamlAuthenticationFilterHelper helper = mock(SamlAuthenticationFilterHelper.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY, config);
        attributes.put(SamlAppConfiguration.HELPER_CONTEXT_KEY, helper);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        ServletContext context = mock(ServletContext.class);

        when(request.getServletContext()).thenReturn(context);
        when(request.getRequestURI()).thenReturn("/acs");
        when(request.getMethod()).thenReturn("POST");
        when(context.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        filter.doFilter(request, response, chain);

        verify(helper).handleAcsRequest(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void handlesSloRequest() throws Exception {
        SamlJakartaFilter filter = new SamlJakartaFilter();
        SamlAuthenticationFilterConfig config = buildConfig();
        SamlAuthenticationFilterHelper helper = mock(SamlAuthenticationFilterHelper.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY, config);
        attributes.put(SamlAppConfiguration.HELPER_CONTEXT_KEY, helper);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        ServletContext context = mock(ServletContext.class);

        when(request.getServletContext()).thenReturn(context);
        when(request.getRequestURI()).thenReturn("/slo");
        when(context.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        filter.doFilter(request, response, chain);

        verify(helper).handleSloRequest(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void passesThroughErrorPath() throws Exception {
        SamlJakartaFilter filter = new SamlJakartaFilter();
        SamlAuthenticationFilterConfig config = buildConfig();
        SamlAuthenticationFilterHelper helper = mock(SamlAuthenticationFilterHelper.class);
        SamlAppConfiguration appConfig = new SamlAppConfiguration(
                TestConfigurations.minimalConfig(BindingType.HTTP_POST),
                "saml.principal",
                java.util.List.of("/secure/*"),
                "/acs",
                "/slo",
                Duration.ofMinutes(5),
                "/saml/error"
        );
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY, config);
        attributes.put(SamlAppConfiguration.HELPER_CONTEXT_KEY, helper);
        attributes.put(SamlAppConfiguration.CONFIG_CONTEXT_KEY, appConfig);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        ServletContext context = mock(ServletContext.class);

        when(request.getServletContext()).thenReturn(context);
        when(request.getRequestURI()).thenReturn("/saml/error");
        when(context.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(helper, never()).shouldRedirectToIdP(request, response);
    }

    @Test
    void redirectsToIdpOnHttpRedirect() throws Exception {
        SamlJakartaFilter filter = new SamlJakartaFilter();
        SamlAuthenticationFilterConfig config = buildConfig();
        SamlAuthenticationFilterHelper helper = mock(SamlAuthenticationFilterHelper.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY, config);
        attributes.put(SamlAppConfiguration.HELPER_CONTEXT_KEY, helper);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        ServletContext context = mock(ServletContext.class);

        BindingMessage message = new BindingMessage(
                BindingType.HTTP_REDIRECT,
                URI.create("https://idp.example.com/sso"),
                "payload",
                "relay"
        );
        when(helper.shouldRedirectToIdP(request, response)).thenReturn(Optional.of(message));
        when(request.getServletContext()).thenReturn(context);
        when(request.getRequestURI()).thenReturn("/secure");
        when(request.getMethod()).thenReturn("GET");
        when(context.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        filter.doFilter(request, response, chain);

        String expected = "https://idp.example.com/sso?SAMLRequest="
                + URLEncoder.encode("payload", StandardCharsets.UTF_8)
                + "&RelayState=" + URLEncoder.encode("relay", StandardCharsets.UTF_8);
        verify(response).sendRedirect(expected);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rendersPostWhenBindingIsPost() throws Exception {
        SamlJakartaFilter filter = new SamlJakartaFilter();
        SamlAuthenticationFilterConfig config = buildConfig();
        SamlAuthenticationFilterHelper helper = mock(SamlAuthenticationFilterHelper.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY, config);
        attributes.put(SamlAppConfiguration.HELPER_CONTEXT_KEY, helper);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        ServletContext context = mock(ServletContext.class);

        BindingMessage message = new BindingMessage(
                BindingType.HTTP_POST,
                URI.create("https://idp.example.com/sso"),
                "payload-post",
                "relay-post"
        );
        when(helper.shouldRedirectToIdP(request, response)).thenReturn(Optional.of(message));
        when(request.getServletContext()).thenReturn(context);
        when(request.getRequestURI()).thenReturn("/secure");
        when(request.getMethod()).thenReturn("GET");
        when(context.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        StringWriter writer = new StringWriter();
        doReturn(new PrintWriter(writer)).when(response).getWriter();

        filter.doFilter(request, response, chain);

        String body = writer.toString();
        assertTrue(body.contains("name=\"SAMLRequest\" value=\"payload-post\""));
        assertTrue(body.contains("name=\"RelayState\" value=\"relay-post\""));
        verify(chain, never()).doFilter(request, response);
    }

    private SamlAuthenticationFilterConfig buildConfig() {
        return SamlAuthenticationFilterConfig.builder()
                .protectedPaths(java.util.List.of("/secure/*"))
                .acsPath("/acs")
                .sloPath("/slo")
                .samlServiceProvider(mock(SamlServiceProvider.class))
                .relayStateStore(mock(RelayStateStore.class))
                .build();
    }
}
