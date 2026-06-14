package com.archive.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DestructionDestroyRequest {

    /** shredding / burning / entrusted。 */
    @NotBlank(message = "销毁方式不能为空")
    private String destroyMethod;

    @NotBlank(message = "监销人1不能为空")
    private String supervisorName1;

    @NotBlank(message = "监销人2不能为空")
    private String supervisorName2;

    private String destroyNote;
}
