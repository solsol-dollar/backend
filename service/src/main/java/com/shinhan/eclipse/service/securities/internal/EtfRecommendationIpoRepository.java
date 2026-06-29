package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * com.shinhan.eclipse.service.ipo.internal.IpoRepository는 패키지 프라이빗이라 재사용할 수 없어
 * (Spring Data 빈 이름이 단순 클래스명 "ipoRepository"로 겹쳐서 충돌이 나므로) 별도 이름으로 분리.
 */
interface EtfRecommendationIpoRepository extends JpaRepository<Ipo, Long> {
}
