package core.framework.internal.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class ActionLogTest {
    private ActionLog log;

    @BeforeEach
    void createActionLog() {
        log = new ActionLog("begin", "actionId");
    }

    @Test
    void contextValueIsTooLong() {
        log.context("key", "x".repeat(ActionLog.MAX_CONTEXT_VALUE_LENGTH + 1));
        assertThat(log.result()).isEqualTo("WARN");
        assertThat(log.errorMessage).contains("context value is too long");
        assertThat(log.context.get("key")).isEmpty();
    }

    @Test
    void multipleContextValues() {
        log.context("key", "value1");
        log.context("key", "value2");
        assertThat(log.context).containsEntry("key", List.of("value1", "value2"));
    }

    @Test
    void flushTraceLogWithTrace() {
        log.trace = Trace.CURRENT;
        assertThat(log.flushTraceLog()).isTrue();

        log.trace = Trace.CASCADE;
        assertThat(log.flushTraceLog()).isTrue();
    }

    @Test
    void flushTraceLogWithWarning() {
        log.process(new LogEvent("logger", null, LogLevel.WARN, null, null, null));

        assertThat(log.flushTraceLog()).isTrue();
    }

    @Test
    void flushTraceLog() {
        assertThat(log.flushTraceLog()).isFalse();
    }

    @Test
    void result() {
        assertThat(log.result()).isEqualTo("OK");

        log.process(new LogEvent("logger", null, LogLevel.WARN, null, null, null));
        assertThat(log.result()).isEqualTo("WARN");
    }

    @Test
    void errorCode() {
        assertThat(log.errorCode()).isNull();

        log.process(new LogEvent("logger", null, LogLevel.WARN, null, null, null));
        assertThat(log.errorCode()).isEqualTo("UNASSIGNED");
    }

    @Test
    void truncateErrorMessage() {
        log.process(new LogEvent("logger", null, LogLevel.WARN, "x".repeat(ActionLog.MAX_CONTEXT_VALUE_LENGTH + 1), null, null));

        assertThat(log.errorMessage.length()).isEqualTo(ActionLog.MAX_CONTEXT_VALUE_LENGTH);
    }

    @Test
    void stat() {
        log.stat("stat", 1);
        assertThat(log.stats.get("stat").intValue()).isEqualTo(1);

        log.stat("stat", 1);
        assertThat(log.stats.get("stat").intValue()).isEqualTo(2);
    }

    @Test
    void track() {
        assertThat(log.track("db", 1000, 1, 0)).isEqualTo(1);
        PerformanceStat stat = log.performanceStats.get("db");
        assertThat(stat.count).isEqualTo(1);
        assertThat(stat.totalElapsed).isEqualTo(1000);
        assertThat(stat.readEntries).isEqualTo(1);
        assertThat(stat.writeEntries).isEqualTo(0);

        assertThat(log.track("db", 1000, 1, 1)).isEqualTo(2);
        stat = log.performanceStats.get("db");
        assertThat(stat.count).isEqualTo(2);
        assertThat(stat.totalElapsed).isEqualTo(2000);
        assertThat(stat.readEntries).isEqualTo(2);
        assertThat(stat.writeEntries).isEqualTo(1);

        assertThat(log.track("http", 3000, 0, 0)).isEqualTo(1);
        stat = log.performanceStats.get("http");
        assertThat(stat.count).isEqualTo(1);
        assertThat(stat.totalElapsed).isEqualTo(3000);
        assertThat(stat.readEntries).isZero();
        assertThat(stat.writeEntries).isZero();
    }

    @Test
    void trace() {
        String suffix = "...(soft trace limit reached)\n";
        String trace = log.trace(200, 500);
        assertThat(trace).hasSize(200 + suffix.length())
            .contains("ActionLog - begin")
            .endsWith(suffix);

        log.process(new LogEvent("logger", null, LogLevel.WARN, "warning", null, null));

        trace = log.trace(200, 500);
        assertThat(trace).endsWith("warning\n");

        trace = log.trace(250, 500);   // the max debug length will hit warning event
        assertThat(trace).contains("warning")
            .endsWith(suffix);

        log.process(new LogEvent("logger", null, LogLevel.WARN, "warning2", null, null));
        trace = log.trace(250, 320);   // truncate with hard limit
        assertThat(trace).endsWith("...(hard trace limit reached)");
    }
}
