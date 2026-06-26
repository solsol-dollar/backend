package com.shinhan.eclipse.worker.ipo.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
public final class EodhdNewsUtil {

    private static final ZoneId ET = ZoneId.of("America/New_York");
    private static final DateTimeFormatter EODHD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private EodhdNewsUtil() {}

    public static String extractSource(String link) {
        if (link == null || link.isBlank()) return null;
        try {
            String host = new java.net.URI(link).getHost();
            if (host == null) return null;
            if (host.startsWith("www.")) host = host.substring(4);
            if (host.equals("finance.yahoo.com") || host.endsWith(".finance.yahoo.com")) return "Yahoo Finance";
            return switch (host) {
                case "nasdaq.com"        -> "Nasdaq";
                case "seekingalpha.com"  -> "Seeking Alpha";
                case "benzinga.com"      -> "Benzinga";
                case "globenewswire.com" -> "GlobeNewsWire";
                case "marketwatch.com"   -> "MarketWatch";
                case "reuters.com"       -> "Reuters";
                case "bloomberg.com"     -> "Bloomberg";
                case "cnbc.com"          -> "CNBC";
                case "wsj.com"           -> "Wall Street Journal";
                case "businesswire.com"  -> "Business Wire";
                case "prnewswire.com"    -> "PR Newswire";
                default                  -> host;
            };
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDateTime parseDate(String date) {
        if (date == null) return null;
        try {
            return OffsetDateTime.parse(date, EODHD_DATE_FORMAT).toLocalDateTime();
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(date).toLocalDateTime();
            } catch (Exception e2) {
                log.warn("날짜 파싱 실패: {}", date);
                return null;
            }
        }
    }

    public static LocalDate parseDateET(String date) {
        if (date == null) return null;
        try {
            return OffsetDateTime.parse(date, EODHD_DATE_FORMAT).atZoneSameInstant(ET).toLocalDate();
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(date).atZoneSameInstant(ET).toLocalDate();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
