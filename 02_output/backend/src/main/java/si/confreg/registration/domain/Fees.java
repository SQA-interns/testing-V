package si.confreg.registration.domain;

import java.math.BigDecimal;

/** Net fee, VAT and gross fee of one registration, in EUR with two decimals. */
public record Fees(BigDecimal netFee, BigDecimal vat, BigDecimal grossFee) {}
