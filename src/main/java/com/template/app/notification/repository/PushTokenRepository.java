package com.template.app.notification.repository;

import com.template.app.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing push notification tokens.
 */
@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    /**
     * Find all enabled tokens for a specific user
     */
    List<PushToken> findByUserIdAndEnabledTrue(Long userId);

    /**
     * Find a token by device token string
     */
    Optional<PushToken> findByDeviceToken(String deviceToken);
}
