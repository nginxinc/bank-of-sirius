package sirius.samples.bankofsirius.security;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(final String s) {
        super(s);
    }

    public AuthenticationException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
