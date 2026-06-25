package com.hyuk.settlement.infrastructure.adserving;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSegmentJpaRepository extends JpaRepository<UserSegmentEntity, UserSegmentEntity.UserSegmentId> {
}
