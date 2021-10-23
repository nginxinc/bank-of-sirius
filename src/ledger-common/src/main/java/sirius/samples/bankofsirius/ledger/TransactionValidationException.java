package sirius.samples.bankofsirius.ledger;

public class TransactionValidationException extends Exception {
    private final String httpErrorMessage;

    public TransactionValidationException(final String msg,
                                          final String httpErrorMessage) {
        super(msg);
        this.httpErrorMessage = httpErrorMessage;
    }

    public String getHttpErrorMessage() {
        return httpErrorMessage;
    }
}
