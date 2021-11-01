package sirius.samples.bankofsirius.tracing;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.Scanner;

public class TracingStatementInspector implements StatementInspector {
    static final String UNKNOWN_TRACE_ID = "00000000000000000000000000000000";
    static final String UNKNOWN_SPAN_ID = "0000000000000000";

    private final Tracer tracer;

    public TracingStatementInspector(final Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Checks to see that a given id is only a hex string.
     * Although this method may seem unneeded, it is an important security
     * safeguard because it prevents a potential SQL injection from
     * a trace id.
     *
     * @param id string to check
     * @return true if not a valid hex string
     */
    static boolean isInvalidId(final String id) {
        if (id == null || id.isEmpty()) {
            return true;
        }

        if (id.length() % 2 != 0) {
            return true;
        }

        final int beforeZero = ((int) '0') - 1;
        final int afterNine = ((int) '9') + 1;
        final int beforeA = ((int) 'a') - 1;
        final int afterF = ((int) 'f') + 1;

        for (final int charCode : id.toCharArray()) {
            final boolean isNumberOrHexLetter = (charCode > beforeZero && charCode < afterNine)
                    || (charCode > beforeA && charCode < afterF);
            if (!isNumberOrHexLetter) {
                return true;
            }
        }

        return false;
    }

    private Span findSpan() {
        if (tracer == null) {
            return null;
        }

        return tracer.currentSpan();
    }

    @Override
    public String inspect(final String sql) {
        final Span span = findSpan();

        if (span == null) {
            return sql;
        }

        final String traceId = span.context().traceId();
        final String spanId = span.context().spanId();
        final boolean traceIdUnknown = UNKNOWN_TRACE_ID.equals(traceId) || isInvalidId(traceId);
        final boolean spanIdUnknown = UNKNOWN_SPAN_ID.equals(spanId) || isInvalidId(spanId);

        span.tag("query", sql);

        // There is no trace id and there is no span id
        if (traceIdUnknown && spanIdUnknown) {
            return sql;
        }

        final StringBuilder sqlWithTraceIds = new StringBuilder();

        /* Allow existing comments to be persisted at the top of the SQL statement
         * then insert tracing comments below those comments. */
        try (Scanner scanner = new Scanner(sql)) {
            boolean inTopLevelComments = true;

            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                if (inTopLevelComments && !line.startsWith("--")) {
                    appendTraceIds(sqlWithTraceIds, traceId, spanId, traceIdUnknown, spanIdUnknown);
                    inTopLevelComments = false;
                }

                sqlWithTraceIds.append(line);

                if (scanner.hasNextLine()) {
                    sqlWithTraceIds.append(System.lineSeparator());
                }
            }
        }

        return sqlWithTraceIds.toString();
    }

    private static void appendTraceIds(final StringBuilder sqlWithTraceIds,
                                       final String traceId,
                                       final String spanId,
                                       final boolean traceIdUnknown,
                                       final boolean spanIdUnknown) {
        final String tracingComment;

        // There is a trace id but there is no span id
        if (!traceIdUnknown && spanIdUnknown) {
            tracingComment = String.format("-- trace_id: %s", traceId);
        // There is no trace id and there is a span id
        } else if (traceIdUnknown) {
            tracingComment = String.format("-- span_id: %s", spanId);
            // There is a trace id and there is a span id
        } else {
            tracingComment = String.format("-- trace_id: %s span_id: %s",
                    traceId, spanId);
        }

        sqlWithTraceIds.append(tracingComment).append(System.lineSeparator());
    }
}
