/// Jackson serializes Java `BigDecimal` fields (quantities, unit prices) as JSON *numbers*,
/// not strings, even though the equivalent TypeScript interfaces in `pantheon-web` label them
/// `string` (harmless there — JS has no runtime type checking, so `${aNumber}` just works in a
/// template). Dart's `as String` is a real runtime check, so every such field must be read
/// through this helper instead of casting directly.
String? asDecimalStringOrNull(dynamic value) {
  if (value == null) return null;
  return asDecimalString(value);
}

String asDecimalString(dynamic value) {
  if (value is String) return value;
  if (value is num) return value.toString();
  return value.toString();
}
