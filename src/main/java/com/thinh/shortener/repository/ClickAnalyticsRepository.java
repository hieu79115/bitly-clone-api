package com.thinh.shortener.repository;

import com.thinh.shortener.domain.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    List<ClickAnalytics> findByUrlIdOrderByClickedAtDesc(Long urlId);
    long countByUrlId (Long urlId);

    @Query("SELECT c.browser AS browser, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.id = :urlId GROUP BY c.browser")
    List<Map<String, Object>> countClicksByBrowser (@Param("urlId") Long urlId);

    @Query("SELECT c.operatingSystem AS os, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.id = :urlId GROUP BY c.operatingSystem")
    List<Map<String, Object>> countClicksByOs(@Param("urlId") Long urlId);

    @Query("SELECT c.deviceType AS device, COUNT(c) AS count FROM ClickAnalytics c WHERE c.url.id = :urlId GROUP BY c.deviceType")
    List<Map<String, Object>> countClicksByDevice(@Param("urlId") Long urlId);
}
