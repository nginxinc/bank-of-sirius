package sirius.samples.bankofsirius.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

public class LedgerReader implements Runnable {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LedgerReader.class);
    private static final long STARTING_TRANSACTION_ID = -1;

    private long latestTransactionId;
    private final TransactionRepository dbRepo;
    private final LedgerReaderCallback processTransactionCallback;
    private final Tracer tracer;
    private volatile boolean firstRun = true;
    private volatile Instant lastRun;

    public LedgerReader(final TransactionRepository dbRepo,
                        final LedgerReaderCallback processTransactionCallback,
                        final Tracer tracer) {
        this.dbRepo = dbRepo;
        this.tracer = tracer;

        this.processTransactionCallback = processTransactionCallback;
    }

    @Scheduled(fixedDelayString = "${POLL_MS:100}")
    @Override
    public void run() {
        final Span span = tracer.currentSpan();
        if (span != null) {
            span.name("poll_ledger");
        }

        synchronized (this) {
            this.lastRun = Instant.now();

            if (firstRun) {
                initializeTransactionId();
                LOGGER.info("First run - initialized transaction ids [latest_transaction_id={}]",
                        this.latestTransactionId);
                firstRun = false;
            }
        }

        try {
            readTransactionIds();
        } finally {
            if (span != null) {
                span.tag("latest_transaction_id", Long.toString(this.latestTransactionId));
            }
        }
    }

    private void initializeTransactionId() {
        final Span span = tracer.spanBuilder()
                .name("poll_ledger_init")
                .start();

        try (final Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            this.latestTransactionId = getLatestTransactionId();
        } finally {
            span.tag("latest_transaction_id", Long.toString(this.latestTransactionId));
            span.end();
        }
    }

    /**
     * Checks to new updates in ledger.
     */
    private void readTransactionIds() {
        final Span span = tracer.nextSpan().name("read_transaction_ids").start();

        try {
            long remoteLatest;

            try {
                remoteLatest = getLatestTransactionId();
                span.tag("latest_transaction_id", Long.toString(remoteLatest));
            } catch (RuntimeException e) {
                remoteLatest = latestTransactionId;
                final String msg = "Could not reach ledger database";
                span.event(msg);
                span.error(e);
                LOGGER.warn(msg);
            }

            // if there are new transactions, poll the database
            if (remoteLatest > latestTransactionId) {
                latestTransactionId = pollTransactions(latestTransactionId);
            } else if (remoteLatest < latestTransactionId) {
                // remote database out of sync
                // suspend processing transactions to reset service
                final String msg = "Remote transaction id out of sync";
                LOGGER.error(msg);
                span.tag("error", "true");
                span.event(msg);
            }
        } finally {
            span.end();
        }
    }

    /**
     * Poll for new transactions
     * Execute callback for each one
     *
     * @param startingId the transaction to start reading after.
     *                            -1 = start reading at beginning of the ledger
     * @return long id of latest transaction processed
     */
    private long pollTransactions(final long startingId) {
        final Span span = tracer.nextSpan().name("poll_transactions").start();

        try {
            long latestId = startingId;
            Iterable<Transaction> transactionList = dbRepo.findLatest(startingId);
            LOGGER.debug("Polling Transactions");

            long count = 0L;
            for (Transaction transaction : transactionList) {
                processTransactionCallback.processTransaction(transaction);
                latestId = transaction.getTransactionId();
                count++;
            }

            span.tag("total_processed_transactions", Long.toString(count));
            span.tag("latest_transaction_id", Long.toString(latestId));
            return latestId;
        } finally {
            span.end();
        }
    }

    /**
     * Returns the id of the most recent transaction.
     *
     * @return the transaction id as a long or -1 if no transactions exist
     */
    long getLatestTransactionId() {
        final Span span = tracer.nextSpan().name("get_latest_transaction_id").start();

        try {
            Long latestId = null;

            try {
                latestId = dbRepo.latestTransactionId();
                span.tag("latest_transaction_id", Long.toString(latestId));
            } catch (RuntimeException e) {
                final String msg;
                if (this.latestTransactionId == STARTING_TRANSACTION_ID) {
                    msg = "Could not contact ledger database at init";
                } else {
                    msg = "Could not contact ledger database";
                }

                span.tag("error", "true");
                span.event(msg);
                span.error(e);
                LOGGER.warn(msg, e);
            }

            if (latestId == null) {
                return STARTING_TRANSACTION_ID;
            }
            return latestId;
        } finally {
            span.end();
        }
    }

    public Instant getLastRun() {
        return lastRun;
    }
}
