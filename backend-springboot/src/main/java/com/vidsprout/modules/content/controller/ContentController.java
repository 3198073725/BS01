package com.vidsprout.modules.content.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.modules.content.model.Category;
import com.vidsprout.modules.content.model.Tag;
import com.vidsprout.modules.content.repository.CategoryRepository;
import com.vidsprout.modules.content.repository.TagRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public ContentController(CategoryRepository categoryRepository, TagRepository tagRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @GetMapping("/tags/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTags() {
        List<Map<String, Object>> tags = tagRepository.findAll().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId().toString());
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("created_at", t.getCreatedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true)
                .results(tags)
                .page(1)
                .pageSize(tags.size())
                .total(tags.size())
                .hasNext(false)
                .build());
    }

    @PostMapping("/tags/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTag(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("标签名不能为空");
        }
        Tag tag = tagRepository.save(Tag.builder().name(name.trim()).description(body.getOrDefault("description", "")).build());
        Map<String, Object> m = new HashMap<>();
        m.put("id", tag.getId().toString());
        m.put("name", tag.getName());
        m.put("description", tag.getDescription());
        m.put("created_at", tag.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.success(m));
    }

    @GetMapping("/categories/")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategories() {
        List<Map<String, Object>> categories = categoryRepository.findAll().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId().toString());
            m.put("name", c.getName());
            m.put("description", c.getDescription());
            m.put("created_at", c.getCreatedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .ok(true)
                .results(categories)
                .page(1)
                .pageSize(categories.size())
                .total(categories.size())
                .hasNext(false)
                .build());
    }
}
