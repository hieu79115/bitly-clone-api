package com.thinh.shortener.repository;

import com.thinh.shortener.domain.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    List<ClickAnalytics> findByUrlIdOrderByClickedAtDesc(Long urlId);

    long countByUrlId(Long urlId);

    // Queries for a single URL
    @Query("SELECT CAST(c.clickedAt AS LocalDate) AS clickDate, COUNT(c) AS count " +
           "FROM ClickAnalytics c WHERE c.url.id = :urlId AND c.clickedAt >= :startDate " +
           "GROUP BY CAST(c.clickedAt AS LocalDate) ORDER BY CAST(c.clickedAt AS LocalDate) ASC")
    List<Map<String, Object>> countClicksByDateForUrl(@Param("urlId") Long urlId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT c.browser AS browser, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.id = :urlId GROUP BY c.browser")
    List<Map<String, Object>> countClicksByBrowser(@Param("urlId") Long urlId);

    @Query("SELECT c.operatingSystem AS os, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.id = :urlId GROUP BY c.operatingSystem")
    List<Map<String, Object>> countClicksByOs(@Param("urlId") Long urlId);

    @Query("SELECT c.deviceType AS device, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.id = :urlId GROUP BY c.deviceType")
    List<Map<String, Object>> countClicksByDevice(@Param("urlId") Long urlId);

    // Queries for whole User (Account-wide Overview)
    @Query("SELECT COUNT(c) FROM ClickAnalytics c WHERE c.url.user.id = :userId")
    long countTotalClicksByUserId(@Param("userId") Long userId);

    @Query("SELECT CAST(c.clickedAt AS LocalDate) AS clickDate, COUNT(c) AS count " +
           "FROM ClickAnalytics c WHERE c.url.user.id = :userId AND c.clickedAt >= :startDate " +
           "GROUP BY CAST(c.clickedAt AS LocalDate) ORDER BY CAST(c.clickedAt AS LocalDate) ASC")
    List<Map<String, Object>> countClicksByDateForUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT c.browser AS browser, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.user.id = :userId GROUP BY c.browser")
    List<Map<String, Object>> countClicksByBrowserForUser(@Param("userId") Long userId);

    @Query("SELECT c.operatingSystem AS os, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.user.id = :userId GROUP BY c.operatingSystem")
    List<Map<String, Object>> countClicksByOsForUser(@Param("userId") Long userId);

    @Query("SELECT c.deviceType AS device, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.user.id = :userId GROUP BY c.deviceType")
    List<Map<String, Object>> countClicksByDeviceForUser(@Param("userId") Long userId);
}
