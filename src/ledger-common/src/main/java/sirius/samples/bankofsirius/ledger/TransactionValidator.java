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

package sirius.samples.bankofsirius.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;


/**
 * Validator to authenticate transaction.
 *
 * Functions to validate transaction details before adding to the ledger.
 */
@Component
public class TransactionValidator {

    // account ids should be 10 digits between 0 and 9
    private static final Pattern ACCT_REGEX = Pattern.compile("^[0-9]{10}$");
    // route numbers should be 9 digits between 0 and 9
    private static final Pattern ROUTE_REGEX = Pattern.compile("^[0-9]{9}$");

    private static final Logger LOGGER =
        LoggerFactory.getLogger(TransactionValidator.class);

    private final Tracer tracer;

    @Autowired
    public TransactionValidator(final Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     *   - Ensure sender is the same user authenticated by auth token
     *   - Ensure account and routing numbers are in the correct format
     *   - Ensure sender and receiver are different accounts
     *   - Ensure amount is positive
     *
     * @throws IllegalArgumentException  on validation error
     */
    public void validateTransaction(final String localRoutingNum,
                                    final String authedAcct,
                                    final Transaction transaction)
            throws TransactionValidationException {
        final Span span = tracer.spanBuilder().name("validate_transaction").start();

        try {
            LOGGER.debug("Validating transaction");

            final String fromAcct = transaction.getFromAccountNum();
            final String fromRoute = transaction.getFromRoutingNum();
            final String toAcct = transaction.getToAccountNum();
            final String toRoute = transaction.getToRoutingNum();
            final Integer amount = transaction.getAmount();

            Objects.requireNonNull(fromAcct, "fromAcct must not be null");
            Objects.requireNonNull(fromRoute, "fromRoute must not be null");
            Objects.requireNonNull(toAcct, "toAcct must not be null");
            Objects.requireNonNull(toRoute, "toRoute must not be null");
            Objects.requireNonNull(amount, "amount must not be null");

            // Validate account and routing numbers.
            if (!ACCT_REGEX.matcher(fromAcct).matches()
                    || !ACCT_REGEX.matcher(toAcct).matches()
                    || !ROUTE_REGEX.matcher(
                    fromRoute).matches()
                    || !ROUTE_REGEX.matcher(
                    toRoute).matches()) {
                throw new TransactionValidationException(
                        "Invalid transaction: Invalid account details",
                        ExceptionMessages.EXCEPTION_MESSAGE_INVALID_NUMBER);
            }
            // If this is an internal transaction,
            // ensure it originated from the authenticated user.
            if (fromRoute.equals(localRoutingNum) && !fromAcct.equals(authedAcct)) {
                throw new TransactionValidationException(
                        "Invalid transaction: Sender not authorized",
                        ExceptionMessages.EXCEPTION_MESSAGE_NOT_AUTHENTICATED);
            }
            // Ensure sender isn't receiver.
            if (fromAcct.equals(toAcct) && fromRoute.equals(toRoute)) {
                throw new TransactionValidationException(
                        "Invalid transaction: Sender is also receiver",
                        ExceptionMessages.EXCEPTION_MESSAGE_SEND_TO_SELF);
            }
            // Ensure amount is valid value.
            if (amount <= 0) {
                throw new TransactionValidationException(
                        "Invalid transaction: Transaction amount invalid",
                        ExceptionMessages.EXCEPTION_MESSAGE_INVALID_AMOUNT);
            }
        } catch (RuntimeException e) {
            span.tag("transaction", Objects.toString(transaction));
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
