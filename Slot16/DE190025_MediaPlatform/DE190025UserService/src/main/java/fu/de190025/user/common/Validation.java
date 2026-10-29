package fu.de190025.user.common;

public final class Validation {
  private Validation() {}

  private static final jakarta.validation.Validator VALIDATOR =
      jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

  public static void require(boolean condition, String message) {
    if (!condition) throw new ApiException(2, message);
  }

  public static void text(String value, int max, String field) {
    require(
        value != null && !value.isBlank() && value.length() <= max,
        field + " is required, max " + max);
  }

  public static void email(String value) {
    text(value, 100, "email");
    require(VALIDATOR.validateValue(EmailValue.class, "email", value).isEmpty(), "Invalid email");
  }

  private static class EmailValue {
    @jakarta.validation.constraints.Email String email;
  }

  public static org.springframework.data.domain.Pageable page(int page, int size) {
    require(page >= 0 && size >= 1 && size <= 100, "Invalid pagination");
    return org.springframework.data.domain.PageRequest.of(
        page, size, org.springframework.data.domain.Sort.by("userId"));
  }
}
