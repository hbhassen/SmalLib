package org.hmiso.saml.config;

/**
 * Contrat commun à tous les chargeurs de configuration SmalLib.
 */
public interface ConfigLoader {
    /**
     * Charge une {@link SamlConfiguration} depuis la source sous-jacente.
     * @return configuration complète et validée
     */
    SamlConfiguration load();

    /**
     * Valide la configuration fournie et lève une {@link ConfigValidationException} en cas d'incohérence.
     */
    void validate(SamlConfiguration configuration) throws ConfigValidationException;
}
