package com.archive.util;

import com.archive.entity.Archive;
import com.archive.entity.BorrowRequest;
import com.archive.entity.IntakeBatch;
import com.archive.entity.IntakeItem;
import com.archive.entity.Organization;
import com.archive.entity.User;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 清单和回执 PDF 生成工具。
 */
@Component
public class PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 生成移交清单 PDF。
     */
    public byte[] generateTransferPdf(IntakeBatch batch, List<IntakeItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = getCnFont(18, Font.BOLD);
            Font headerFont = getCnFont(10, Font.BOLD);
            Font normalFont = getCnFont(9, Font.NORMAL);

            // 标题
            Paragraph title = new Paragraph(batch.getTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            // 批次信息表格
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(infoTable, "清单号", batch.getBatchNo(), "状态", batch.getStatus().getDisplayName(), normalFont);
            addInfoRow(infoTable, "移交单位", String.valueOf(batch.getOrganizationId()), "移交部门", nullSafe(batch.getDepartmentName()), normalFont);
            addInfoRow(infoTable, "联系人", nullSafe(batch.getContactName()), "联系电话", nullSafe(batch.getContactPhone()), normalFont);
            addInfoRow(infoTable, "档案年度", nullSafe(batch.getArchiveYear()), "预计移交日期", formatLd(batch.getExpectedTransferDate()), normalFont);
            addInfoRow(infoTable, "提交时间", formatOdt(batch.getSubmittedAt()), "条目数量", String.valueOf(items.size()), normalFont);
            doc.add(infoTable);

            doc.add(Chunk.NEWLINE);

            // 条目表格
            PdfPTable itemTable = new PdfPTable(7);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{0.6f, 3f, 1f, 1.2f, 1.2f, 1f, 2f});

            String[] headers = {"序号", "题名", "页数", "保管期限", "载体状态", "密级", "电子文件名"};
            for (String h : headers) {
                itemTable.addCell(makeCell(h, headerFont, Color.LIGHT_GRAY, Element.ALIGN_CENTER));
            }

            for (IntakeItem item : items) {
                itemTable.addCell(makeCell(String.valueOf(item.getItemNo()), normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(nullSafe(item.getInputTitle()), normalFont, null, Element.ALIGN_LEFT));
                itemTable.addCell(makeCell(nullSafe(item.getPageCount()), normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(item.getRetentionPeriod() != null ? item.getRetentionPeriod().getDisplayName() : "", normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(item.getCarrierStatus() != null ? item.getCarrierStatus().getDisplayName() : "", normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(nullSafe(item.getSecurityLevel()), normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(nullSafe(item.getExpectedFilename()), normalFont, null, Element.ALIGN_LEFT));
            }
            doc.add(itemTable);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成接收回执 PDF。
     */
    public byte[] generateReceiptPdf(IntakeBatch batch, List<IntakeItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = getCnFont(16, Font.BOLD);
            Font headerFont = getCnFont(10, Font.BOLD);
            Font normalFont = getCnFont(9, Font.NORMAL);

            // 标题
            Paragraph title = new Paragraph("档案接收回执", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            // 回执信息
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(infoTable, "清单号", batch.getBatchNo(), "清单标题", nullSafe(batch.getTitle()), normalFont);
            addInfoRow(infoTable, "来源类型", batch.getSourceType() == com.archive.enums.SourceType.transfer ? "移交" : "征集",
                    "联系人", nullSafe(batch.getContactName()), normalFont);
            addInfoRow(infoTable, "接收人", String.valueOf(batch.getAcceptedBy()), "接收时间", formatOdt(batch.getAcceptedAt()), normalFont);

            long accepted = items.stream().filter(i -> i.getStatus() == com.archive.enums.ItemStatus.accepted).count();
            long rejected = items.stream().filter(i -> i.getStatus() == com.archive.enums.ItemStatus.rejected).count();
            addInfoRow(infoTable, "接收条目", String.valueOf(accepted), "回退条目", String.valueOf(rejected), normalFont);
            doc.add(infoTable);

            doc.add(Chunk.NEWLINE);

            // 接收条目明细
            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{0.6f, 3f, 1.2f, 1.2f, 2.5f});

            String[] headers = {"序号", "题名", "验收结果", "载体状态", "备注"};
            for (String h : headers) {
                itemTable.addCell(makeCell(h, headerFont, Color.LIGHT_GRAY, Element.ALIGN_CENTER));
            }

            for (IntakeItem item : items) {
                String result = item.getStatus() == com.archive.enums.ItemStatus.accepted ? "已接收" : "已回退";
                String note = item.getStatus() == com.archive.enums.ItemStatus.rejected
                        ? nullSafe(item.getRejectReason()) : nullSafe(item.getAcceptanceNote());

                itemTable.addCell(makeCell(String.valueOf(item.getItemNo()), normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(nullSafe(item.getInputTitle()), normalFont, null, Element.ALIGN_LEFT));
                itemTable.addCell(makeCell(result, normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(item.getCarrierStatus() != null ? item.getCarrierStatus().getDisplayName() : "", normalFont, null, Element.ALIGN_CENTER));
                itemTable.addCell(makeCell(nullSafe(note), normalFont, null, Element.ALIGN_LEFT));
            }
            doc.add(itemTable);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("此回执由系统自动生成，作为档案接收凭证。", normalFont));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("回执 PDF 生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成借阅凭证 PDF（业务场景八）。
     * 内容：凭证信息、借阅人信息、档案信息、单位意见盖章区、已归还盖章区。
     */
    public byte[] generateBorrowVoucherPdf(BorrowRequest borrow, Archive archive,
                                           User borrower, Organization org) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = getCnFont(18, Font.BOLD);
            Font headerFont = getCnFont(10, Font.BOLD);
            Font normalFont = getCnFont(9, Font.NORMAL);

            // 标题
            Paragraph title = new Paragraph("档案借阅凭证", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            // 凭证信息
            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(info, "凭证号", nullSafe(borrow.getVoucherNo()),
                    "申请号", nullSafe(borrow.getRequestNo()), normalFont);
            addInfoRow(info, "审批时间", formatOdt(borrow.getApprovedAt()),
                    "状态", borrow.getStatus() != null ? borrow.getStatus().getDisplayName() : "", normalFont);
            doc.add(info);

            doc.add(Chunk.NEWLINE);

            // 借阅人信息
            PdfPTable borrowerTab = new PdfPTable(4);
            borrowerTab.setWidthPercentage(100);
            borrowerTab.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(borrowerTab, "姓名", borrower != null ? nullSafe(borrower.getRealName()) : "",
                    "工号", borrower != null ? nullSafe(borrower.getEmployeeNo()) : "", normalFont);
            addInfoRow(borrowerTab, "联系电话", borrower != null ? nullSafe(borrower.getPhone()) : "",
                    "部门", borrower != null ? nullSafe(borrower.getDepartmentName()) : "", normalFont);
            addInfoRow(borrowerTab, "所在单位", org != null ? nullSafe(org.getOrgName()) : "",
                    "", "", normalFont);
            doc.add(borrowerTab);

            doc.add(Chunk.NEWLINE);

            // 档案信息
            PdfPTable archTab = new PdfPTable(4);
            archTab.setWidthPercentage(100);
            archTab.setWidths(new float[]{1.5f, 3f, 1.5f, 3f});
            addInfoRow(archTab, "档号", archive != null ? nullSafe(archive.getArchiveNo()) : "",
                    "题名", archive != null ? nullSafe(archive.getTitle()) : "", normalFont);
            addInfoRow(archTab, "载体状态",
                    archive != null && archive.getCarrierStatus() != null
                            ? archive.getCarrierStatus().getDisplayName() : "",
                    "借阅天数", nullSafe(borrow.getExpectedDays()), normalFont);
            addInfoRow(archTab, "预计到馆", formatOdt(borrow.getExpectedVisitAt()),
                    "借阅理由", nullSafe(borrow.getReason()), normalFont);
            doc.add(archTab);

            doc.add(Chunk.NEWLINE);

            // 单位意见盖章区
            doc.add(new Paragraph("单位意见（盖章）：", headerFont));
            doc.add(new Paragraph("\n\n\n（借阅人所在单位意见与盖章位置）\n\n\n", normalFont));

            // 已归还盖章区
            doc.add(new Paragraph("档案馆归还确认（盖章）：", headerFont));
            doc.add(new Paragraph("\n\n\n（归还确认与盖章位置）\n\n\n", normalFont));

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("本凭证由系统自动生成。首次导出时生成凭证号，重复导出复用原号。", normalFont));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("借阅凭证 PDF 生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成编研正文 PDF。把 contentHtml 去标签后按段落渲染，标题居中。
     */
    public byte[] generateCompilationPdf(String title, String contentHtml) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();
            Font titleFont = getCnFont(18, Font.BOLD);
            Font normalFont = getCnFont(11, Font.NORMAL);
            Paragraph t = new Paragraph(title != null ? title : "编研成果", titleFont);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingAfter(12);
            doc.add(t);
            String plain = contentHtml != null ? contentHtml : "";
            for (String para : plain.split("(?i)</p>|<br\\s*/?>|\n")) {
                String text = para.replaceAll("<[^>]+>", "").trim();
                if (!text.isEmpty()) {
                    doc.add(new Paragraph(text, normalFont));
                }
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("编研 PDF 生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成统计报表 PDF（总览四项总量 + 分类维度）。
     */
    public byte[] generateStatisticsPdf(com.archive.dto.response.StatisticsOverviewResponse o,
                                        com.archive.dto.response.StatisticsCategoryResponse c) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();
            Font tf = getCnFont(16, Font.BOLD);
            Font nf = getCnFont(10, Font.NORMAL);
            Paragraph t = new Paragraph("档案统计报表", tf);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingAfter(12);
            doc.add(t);

            PdfPTable tab = new PdfPTable(2);
            tab.setWidthPercentage(100);
            tab.setWidths(new float[]{3f, 2f});
            addInfoRow(tab, "馆藏总量", String.valueOf(o.getTotals().getTotalArchives()),
                    "公开数量", String.valueOf(o.getTotals().getOpenArchives()), nf);
            addInfoRow(tab, "借阅量", String.valueOf(o.getTotals().getBorrowCount()),
                    "销毁量", String.valueOf(o.getTotals().getDestroyedCount()), nf);
            doc.add(tab);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("分类维度：", getCnFont(11, Font.BOLD)));
            appendGroup(doc, "按门类", c.getByCategory(), nf);
            appendGroup(doc, "按年度", c.getByYear(), nf);
            appendGroup(doc, "按来源", c.getBySource(), nf);
            appendGroup(doc, "按载体", c.getByCarrier(), nf);
            appendGroup(doc, "按密级", c.getBySecurity(), nf);
            appendGroup(doc, "按公开状态", c.getByOpenStatus(), nf);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("统计 PDF 生成失败: " + e.getMessage(), e);
        }
    }

    private void appendGroup(Document doc, String title,
                             java.util.List<com.archive.dto.response.StatisticsCategoryResponse.Group> g, Font nf) throws Exception {
        if (g == null || g.isEmpty()) return;
        StringBuilder sb = new StringBuilder(title).append("：");
        for (com.archive.dto.response.StatisticsCategoryResponse.Group x : g) {
            sb.append(x.getLabel()).append("(").append(x.getCount()).append(") ");
        }
        doc.add(new Paragraph(sb.toString(), nf));
    }

    // ==================== 工具方法 ====================

    private Font getCnFont(float size, int style) {
        try {
            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception e) {
            // 降级为默认字体
            return new Font(Font.HELVETICA, size, style);
        }
    }

    private PdfPCell makeCell(String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        return cell;
    }

    private void addInfoRow(PdfPTable table, String label1, String value1,
                            String label2, String value2, Font font) {
        table.addCell(makeCell(label1, font, null, Element.ALIGN_RIGHT));
        table.addCell(makeCell(value1, font, null, Element.ALIGN_LEFT));
        table.addCell(makeCell(label2, font, null, Element.ALIGN_RIGHT));
        table.addCell(makeCell(value2, font, null, Element.ALIGN_LEFT));
    }

    private String nullSafe(Object val) {
        return val != null ? String.valueOf(val) : "";
    }

    private String formatLd(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    private String formatOdt(java.time.OffsetDateTime odt) {
        return odt != null ? odt.format(DATETIME_FMT) : "";
    }
}
