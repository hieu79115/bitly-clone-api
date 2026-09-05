package com.thinh.shortener.util;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    public String getBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome") && !ua.contains("edg")) return "Chrome";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("opera") || ua.contains("opr/")) return "Opera";
        return "Other";
    }

    public String getOperatingSystem(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) return "iOS";
        if (ua.contains("mac os") || ua.contains("macintosh")) return "MacOS";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }

    public String getDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "Tablet";
        }
        return "Desktop";
    }
}
