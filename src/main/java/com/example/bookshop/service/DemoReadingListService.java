package com.example.bookshop.service;

import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;

@Service
public class DemoReadingListService {
    private final JdbcTemplate jdbc;
    private final UserMapper users;
    private final ReadingListMapper mapper;
    private final ReadingListService lists;
    private final ObjectMapper json;
    public DemoReadingListService(JdbcTemplate jdbc,UserMapper users,ReadingListMapper mapper,
                                  ReadingListService lists,ObjectMapper json) {
        this.jdbc=jdbc; this.users=users; this.mapper=mapper; this.lists=lists; this.json=json;
    }
    @Transactional(rollbackFor=Exception.class)
    public void initialize() throws IOException {
        if (jdbc.update("INSERT IGNORE INTO t_app_migration(name) VALUES('demo-reading-lists-v1')")==0) return;
        try (var input=new ClassPathResource("db/demo-reading-lists.json").getInputStream()) {
            for (var row:json.readTree(input)) {
                var owner=users.findByUsername(row.get("owner").asText());
                if (owner==null || !row.get("role").asText().equals(owner.getRole())) continue;
                String name=row.get("name").asText();
                if (mapper.named(owner.getId(),name)!=null) continue;
                var user=new LoginUser(owner.getId(),owner.getUsername(),owner.getRole());
                Long id=lists.save(null,name,row.get("description").asText(),user);
                for (var title:row.get("titles")) {
                    var ids=jdbc.queryForList("SELECT id FROM t_book WHERE title=? ORDER BY id LIMIT 1",Long.class,title.asText());
                    if (!ids.isEmpty()) lists.add(id,ids.getFirst(),user);
                }
            }
        }
    }
}
