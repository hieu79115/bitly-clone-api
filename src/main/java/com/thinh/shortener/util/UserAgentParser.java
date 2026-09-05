package com.thinh.shortener.util;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import ua_parser.Client;
import ua_parser.Parser;

@Component
public class UserAgentParser {

    private Parser uaParser;

    @PostConstruct
    public void init() {
        this.uaParser = new Parser();
    }

    public String getBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        Client client = uaParser.parse(userAgent);
        return client.userAgent.family;
    }

    public String getOperatingSystem(String userAgent) {
        if (userAgent == null) return "Unknown";
        Client client = uaParser.parse(userAgent);
        return client.os.family;
    }

    public String getDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        Client client = uaParser.parse(userAgent);
        String device = client.device.family;

        if ("Spider".equalsIgnoreCase(device)) {
            return "Bot/Crawler";
        }

        String os = client.os.family.toLowerCase();
        if (os.contains("android") || os.contains("ios")) {
            return "Mobile";
        }
        return "Desktop";
    }
}
