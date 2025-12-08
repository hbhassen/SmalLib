package org.hmiso.saml.integration;

import org.hmiso.saml.api.SamlPrincipal;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Helper JASPIC pour peupler le {@link Subject} à partir d'un {@link SamlPrincipal} (VII - JASPIC / WildFly Security).
 */
public class SamlServerAuthModuleHelper {

    public Subject createSubjectFromPrincipal(SamlPrincipal principal) {
        Subject subject = new Subject();
        populateSubjectPrincipals(subject, principal);
        return subject;
    }

    public Principal createCallerPrincipal(SamlPrincipal principal) {
        return (Principal) () -> principal.getNameId();
    }

    public Set<Principal> createGroupsFromRoles(Set<String> roles) {
        Set<Principal> groups = new HashSet<>();
        if (roles != null) {
            for (String role : roles) {
                groups.add((Principal) () -> role);
            }
        }
        return groups;
    }

    public void populateSubjectPrincipals(Subject subject, SamlPrincipal principal) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(principal, "principal");
        subject.getPrincipals().add(createCallerPrincipal(principal));
        subject.getPublicCredentials().add(principal);
    }
}
