package com.thinh.shortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String ALLOWED_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final char[] ALPHABET = ALLOWED_STRING.toCharArray();
    private static final int BASE = ALPHABET.length;

    public String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET[0]);
        }

        StringBuilder str = new StringBuilder();
        while (id > 0) {
            str.insert(0, ALPHABET[(int) (id % BASE)]);
            id = id / BASE;
        }

        return str.toString();
    }

    public long decode(String shortCode) {
        long id = 0;
        for (int i = 0; i < shortCode.length(); i++) {
            id = id * BASE + ALLOWED_STRING.indexOf(shortCode.charAt(i));
        }
        return id;
    }
}
