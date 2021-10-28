// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sirius.samples.bankofsirius.ledger;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;

/**
 * Repository class for performing queries on the Transaction database
 */
@Repository
@Component
public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    @Query(value = "--find_balance \n"
    + "SELECT "
    + "(SELECT SUM(amount) FROM transactions t "
    + "     WHERE (to_acct = ?1 AND to_route = ?2)) - "
    + " (SELECT COALESCE((SELECT SUM(amount) FROM transactions t "
    + "     WHERE (from_acct = ?1 AND from_route = ?2)),0))",
        nativeQuery = true)
    Long findBalance(String accountNum, String routeNum);

    @Query(value = "--find_latest \n"
        + "SELECT * FROM transactions t "
        + "WHERE t.transaction_id > ?1 "
        + "ORDER BY t.transaction_id ASC",
        nativeQuery = true)
    List<Transaction> findLatest(long latestTransaction);

    /**
     * Returns the id of the latest transaction, or NULL if none exist.
     */
    @Query(value = "--latest_transaction_id \n"
        + "SELECT MAX(transaction_id) FROM transactions",
        nativeQuery = true)
    Long latestTransactionId();

    @Query(value = "--find_for_account \n"
            + "SELECT * FROM transactions t\n" +
            " WHERE (t.from_acct=?1 AND t.from_route=?2) OR (t.to_acct=?1 AND t.to_route=?2)\n" +
            " ORDER BY t.timestamp DESC",
            nativeQuery = true)
    LinkedList<Transaction> findForAccount(final String accountNum,
                                           final String routingNum,
                                           final Pageable pager);
}
