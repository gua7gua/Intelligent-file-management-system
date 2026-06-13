package com.archive.service;

import com.archive.entity.AiTaskBatch;
import com.archive.entity.IntakeItem;
import com.archive.mapper.CategoryMapper;
import com.archive.mapper.IntakeItemMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * AI 接收清单补全服务。
 * 组装提示词、白名单校验、把合法字段建议写入 intake_items.ai_suggestion。
 * 不直接写 archives 等业务表，AI 结果必须经管理员确认（9.6）后入库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSuggestionService {

    private final IntakeItemMapper intakeItemMapper;
    private final CategoryMapper categoryMapper;

    /** AI 可写的字段白名单 */
    private static final Set<String> ALLOWED_FIELDS = Set.of("title", "responsible", "formedDate", "category", "tags");
    /** 受保护字段，出现时必须剥离 */
    private static final Set<String> PROTECTED_FIELDS = Set.of(
            "securityLevel", "retentionPeriod", "openStatus", "allowDigitization",
            "archiveNo", "warehouseLocation", "destructionStatus");

    public String buildSystemPrompt() {
        return """
                你是档案管理系统的 AI 助手，帮助档案管理员从清单已有字段中补全正式入库所需的元数据。

                约束：
                - 你只能根据清单字段、文件名、移交单位、移交部门等元数据推测。
                - 你不能读取电子文件正文。
                - 你不能覆盖密级、保管期限、是否公开、是否允许数字化等业务判断字段。
                - 分类必须从系统给定分类中选择（document/technology/accounting/audio_video/personnel）。
                - 标签优先从系统给定标签中选择，必要时可建议新增标签。
                - 每次请求最多处理 50 条清单条目。
                - 输出必须是 JSON，并使用 <JSON> 标签包裹。

                允许补全字段：title、responsible、formedDate、category、tags

                输出格式：
                <JSON>
                {
                  "ruleType": "fieldCompletion",
                  "items": [
                    {
                      "listItemId": 清单条目ID,
                      "fields": {
                        "title": "正式题名",
                        "responsible": "责任者",
                        "formedDate": "yyyy-MM-dd",
                        "category": "分类code或中文名",
                        "tags": ["标签1", "标签2"]
                      }
                    }
                  ]
                }
                </JSON>
                """;
    }

    /** 组装用户消息：注入本批条目的元数据，受保护字段只作上下文参考。 */
    public String buildUserMessage(List<IntakeItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下清单条目补全元数据（分类请使用 code：document/technology/accounting/audio_video/personnel）：\n");
        for (IntakeItem it : items) {
            sb.append("- listItemId: ").append(it.getId()).append('\n');
            if (it.getInputTitle() != null) sb.append("  标题: ").append(it.getInputTitle()).append('\n');
            if (it.getExpectedFilename() != null) sb.append("  文件名: ").append(it.getExpectedFilename()).append('\n');
            if (it.getFormedDate() != null) sb.append("  形成日期: ").append(it.getFormedDate()).append('\n');
            if (it.getCarrierStatus() != null) sb.append("  载体状态: ").append(it.getCarrierStatus()).append('\n');
            if (it.getPageCount() != null) sb.append("  页数: ").append(it.getPageCount()).append('\n');
            if (it.getSecurityLevel() != null) sb.append("  密级(仅参考,不可写): ").append(it.getSecurityLevel()).append('\n');
            if (it.getRetentionPeriod() != null) sb.append("  保管期限(仅参考,不可写): ").append(it.getRetentionPeriod()).append('\n');
        }
        return sb.toString();
    }

    /**
     * 解析 AI 返回 JSON，过滤受保护字段、校验枚举与 ID 归属。
     *
     * @param aiResult    AiClient 提取的 JSON 节点
     * @param targetIds   本批合法的清单条目 ID
     * @param categoryMap 分类 code/中文名 → id 映射（由 loadCategoryMap 提供）
     * @return listItemId → 合法字段 Map；无合法内容则空
     */
    public Map<Long, Map<String, Object>> parseAndFilterItems(JsonNode aiResult, Set<Long> targetIds,
                                                              Map<String, Integer> categoryMap) {
        Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
        if (aiResult == null) return out;
        JsonNode ruleType = aiResult.get("ruleType");
        if (ruleType == null || !"fieldCompletion".equals(ruleType.asText())) return out;
        JsonNode items = aiResult.get("items");
        if (items == null || !items.isArray()) return out;

        for (JsonNode item : items) {
            JsonNode idNode = item.get("listItemId");
            if (idNode == null || !idNode.canConvertToLong()) continue;
            long itemId = idNode.asLong();
            if (!targetIds.contains(itemId)) continue;
            JsonNode fields = item.get("fields");
            if (fields == null || !fields.isObject()) continue;

            Map<String, Object> clean = new LinkedHashMap<>();
            Iterator<String> names = fields.fieldNames();
            while (names.hasNext()) {
                String fname = names.next();
                if (PROTECTED_FIELDS.contains(fname)) continue;   // 剥离受保护字段
                if (!ALLOWED_FIELDS.contains(fname)) continue;   // 非白名单丢弃
                JsonNode fval = fields.get(fname);
                switch (fname) {
                    case "formedDate" -> {
                        if (fval.isTextual() && isValidDate(fval.asText())) clean.put("formedDate", fval.asText());
                    }
                    case "category" -> {
                        if (fval.isTextual()) {
                            Integer catId = categoryMap.get(fval.asText());
                            if (catId != null) clean.put("categoryId", catId);
                        }
                    }
                    case "tags" -> {
                        if (fval.isArray()) {
                            List<String> tags = new ArrayList<>();
                            fval.forEach(t -> { if (t.isTextual()) tags.add(t.asText()); });
                            if (!tags.isEmpty()) clean.put("tags", tags);
                        }
                    }
                    default -> { // title、responsible
                        if (fval.isTextual() && !fval.asText().isBlank()) clean.put(fname, fval.asText());
                    }
                }
            }
            if (!clean.isEmpty()) {
                out.merge(itemId, clean, (a, b) -> { a.putAll(b); return a; });
            }
        }
        return out;
    }

    /**
     * 校验 AI 结果并写入 intake_items.ai_suggestion，更新 batch.validatedResult。
     * 返回通过校验并落库的条目数。
     */
    public int validateAndPersist(JsonNode aiResult, AiTaskBatch batch) {
        Map<String, Integer> categoryMap = loadCategoryMap();
        Map<Long, Map<String, Object>> filtered = parseAndFilterItems(
                aiResult, new HashSet<>(batch.getTargetIds() != null ? batch.getTargetIds() : List.of()), categoryMap);

        // 组装 validatedResult（通过校验的 JSON）
        List<Map<String, Object>> itemList = new ArrayList<>();
        filtered.forEach((itemId, fields) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("listItemId", itemId);
            entry.put("fields", fields);
            itemList.add(entry);
        });
        Map<String, Object> validated = new LinkedHashMap<>();
        validated.put("ruleType", "fieldCompletion");
        validated.put("items", itemList);
        batch.setValidatedResult(validated);

        // 写入 ai_suggestion，只覆盖允许字段，不清空管理员已确认字段
        for (Map.Entry<Long, Map<String, Object>> e : filtered.entrySet()) {
            Long itemId = e.getKey();
            IntakeItem item = intakeItemMapper.selectById(itemId);
            if (item == null) continue;
            Map<String, Object> suggestion = item.getAiSuggestion() != null
                    ? new LinkedHashMap<>(item.getAiSuggestion()) : new LinkedHashMap<>();
            suggestion.putAll(e.getValue());
            item.setAiSuggestion(suggestion);
            intakeItemMapper.updateById(item);
        }
        return filtered.size();
    }

    /** 查询固定五类，返回 code 和 category_name → id 的合并映射。Category.id 为 Short，转为 Integer。 */
    private Map<String, Integer> loadCategoryMap() {
        Map<String, Integer> map = new HashMap<>();
        categoryMapper.selectList(null).forEach(c -> {
            if (c.getId() != null) {
                int id = c.getId().intValue();
                if (c.getCategoryCode() != null) map.put(c.getCategoryCode(), id);
                if (c.getCategoryName() != null) map.put(c.getCategoryName(), id);
            }
        });
        return map;
    }

    private boolean isValidDate(String s) {
        try {
            LocalDate.parse(s); // ISO yyyy-MM-dd
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
