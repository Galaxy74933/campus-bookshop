package com.example.bookshop.service;

import com.example.bookshop.dto.*;
import com.example.bookshop.entity.Book;
import com.example.bookshop.exception.BizException;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AiRecommendationService {
    public record Item(Book book,String reason) {}
    public record Result(List<Item> items,String summary,BigDecimal total,BigDecimal budget,String language,boolean limited) {}
    private final BookService books;
    private final AiClient client;
    private final ObjectMapper json;
    private final ReadingListService lists;
    public AiRecommendationService(BookService books,AiClient client,ObjectMapper json,ReadingListService lists) {
        this.books=books; this.client=client; this.json=json; this.lists=lists;
    }
    public Result recommend(String key,String need,BigDecimal explicitBudget) {
        if(need==null || need.isBlank() || need.length()>1000) throw new BizException("需求须为1～1000个字符");
        if(explicitBudget!=null && (explicitBudget.signum()<0 || explicitBudget.scale()>2 || explicitBudget.compareTo(new BigDecimal("1000000"))>0))
            throw new BizException("预算须为0～1000000元，最多两位小数");
        var page=books.search(new BookQuery(),100,false);
        var candidates=page.getRecords().stream().filter(b -> !b.isPurchasable() || b.getStock()>0).toList();
        if(candidates.isEmpty()) return new Result(List.of(),"当前没有可推荐的资源。",BigDecimal.ZERO,explicitBudget,null,false);
        try {
            List<Map<String,Object>> metadata=new ArrayList<>();
            for(Book b:candidates) {
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("id",b.getId()); row.put("title",b.getTitle()); row.put("author",b.getAuthor());
                row.put("category",b.getCategoryName()); row.put("type",b.getResourceType());
                row.put("language",b.getLanguage()); row.put("description",cut(b.getDescription(),1200));
                row.put("price",b.isPurchasable()?b.getPrice():null); row.put("stock",b.getStock()); metadata.add(row);
            }
            String prompt=json.writeValueAsString(Map.of("需求",need,"明确预算（优先于文本）",explicitBudget==null?"未单独指定":explicitBudget,
                    "候选资源",metadata));
            String response=client.recommend(key,prompt);
            JsonNode root=json.readTree(response);
            if(!root.isObject() || !root.path("items").isArray()) throw new IllegalArgumentException();
            BigDecimal budget=explicitBudget;
            if(budget==null && !root.path("budget").isMissingNode() && !root.path("budget").isNull()) {
                if(!root.path("budget").isNumber()) throw new IllegalArgumentException();
                budget=root.path("budget").decimalValue();
                if(budget.signum()<0) throw new IllegalArgumentException();
            }
            String language=root.path("requiredLanguage").isTextual()?cut(root.path("requiredLanguage").asText(),30):null;
            Set<Long> allowed=new HashSet<>(); candidates.forEach(b -> allowed.add(b.getId()));
            Set<Long> seen=new HashSet<>(); List<Item> result=new ArrayList<>(); BigDecimal total=BigDecimal.ZERO;
            for(JsonNode node:root.path("items")) {
                if(result.size()==5) break;
                if(!node.path("id").isIntegralNumber()) continue;
                long id=node.path("id").asLong();
                if(!allowed.contains(id) || !seen.add(id)) continue;
                Book book;
                try { book=books.findById(id); } catch(BizException deleted) { continue; }
                if(language!=null && !language.isBlank() && !language.equals(book.getLanguage())) continue;
                if(book.isPurchasable() && book.getStock()<=0) continue;
                BigDecimal price=book.isPurchasable()?book.getPrice():BigDecimal.ZERO;
                if(budget!=null && total.add(price).compareTo(budget)>0) continue;
                total=total.add(price);
                result.add(new Item(book,cut(node.path("reason").asText("依据站内资料推荐，请查看详情确认。"),800)));
            }
            // Do not display unverified provider summaries containing invented books or prices.
            String summary=result.isEmpty()?"当前没有通过校验的匹配资源，请调整需求后重试。":"以下按 AI 建议的阅读顺序排列；理由仅供参考，请结合详情选择。";
            return new Result(List.copyOf(result),summary,total,budget,language,page.getTotal()>100);
        } catch(BizException e) { throw e; }
        catch(Exception e) { throw new BizException("AI 推荐格式不正确，请重新生成"); }
    }
    @Transactional
    public Long save(Result result,List<Long> selected,String name,LoginUser user) {
        if(result==null || selected==null || selected.isEmpty()) throw new BizException("请先生成推荐并勾选资源");
        var allowed=result.items().stream().map(i -> i.book().getId()).toList();
        var ids=selected.stream().distinct().toList();
        if(!allowed.containsAll(ids)) throw new BizException("推荐结果已变化，请重新选择");
        Long id=lists.save(null,name,"由 AI 选书推荐整理，阅读时请结合资源详情。",user);
        for(Long bookId:ids) lists.add(id,bookId,user);
        return id;
    }
    private static String cut(String value,int max) { return value==null?"":value.substring(0,Math.min(value.length(),max)); }
}
