package sirius.samples.bankofsirius.transactionhistory;

import com.google.common.cache.LoadingCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import sirius.samples.bankofsirius.ledger.LedgerReaderCallback;
import sirius.samples.bankofsirius.ledger.Transaction;

import java.util.Deque;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class InitLedgerReaderCallback implements LedgerReaderCallback {
    private static final Logger LOGGER = LogManager.getLogger(InitLedgerReaderCallback.class);
    private final LoadingCache<String, Deque<Transaction>> cache;
    private final String localRoutingNum;
    private final Integer historyLimit;

    @Autowired
    public InitLedgerReaderCallback(final LoadingCache<String, Deque<Transaction>> cache,
                                    @Value("${LOCAL_ROUTING_NUM}") final String localRoutingNum,
                                    @Value("${HISTORY_LIMIT:100}") final Integer historyLimit) {
        this.cache = cache;
        this.localRoutingNum = localRoutingNum;
        this.historyLimit = historyLimit;
    }

    @Override
    public void processTransaction(final Transaction transaction) {
        final String fromId = transaction.getFromAccountNum();
        final String fromRouting = transaction.getFromRoutingNum();
        final String toId = transaction.getToAccountNum();
        final String toRouting = transaction.getToRoutingNum();

        if (fromRouting.equals(localRoutingNum)
                && cache.asMap().containsKey(fromId)) {
            addSingleTransactionToCache(fromId, transaction);
        }
        if (toRouting.equals(localRoutingNum)
                && cache.asMap().containsKey(toId)) {
            addSingleTransactionToCache(toId, transaction);
        }
        LOGGER.info("Initialized transaction processor");
    }

    /**
     * Helper function to add a single transaction to the internal cache
     *
     * @param accountId   the accountId associated with the transaction
     * @param transaction the full transaction object
     */
    private void addSingleTransactionToCache(String accountId, Transaction transaction) {
        LOGGER.debug("Modifying transaction cache: " + accountId);
        final Deque<Transaction> tList = this.cache.asMap().get(accountId);
        tList.addFirst(transaction);
        // Drop old transactions
        if (tList.size() > historyLimit) {
            tList.removeLast();
        }
    }
}
