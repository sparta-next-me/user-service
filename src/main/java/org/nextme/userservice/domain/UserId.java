package org.nextme.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserId {
    @Column(length = 45, name="user_id")
    private UUID id;

    public UserId(UUID id){
        this.id = id;
    }

    public static UserId of(UUID id){
        return new UserId(id);
    }

    // 새 유저 생성 시 사용할 랜덤 ID 팩토리
    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
