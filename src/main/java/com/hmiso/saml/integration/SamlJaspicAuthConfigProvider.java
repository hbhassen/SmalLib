package com.hmiso.saml.integration;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.ClientAuthConfig;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

public class SamlJaspicAuthConfigProvider implements AuthConfigProvider {
    private final Map<String, String> properties;

    public SamlJaspicAuthConfigProvider() {
        this(null);
    }

    public SamlJaspicAuthConfigProvider(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, CallbackHandler handler) {
        return null;
    }

    @Override
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler handler) {
        return new SamlServerAuthConfig(layer, appContext, handler, properties);
    }

    @Override
    public void refresh() {
    }

    private static final class SamlServerAuthConfig implements ServerAuthConfig {
        private final String layer;
        private final String appContext;
        private final CallbackHandler handler;
        private final Map<String, String> properties;

        private SamlServerAuthConfig(String layer,
                                     String appContext,
                                     CallbackHandler handler,
                                     Map<String, String> properties) {
            this.layer = layer;
            this.appContext = appContext;
            this.handler = handler;
            this.properties = properties;
        }

        @Override
        public String getMessageLayer() {
            return layer;
        }

        @Override
        public String getAppContext() {
            return appContext;
        }

        @Override
        public String getAuthContextID(MessageInfo messageInfo) {
            return "SmalLibJaspic";
        }

        @Override
        public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject, Map properties)
                throws AuthException {
            return new SamlServerAuthContext(handler, this.properties);
        }

        @Override
        public void refresh() {
        }

        @Override
        public boolean isProtected() {
            return true;
        }
    }

    private static final class SamlServerAuthContext implements ServerAuthContext {
        private final SamlJwtServerAuthModule module;

        private SamlServerAuthContext(CallbackHandler handler, Map<String, String> properties) throws AuthException {
            this.module = new SamlJwtServerAuthModule();
            this.module.initialize(null, null, handler, properties);
        }

        @Override
        public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
                throws AuthException {
            return module.validateRequest(messageInfo, clientSubject, serviceSubject);
        }

        @Override
        public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
            return module.secureResponse(messageInfo, serviceSubject);
        }

        @Override
        public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
            module.cleanSubject(messageInfo, subject);
        }
    }
}
