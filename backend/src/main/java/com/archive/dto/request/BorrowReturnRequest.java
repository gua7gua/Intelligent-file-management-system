package com.archive.dto.request;

import com.archive.enums.ReturnCheckResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 确认归还入参（12.5）。
 */
@Data
public class BorrowReturnRequest {

    @NotNull(message = "归还检查结果不能为空")
    private ReturnCheckResult returnCheckResult;

    @Size(max = 500)
    private String returnNote;
}
