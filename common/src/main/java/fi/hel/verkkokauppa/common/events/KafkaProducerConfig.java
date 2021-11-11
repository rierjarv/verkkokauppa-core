package fi.hel.verkkokauppa.common.events;

import fi.hel.verkkokauppa.common.events.message.EventMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    // add to your application.properties spring.kafka.bootstrap-servers=localhost:9092
    @Value("${spring.kafka.bootstrap-servers:#{null}}")
    private String bootstrapServers;

    @Value("${kafka.client.authentication.enabled:#{false}}")
    private Boolean kafkaClientAuthenticationEnabled;

    @Value("${kafka.user:#{null}}")
    private String kafkaUser;

    @Value("${kafka.password:#{null}}")
    private String kafkaPassword;

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        if (kafkaClientAuthenticationEnabled) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put(SaslConfigs.SASL_ENABLED_MECHANISMS, "SCRAM-SHA-512");
            props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
            props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + kafkaUser + "\" password=\"" + kafkaPassword + "\";");
        }

        return props;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, EventMessage> eventMessageProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, EventMessage> eventMessageKafkaTemplate() {
        return new KafkaTemplate<>(eventMessageProducerFactory());
    }

}