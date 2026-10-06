package si.confreg.registration.application;

import java.math.BigDecimal;
import si.confreg.registration.domain.FeeSchedule;
import si.confreg.registration.domain.WorkshopCatalogue;

/** Business values from configuration (AR-04), built by the config component. */
public record BusinessSettings(
    FeeSchedule feeSchedule, BigDecimal vatRate, WorkshopCatalogue workshops) {}
