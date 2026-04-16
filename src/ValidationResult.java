final class ValidationResult {
    final boolean valid;
    final String message;

    private ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    static ValidationResult valid() {
        return new ValidationResult(true, "");
    }

    static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }
}
