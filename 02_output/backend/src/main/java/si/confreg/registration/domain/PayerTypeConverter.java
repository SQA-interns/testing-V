package si.confreg.registration.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores {@link PayerType} with its API value, as the schema's check constraint expects. */
@Converter
public class PayerTypeConverter implements AttributeConverter<PayerType, String> {

  @Override
  public String convertToDatabaseColumn(PayerType payerType) {
    return payerType == null ? null : payerType.apiValue();
  }

  @Override
  public PayerType convertToEntityAttribute(String value) {
    return value == null
        ? null
        : PayerType.fromApiValue(value)
            .orElseThrow(() -> new IllegalStateException("unknown payer type in database"));
  }
}
