package com.pulsepass.service;

import com.pulsepass.domain.enums.TicketType;

import java.math.BigDecimal;

/** Precios fijos en COP por tipo (el MVP no guarda precio en el evento). */
final class TicketPricing {
    private TicketPricing() {}

    static BigDecimal priceFor(TicketType type) {
        return switch (type) {
            case GENERAL -> new BigDecimal("80000.00");
            case VIP -> new BigDecimal("250000.00");
            case BACKSTAGE -> new BigDecimal("450000.00");
            case STUDENT -> new BigDecimal("50000.00");
        };
    }
}
