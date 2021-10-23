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

import com.google.common.cache.LoadingCache;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import sirius.samples.bankofsirius.health.ScheduledTaskHealthIndicator;
import sirius.samples.bankofsirius.ledger.LedgerReader;
import sirius.samples.bankofsirius.ledger.LedgerReaderCallback;
import sirius.samples.bankofsirius.ledger.TransactionRepository;

import javax.annotation.PreDestroy;
import java.time.Duration;

/**
 * Entry point for the BalanceReader Spring Boot application.
 *
 * Microservice to track the bank balance for each user account.
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "sirius.samples.bankofsirius.ledger")
@EntityScan(basePackages = "sirius.samples.bankofsirius.ledger")
@ComponentScan(basePackages = "sirius.samples.bankofsirius")
public class BalanceReaderApplication {
    private static final Logger LOGGER =
        LogManager.getLogger(BalanceReaderApplication.class);

    private static final String[] EXPECTED_ENV_VARS = {
        "VERSION",
        "PORT",
        "LOCAL_ROUTING_NUM",
        "PUB_KEY_PATH",
        "SPRING_DATASOURCE_URL",
        "SPRING_DATASOURCE_USERNAME",
        "SPRING_DATASOURCE_PASSWORD"
    };

    public static void main(String[] args) {
        // Check that all required environment variables are set.
        for (String v : EXPECTED_ENV_VARS) {
            String value = System.getenv(v);
            if (value == null) {
                LOGGER.fatal(String.format(
                    "%s environment variable not set", v));
                System.exit(1);
            }
        }
        SpringApplication.run(BalanceReaderApplication.class, args);
        LOGGER.log(Level.forName("STARTUP", Level.FATAL.intLevel()),
            String.format("Started BalanceReader service. Log level is: %s",
                LOGGER.getLevel().toString()));

    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public LedgerReaderCallback initLedgerReader(@Value("${LOCAL_ROUTING_NUM}")
                                                 final String localRoutingNum,
                                                 final LoadingCache<String, Long> cache) {
        return transaction -> {
            final String fromId = transaction.getFromAccountNum();
            final String fromRouting = transaction.getFromRoutingNum();
            final String toId = transaction.getToAccountNum();
            final String toRouting = transaction.getToRoutingNum();
            final Integer amount = transaction.getAmount();

            if (fromRouting.equals(localRoutingNum)
                    && cache.asMap().containsKey(fromId)) {
                Long prevBalance = cache.asMap().get(fromId);
                cache.put(fromId, prevBalance - amount);
            }
            if (toRouting.equals(localRoutingNum)
                    && cache.asMap().containsKey(toId)) {
                Long prevBalance = cache.asMap().get(toId);
                cache.put(toId, prevBalance + amount);
            }
            final Logger logger = LogManager.getLogger(LedgerReaderCallback.class);
            logger.info("Initialized transaction processor");
        };
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public LedgerReader ledgerReader(final TransactionRepository dbRepo,
                                     final LedgerReaderCallback processTransactionCallback,
                                     final Tracer tracer) {
        return new LedgerReader(dbRepo, processTransactionCallback, tracer);
    }

    @Bean
    public ScheduledTaskHealthIndicator ledgerReaderHealth(@Value("${POLL_MS:100}") final Integer pollMs,
                                                           final LedgerReader ledgerReader) {
        final Duration expectedRuntimeOfTask = Duration.ofSeconds(50L);
        final Duration acceptableVariance = Duration.ofSeconds(30L);
        final Duration expectedIntervalBetweenRuns = Duration.ofMillis(pollMs);

        return new ScheduledTaskHealthIndicator("LedgerReader",
                expectedRuntimeOfTask, acceptableVariance, expectedIntervalBetweenRuns,
                ledgerReader::getLastRun);
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("BalanceReader service shutting down");
    }
}
