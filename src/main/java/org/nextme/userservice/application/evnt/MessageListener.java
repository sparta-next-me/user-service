package org.nextme.userservice.application.evnt;

import org.nextme.userservice.application.evnt.dto.ChatMessage;
import org.nextme.userservice.infrastructure.kafka.MessageTpl;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    @KafkaListener(
            topics = "chat.message",                 // 실제 사용하는 토픽 이름으로 맞추기
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(MessageTpl message,
                       @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        ChatMessage chatMessage = (ChatMessage) message;
        System.out.printf("수신 key=%s, userId=%s%n", key, chatMessage.getUserId());
    }
}
