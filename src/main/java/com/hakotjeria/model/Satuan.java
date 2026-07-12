package com.hakotjeria.model;

/**
 * Dropdown Satuan terkunci sesuai SRS (dilarang free-text):
 * Gram, ml, Pcs, Slice, Cm.
 */
public enum Satuan {
    GRAM("Gram"),
    ML("ml"),
    PCS("Pcs"),
    SLICE("Slice"),
    CM("Cm");

    private final String label;

    Satuan(String label) {
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
