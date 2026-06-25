package com.hyuk.settlement.infrastructure.adserving;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "user_segment")
@IdClass(UserSegmentEntity.UserSegmentId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSegmentEntity {
    @Id
    private String userId;

    @Id
    private String segmentId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSegmentId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String userId;
        private String segmentId;
    }
}
