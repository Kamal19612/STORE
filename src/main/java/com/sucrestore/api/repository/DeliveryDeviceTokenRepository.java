package com.sucrestore.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sucrestore.api.entity.DeliveryDeviceToken;

public interface DeliveryDeviceTokenRepository extends JpaRepository<DeliveryDeviceToken, Long> {

    Optional<DeliveryDeviceToken> findByFcmToken(String fcmToken);

    List<DeliveryDeviceToken> findByUserUsernameAndIsActiveTrue(String username);

    List<DeliveryDeviceToken> findByUserRoleAndIsActiveTrue(com.sucrestore.api.entity.User.Role role);
}

