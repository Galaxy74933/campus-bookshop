package com.example.bookshop.controller;

import com.example.bookshop.config.WebConfig;
import com.example.bookshop.dto.RegisterForm;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.entity.User;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {AuthController.class, HomeController.class,
        WebConfig.class, AuthInterceptor.class})
class AuthPagesTest {
    @Autowired MockMvc mvc;
    @MockBean UserService userService;

    @ParameterizedTest
    @CsvSource({"/reader/entry,/login,/register,/admin/entry",
            "/admin/entry,/admin/login,/admin/register,/reader/entry"})
    void entrancesOfferSeparateLoginAndRegistration(String entry, String login, String register, String other) throws Exception {
        mvc.perform(get(entry)).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"" + login + "\"")))
                .andExpect(content().string(containsString("href=\"" + register + "\"")))
                .andExpect(content().string(containsString("href=\"" + other + "\"")));
    }

    @ParameterizedTest
    @CsvSource({"/login,/reader/entry", "/register,/reader/entry",
            "/admin/login,/admin/entry", "/admin/register,/admin/entry"})
    void formsArePublicAndReturnToTheirOwnEntrance(String page, String entry) throws Exception {
        mvc.perform(get(page)).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"" + entry + "\"")))
                .andExpect(content().string(containsString("action=\"" + page + "\"")))
                .andExpect(content().string(not(containsString("name=\"role\""))));
    }

    @ParameterizedTest
    @CsvSource({"/register,READER,ADMIN,/login", "/admin/register,ADMIN,READER,/admin/login"})
    void registrationUsesRouteRoleEvenWhenClientSendsAnotherRole(String route, String role, String forged, String login) throws Exception {
        mvc.perform(post(route).param("username", "newuser").param("password", "123456")
                        .param("confirmPassword", "123456").param("role", forged))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(login));
        verify(userService).register(argThat(form -> role.equals(form.getRole())));
    }

    @ParameterizedTest
    @CsvSource({"/register,register,/reader/entry", "/admin/register,admin/register,/admin/entry"})
    void registrationErrorsStayOnTheirOwnPage(String route, String view, String entry) throws Exception {
        doThrow(new BizException("两次输入的密码不一致")).when(userService).register(any(RegisterForm.class));
        mvc.perform(post(route).param("username", "newuser").param("password", "123456")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk()).andExpect(view().name(view))
                .andExpect(model().attribute("username", "newuser"))
                .andExpect(content().string(containsString("两次输入的密码不一致")))
                .andExpect(result -> assertThat(breadcrumbHtml(result)).contains("aria-current=\"page\">注册</span>"))
                .andExpect(content().string(containsString("href=\"" + entry + "\"")));
    }

    @ParameterizedTest
    @CsvSource({"/login,READER,/shop/books", "/admin/login,ADMIN,/admin/books"})
    void eachLoginEntersItsOwnArea(String route, String role, String destination) throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setRole(role);
        when(userService.login("demo", "123456")).thenReturn(user);
        mvc.perform(post(route).param("username", "demo").param("password", "123456"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(destination));
    }

    @ParameterizedTest
    @CsvSource({"/login,ADMIN,login", "/admin/login,READER,admin/login"})
    void wrongRoleCannotLoginThroughOtherEntrance(String route, String role, String view) throws Exception {
        User user = new User();
        user.setRole(role);
        when(userService.login("demo", "123456")).thenReturn(user);
        mvc.perform(post(route).param("username", "demo").param("password", "123456"))
                .andExpect(status().isOk()).andExpect(view().name(view))
                .andExpect(model().attributeExists("error"))
                .andExpect(request().sessionAttributeDoesNotExist(AuthInterceptor.SESSION_KEY));
    }

    @Test
    void homeOffersBothEntrancesAndAdminAreaRemainsProtected() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("home"))
                .andExpect(content().string(containsString("href=\"/admin/entry\"")))
                .andExpect(content().string(containsString("href=\"/reader/entry\"")))
                .andExpect(result -> assertThat(breadcrumbHtml(result)).contains("aria-current=\"page\">主界面</span>"));
        mvc.perform(get("/admin/books")).andExpect(redirectedUrl("/admin/login"));
    }

    @ParameterizedTest
    @CsvSource({"/reader/entry,读者入口,,2", "/admin/entry,管理员入口,,2",
            "/login,读者入口,登录,3", "/register,读者入口,注册,3",
            "/admin/login,管理员入口,登录,3", "/admin/register,管理员入口,注册,3"})
    void breadcrumbsShowHierarchyWithWorkingParentLinks(String page, String entryLabel, String current, int count) throws Exception {
        String html = breadcrumbHtml(mvc.perform(get(page)).andExpect(status().isOk()).andReturn());
        assertThat(Pattern.compile("<li>").matcher(html).results().count()).isEqualTo(count);
        assertThat(html).contains("<a href=\"/\">主界面</a>")
                .contains("aria-current=\"page\">" + (current == null ? entryLabel : current) + "</span>");
        if (current != null) {
            String entryUrl = page.startsWith("/admin/") ? "/admin/entry" : "/reader/entry";
            assertThat(html).contains("<a href=\"" + entryUrl + "\">" + entryLabel + "</a>");
        }
    }

    @ParameterizedTest
    @CsvSource({"ADMIN", "READER"})
    void loggedInUsersCanAlsoReturnToHome(String role) throws Exception {
        mvc.perform(get("/").sessionAttr(AuthInterceptor.SESSION_KEY, new LoginUser(1L, "demo", role)))
                .andExpect(status().isOk()).andExpect(view().name("home"));
    }

    @ParameterizedTest
    @CsvSource({"/logout,READER,/reader/entry", "/admin/logout,ADMIN,/admin/entry"})
    void logoutClearsSessionAndReturnsToEntrance(String route, String role, String entry) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthInterceptor.SESSION_KEY, new LoginUser(1L, "demo", role));
        mvc.perform(get(route).session(session)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(entry));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get(entry)).andExpect(status().isOk())
                .andExpect(result -> assertThat(breadcrumbHtml(result)).contains("<a href=\"/\">主界面</a>"));
    }

    private String breadcrumbHtml(MvcResult result) throws Exception {
        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var matcher = Pattern.compile("<nav class=\"breadcrumbs\"[^>]*>(.*?)</nav>", Pattern.DOTALL).matcher(html);
        assertThat(matcher.find()).as("page contains the breadcrumb navigation").isTrue();
        return matcher.group(1);
    }
}
