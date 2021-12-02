/*
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
package sirius.samples.bankofsirius.ledgerwriter.http;

import org.apache.http.conn.DnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation {@link DnsResolver} that shuffles the results of a DNS query
 * upon every query, so that we get better distribution of connections to
 * different hosts backed by the same name.
 */
public class ShufflingDnsResolver implements DnsResolver {
    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {
        final InetAddress[] addresses = InetAddress.getAllByName(host);
        shuffle(addresses);

        return addresses;
    }

    /**
     * Shuffles an array of addresses that were returned from a DNS query.
     * Shuffle algorithm inspired by <a href="http://stackoverflow.com/a/1520212/33611">this stackoverflow post.</a>
     * @param addresses addresses to shuffle
     */
    private static void shuffle(final InetAddress[] addresses) {
        // Only shuffle if we have 2 or more addresses
        if (addresses.length < 2) {
            return;
        }

        final Random randomizer = ThreadLocalRandom.current();

        for (int i = addresses.length - 1; i > 0; i--) {
            int index = randomizer.nextInt(i + 1);
            // Simple swap
            InetAddress a = addresses[index];
            addresses[index] = addresses[i];
            addresses[i] = a;
        }
    }
}
