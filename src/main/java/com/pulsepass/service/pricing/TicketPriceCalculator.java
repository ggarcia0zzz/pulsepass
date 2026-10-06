package com.pulsepass.service.pricing;

import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estrategia de precios encapsulada (el precio nunca viene del cliente).
 *
 * <pre>
 * GENERAL   -> precio base
 * STUDENT   -> 60 % del precio base   (descuento)
 * VIP       -> 2.5 x precio base      (multiplicador)
 * BACKSTAGE -> 5 x precio base        (multiplicador superior)
 * </pre>
 *
 * Usa BigDecimal con escala 2 (NUMERIC(10,2) en la BD) y garantiza BR-TICKET-009:
 * nunca devuelve un precio negativo.
 */
@Component
public class TicketPriceCalculator {

    private static final int SCALE = 2;

    private final BigDecimal basePrice;

    public TicketPriceCalculator(
            @Value("${pulsepass.ticket.base-price:100000.00}") BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() < 0) {
            throw new IllegalArgumentException("Base ticket price must not be negative");
        }
        this.basePrice = basePrice;
    }

    public BigDecimal calculate(TicketType type) {
        BigDecimal price = basePrice
                .multiply(multiplierOf(type))
                .setScale(SCALE, RoundingMode.HALF_UP);

        // BR-TICKET-009: nunca se permite price < 0
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative: " + price);
        }
        return price;
    }

    private BigDecimal multiplierOf(TicketType type) {
        return switch (type) {
            case GENERAL -> new BigDecimal("1.00");
            case STUDENT -> new BigDecimal("0.60");
            case VIP -> new BigDecimal("2.50");
            case BACKSTAGE -> new BigDecimal("5.00");
        };
    }
}
