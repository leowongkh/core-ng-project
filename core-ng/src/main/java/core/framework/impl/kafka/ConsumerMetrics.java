package core.framework.impl.kafka;

import core.framework.api.util.Maps;
import core.framework.impl.log.stat.StatsCollector;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import java.util.Map;

/**
 * @author neo
 */
public class ConsumerMetrics implements StatsCollector {
    private final String name;
    private final Map<String, Metric> recordsLagMax = Maps.newConcurrentHashMap();
    private final Map<String, Metric> recordsConsumedRate = Maps.newConcurrentHashMap();
    private final Map<String, Metric> bytesConsumedRate = Maps.newConcurrentHashMap();
    private final Map<String, Metric> fetchRate = Maps.newConcurrentHashMap();

    public ConsumerMetrics(String name) {
        this.name = name;
    }

    @Override
    public void collect(Map<String, Double> stats) {
        stats.put(statName("records_max_lag"), sum(recordsLagMax));
        stats.put(statName("records_consumed_rate"), sum(recordsConsumedRate));
        stats.put(statName("bytes_consumed_rate"), sum(bytesConsumedRate));
        stats.put(statName("fetch_rate"), sum(fetchRate));
    }

    void addMetrics(String clientId, Map<MetricName, ? extends Metric> kafkaMetrics) {
        for (Map.Entry<MetricName, ? extends Metric> entry : kafkaMetrics.entrySet()) {
            MetricName name = entry.getKey();
            if ("consumer-fetch-manager-metrics".equals(name.group())) {
                if ("records-lag-max".equals(name.name())) recordsLagMax.put(clientId, entry.getValue());
                else if ("records-consumed-rate".equals(name.name())) recordsConsumedRate.put(clientId, entry.getValue());
                else if ("bytes-consumed-rate".equals(name.name())) bytesConsumedRate.put(clientId, entry.getValue());
                else if ("fetch-rate".equals(name.name())) fetchRate.put(clientId, entry.getValue());
            }
        }
    }

    void removeMetrics(String clientId) {
        recordsLagMax.remove(clientId);
        recordsConsumedRate.remove(clientId);
        bytesConsumedRate.remove(clientId);
        fetchRate.remove(clientId);
    }

    private double sum(Map<String, Metric> metrics) {
        return metrics.values().stream().mapToDouble(Metric::value).filter(Double::isFinite).sum();
    }

    private String statName(String statName) {
        StringBuilder builder = new StringBuilder("kafka_consumer");
        if (name != null) builder.append('_').append(name);
        builder.append('_').append(statName);
        return builder.toString();
    }
}
