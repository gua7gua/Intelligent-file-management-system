package com.archive.service;

import com.archive.mapper.CategoryMapper;
import com.archive.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final RoleMapper roleMapper;
    private final CategoryMapper categoryMapper;

    public Map<String, Object> getAllDictionaries() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("roles", roleMapper.selectList(null).stream()
                .map(r -> Map.of("roleCode", (Object) r.getRoleCode(),
                        "roleName", r.getRoleName(),
                        "enabled", r.getEnabled()))
                .collect(Collectors.toList()));
        dict.put("categories", categoryMapper.selectList(null).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("categoryId", c.getId());
                    m.put("categoryCode", c.getCategoryCode());
                    m.put("categoryName", c.getCategoryName());
                    m.put("enabled", c.getEnabled());
                    return m;
                })
                .collect(Collectors.toList()));
        return dict;
    }

    public Object getDictionary(String dictCode) {
        Map<String, Object> all = getAllDictionaries();
        return all.get(dictCode);
    }
}
