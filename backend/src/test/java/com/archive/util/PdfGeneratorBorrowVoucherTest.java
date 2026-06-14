package com.archive.util;

import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.entity.Organization;
import com.archive.entity.User;
import com.archive.enums.BorrowStatus;
import com.archive.enums.CarrierStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGeneratorBorrowVoucherTest {

    @Test
    void 生成非空PDF且以PDF头开头() {
        BorrowRequest b = new BorrowRequest();
        b.setRequestNo("BRW-000001");
        b.setVoucherNo("VCH-000001");
        b.setStatus(BorrowStatus.voucher_issued);
        b.setReason("财政预算核查需要查阅原件");
        b.setExpectedDays(7);
        b.setExpectedVisitAt(OffsetDateTime.now().plusDays(1));
        b.setApprovedAt(OffsetDateTime.now());

        Archive a = new Archive();
        a.setArchiveNo("ARC-000001");
        a.setTitle("2025 年第一季度会计凭证");
        a.setCarrierStatus(CarrierStatus.paper);

        User u = new User();
        u.setRealName("小李");
        u.setEmployeeNo("E001");
        u.setPhone("13800000004");
        u.setDepartmentName("财务部");

        Organization org = new Organization();
        org.setOrgName("克拉玛依市某单位");

        byte[] pdf = new PdfGenerator().generateBorrowVoucherPdf(b, a, u, org);

        assertThat(pdf).isNotEmpty();
        assertThat(pdf.length).isGreaterThan(100);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void 借阅人或单位为空时不抛异常() {
        BorrowRequest b = new BorrowRequest();
        b.setRequestNo("BRW-000001");
        b.setVoucherNo("VCH-000001");
        b.setStatus(BorrowStatus.voucher_issued);
        b.setExpectedDays(7);

        Archive a = new Archive();
        a.setArchiveNo("ARC-000001");
        a.setTitle("测试档案");
        a.setCarrierStatus(CarrierStatus.paper);

        byte[] pdf = new PdfGenerator().generateBorrowVoucherPdf(b, a, null, null);
        assertThat(pdf).isNotEmpty();
    }
}
