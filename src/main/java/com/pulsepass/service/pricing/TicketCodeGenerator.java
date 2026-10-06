package com.pulsepass.service.pricing;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** Genera el codigo de negocio del ticket (el cliente no lo envia). Ej: TCK-3F9A1C07B2D4. */
@Component
public class TicketCodeGenerator {

    public String next() {
        return "TCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
