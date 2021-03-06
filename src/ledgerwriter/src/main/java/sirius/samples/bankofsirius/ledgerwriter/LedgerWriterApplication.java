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

package sirius.samples.bankofsirius.ledgerwriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PreDestroy;

/**
 * Entry point for the LedgerWriter Spring Boot application.
 *
 * Microservice to accept new transactions for the bank ledger.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "sirius.samples.bankofsirius.ledger")
@EntityScan(basePackages = "sirius.samples.bankofsirius.ledger")
@ComponentScan(basePackages = "sirius.samples.bankofsirius")
public class LedgerWriterApplication {

    private static final Logger LOGGER =
        LogManager.getLogger(LedgerWriterApplication.class);

    private static final String[] EXPECTED_ENV_VARS = {
        "VERSION",
        "PORT",
        "LOCAL_ROUTING_NUM",
        "BALANCES_API_ADDR",
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
        SpringApplication.run(LedgerWriterApplication.class, args);
        LOGGER.log(Level.forName("STARTUP", Level.FATAL.intLevel()),
            String.format("Started LedgerWriter service. Log level is: %s",
                LOGGER.getLevel().toString()));
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("LedgerWriter service shutting down");
    }
}
