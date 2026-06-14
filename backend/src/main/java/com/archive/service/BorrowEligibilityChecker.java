package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.enums.BorrowStatus;
import com.archive.enums.CarrierStatus;
import com.archive.enums.ConditionStatus;
import com.archive.enums.LifecycleStatus;
import com.archive.enums.LoanStatus;
import com.archive.exception.BusinessException;
import com.archive.mapper.ArchiveMapper;
import com.archive.mapper.BorrowRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 借阅可借性校验组件。
 *
 * <p>在创建借阅申请 / 出库前调用 {@link #checkBorrowable(Long)}，集中校验档案是否满足借阅前置条件：
 * 档案存在、载体允许借阅、生命周期/借阅/实体状态正常、未命中运行中盘点、无未结束借阅申请。
 * 任一条件不满足即抛出 {@link BusinessException}，由全局异常处理器统一响应。
 */
@Component
@RequiredArgsConstructor
public class BorrowEligibilityChecker {

    private final ArchiveMapper archiveMapper;
    private final BorrowRequestMapper borrowRequestMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 通过档案查找其所在盒位对应的库房 room_id；档案未入盒时查不到行。 */
    private static final String ROOM_OF_ARCHIVE_SQL =
            "SELECT sl.room_id FROM archive_box_items abi " +
            "JOIN archive_boxes ab ON ab.id = abi.box_id " +
            "JOIN storage_locations sl ON sl.id = ab.location_id " +
            "WHERE abi.archive_id = ? AND abi.deleted_at IS NULL " +
            "LIMIT 1";

    /** 统计某库房 + 门类下处于 running 状态的盘点任务数量。 */
    private static final String RUNNING_INVENTORY_SQL =
            "SELECT COUNT(*) FROM inventory_tasks " +
            "WHERE status = 'running' AND room_id = ? AND category_id = ?";

    public void checkBorrowable(Long archiveId) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档案不存在");
        }

        CarrierStatus carrier = archive.getCarrierStatus();
        if (carrier != CarrierStatus.paper && carrier != CarrierStatus.paper_electronic) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "纯电子档案不可借阅");
        }

        if (archive.getLifecycleStatus() != LifecycleStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案当前不可借阅（生命周期状态异常）");
        }

        if (archive.getLoanStatus() != LoanStatus.available) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案已借出中");
        }

        if (archive.getConditionStatus() != ConditionStatus.normal) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "档案实体状态异常，暂停借阅");
        }

        Long roomId = resolveRoomId(archiveId);
        if (roomId != null) {
            Integer running = jdbcTemplate.queryForObject(
                    RUNNING_INVENTORY_SQL, Integer.class, roomId, archive.getCategoryId());
            if (running != null && running > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                        "档案所在架位/门类正在盘点，暂停借阅");
            }
        }

        QueryWrapper<BorrowRequest> qw = new QueryWrapper<>();
        qw.eq("archive_id", archiveId)
          .isNull("deleted_at")
          .in("status",
                  BorrowStatus.applied.name(),
                  BorrowStatus.approved.name(),
                  BorrowStatus.voucher_issued.name(),
                  BorrowStatus.checked_out.name());
        Long openCount = borrowRequestMapper.selectCount(qw);
        if (openCount != null && openCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,
                    "该档案存在未结束的借阅申请");
        }
    }

    /**
     * 解析档案所在盒位的库房 room_id。
     *
     * <p>档案未入盒（无盒位关联）时查询返回空集，{@link JdbcTemplate#queryForObject} 会抛
     * {@link EmptyResultDataAccessException}，此时返回 null，调用方据此跳过盘点拦截。
     */
    private Long resolveRoomId(Long archiveId) {
        try {
            return jdbcTemplate.queryForObject(ROOM_OF_ARCHIVE_SQL, Long.class, archiveId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
