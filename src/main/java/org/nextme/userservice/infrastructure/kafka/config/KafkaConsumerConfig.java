package org.nextme.userservice.infrastructure.kafka.config;

import org.nextme.userservice.infrastructure.kafka.MessageTpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MessageTpl>
    kafkaListenerContainerFactory(ConsumerFactory<String, MessageTpl> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, MessageTpl> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
