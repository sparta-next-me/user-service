package org.nextme.userservice.application.evnt;

import org.nextme.userservice.application.evnt.dto.UserPointEarnedMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    @KafkaListener(
            topics = "user.point.earned",          // 새 토픽 이름
            containerFactory = "kafkaListenerContainerFactory"
            // groupId 는 application.yml 에서 설정했으니 여기서는 생략해도 됨
    )
    public void listen(UserPointEarnedMessage message,
                       @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        System.out.printf(
                "수신 key=%s, userId=%s, amount=%d, promotionId=%s, promotionName=%s, earnedAt=%s%n",
                key,
                message.getUserId(),
                message.getAmount(),
                message.getPromotionId(),
                message.getPromotionName(),
                message.getEarnedAt()
        );

        // TODO: 여기서 포인트 적립 비즈니스 로직 호출하면 됨
        // pointService.earnPoint(...);
    }
}
