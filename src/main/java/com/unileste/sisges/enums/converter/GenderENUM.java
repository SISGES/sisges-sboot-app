package com.unileste.sisges.enums.converter;

public enum GenderENUM {

    MASCULINO("M"),
    FEMININO("F"),
    NAO_BINARIO("NB"),
    OUTRO("O");

    private final String code;

    GenderENUM(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static GenderENUM fromCode(String code) {
        for (GenderENUM gender : values()) {
            if (gender.code.equalsIgnoreCase(code)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Código inválido para GenderENUM: " + code);
    }
}