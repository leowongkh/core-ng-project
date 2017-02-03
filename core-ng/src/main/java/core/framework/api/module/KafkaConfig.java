package core.framework.api.module;

import core.framework.api.kafka.BulkMessageHandler;
import core.framework.api.kafka.MessageHandler;
import core.framework.api.kafka.MessagePublisher;
import core.framework.api.util.Types;
import core.framework.impl.kafka.ConsumerMetrics;
import core.framework.impl.kafka.Kafka;
import core.framework.impl.kafka.KafkaMessagePublisher;
import core.framework.impl.kafka.ProducerMetrics;
import core.framework.impl.module.ModuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
public final class KafkaConfig {
    private final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    private final ModuleContext context;
    private final Kafka kafka;
    private final String name;

    public KafkaConfig(ModuleContext context, String name) {
        this.context = context;
        this.name = name;
        if (context.beanFactory.registered(Kafka.class, name)) {
            kafka = context.beanFactory.bean(Kafka.class, name);
        } else {
            if (context.isTest()) {
                kafka = context.mockFactory.create(Kafka.class);
            } else {
                ProducerMetrics producerMetrics = new ProducerMetrics(name);
                ConsumerMetrics consumerMetrics = new ConsumerMetrics(name);
                context.statsCollectors.add(producerMetrics);
                context.statsCollectors.add(consumerMetrics);
                Kafka kafka = new Kafka(name, context.logManager, producerMetrics, consumerMetrics);
                context.startupHook.add(kafka::initialize);
                context.shutdownHook.add(kafka::close);
                this.kafka = kafka;
            }
            context.beanFactory.bind(Kafka.class, name, kafka);
        }
    }

    public <T> void publish(String topic, Class<T> messageClass) {
        logger.info("create message publisher, topic={}, messageClass={}, beanName={}", topic, messageClass.getTypeName(), name);
        kafka.validator.register(messageClass);
        MessagePublisher<T> publisher = new KafkaMessagePublisher<>(kafka.producer(), kafka.validator, topic, messageClass, context.logManager);
        context.beanFactory.bind(Types.generic(MessagePublisher.class, messageClass), name, publisher);
    }

    public <T> KafkaConfig subscribe(String topic, Class<T> messageClass, MessageHandler<T> handler) {
        kafka.validator.register(messageClass);
        kafka.listener().subscribe(topic, messageClass, handler, null);
        return this;
    }

    public <T> KafkaConfig subscribe(String topic, Class<T> messageClass, BulkMessageHandler<T> handler) {
        kafka.validator.register(messageClass);
        kafka.listener().subscribe(topic, messageClass, null, handler);
        return this;
    }

    public void poolSize(int poolSize) {
        kafka.listener().poolSize = poolSize;
    }

    public void uri(String uri) {
        if (!context.isTest()) {
            kafka.uri = uri;
        }
    }
}
