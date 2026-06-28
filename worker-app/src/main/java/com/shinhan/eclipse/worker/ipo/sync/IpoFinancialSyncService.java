package com.shinhan.eclipse.worker.ipo.sync;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.ipo.IpoFinancial;
import com.shinhan.eclipse.worker.ipo.repository.IpoFinancialRepository;
import com.shinhan.eclipse.worker.ipo.repository.IpoNewsIpoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpoFinancialSyncService {

    private final SecEdgarFinancialParser parser;
    private final IpoFinancialRepository financialRepository;
    private final IpoNewsIpoRepository ipoRepository;

    public Map<String, String> syncAll() {
        Map<String, Long> tickerToId = ipoRepository.findAll().stream()
                .collect(Collectors.toMap(Ipo::getTicker, Ipo::getId, (a, b) -> a));

        Map<String, String> results = new LinkedHashMap<>();

        for (String ticker : SecEdgarFinancialParser.CIK_MAP.keySet()) {
            Long ipoId = tickerToId.get(ticker);
            if (ipoId == null) {
                log.warn("{}: DB에 없는 종목, 스킵", ticker);
                results.put(ticker, "SKIP - DB에 없음");
                continue;
            }

            try {
                List<SecEdgarFinancialParser.AnnualFinancial> financials = parser.fetch(ticker);
                String currency = parser.getCurrency(ticker);
                int saved = 0;

                for (SecEdgarFinancialParser.AnnualFinancial f : financials) {
                    if (!financialRepository.existsByIpoIdAndFiscalYear(ipoId, f.year())) {
                        financialRepository.save(IpoFinancial.create(
                                ipoId, f.year(), f.revenue(), f.operatingIncome(), f.netIncome(), currency
                        ));
                        saved++;
                    }
                }

                results.put(ticker, "OK - " + saved + "건 저장 / " + financials.size() + "건 파싱");
                log.info("{}: {}건 저장 완료 ({}건 파싱)", ticker, saved, financials.size());

                // SEC EDGAR rate limit (10 req/sec)
                Thread.sleep(300);
            } catch (Exception e) {
                log.error("{}: 동기화 실패 - {}", ticker, e.getMessage());
                results.put(ticker, "ERROR - " + e.getMessage());
            }
        }

        return results;
    }
}
