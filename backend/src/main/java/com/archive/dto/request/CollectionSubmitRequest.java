package com.archive.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * 提交征集清单请求。
 */
@Data
public class CollectionSubmitRequest {

    @AssertTrue(message = "必须同意捐赠协议")
    private Boolean agreementAccepted;
}
