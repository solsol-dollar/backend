package com.shinhan.eclipse.ledger.allocation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AllocationResultListRes {
    private final List<AllocationResultRes> results;
}
