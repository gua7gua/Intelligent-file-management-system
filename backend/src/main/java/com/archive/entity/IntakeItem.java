package com.archive.entity;

import com.archive.enums.CarrierStatus;
import com.archive.enums.FileMatchStatus;
import com.archive.enums.ItemStatus;
import com.archive.enums.RetentionPeriod;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 移交/征集清单条目。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "intake_items", autoResultMap = true)
public class IntakeItem extends BaseEntity {

    private Long batchId;

    private Integer itemNo;

    @EnumValue
    private ItemStatus status;

    private String inputTitle;

    private Integer pageCount;

    @EnumValue
    private RetentionPeriod retentionPeriod;

    @EnumValue
    private CarrierStatus carrierStatus;

    private Integer securityLevel;

    private String openStatus;

    private Boolean allowDigitization;

    private String electronicFormat;

    private String expectedFilename;

    private LocalDate formedDate;

    private String acceptanceNote;

    private String rejectReason;

    @EnumValue
    private FileMatchStatus fileMatchStatus;

    /** AI 补全建议 JSON。JacksonTypeHandler 以 Map 形式读写 JSONB。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> aiSuggestion;

    private String confirmedTitle;

    private String confirmedResponsibleText;

    private LocalDate confirmedFormedDate;

    private Integer confirmedCategoryId;

    /** 后台确认后的标签名数组。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> confirmedTags;

    private Long generatedArchiveId;

    /** 软删除时间。 */
    private OffsetDateTime deletedAt;
}
