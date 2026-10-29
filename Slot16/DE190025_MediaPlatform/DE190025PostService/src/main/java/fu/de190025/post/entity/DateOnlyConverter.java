package fu.de190025.post.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/** Keep the exam's java.util.Date API while persisting date-only values. */
@Converter
public class DateOnlyConverter implements AttributeConverter<Date, LocalDate> {
  public LocalDate convertToDatabaseColumn(Date value) {
    return value == null
        ? null
        : java.time.Instant.ofEpochMilli(value.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
  }

  public Date convertToEntityAttribute(LocalDate value) {
    return value == null ? null : Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
  }
}
