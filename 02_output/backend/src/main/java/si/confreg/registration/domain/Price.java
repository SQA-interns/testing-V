package si.confreg.registration.domain;

import java.math.BigDecimal;

/** Fee of one registration in EUR, two decimals: {@code grossFee = netFee + vat}. */
public record Price(BigDecimal netFee, BigDecimal vat, BigDecimal grossFee) {}
