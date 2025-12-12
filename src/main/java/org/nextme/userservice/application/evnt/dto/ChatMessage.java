package org.nextme.userservice.application.evnt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextme.userservice.infrastructure.kafka.MessageTpl;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements MessageTpl {
    private UUID userId;
}
