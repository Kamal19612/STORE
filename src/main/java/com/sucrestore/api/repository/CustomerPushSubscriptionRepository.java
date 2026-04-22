package com.sucrestore.api.repository;

import com.sucrestore.api.entity.CustomerPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPushSubscriptionRepository extends JpaRepository<CustomerPushSubscription, Long> {
    Optional<CustomerPushSubscription> findByEndpoint(String endpoint);
    List<CustomerPushSubscription> findByOrderNumber(String orderNumber);
}