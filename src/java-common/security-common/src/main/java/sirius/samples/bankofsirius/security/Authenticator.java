package sirius.samples.bankofsirius.security;

public interface Authenticator {
    void verify(String authorization, String accountId) throws AuthenticationException;
}
