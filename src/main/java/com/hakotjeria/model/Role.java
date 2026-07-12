package com.hakotjeria.model;

/** Peran pengguna aplikasi. */
public enum Role {
    SUPERVISOR("Supervisor"),
    STAFF("Staff");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
