package org.nextme.userservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 내 정보 수정 요청 DTO
 * - name, slackId 변경
 */
public record UpdateMyInfoRequest (
        @NotBlank(message= "이름은 필수 입니다.")
        @Size(max = 50, message = "이름은 최대 50자까지 가능합니다.")
        String name,

        @Size(max = 100, message = "슬랙 ID는 최대 100자까지 가능합니다.")
        String slackId
){
}
