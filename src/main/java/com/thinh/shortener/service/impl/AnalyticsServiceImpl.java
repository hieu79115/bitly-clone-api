package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.response.AnalyticsSummaryResponseDto;
import com.thinh.shortener.domain.dto.response.TopUrlResponseDto;
import com.thinh.shortener.domain.entity.ClickAnalytics;
import com.thinh.shortener.domain.entity.Url;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.exception.ResourceNotFoundException;
import com.thinh.shortener.repository.ClickAnalyticsRepository;
import com.thinh.shortener.repository.UrlRepository;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.service.AnalyticsService;
import com.thinh.shortener.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final UserAgentParser userAgentParser;

    @Value("${app.domain:http://localhost:8080/}")
    private String domain;

    @Override
    @Async
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        Url url = urlRepository.findByShortCode(shortCode).orElse(null);
        if (url == null) {
            log.warn("Skipping click recording: Short code '{}' not found", shortCode);
            return;
        }

        ClickAnalytics analytics = ClickAnalytics.builder()
                .url(url)
                .ipAddress(ipAddress)
                .browser(userAgentParser.getBrowser(userAgent))
                .operatingSystem(userAgentParser.getOperatingSystem(userAgent))
                .deviceType(userAgentParser.getDeviceType(userAgent))
                .referer(referer != null ? referer : "Direct")
                .build();

        clickAnalyticsRepository.save(analytics);

        url.setClickCount(url.getClickCount() + 1);
        urlRepository.save(url);

        log.debug("Recorded click event for shortCode={}, browser={}, os={}, device={}", 
                shortCode, analytics.getBrowser(), analytics.getOperatingSystem(), analytics.getDeviceType());
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponseDto getAnalytics(String shortCode, Integer days, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Url not found"));

        if (!url.getUser().getId().equals(user.getId())) {
            log.warn("Access denied: User '{}' tried to view analytics for shortCode '{}' belonging to another user", email, shortCode);
            throw new AccessDeniedException("You do not have permission to view analytics for this URL");
        }

        LocalDateTime startDate = calculateStartDate(days);
        LocalDateTime endDate = LocalDateTime.now();

        List<Map<String, Object>> dbDateResults = clickAnalyticsRepository.countClicksByDateForUrl(url.getId(), startDate);
        List<Map<String, Object>> filledClicksByDate = fillMissingDates(dbDateResults, startDate, endDate);

        AnalyticsSummaryResponseDto summary = AnalyticsSummaryResponseDto.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .title(url.getTitle())
                .totalClicks(clickAnalyticsRepository.countByUrlId(url.getId()))
                .clicksByDate(filledClicksByDate)
                .clicksByBrowser(clickAnalyticsRepository.countClicksByBrowser(url.getId()))
                .clicksByOs(clickAnalyticsRepository.countClicksByOs(url.getId()))
                .clicksByDevice(clickAnalyticsRepository.countClicksByDevice(url.getId()))
                .build();

        log.info("Retrieved analytics summary for shortCode={}, days={}, user={}", shortCode, days, email);
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponseDto getOverviewAnalytics(Integer days, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime startDate = calculateStartDate(days);
        LocalDateTime endDate = LocalDateTime.now();

        List<Map<String, Object>> dbDateResults = clickAnalyticsRepository.countClicksByDateForUser(user.getId(), startDate);
        List<Map<String, Object>> filledClicksByDate = fillMissingDates(dbDateResults, startDate, endDate);

        List<TopUrlResponseDto> topUrls = urlRepository.findTop5ByUserIdOrderByClickCountDesc(user.getId())
                .stream()
                .map(u -> TopUrlResponseDto.builder()
                        .id(u.getId())
                        .title(u.getTitle())
                        .shortCode(u.getShortCode())
                        .shortUrl(domain + u.getShortCode())
                        .originalUrl(u.getOriginalUrl())
                        .clickCount(u.getClickCount())
                        .build())
                .toList();

        AnalyticsSummaryResponseDto summary = AnalyticsSummaryResponseDto.builder()
                .shortCode(null)
                .originalUrl(null)
                .title("All Links Overview")
                .totalClicks(clickAnalyticsRepository.countTotalClicksByUserId(user.getId()))
                .clicksByDate(filledClicksByDate)
                .clicksByBrowser(clickAnalyticsRepository.countClicksByBrowserForUser(user.getId()))
                .clicksByOs(clickAnalyticsRepository.countClicksByOsForUser(user.getId()))
                .clicksByDevice(clickAnalyticsRepository.countClicksByDeviceForUser(user.getId()))
                .topUrls(topUrls)
                .build();

        log.info("Retrieved account-wide overview analytics for user={}, days={}", email, days);
        return summary;
    }

    private LocalDateTime calculateStartDate(Integer days) {
        if (days == null || days <= 0) {
            return LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        return LocalDateTime.now().minusDays(days - 1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    private List<Map<String, Object>> fillMissingDates(List<Map<String, Object>> dbResults, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> dateCountMap = new HashMap<>();
        if (dbResults != null) {
            for (Map<String, Object> row : dbResults) {
                Object dateObj = row.get("clickDate");
                Object countObj = row.get("count");
                if (dateObj != null && countObj != null) {
                    dateCountMap.put(dateObj.toString(), ((Number) countObj).longValue());
                }
            }
        }

        List<Map<String, Object>> filled = new ArrayList<>();
        LocalDate start = (startDate != null) ? startDate.toLocalDate() : LocalDate.now().minusDays(6);
        LocalDate end = (endDate != null) ? endDate.toLocalDate() : LocalDate.now();

        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            String dateStr = curr.toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);
            item.put("count", dateCountMap.getOrDefault(dateStr, 0L));
            filled.add(item);
            curr = curr.plusDays(1);
        }
        return filled;
    }
}
