package com.example.bookshop.controller;

import com.example.bookshop.config.WebConfig;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.entity.Book;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes={AiRecommendationController.class,AuthController.class,CartController.class,ResourceDetailController.class,ReadingListController.class,WebConfig.class,AuthInterceptor.class})
class AiRecommendationControllerTest {
    @Autowired MockMvc mvc;
    @MockBean AiRecommendationService ai;
    @MockBean UserService users;
    @MockBean CartService cart;
    @MockBean OrderService orders;
    @MockBean BookService books;
    @MockBean ReadingListService lists;
    MockHttpSession reader(long id) {
        var s=new MockHttpSession(); s.setAttribute(AuthInterceptor.SESSION_KEY,new LoginUser(id,"reader","READER")); return s;
    }
    String token(MockHttpSession s) throws Exception {
        return (String)mvc.perform(get("/shop/ai").session(s)).andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("csrf");
    }
    @Test void requiresReaderAndCsrf() throws Exception {
        mvc.perform(get("/shop/ai")).andExpect(status().is3xxRedirection());
        var s=reader(1); s.setAttribute(AuthInterceptor.SESSION_KEY,new LoginUser(1L,"admin","ADMIN"));
        mvc.perform(get("/shop/ai").session(s)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/shop/ai/recommend").param("need","测试需求").session(reader(1)).param("csrf","forged").param("apiKey","sk-test-only-1234567890")).andExpect(status().isForbidden());
    }
    @Test void credentialsStayPrivateAndAreIsolatedAndClearable() throws Exception {
        var a=reader(1); var b=reader(2); String csrf=token(a);
        mvc.perform(post("/shop/ai/recommend").param("need","测试需求").session(a).param("csrf",csrf).param("apiKey","sk-test-only-1234567890")).andExpect(redirectedUrl("/shop/ai"));
        mvc.perform(get("/shop/ai").session(a)).andExpect(model().attribute("configured",true))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(content().string(not(containsString("sk-test-only"))));
        mvc.perform(get("/shop/ai").session(b)).andExpect(model().attribute("configured",false));
        mvc.perform(post("/shop/ai/clear").session(a).param("csrf",csrf)).andExpect(redirectedUrl("/shop/ai"));
        mvc.perform(get("/shop/ai").session(a)).andExpect(model().attribute("configured",false));
    }
    @Test void rendersRecommendationsEscapedAndUsesSessionKey() throws Exception {
        var s=reader(1); String csrf=token(s);
        mvc.perform(post("/shop/ai/recommend").param("need","测试需求").session(s).param("csrf",csrf).param("apiKey","sk-test-only-1234567890"));
        Book book=new Book(); book.setId(1L); book.setTitle("Java 示例"); book.setPrice(BigDecimal.TEN); book.setStock(2);
        var r=new AiRecommendationService.Result(List.of(new AiRecommendationService.Item(book,"<script>bad()</script>")),"推荐",BigDecimal.TEN,null,null,false);
        when(ai.recommend(eq("sk-test-only-1234567890"),eq("学Java"),isNull())).thenReturn(r);
        mvc.perform(post("/shop/ai/recommend").session(s).param("csrf",csrf).param("need","学Java")).andExpect(redirectedUrl("/shop/ai"));
        mvc.perform(get("/shop/ai").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("Java 示例")))
                .andExpect(content().string(not(containsString("<script>bad()"))));
        verify(ai).recommend("sk-test-only-1234567890","学Java",null);
        when(books.findById(1L)).thenReturn(book);
        mvc.perform(get("/shop/books/1").param("source","ai").session(s))
                .andExpect(status().isOk()).andExpect(model().attribute("fromAi",true))
                .andExpect(content().string(containsString("source=ai")));
        mvc.perform(get("/shop/books/1").session(s)).andExpect(status().isOk())
                .andExpect(model().attribute("fromAi",false));
        var personal=new com.example.bookshop.entity.ReadingList(); personal.setId(2L); personal.setName("个人计划");
        var publicList=new com.example.bookshop.entity.ReadingList(); publicList.setId(3L); publicList.setName("管理员公开书单");
        when(lists.visible(any())).thenReturn(List.of(personal,publicList));
        when(lists.editable(eq(personal),any())).thenReturn(true);
        mvc.perform(get("/shop/reading-lists/choose").session(s).param("bookId","1").param("source","ai"))
                .andExpect(status().isOk()).andExpect(model().attribute("fromAi",true))
                .andExpect(model().attribute("lists",List.of(personal)))
                .andExpect(content().string(containsString("reading-choice")));
        mvc.perform(post("/shop/reading-lists/2/books").session(s).param("bookId","1").param("source","ai"))
                .andExpect(redirectedUrl("/shop/ai"));
        mvc.perform(post("/shop/reading-lists/2/books").session(s).param("bookId","1"))
                .andExpect(redirectedUrl("/shop/reading-lists/2"));
        mvc.perform(post("/shop/cart/add").session(s).param("bookId","1").param("source","ai"))
                .andExpect(redirectedUrl("/shop/ai")).andExpect(flash().attribute("msg","已加入购物车"));
        mvc.perform(get("/shop/ai").session(s)).andExpect(model().attribute("result",r));
        verify(cart).add(1L,1L,1);
        verify(ai,times(1)).recommend("sk-test-only-1234567890","学Java",null);
        doThrow(new com.example.bookshop.exception.BizException("库存不足")).when(cart).add(1L,1L,1);
        mvc.perform(post("/shop/cart/add").session(s).param("bookId","1").param("source","ai"))
                .andExpect(redirectedUrl("/shop/ai")).andExpect(flash().attribute("error","库存不足"));
        mvc.perform(post("/shop/cart/add").session(s).param("bookId","1"))
                .andExpect(redirectedUrl("/shop/books"));
        mvc.perform(post("/shop/cart/add").session(s).param("bookId","1").param("source","https://example.com"))
                .andExpect(redirectedUrl("/shop/books"));
    }
    @Test void changedIdentityAndLogoutDiscardCredentials() throws Exception {
        var s=reader(1); String csrf=token(s);
        mvc.perform(post("/shop/ai/recommend").param("need","测试需求").session(s).param("csrf",csrf).param("apiKey","sk-test-only-1234567890"));
        s.setAttribute(AuthInterceptor.SESSION_KEY,new LoginUser(2L,"other","READER"));
        mvc.perform(get("/shop/ai").session(s)).andExpect(model().attribute("configured",false));
        mvc.perform(get("/logout").session(s)).andExpect(redirectedUrl("/reader/entry"));
        assertThat(s.isInvalid()).isTrue();
    }
    @Test void oneSubmitPreservesDraftOnInvalidKeyAndProviderFailure() throws Exception {
        var s=reader(1); String csrf=token(s);
        mvc.perform(get("/shop/ai").session(s))
                .andExpect(content().string(containsString("form=\"ai-recommend\"")))
                .andExpect(content().string(not(containsString("暂存密钥"))));
        mvc.perform(post("/shop/ai/recommend").session(s).param("csrf",csrf)
                .param("apiKey","bad").param("need","我的选书需求").param("budget","120"))
                .andExpect(redirectedUrl("/shop/ai")).andExpect(flash().attributeExists("error"));
        mvc.perform(get("/shop/ai").session(s)).andExpect(model().attribute("need","我的选书需求"))
                .andExpect(model().attribute("budget","120"));
        verifyNoInteractions(ai);
        when(ai.recommend(anyString(),anyString(),any())).thenThrow(new com.example.bookshop.exception.BizException("服务暂时不可用"));
        mvc.perform(post("/shop/ai/recommend").session(s).param("csrf",csrf)
                .param("apiKey","sk-test-only-1234567890").param("need","我的选书需求").param("budget","120"))
                .andExpect(flash().attribute("error","服务暂时不可用"));
        mvc.perform(get("/shop/ai").session(s)).andExpect(model().attribute("need","我的选书需求"))
                .andExpect(model().attribute("budget","120")).andExpect(model().attribute("configured",true));
    }
}
