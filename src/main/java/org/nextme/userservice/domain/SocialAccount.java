package org.nextme.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@ToString
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "email", length = 255)
    private String email;

    @Builder
    private SocialAccount(SocialProvider provider, String providerUserId, String email) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
    }

    public static SocialAccount of(SocialProvider provider, String providerUserId, String email) {
        return SocialAccount.builder()
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .build();
    }

    /**
     * equals() / hashCode() 목적:
     *
     * SocialAccount는 @Embeddable 값 객체(Value Object)
     * 값 객체는 '값 자체가 같으면 같은 객체'로 판단해야 하므로
     * 동등성 비교(equality)를 명시적으로 정의해야 함
     *
     * 특히 @ElementCollection(Set)에서 값 객체를 관리할 때
     * JPA는 equals/hashCode를 이용하여:
     *   - 중복 여부 판단
     *   - 삭제/추가 변경 감지(dirty checking)
     *   - 컬렉션 동작(Set 중복 제거)
     *
     * 만약 equals/hashCode를 구현하지 않으면
     * 같은 소셜 계정(provider + providerUserId가 동일)이라도
     * 서로 다른 객체로 인식되어 중복 저장/변경 감지가 불가능해짐
     */

    @Override
    public boolean equals(Object o) {

        // 1) 같은 객체(주소까지 동일)라면 true 바로 반환 (빠른 비교)
        if (this == o) return true;

        // 2) 비교 대상이 SocialAccount 타입이 아니면 false
        //    instanceof + 패턴 매칭으로 안전하게 타입 캐스팅
        if (!(o instanceof SocialAccount that)) return false;

        // 3) provider(Enum)가 다르면 다른 소셜 계정으로 간주
        if (provider != that.provider) return false;

        // 4) providerUserId(소셜에서 받은 유니크한 식별자)가 동일한지 비교
        //    null-safe하게 equals 비교 수행
        return providerUserId != null ? providerUserId.equals(that.providerUserId) : that.providerUserId == null;
    }

    @Override
    public int hashCode() {

        // 1) provider(Enum)의 hashCode를 기본값으로 사용
        //    Enum은 고유 hashCode를 가지므로 안정적
        int result = provider != null ? provider.hashCode() : 0;

        // 2) providerUserId의 hashCode를 조합하여
        //    동일한 값 객체는 동일한 hashCode를 갖도록 보장
        result = 31 * result + (providerUserId != null ? providerUserId.hashCode() : 0);

        return result;
    }

}
