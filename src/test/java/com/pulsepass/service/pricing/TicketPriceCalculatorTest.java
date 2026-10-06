package com.pulsepass.service.pricing;

import com.pulsepass.domain.enums.TicketType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba de la estrategia de precios (BR-TICKET-009). Test puro: sin Mockito ni Spring. */
class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator(new BigDecimal("100000.00"));

    @Test
    void general_isTheBasePrice() {
        assertThat(calculator.calculate(TicketType.GENERAL)).isEqualByComparingTo("100000.00");
    }

    @Test
    void student_hasDiscount() {
        assertThat(calculator.calculate(TicketType.STUDENT)).isEqualByComparingTo("60000.00");
    }

    @Test
    void vip_usesMultiplier() {
        assertThat(calculator.calculate(TicketType.VIP)).isEqualByComparingTo("250000.00");
    }

    @Test
    void backstage_usesHigherMultiplierThanVip() {
        BigDecimal backstage = calculator.calculate(TicketType.BACKSTAGE);

        assertThat(backstage).isEqualByComparingTo("500000.00");
        assertThat(backstage).isGreaterThan(calculator.calculate(TicketType.VIP));
    }

    @Test
    void price_isRoundedToTwoDecimals() {
        TicketPriceCalculator odd = new TicketPriceCalculator(new BigDecimal("33333.33"));

        // 33333.33 * 0.60 = 19999.998 -> 20000.00
        assertThat(odd.calculate(TicketType.STUDENT)).isEqualTo(new BigDecimal("20000.00"));
    }

    @ParameterizedTest
    @EnumSource(TicketType.class)
    void price_isNeverNegative(TicketType type) {
        assertThat(calculator.calculate(type).signum()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void negativeBasePrice_isRejected() {
        assertThatThrownBy(() -> new TicketPriceCalculator(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
