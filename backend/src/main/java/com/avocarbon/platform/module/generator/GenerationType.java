package com.avocarbon.platform.module.generator;

/**
 * Specifies which Angular artefacts should be generated.
 *
 * ANGULAR_FULL  → complete standalone app: routing, services, list + form per resource
 * ANGULAR_CRUD  → services + list components only
 * ANGULAR_FORMS → reactive form components only
 */
public enum GenerationType {
    ANGULAR_FULL,
    ANGULAR_CRUD,
    ANGULAR_FORMS
}
