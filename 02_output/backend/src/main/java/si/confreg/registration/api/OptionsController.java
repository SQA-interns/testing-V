package si.confreg.registration.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import si.confreg.registration.application.AppProperties;
import si.confreg.registration.application.PricingService;
import si.confreg.registration.application.TimeSource;
import si.confreg.registration.domain.Price;

/** Workshops and the price that applies now, for the registration page (AC-001-20, 22). */
@RestController
class OptionsController {

  private final AppProperties properties;
  private final PricingService pricing;
  private final TimeSource timeSource;

  OptionsController(AppProperties properties, PricingService pricing, TimeSource timeSource) {
    this.properties = properties;
    this.pricing = pricing;
    this.timeSource = timeSource;
  }

  record WorkshopOption(String id, String title) {}

  record PriceOption(String tier, BigDecimal netFee, BigDecimal vat, BigDecimal grossFee) {}

  record OptionsResponse(
      List<WorkshopOption> workshops,
      String conferenceTimeZone,
      LocalDate earlyBirdDeadline,
      PriceOption price) {}

  @GetMapping("/api/registration-options")
  OptionsResponse options(@RequestParam(defaultValue = "false") boolean student) {
    Price price = pricing.price(student, timeSource.now());
    List<WorkshopOption> workshops =
        properties.workshops().entrySet().stream()
            .map(e -> new WorkshopOption(e.getKey(), e.getValue()))
            .toList();
    return new OptionsResponse(
        workshops,
        properties.conferenceTz().getId(),
        properties.earlyBirdDeadline(),
        new PriceOption(
            price.tier().name().toLowerCase(Locale.ROOT),
            price.netFee(),
            price.vat(),
            price.grossFee()));
  }
}
