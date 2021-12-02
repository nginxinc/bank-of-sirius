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
package sirius.samples.bankofsirius.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

public class ScheduledTaskHealthIndicator extends AbstractHealthIndicator {
    private final String taskName;
    private final Duration expectedRuntimeOfTask;
    private final Duration acceptableVariance;
    private final Duration expectedIntervalBetweenRuns;
    private final Duration maxIntervalBetweenRuns;
    private final Supplier<Instant> lastRunTime;

    public ScheduledTaskHealthIndicator(final String taskName,
                                        final Duration expectedRuntimeOfTask,
                                        final Duration acceptableVariance,
                                        final Duration expectedIntervalBetweenRuns,
                                        final Supplier<Instant> lastRunTime) {
        super("Scheduled task is not healthy");

        this.taskName = taskName;
        this.expectedRuntimeOfTask = expectedRuntimeOfTask;
        this.acceptableVariance = acceptableVariance;
        this.expectedIntervalBetweenRuns = expectedIntervalBetweenRuns;
        this.maxIntervalBetweenRuns = calculateMaxIntervalBetweenRuns();
        this.lastRunTime = lastRunTime;
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) {
        builder.withDetail("taskName", this.taskName);
        final Instant lastRun = lastRunTime.get();
        builder.withDetail("lastRun", Objects.toString(lastRun));

        if (lastRun == null) {
            throw new NullPointerException("Unable to get last run time");
        }

        final Duration timeSinceLastRun = Duration.between(lastRun, Instant.now());
        builder.withDetail("timeSinceLastRunMs", timeSinceLastRun.toMillis());

        if (maxIntervalBetweenRuns.minus(timeSinceLastRun).isNegative()) {
            String msg = String.format("%s is not running with the "
                    + "expected schedule", this.taskName);
            builder.withDetail("error", msg);
            builder.down();
        } else {
            builder.up();
        }
    }

    protected Duration calculateMaxIntervalBetweenRuns() {
        return this.expectedRuntimeOfTask
                .plus(this.expectedIntervalBetweenRuns)
                .plus(this.acceptableVariance);
    }
}
