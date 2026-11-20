package com.example.bookshop.service;

import com.example.bookshop.dto.*;
import com.example.bookshop.entity.Book;
import com.example.bookshop.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiRecommendationServiceTest {
    BookService books=mock(BookService.class);
    AiClient client=mock(AiClient.class);
    ReadingListService lists=mock(ReadingListService.class);
    AiRecommendationService service=new AiRecommendationService(books,client,new ObjectMapper(),lists);
    Book book(long id,String language,String price) {
        Book b=new Book(); b.setId(id); b.setTitle("书"+id); b.setLanguage(language);
        b.setPrice(new BigDecimal(price)); b.setStock(3); return b;
    }
    void catalog(Book... b) {
        when(books.search(any(),eq(100),eq(false))).thenReturn(new PageResult<>(List.of(b),b.length,1,100,1));
        for(Book item:b) when(books.findById(item.getId())).thenReturn(item);
    }
    @Test void rejectsInventedIdsDuplicatesWrongLanguageAndOverBudget() {
        catalog(book(1,"中文","60"),book(2,"中文","60"),book(3,"外文","20"));
        when(client.recommend(anyString(),anyString())).thenReturn("""
            {"budget":200,"requiredLanguage":"中文","items":[
            {"id":999,"reason":"不存在"},{"id":1,"reason":"基础"},{"id":1}, {"id":2},{"id":3}]}
            """);
        var result=service.recommend("test-key","学习",new BigDecimal("100"));
        assertThat(result.items()).extracting(i -> i.book().getId()).containsExactly(1L);
        assertThat(result.total()).isEqualByComparingTo("60");
        assertThat(result.budget()).isEqualByComparingTo("100");
    }
    @Test void respectsExtractedBudgetAndDoesNotChargeNonBooks() {
        Book paper=book(2,"中文","0"); paper.setResourceType("论文"); paper.setStock(0);
        catalog(book(1,"中文","60"),paper);
        when(client.recommend(anyString(),anyString())).thenReturn("{\"budget\":50,\"items\":[{\"id\":1},{\"id\":2}]}");
        var r=service.recommend("test-key","预算50",null);
        assertThat(r.items()).extracting(i -> i.book().getId()).containsExactly(2L);
        assertThat(r.total()).isEqualByComparingTo("0");
    }
    @Test void malformedResponseProducesSafeError() {
        catalog(book(1,"中文","60"));
        when(client.recommend(anyString(),anyString())).thenReturn("secret-provider-body");
        assertThatThrownBy(() -> service.recommend("secret-key","学习",null)).isInstanceOf(BizException.class)
                .hasMessage("AI 推荐格式不正确，请重新生成");
    }
    @Test void invalidInputDoesNotCallProvider() {
        assertThatThrownBy(() -> service.recommend("key"," ",null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.recommend("key","学习",new BigDecimal("-1"))).isInstanceOf(BizException.class);
        verifyNoInteractions(client);
    }
    @Test void forgedSelectionCannotCreateList() {
        var result=new AiRecommendationService.Result(List.of(new AiRecommendationService.Item(book(1,"中文","60"),"理由")),"",BigDecimal.ZERO,null,null,false);
        assertThatThrownBy(() -> service.save(result,List.of(99L),"书单",new LoginUser(1L,"reader","READER"))).isInstanceOf(BizException.class);
        verifyNoInteractions(lists);
    }
    @Test void providerErrorsAreSanitized() {
        assertThat(AiClient.failure(401).getMessage()).contains("密钥无效");
        assertThat(AiClient.failure(402).getMessage()).contains("余额不足");
        assertThat(AiClient.failure(429).getMessage()).contains("频繁");
    }
}
