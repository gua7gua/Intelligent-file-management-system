package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.entity.Archive;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.archive.enums.CarrierStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.LoanStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowEligibilityCheckerTest {

    private BorrowEligibilityChecker checker;
    private ArchiveMapper archiveMapper;
    private BorrowRequestMapper borrowRequestMapper;
    private JdbcTemplate jdbcTemplate;

    private static final Long ARCHIVE_ID = 1L;
    private static final Long ROOM_ID = 5L;
    private static final Integer CATEGORY_ID = 3;

    @BeforeEach
    void setup() {
        archiveMapper = mock(ArchiveMapper.class);
        borrowRequestMapper = mock(BorrowRequestMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        checker = new BorrowEligibilityChecker(archiveMapper, borrowRequestMapper, jdbcTemplate);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARCHIVE_ID)))
                .thenReturn(ROOM_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ROOM_ID), eq(CATEGORY_ID)))
                .thenReturn(0);
        when(borrowRequestMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
    }

    private Archive borrowableArchive() {
        Archive a = new Archive();
        a.setId(ARCHIVE_ID);
        a.setCarrierStatus(CarrierStatus.paper);
        a.setLifecycleStatus(LifecycleStatus.normal);
        a.setLoanStatus(LoanStatus.available);
        a.setConditionStatus(ConditionStatus.normal);
        a.setCategoryId(CATEGORY_ID);
        return a;
    }

    private void assertBiz(Runnable action, ErrorCode code, String msgFragment) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(code))
                .hasMessageContaining(msgFragment);
    }

    @Test
    void 档案不存在抛NOT_FOUND() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(null);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID), ErrorCode.NOT_FOUND, "档案不存在");
    }

    @Test
    void 纯电子档案拒绝() {
        Archive a = borrowableArchive();
        a.setCarrierStatus(CarrierStatus.electronic);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.VALIDATION_FAILED, "纯电子档案不可借阅");
    }

    @Test
    void 生命周期非正常拒绝() {
        Archive a = borrowableArchive();
        a.setLifecycleStatus(LifecycleStatus.pending_shelf);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "生命周期状态异常");
    }

    @Test
    void 已借出拒绝() {
        Archive a = borrowableArchive();
        a.setLoanStatus(LoanStatus.on_loan);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "已借出中");
    }

    @Test
    void 实体损坏拒绝() {
        Archive a = borrowableArchive();
        a.setConditionStatus(ConditionStatus.damaged);
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(a);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "实体状态异常");
    }

    @Test
    void 命中运行盘点拒绝() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(ROOM_ID), eq(CATEGORY_ID)))
                .thenReturn(1);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "正在盘点");
    }

    @Test
    void 存在未结束申请拒绝() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        when(borrowRequestMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        assertBiz(() -> checker.checkBorrowable(ARCHIVE_ID),
                ErrorCode.BUSINESS_CONFLICT, "未结束的借阅申请");
    }

    @Test
    void 排除当前申请时条件7不误拦() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        // 模拟：当前申请（id=77）是唯一未结束申请；排除它后 count=0 → 通过
        when(borrowRequestMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        assertThatCode(() -> checker.checkBorrowable(ARCHIVE_ID, 77L)).doesNotThrowAnyException();

        // 不排除时（apply 场景）count=1 → 拒绝
        when(borrowRequestMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> checker.checkBorrowable(ARCHIVE_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未结束的借阅申请");
    }

    @Test
    void 全部条件满足通过() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        assertThatCode(() -> checker.checkBorrowable(ARCHIVE_ID)).doesNotThrowAnyException();
    }

    @Test
    void 档案无盒位时跳过盘点仍通过() {
        when(archiveMapper.selectById(ARCHIVE_ID)).thenReturn(borrowableArchive());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARCHIVE_ID)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThatCode(() -> checker.checkBorrowable(ARCHIVE_ID)).doesNotThrowAnyException();
    }
}
