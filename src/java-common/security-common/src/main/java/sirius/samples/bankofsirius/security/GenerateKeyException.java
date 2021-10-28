package sirius.samples.bankofsirius.security;

public class GenerateKeyException extends RuntimeException {
    public GenerateKeyException(String message, Exception e) {
        super(message, e);
    }
}
