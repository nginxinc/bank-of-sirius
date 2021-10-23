/*
 * Copyright 2020, Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sirius.samples.bankofsirius.transactionhistory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.ResourceAccessException;
import sirius.samples.bankofsirius.ledger.Transaction;
import sirius.samples.bankofsirius.ledger.TransactionRepository;

import java.util.Deque;
import java.util.concurrent.TimeUnit;


/**
 * TransactionCache creates the LoadingCache that handles caching
 * and retrieving account transactions from the TransactionRepository.
 */
@Configuration
public class TransactionCache {

    private static final Logger LOGGER =
            LogManager.getLogger(TransactionCache.class);

    private final TransactionRepository dbRepo;
    private final Tracer tracer;

    @Autowired
    public TransactionCache(final TransactionRepository dbRepo,
                            final Tracer tracer) {
        this.dbRepo = dbRepo;
        this.tracer = tracer;
    }

    /**
     * Initializes the LoadingCache for the TransactionHistoryController
     *
     * @param expireSize      max size of the cache
     * @param localRoutingNum bank routing number for account
     * @return the LoadingCache storing accountIds and their transactions
     */
    @Bean
    public LoadingCache<String, Deque<Transaction>> initializeCache(
            @Value("${CACHE_SIZE:1000000}") final Integer expireSize,
            @Value("${CACHE_MINUTES:60}") final Integer expireMinutes,
            @Value("${LOCAL_ROUTING_NUM}") final String localRoutingNum,
            @Value("${HISTORY_LIMIT:100}") final Integer historyLimit) {

        final CacheLoader<String, Deque<Transaction>> loader =
                new CacheLoader<String, Deque<Transaction>>() {
                    @Override
                    public Deque<Transaction> load(final String accountId)
                            throws ResourceAccessException, DataAccessResourceFailureException {
                        final Span span = tracer.spanBuilder().name("load_cache").start();

                        try {
                            Pageable request = PageRequest.of(0, historyLimit);
                            Deque<Transaction> transactions = dbRepo.findForAccount(
                                    accountId, localRoutingNum, request);

                            if (LOGGER.isDebugEnabled()) {
                                String msg = "Loaded from db into cache [count={}, accountId={}]";
                                LOGGER.debug(msg, transactions.size(), accountId);
                            }

                            span.tag("cache.miss", "true");
                            return transactions;
                        } finally {
                            span.end();
                        }
                    }
                };

        return CacheBuilder.newBuilder()
                .recordStats()
                .maximumSize(expireSize)
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .build(loader);
    }
}
