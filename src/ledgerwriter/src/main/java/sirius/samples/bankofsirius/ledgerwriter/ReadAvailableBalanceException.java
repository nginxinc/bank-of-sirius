package sirius.samples.bankofsirius.ledgerwriter;

public class ReadAvailableBalanceException extends RuntimeException {
    public ReadAvailableBalanceException(final String s) {
        super(s);
    }

    public ReadAvailableBalanceException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
