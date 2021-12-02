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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.StringJoiner;

@Configuration
@ConfigurationProperties("http.client")
public class HttpClientProperties {

    /**
     * Default HTTP connection keep-alive time in milliseconds. -1 for indefinite keep alive.
     */
    private int defaultKeepAliveTime = -1;

    /**
     * Properties relating to HTTP client connection timeouts.
     */
    private TimeoutsProperties timeouts;

    /**
     * Properties used when HTTP client connection pooling is enabled.
     */
    private PoolingProperties pooling;

    @Autowired
    public HttpClientProperties(final TimeoutsProperties timeouts,
                                final PoolingProperties pooling) {
        this.timeouts = timeouts;
        this.pooling = pooling;
    }

    public int getDefaultKeepAliveTime() {
        return defaultKeepAliveTime;
    }

    public void setDefaultKeepAliveTime(final int defaultKeepAliveTime) {
        this.defaultKeepAliveTime = defaultKeepAliveTime;
    }

    public TimeoutsProperties getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(final TimeoutsProperties timeouts) {
        this.timeouts = timeouts;
    }

    public PoolingProperties getPooling() {
        return pooling;
    }

    public void setPooling(final PoolingProperties pooling) {
        this.pooling = pooling;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HttpClientProperties.class.getSimpleName() + "[", "]")
                .add("defaultKeepAliveTime=" + defaultKeepAliveTime)
                .add("timeouts=" + timeouts)
                .add("pooling=" + pooling)
                .toString();
    }
}
