package com.sucrestore.api.service;

import com.sucrestore.api.dto.DashboardStatsResponse;
import com.sucrestore.api.dto.DashboardStatsResponse.DailyStat;
import com.sucrestore.api.dto.DashboardStatsResponse.RecentOrder;
import com.sucrestore.api.entity.Order;
import com.sucrestore.api.repository.OrderRepository;
import com.sucrestore.api.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    public void resetStatistics() {
        orderRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStatistics() {
        Long pendingOrders   = orderRepository.countByStatusAndNotDeleted(Order.Status.PENDING);
        Long confirmedOrders = orderRepository.countByStatusAndNotDeleted(Order.Status.CONFIRMED);
        Long shippedOrders   = orderRepository.countByStatusAndNotDeleted(Order.Status.SHIPPED);
        Long deliveredOrders = orderRepository.countByStatusAndNotDeleted(Order.Status.DELIVERED);
        Long cancelledOrders = orderRepository.countByStatusAndNotDeleted(Order.Status.CANCELLED);
        Long totalOrders     = pendingOrders + confirmedOrders + shippedOrders + deliveredOrders + cancelledOrders;
        Long totalProducts   = productRepository.count();
        BigDecimal totalRevenue = orderRepository.sumValidRevenue();

        // ── Statistiques journalières 7 jours ────────────────────────────────
        List<Object[]> rawDaily = orderRepository.findDailyStatsLast7Days();
        Map<LocalDate, Object[]> byDate = rawDaily.stream()
                .collect(Collectors.toMap(
                    row -> ((java.sql.Date) row[0]).toLocalDate(),
                    row -> row
                ));

        List<DailyStat> dailyStats = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            String label = day.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            label = label.substring(0, 1).toUpperCase() + label.substring(1, Math.min(3, label.length()));

            Object[] row = byDate.get(day);
            long ordersCount = row != null ? ((Number) row[1]).longValue() : 0L;
            BigDecimal rev   = row != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;

            dailyStats.add(DailyStat.builder()
                    .date(label)
                    .orders(ordersCount)
                    .revenue(rev)
                    .build());
        }

        // ── Commandes récentes ────────────────────────────────────────────────
        List<Order> recent = orderRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc();
        List<RecentOrder> recentOrders = recent.stream().map(o -> RecentOrder.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .customerName(o.getCustomerName())
                .total(o.getTotal())
                .status(o.getStatus().name())
                .createdAt(o.getCreatedAt() != null
                        ? o.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                        : "")
                .build()
        ).collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .totalRevenue(totalRevenue)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .dailyStats(dailyStats)
                .recentOrders(recentOrders)
                .build();
    }
}
