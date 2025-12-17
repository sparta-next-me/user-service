package org.nextme.userservice.application.evnt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextme.userservice.infrastructure.kafka.MessageTpl;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPointEarnedMessage implements MessageTpl {

    private UUID userId;
    private Long amount;
    private UUID promotionId;
    private String promotionName;
    private LocalDateTime earnedAt;
}
