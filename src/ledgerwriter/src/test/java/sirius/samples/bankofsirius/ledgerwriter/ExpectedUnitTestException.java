package sirius.samples.bankofsirius.ledgerwriter;

public class ExpectedUnitTestException extends RuntimeException {
    public ExpectedUnitTestException() {
        super("Predictable exception from unit test");
    }
}
