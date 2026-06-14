package com.archive.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BorrowApplyRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private BorrowApplyRequest valid() {
        BorrowApplyRequest r = new BorrowApplyRequest();
        r.setArchiveId(1L);
        r.setReason("财政预算核查需要查阅原件");
        r.setExpectedDays(7);
        return r;
    }

    @Test
    void 合法请求无违反() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    void expectedDays小于1违反约束() {
        BorrowApplyRequest r = valid();
        r.setExpectedDays(0);
        Set<ConstraintViolation<BorrowApplyRequest>> v = validator.validate(r);
        assertThat(v).anyMatch(c -> c.getPropertyPath().toString().equals("expectedDays"));
    }

    @Test
    void 理由为空违反约束() {
        BorrowApplyRequest r = valid();
        r.setReason("");
        Set<ConstraintViolation<BorrowApplyRequest>> v = validator.validate(r);
        assertThat(v).anyMatch(c -> c.getPropertyPath().toString().equals("reason"));
    }

    @Test
    void 手机号格式错误违反约束() {
        BorrowApplyRequest r = valid();
        r.setContactPhone("12345");
        assertThat(validator.validate(r))
                .anyMatch(c -> c.getPropertyPath().toString().equals("contactPhone"));
    }

    @Test
    void 手机号留空合法() {
        BorrowApplyRequest r = valid();
        r.setContactPhone(null);
        assertThat(validator.validate(r)).isEmpty();
    }
}
