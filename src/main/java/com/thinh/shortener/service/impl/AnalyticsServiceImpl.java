package com.thinh.shortener.service.impl;

import com.thinh.shortener.domain.dto.response.AnalyticsSummaryResponseDto;
import com.thinh.shortener.domain.entity.ClickAnalytics;
import com.thinh.shortener.domain.entity.Url;
import com.thinh.shortener.domain.entity.User;
import com.thinh.shortener.exception.ResourceNotFoundException;
import com.thinh.shortener.repository.ClickAnalyticsRepository;
import com.thinh.shortener.repository.UrlRepository;
import com.thinh.shortener.repository.UserRepository;
import com.thinh.shortener.service.AnalyticsService;
import com.thinh.shortener.util.IpAddressUtil;
import com.thinh.shortener.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final IpAddressUtil ipAddressUtil;
    private final UserAgentParser userAgentParser;

    @Override
    @Async
    public void recordClick(String shortCode, HttpServletRequest request) {

        Url url = urlRepository.findByShortCode(shortCode).orElse(null);
        if (url == null) {
            return;
        }

        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        String ipAddress = ipAddressUtil.getClientIp(request);

        ClickAnalytics analytics = ClickAnalytics.builder()
                .url(url)
                .ipAddress(ipAddress)
                .browser(userAgentParser.getBrowser(userAgent))
                .operatingSystem(userAgentParser.getOperatingSystem(userAgent))
                .deviceType(userAgentParser.getDeviceType(userAgent))
                .referer(referer != null ? referer : "Direct")
                .build();

        clickAnalyticsRepository.save(analytics);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponseDto getAnalytics(String shortCode, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException(("Url not found")));

        if (!url.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view analytics for this URL");
        }

        return AnalyticsSummaryResponseDto.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(clickAnalyticsRepository.countByUrlId(url.getId()))
                .clicksByBrowser(clickAnalyticsRepository.countClicksByBrowser(url.getId()))
                .clicksByOs(clickAnalyticsRepository.countClicksByOs(url.getId()))
                .clicksByDevice(clickAnalyticsRepository.countClicksByDevice(url.getId()))
                .build();
    }
}
