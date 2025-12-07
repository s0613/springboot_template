package com.template.app.auth.infrastructure.repository;

import com.template.app.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // Sub-account methods
    List<User> findByMasterUser(User masterUser);

    /**
     * Find all sub-accounts for a master user
     */
    List<User> findByMasterUserId(Long masterUserId);

    Optional<User> findByMasterUserAndLoginCode(User masterUser, String loginCode);

    Optional<User> findByLoginCode(String loginCode);

    long countByMasterUser(User masterUser);

    boolean existsByLoginCode(String loginCode);

    /**
     * Find all users by user type
     */
    List<User> findByUserType(User.UserType userType);

    // Account deletion methods
    @Query("SELECT u FROM User u WHERE u.isActive = false AND u.deletedAt IS NOT NULL AND u.deletedAt < :expiryDate")
    List<User> findExpiredDeletions(@Param("expiryDate") LocalDateTime expiryDate);
}
