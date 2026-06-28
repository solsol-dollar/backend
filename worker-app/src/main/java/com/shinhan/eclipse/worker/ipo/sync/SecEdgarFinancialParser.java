package com.shinhan.eclipse.worker.ipo.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SecEdgarFinancialParser {

    private static final String SUBMISSIONS_URL = "https://data.sec.gov/submissions/CIK%010d.json";
    private static final String ARCHIVE_BASE = "https://www.sec.gov/Archives/edgar/data/";
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(20[2-9][0-9])\\b");

    public static final Map<String, Long> CIK_MAP = Map.ofEntries(
            Map.entry("ITG",  2110117L),
            Map.entry("LIME", 1699963L),
            Map.entry("BSP",  2004711L),
            Map.entry("FCBM", 1531193L),
            Map.entry("SPCX", 1181412L),
            Map.entry("LFTO", 1850351L),
            Map.entry("QNT",  2110105L),
            Map.entry("BXDC", 2100161L),
            Map.entry("CBRS", 2021728L),
            Map.entry("FRVO", 1853868L),
            Map.entry("HAWK", 1750704L),
            Map.entry("SUJA", 1934114L),
            Map.entry("REA",  2095743L),
            Map.entry("SBMT", 2067674L),
            Map.entry("PS",   2026053L),
            Map.entry("XE",   2088896L),
            Map.entry("YSWY", 1859836L),
            Map.entry("NHP",  1561032L),
            Map.entry("KLRA", 2096997L),
            Map.entry("MAIR", 2098430L),
            Map.entry("JAN",  2100805L),
            Map.entry("PAYP", 2080845L),
            Map.entry("MMED", 2062583L),
            Map.entry("APC",  2080921L),
            Map.entry("AGBK", 2081206L),
            Map.entry("OFRM", 1696556L),
            Map.entry("EIKN", 1861123L),
            Map.entry("LIFE", 1788451L),
            Map.entry("PICS", 1841644L)
    );

    private static final Map<String, String> CURRENCY_MAP = Map.of(
            "AGBK", "BRL",
            "PICS", "BRL",
            "PAYP", "JPY"
    );

    private final RestClient secClient;
    private final ObjectMapper objectMapper;

    public SecEdgarFinancialParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.secClient = RestClient.builder()
                .defaultHeader("User-Agent", "SolsolDollar contact@solsoldollar.com")
                .build();
    }

    public String getCurrency(String ticker) {
        return CURRENCY_MAP.getOrDefault(ticker, "USD");
    }

    public record AnnualFinancial(int year, Long revenue, Long operatingIncome, Long netIncome) {}

    // ── public entry point ──────────────────────────────────────────────────

    public List<AnnualFinancial> fetch(String ticker) {
        Long cik = CIK_MAP.get(ticker);
        if (cik == null) {
            log.warn("{}: CIK 없음", ticker);
            return List.of();
        }
        try {
            String docUrl = resolveFilingUrl(ticker, cik);
            if (docUrl == null) {
                log.warn("{}: 공시 문서 URL 못 찾음", ticker);
                return List.of();
            }
            log.info("{}: 문서 다운로드 중 - {}", ticker, docUrl);
            String html = secClient.get().uri(docUrl).retrieve().body(String.class);
            if (html == null || html.isBlank()) return List.of();

            List<AnnualFinancial> result = parseIncomeStatement(html);
            log.info("{}: {}건 파싱 완료", ticker, result.size());
            return result;
        } catch (Exception e) {
            log.error("{}: 파싱 실패 - {}", ticker, e.getMessage());
            return List.of();
        }
    }

    // ── SEC EDGAR 공시 URL 조회 ─────────────────────────────────────────────

    private String resolveFilingUrl(String ticker, long cik) throws Exception {
        String url = String.format(SUBMISSIONS_URL, cik);
        String json = secClient.get().uri(url).retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(json);
        JsonNode recent = root.path("filings").path("recent");

        JsonNode forms      = recent.path("form");
        JsonNode accessions = recent.path("accessionNumber");
        JsonNode docs       = recent.path("primaryDocument");

        List<String> targets = getFormVariants(ticker);

        for (String target : targets) {
            for (int i = 0; i < forms.size(); i++) {
                if (target.equals(forms.get(i).asText())) {
                    String accession = accessions.get(i).asText().replace("-", "");
                    String doc       = docs.get(i).asText();
                    return ARCHIVE_BASE + cik + "/" + accession + "/" + doc;
                }
            }
        }
        return null;
    }

    private List<String> getFormVariants(String ticker) {
        return switch (ticker) {
            case "AGBK", "PICS" -> List.of("20-F");
            case "BSP"           -> List.of("F-1/A", "F-1");
            case "ITG", "LIME"   -> List.of("S-1/A", "S-1");
            default              -> List.of("424B4");
        };
    }

    // ── HTML 파싱 ──────────────────────────────────────────────────────────

    private List<AnnualFinancial> parseIncomeStatement(String html) {
        Document doc = Jsoup.parse(html);

        String[] sectionKeywords = {
                "STATEMENTS OF OPERATIONS",
                "STATEMENT OF OPERATIONS",
                "STATEMENTS OF INCOME AND COMPREHENSIVE",
                "STATEMENTS OF INCOME",
                "STATEMENT OF INCOME",
                "STATEMENTS OF COMPREHENSIVE INCOME",
                "STATEMENTS OF COMPREHENSIVE LOSS",
                "STATEMENTS OF PROFIT OR LOSS",
                "STATEMENT OF PROFIT OR LOSS",
                "COMBINED STATEMENTS OF OPERATIONS",
                "COMBINED AND CONSOLIDATED STATEMENTS OF OPERATIONS",
                "STATEMENTS OF COMPREHENSIVE"
        };

        // net income 없이 부분적으로만 파싱된 결과 (fallback용)
        List<AnnualFinancial> partialResult = List.of();

        for (String keyword : sectionKeywords) {
            Elements candidates = doc.getElementsContainingOwnText(keyword);
            for (Element el : candidates) {
                Element parentTable = el.closest("table");
                Element table = (parentTable != null) ? parentTable : findNextTable(el);
                if (table == null) continue;

                List<AnnualFinancial> result = parseTable(table);
                if (result.isEmpty()) continue;

                boolean hasNet = result.stream().anyMatch(f -> f.netIncome() != null);
                if (hasNet) return result; // 완전한 결과는 즉시 반환
                if (partialResult.isEmpty()) partialResult = result; // 첫 부분 결과 저장 후 계속 탐색
            }
        }

        return partialResult;
    }

    private Element findNextTable(Element el) {
        Element current = el;
        for (int depth = 0; depth < 6; depth++) {
            Element sib = current.nextElementSibling();
            while (sib != null) {
                if ("table".equals(sib.tagName())) return sib;
                Element inner = sib.selectFirst("table");
                if (inner != null) return inner;
                sib = sib.nextElementSibling();
            }
            if (current.parent() == null) break;
            current = current.parent();
        }
        return null;
    }

    private List<AnnualFinancial> parseTable(Element table) {
        List<Element> rows = table.select("tr");

        // 연도 컬럼 위치 파악
        int[] yearCols = null;
        int[] years    = null;
        int headerIdx  = -1;

        // 연도가 두 행에 걸쳐 있는 경우(PS 등)를 처리하기 위해 첫 10행 중 가장 많은 연도를 가진 행을 선택
        int bestYearCount = 0;
        List<Integer> bestFoundYears = null;
        List<Integer> bestFoundCols  = null;

        for (int i = 0; i < Math.min(rows.size(), 10); i++) {
            List<Element> cells = rows.get(i).select("th, td");
            List<Integer> foundYears = new ArrayList<>();
            List<Integer> foundCols  = new ArrayList<>();

            for (int j = 0; j < cells.size(); j++) {
                String text = cells.get(j).text().trim();
                Matcher m = YEAR_PATTERN.matcher(text);
                Integer lastYear = null;
                while (m.find()) lastYear = Integer.parseInt(m.group(1));
                if (lastYear != null && !foundYears.contains(lastYear)) {
                    foundYears.add(lastYear);
                    foundCols.add(j);
                }
            }

            if (foundYears.size() > bestYearCount) {
                bestYearCount  = foundYears.size();
                bestFoundYears = foundYears;
                bestFoundCols  = foundCols;
                headerIdx      = i;
            }
            if (bestYearCount >= 3) break;
        }

        if (bestFoundYears != null) {
            years    = bestFoundYears.stream().mapToInt(Integer::intValue).toArray();
            yearCols = bestFoundCols.stream().mapToInt(Integer::intValue).toArray();
        }

        if (years == null || years.length == 0) return List.of();

        // FRVO 같이 연도 헤더 행에 라벨 컬럼이 없는 경우 보정
        // yearCols[0]==0이고 데이터 행의 첫 셀이 텍스트(라벨)이면 컬럼 인덱스를 +1 시프트
        if (yearCols[0] == 0) {
            for (int i = headerIdx + 1; i < Math.min(rows.size(), headerIdx + 8); i++) {
                List<Element> dataCells = rows.get(i).select("td");
                if (dataCells.isEmpty()) continue;
                String firstCell = dataCells.get(0).text().trim().toLowerCase();
                if (!firstCell.isEmpty() && !firstCell.equals("-") && parseAmount(firstCell) == null) {
                    for (int k = 0; k < yearCols.length; k++) yearCols[k]++;
                }
                break;
            }
        }

        Map<Integer, Long> revenues         = new LinkedHashMap<>();
        Map<Integer, Long> operatingIncomes = new LinkedHashMap<>();
        Map<Integer, Long> netIncomes       = new LinkedHashMap<>();

        for (int i = headerIdx + 1; i < rows.size(); i++) {
            List<Element> cells = rows.get(i).select("td");
            if (cells.isEmpty()) continue;
            String rawLabel = cells.get(0).text().trim().toLowerCase();
            // col0이 숫자/영문자 없는 순수 공백/기호 셀이면 col1을 라벨로 사용 (AGBK, BSP 등)
            if (!rawLabel.matches(".*[a-z0-9].*") && cells.size() > 1) {
                rawLabel = cells.get(1).text().trim().toLowerCase();
            }
            // 콜론, 점, 공백 등 후행 문자 제거 ("Revenues:" → "revenues")
            String label = rawLabel.replaceAll("[:\\s.]+$", "");

            if (revenues.isEmpty() && matchesRevenue(label)) {
                extractValues(cells, yearCols, years, revenues);
            } else if (operatingIncomes.isEmpty() && matchesOperatingIncome(label)) {
                extractValues(cells, yearCols, years, operatingIncomes);
            } else if (netIncomes.isEmpty() && matchesNetIncome(label)) {
                extractValues(cells, yearCols, years, netIncomes);
            }

            if (!revenues.isEmpty() && !operatingIncomes.isEmpty() && !netIncomes.isEmpty()) break;
        }

        List<AnnualFinancial> result = new ArrayList<>();
        for (int year : years) {
            Long rev  = revenues.get(year);
            Long opIn = operatingIncomes.get(year);
            Long net  = netIncomes.get(year);
            if (rev != null || opIn != null || net != null) {
                result.add(new AnnualFinancial(year, rev, opIn, net));
            }
        }
        return result;
    }

    // ── 키워드 매칭 ────────────────────────────────────────────────────────

    private boolean matchesRevenue(String label) {
        return label.equals("revenue")
                || label.startsWith("revenues")
                || label.startsWith("total revenue")
                || label.startsWith("net revenue")
                || label.startsWith("net sales")
                || label.startsWith("net interest income")
                || label.startsWith("services revenue")
                || label.startsWith("service revenue")
                || label.startsWith("transaction and service")
                || label.startsWith("total revenue and");
    }

    private boolean matchesOperatingIncome(String label) {
        return label.contains("loss from operations")
                || label.contains("income from operations")
                || label.contains("operating income")
                || label.contains("operating loss")
                || label.contains("operating profit")
                || label.contains("income (loss) from operations")
                || label.contains("loss (income) from operations");
    }

    private boolean matchesNetIncome(String label) {
        return label.equals("net income")
                || label.startsWith("net income")
                || label.startsWith("net loss")
                || label.startsWith("net profit")
                || label.startsWith("profit for the")
                || label.startsWith("profit (loss) for")
                || label.startsWith("loss for the");
    }

    // ── 값 추출 ────────────────────────────────────────────────────────────

    // "$", "-", 빈 셀 같은 구분자를 건너뛰며 실제 금액 셀을 찾는 누적 오프셋 알고리즘.
    // 헤더 행과 데이터 행의 컬럼 수가 달라도 (달러 기호 삽입 등) 올바른 값을 추출.
    private void extractValues(List<Element> cells, int[] yearCols, int[] years, Map<Integer, Long> target) {
        int offset = 0;
        for (int k = 0; k < yearCols.length && k < years.length; k++) {
            int col = yearCols[k] + offset;
            boolean pendingNegative = false;
            while (col < cells.size()) {
                String cellText = cells.get(col).text().trim();
                if (cellText.isEmpty()
                        || cellText.equals("-") || cellText.equals("—") || cellText.equals("–")
                        || cellText.equals("$") || cellText.equals("¥") || cellText.equals("￥")
                        || cellText.equals("€") || cellText.equals("£") || cellText.equals("R$")) {
                    col++; offset++;
                } else if (cellText.equals("(")) {
                    // 음수 표시 괄호가 별도 셀에 있는 경우 (PAYP 등)
                    pendingNegative = true;
                    col++; offset++;
                } else {
                    break;
                }
            }
            if (col >= cells.size()) continue;
            Long val = parseAmount(cells.get(col).text());
            if (val != null) {
                if (pendingNegative && val > 0) val = -val;
                target.put(years[k], val);
            }
        }
    }

    private static Long parseAmount(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty() || s.equals("—") || s.equals("–") || s.equals("-") || s.equals("*")) return null;
        // "(" 로 시작하면 음수 (닫는 괄호가 다른 셀에 있어도 처리)
        boolean negative = s.startsWith("(");
        s = s.replaceAll("[$¥￥€£,()\\s]", "");
        if (s.isEmpty()) return null;
        try {
            if (s.contains(".")) return (long) Double.parseDouble(s);
            long val = Long.parseLong(s);
            return negative ? -val : val;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
