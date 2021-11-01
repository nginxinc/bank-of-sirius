package sirius.samples.bankofsirius.ledgerwriter;

import org.springframework.web.client.RestClientException;

public class ReadAvailableBalanceException extends RuntimeException {
    public ReadAvailableBalanceException(final String s) {
        super(s);
    }

    public ReadAvailableBalanceException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public boolean isCauseFromRemoteAccessFailure() {
        if (getCause() instanceof RestClientException) {
            return false;
        }

        return true;
    }
}
