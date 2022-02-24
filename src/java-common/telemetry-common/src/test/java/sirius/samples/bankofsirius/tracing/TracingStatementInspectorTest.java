package sirius.samples.bankofsirius.tracing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class TracingStatementInspectorTest {
    @Mock
    private Tracer tracer;
    @Mock
    private Span span;
    @Mock
    private TraceContext traceContext;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        this.mocks = openMocks(this);

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (this.mocks != null) {
            this.mocks.close();
        }
    }

    @Test
    public void queryUnmodifiedWhenNoTrace() {
        TracingStatementInspector inspector = new TracingStatementInspector(null);
        String sql = "--latest_transaction_id \n"
                + "SELECT MAX(transaction_id) FROM transactions";
        String actual = inspector.inspect(sql);
        assertEquals(sql, actual);
    }

    @Test
    public void queryUnmodifiedWhenNoSpan() {
        TracingStatementInspector inspector = new TracingStatementInspector(tracer);
        String sql = "--latest_transaction_id \n"
                + "SELECT MAX(transaction_id) FROM transactions";
        String actual = inspector.inspect(sql);
        assertEquals(sql, actual);
    }

    @Test
    public void queryUnmodifiedWhenUnknownTracingAndSpanId() {
        when(traceContext.traceId()).thenReturn(TracingStatementInspector.UNKNOWN_TRACE_ID);
        when(traceContext.spanId()).thenReturn(TracingStatementInspector.UNKNOWN_SPAN_ID);

        TracingStatementInspector inspector = new TracingStatementInspector(tracer);
        String sql = "--latest_transaction_id \n"
                + "SELECT MAX(transaction_id) FROM transactions";
        String actual = inspector.inspect(sql);
        assertEquals(sql, actual);
    }

    @Test
    public void queryWithUnknownTraceId() {
        String spanId = "1234567890abcdef";
        when(traceContext.traceId()).thenReturn(TracingStatementInspector.UNKNOWN_TRACE_ID);
        when(traceContext.spanId()).thenReturn(spanId);

        TracingStatementInspector inspector = new TracingStatementInspector(tracer);
        String sql = "--latest_transaction_id \n"
                + "SELECT MAX(transaction_id) FROM transactions";
        String expected = String.format("--latest_transaction_id \n"
                + "-- span_id: %s\n"
                + "SELECT MAX(transaction_id) FROM transactions", spanId);
        String actual = inspector.inspect(sql);
        assertEquals(expected, actual);
    }

    @Test
    public void queryWithUnknownSpanId() {
        String traceId = "12345678901234567890123456789abc";
        when(traceContext.traceId()).thenReturn(traceId);
        when(traceContext.spanId()).thenReturn(TracingStatementInspector.UNKNOWN_SPAN_ID);

        TracingStatementInspector inspector = new TracingStatementInspector(tracer);
        String sql = "--latest_transaction_id \n"
                + "SELECT MAX(transaction_id) FROM transactions";
        String expected = String.format("--latest_transaction_id \n"
                + "-- trace_id: %s\n"
                + "SELECT MAX(transaction_id) FROM transactions", traceId);
        String actual = inspector.inspect(sql);
        assertEquals(expected, actual);
    }

    @Test
    public void queryWithTraceIdAndSpanId() {
        String traceId = "12345678901234567890123456789abc";
        String spanId = "1234567890abcdef";
        when(traceContext.traceId()).thenReturn(traceId);
        when(traceContext.spanId()).thenReturn(spanId);

        TracingStatementInspector inspector = new TracingStatementInspector(tracer);
        String sql = "--latest_transaction_id \n"
                + "SELECT MAX(transaction_id) FROM transactions";
        String expected = String.format("--latest_transaction_id \n"
                + "-- trace_id: %s span_id: %s\n"
                + "SELECT MAX(transaction_id) FROM transactions", traceId, spanId);
        String actual = inspector.inspect(sql);
        assertEquals(expected, actual);
    }

    @Test
    public void invalidIdEmptyFails() {
        String badTraceId = "";
        boolean actual = TracingStatementInspector.isInvalidId(badTraceId);
        assertTrue(actual, "empty trace id should fail validity check");
    }

    @Test
    public void invalidIdWithOddLengthFails() {
        String badTraceId = "0101a";
        boolean actual = TracingStatementInspector.isInvalidId(badTraceId);
        assertTrue(actual, "trace id with non-hex chars should fail validity check");
    }

    @Test
    public void invalidIdWithNonHexCharacterFails() {
        String badTraceId = "gg";
        boolean actual = TracingStatementInspector.isInvalidId(badTraceId);
        assertTrue(actual, "trace id with non-hex chars should fail validity check");
    }
}
