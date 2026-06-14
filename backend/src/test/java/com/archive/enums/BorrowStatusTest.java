package com.archive.enums;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BorrowStatusTest {

    @Test
    void borrowStatus名称集合对齐borrow_requests表的CHECK约束() {
        Set<String> names = java.util.Arrays.stream(BorrowStatus.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder(
                "applied", "rejected", "approved",
                "voucher_issued", "checked_out", "returned", "abnormal_return");
    }

    @Test
    void returnCheckResult名称集合对齐CHECK约束() {
        Set<String> names = java.util.Arrays.stream(ReturnCheckResult.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder(
                "normal", "damaged", "missing_page", "other");
    }

    @Test
    void 每个借阅状态都有中文展示名() {
        for (BorrowStatus s : BorrowStatus.values()) {
            assertThat(s.getDisplayName()).isNotBlank();
        }
    }
}
