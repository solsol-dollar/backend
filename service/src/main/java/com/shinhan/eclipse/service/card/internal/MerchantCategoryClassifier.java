package com.shinhan.eclipse.service.card.internal;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
class MerchantCategoryClassifier {

    // 순서 보장을 위해 List<Map.Entry> 사용 (긴 키워드 먼저 검사)
    private static final List<Map.Entry<String, String>> MERCHANT_CATEGORY = List.of(
            // ── 구독 — 생산성/업무/AI 도구 ──────────────────────────────────
            Map.entry("Anthropic",          "구독"),   // Claude Pro
            Map.entry("OpenAI",             "구독"),   // ChatGPT Plus
            Map.entry("GitHub",             "구독"),   // Copilot
            Map.entry("Adobe",              "구독"),   // Creative Cloud
            Map.entry("Microsoft 365",      "구독"),
            Map.entry("Notion",             "구독"),
            Map.entry("Figma",              "구독"),
            Map.entry("Canva",              "구독"),
            Map.entry("Dropbox",            "구독"),
            Map.entry("iCloud",             "구독"),
            Map.entry("Google One",         "구독"),
            Map.entry("Slack",              "구독"),
            Map.entry("Zoom",               "구독"),
            Map.entry("1Password",          "구독"),
            Map.entry("NordVPN",            "구독"),
            Map.entry("ExpressVPN",         "구독"),
            Map.entry("Duolingo",           "구독"),
            Map.entry("Coursera",           "구독"),
            Map.entry("Udemy",              "구독"),
            Map.entry("LinkedIn Learning",  "구독"),
            Map.entry("Skillshare",         "구독"),
            Map.entry("MasterClass",        "구독"),
            Map.entry("Khan Academy",       "구독"),

            // ── 콘텐츠 — 스트리밍/게임/엔터테인먼트 ─────────────────────────
            Map.entry("Netflix",            "콘텐츠"),
            Map.entry("Spotify",            "콘텐츠"),
            Map.entry("Apple Music",        "콘텐츠"),
            Map.entry("YouTube Premium",    "콘텐츠"),
            Map.entry("Disney+",            "콘텐츠"),
            Map.entry("HBO Max",            "콘텐츠"),
            Map.entry("Hulu",               "콘텐츠"),
            Map.entry("Amazon Prime",       "콘텐츠"),
            Map.entry("Steam",              "콘텐츠"),
            Map.entry("Epic Games",         "콘텐츠"),
            Map.entry("PlayStation",        "콘텐츠"),
            Map.entry("Xbox",               "콘텐츠"),
            Map.entry("Nintendo",           "콘텐츠"),
            Map.entry("Twitch",             "콘텐츠"),

            // ── 쇼핑 — 온라인 쇼핑몰 ────────────────────────────────────────
            Map.entry("Amazon",             "쇼핑"),
            Map.entry("AliExpress",         "쇼핑"),
            Map.entry("eBay",               "쇼핑"),
            Map.entry("Etsy",               "쇼핑"),
            Map.entry("Shein",              "쇼핑"),
            Map.entry("Temu",               "쇼핑"),
            Map.entry("Shopify",            "쇼핑"),
            Map.entry("ASOS",               "쇼핑"),
            Map.entry("Zara",               "쇼핑"),
            Map.entry("H&M",                "쇼핑"),
            Map.entry("Nike",               "쇼핑"),
            Map.entry("Adidas",             "쇼핑"),
            Map.entry("Apple Store",        "쇼핑"),
            Map.entry("Best Buy",           "쇼핑"),

            // ── 여행 — 현지 식비/교통/숙박 ──────────────────────────────────
            Map.entry("Starbucks",          "여행"),
            Map.entry("Dunkin",             "여행"),
            Map.entry("Dutch Bros",         "여행"),
            Map.entry("Peet's Coffee",      "여행"),
            Map.entry("Tim Hortons",        "여행"),
            Map.entry("Panera Bread",       "여행"),
            Map.entry("McDonald",           "여행"),
            Map.entry("Burger King",        "여행"),
            Map.entry("Subway",             "여행"),
            Map.entry("KFC",                "여행"),
            Map.entry("Wendy",              "여행"),
            Map.entry("Taco Bell",          "여행"),
            Map.entry("Chipotle",           "여행"),
            Map.entry("Shake Shack",        "여행"),
            Map.entry("Five Guys",          "여행"),
            Map.entry("Chick-fil-A",        "여행"),
            Map.entry("Domino",             "여행"),
            Map.entry("Pizza Hut",          "여행"),
            Map.entry("Popeyes",            "여행"),
            Map.entry("In-N-Out",           "여행"),
            Map.entry("Cheesecake Factory", "여행"),
            Map.entry("Olive Garden",       "여행"),
            Map.entry("IHOP",               "여행"),
            Map.entry("Walmart",            "여행"),
            Map.entry("Target",             "여행"),
            Map.entry("Costco",             "여행"),
            Map.entry("Whole Foods",        "여행"),
            Map.entry("Trader Joe",         "여행"),
            Map.entry("Kroger",             "여행"),
            Map.entry("Safeway",            "여행"),
            Map.entry("CVS",                "여행"),
            Map.entry("Walgreens",          "여행"),
            Map.entry("DoorDash",           "여행"),
            Map.entry("Uber Eats",          "여행"),
            Map.entry("Grubhub",            "여행"),
            Map.entry("Instacart",          "여행"),
            Map.entry("Uber",               "여행"),
            Map.entry("Lyft",               "여행"),
            Map.entry("Airbnb",             "여행"),
            Map.entry("Booking.com",        "여행"),
            Map.entry("Expedia",            "여행"),
            Map.entry("Agoda",              "여행"),
            Map.entry("Hotels.com",         "여행")
    );

    String classify(String merchantName) {
        return MERCHANT_CATEGORY.stream()
                .filter(e -> merchantName.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("기타");
    }
}