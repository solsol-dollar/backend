package com.shinhan.eclipse.service.securities.internal;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * IPO의 세부 업종(예: "Biotechnology")을 9개 대분류 섹터로 묶고, 섹터별 추천 ETF 티커를 제공한다.
 * 매핑이 없는 업종은 임의 기본값으로 떨어지지 않고 {@link Optional#empty()}를 반환한다.
 * 호출 측(SecuritiesServiceImpl)은 이 경우 빈 추천 리스트를 반환하며, 투자성향 기반 AI 추천으로는
 * 폴백하지 않는다(별도 폐기 예정 기능이라 의도적으로 분리됨).
 */
@Component
class SectorMapper {

    static final String IT_TECH            = "IT_TECH";
    static final String HEALTHCARE_BIO     = "HEALTHCARE_BIO";
    static final String INDUSTRIAL_DEFENSE = "INDUSTRIAL_DEFENSE";
    static final String FINANCE_FINTECH    = "FINANCE_FINTECH";
    static final String ENERGY_UTILITY     = "ENERGY_UTILITY";
    static final String MATERIALS_MINING   = "MATERIALS_MINING";
    static final String CONSUMER_RETAIL    = "CONSUMER_RETAIL";
    static final String REAL_ESTATE_INFRA  = "REAL_ESTATE_INFRA";
    static final String LOGISTICS_MOBILITY = "LOGISTICS_MOBILITY";

    /** 업종 분류가 모호하거나 비어 있는 IPO에 대한 티커 단위 예외 매핑. */
    private static final Map<String, String> TICKER_OVERRIDE = Map.of(
            "BSP",  IT_TECH,
            "CUX",  MATERIALS_MINING,
            "SIND", LOGISTICS_MOBILITY,
            "ITG",  IT_TECH
    );

    /** 세부 업종(IPO.sector) → 9개 대분류 섹터. */
    private static final Map<String, String> INDUSTRY_TO_SECTOR = Map.ofEntries(
            // 1. IT / 테크
            Map.entry("Artificial Intelligence",           IT_TECH),
            Map.entry("Quantum Computing",                  IT_TECH),
            Map.entry("Robotics & Automation",               IT_TECH),
            Map.entry("Software - Infrastructure",           IT_TECH),
            Map.entry("Software - Application",              IT_TECH),
            Map.entry("Semiconductors",                      IT_TECH),
            Map.entry("Computer Hardware",                   IT_TECH),
            Map.entry("Advertising Agencies",                IT_TECH),

            // 2. 헬스케어 / 바이오
            Map.entry("Biotechnology",                       HEALTHCARE_BIO),
            Map.entry("Medical - Care Facilities",           HEALTHCARE_BIO),
            Map.entry("Medical - Pharmaceuticals",            HEALTHCARE_BIO),

            // 3. 산업재 / 방산
            Map.entry("Aerospace & Defense",                 INDUSTRIAL_DEFENSE),
            Map.entry("Industrial - Machinery",               INDUSTRIAL_DEFENSE),
            Map.entry("Manufacturing - Metal Fabrication",     INDUSTRIAL_DEFENSE),
            Map.entry("Oil & Gas Equipment & Services",        INDUSTRIAL_DEFENSE),

            // 4. 금융 / 핀테크
            Map.entry("Asset Management",                    FINANCE_FINTECH),
            Map.entry("Insurance - Life",                     FINANCE_FINTECH),
            Map.entry("Banks - Regional",                     FINANCE_FINTECH),

            // 5. 에너지 / 유틸리티
            Map.entry("Clean Energy",                         ENERGY_UTILITY),
            Map.entry("Solar Technology",                     ENERGY_UTILITY),
            Map.entry("Regulated Electric",                    ENERGY_UTILITY),
            Map.entry("General Utilities",                    ENERGY_UTILITY),
            Map.entry("Oil & Gas Refining & Marketing",        ENERGY_UTILITY),

            // 6. 소재 / 광업
            Map.entry("Other Precious Metals",                MATERIALS_MINING),

            // 7. 필수소비재 / 경기소비재
            Map.entry("Specialty Retail",                     CONSUMER_RETAIL),
            Map.entry("Grocery Stores",                       CONSUMER_RETAIL),
            Map.entry("Packaged Foods",                       CONSUMER_RETAIL),
            Map.entry("Beverages - Non-Alcoholic",            CONSUMER_RETAIL),

            // 8. 부동산 / 인프라
            Map.entry("REIT - Specialty",                     REAL_ESTATE_INFRA),
            Map.entry("REIT - Healthcare Facilities",          REAL_ESTATE_INFRA),
            Map.entry("REIT - Industrial",                    REAL_ESTATE_INFRA),

            // 9. 물류 / 모빌리티
            Map.entry("Transportation",                       LOGISTICS_MOBILITY),
            Map.entry("Shipping",                             LOGISTICS_MOBILITY)
    );

    private static final Map<String, List<String>> SECTOR_TO_ETF = Map.of(
            IT_TECH,            List.of("IGV", "WCLD", "AIQ"),
            HEALTHCARE_BIO,     List.of("XBI", "IBB", "ARKG"),
            INDUSTRIAL_DEFENSE, List.of("ITA", "SHLD", "ARKX"),
            FINANCE_FINTECH,    List.of("FINX", "IPAY", "BLOK"),
            ENERGY_UTILITY,     List.of("ICLN", "CNRG", "AMLP"),
            MATERIALS_MINING,   List.of("XME", "COPX", "REMX"),
            CONSUMER_RETAIL,    List.of("PBJ", "XRT", "FDIS"),
            REAL_ESTATE_INFRA,  List.of("SRVR", "IFRA", "PAVE"),
            LOGISTICS_MOBILITY, List.of("IYT", "XTN", "DRIV")
    );

    /**
     * @param ticker IPO 티커 (업종 분류가 모호한 종목에 대한 예외 매핑 우선순위용)
     * @param sector IPO의 세부 업종 (Ipo.sector)
     * @return 9개 대분류 섹터 중 하나, 매핑할 수 없으면 empty
     */
    Optional<String> resolveSectorGroup(String ticker, String sector) {
        if (ticker != null && TICKER_OVERRIDE.containsKey(ticker)) {
            return Optional.of(TICKER_OVERRIDE.get(ticker));
        }
        if (sector == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(INDUSTRY_TO_SECTOR.get(sector));
    }

    List<String> getEtfTickers(String sectorGroup) {
        return SECTOR_TO_ETF.getOrDefault(sectorGroup, List.of());
    }
}
