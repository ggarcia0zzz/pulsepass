package com.pulsepass.shared;

import java.util.Locale;

/** Normaliza texto de entrada antes de validar unicidad o persistir. */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** Codigos de negocio (code, eventCode, ticketCode, ...): sin espacios y en mayusculas. */
    public static String code(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String text(String value) {
        return value.trim();
    }

    /** Texto opcional: null/blank se convierte en null (ej. streamingUrl, description). */
    public static String optionalText(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
