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

package sirius.samples.bankofsirius.balancereader;

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
import org.springframework.web.client.ResourceAccessException;
import sirius.samples.bankofsirius.ledger.TransactionRepository;

/**
 * BalanceCache creates the LoadingCache that handles caching
 * and retrieving account balances from the TransactionRepository.
 */
@Configuration
public class BalanceCache {

    private static final Logger LOGGER =
        LogManager.getLogger(BalanceCache.class);

    private final TransactionRepository dbRepo;
    private final Tracer tracer;

    @Autowired
    public BalanceCache(final TransactionRepository dbRepo,
                        final Tracer tracer) {
        this.dbRepo = dbRepo;
        this.tracer = tracer;
    }

    /**
     * Initializes the LoadingCache for the BalanceReaderController
     *
     * @param expireSize max size of the cache
     * @param localRoutingNum bank routing number for account
     * @return the LoadingCache storing accountIds and their balances
     */
    @Bean(name = "cache")
    public LoadingCache<String, Long> initializeCache(
        @Value("${CACHE_SIZE:1000000}") final Integer expireSize,
        @Value("${LOCAL_ROUTING_NUM}") String localRoutingNum) {
        CacheLoader<String, Long> loader =  new CacheLoader<String, Long>() {
            @Override
            public Long load(String accountId)
                throws ResourceAccessException, DataAccessResourceFailureException {
                final Span span = tracer.nextSpan().name("load_cache");

                try {
                    LOGGER.debug("Balance loaded into cache [accountId={}, localRoutingNum={}]",
                            accountId, localRoutingNum);
                    Long balance = dbRepo.findBalance(accountId, localRoutingNum);
                    if (balance == null) {
                        balance = 0L;
                        LOGGER.warn("No balance found in database for account [accountId={}]",
                                accountId);
                    }

                    span.tag("cache.miss", "true");
                    return balance;
                } finally {
                    span.end();
                }
            }
        };
        return CacheBuilder.newBuilder()
            .recordStats()
            .maximumSize(expireSize)
            .build(loader);
    }
}
