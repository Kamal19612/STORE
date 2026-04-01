package com.sucrestore.api.repository;

import com.sucrestore.api.entity.PushSubscription;
import com.sucrestore.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserRole(User.Role role);
}