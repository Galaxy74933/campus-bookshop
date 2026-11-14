package com.example.bookshop.service;

import com.example.bookshop.dto.BookForm;
import com.example.bookshop.dto.CourseForm;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.dto.MajorForm;
import com.example.bookshop.entity.Course;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.mapper.AcademicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:academic;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.sql.init.data-locations=classpath:db/academic-test-data.sql",
        "logging.level.com.example.bookshop.mapper=WARN"})
@AutoConfigureMockMvc
class AcademicFeatureTest {
    @Autowired AcademicService academic;
    @Autowired BookService books;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired MockMvc mvc;
    @SpyBean AcademicMapper mapper;

    private LoginUser admin() { return new LoginUser(1L, "test-admin", "ADMIN"); }

    @BeforeEach
    void resetIsolatedDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:academic");
        }
        for (String table : List.of("t_book_course", "t_major_course", "t_course", "t_major", "t_book")) {
            jdbc.execute("DELETE FROM " + table);
        }
    }

    private Long major(String name) {
        MajorForm form = new MajorForm();
        form.setName(name);
        academic.saveMajor(null, form);
        return academic.majors().getFirst().getId();
    }

    private CourseForm courseForm(String name, List<Long> majorIds) {
        CourseForm form = new CourseForm();
        form.setName(name);
        form.setMajorIds(majorIds);
        return form;
    }

    private Long course(String name, Long... majorIds) {
        academic.saveCourse(null, courseForm(name, List.of(majorIds)));
        return academic.courses().getFirst().getId();
    }

    private BookForm bookForm(String title, List<Long> courseIds) {
        BookForm form = new BookForm();
        form.setTitle(title);
        form.setAuthor("作者");
        form.setPrice(new BigDecimal("25.50"));
        form.setStock(8);
        form.setCourseIds(courseIds);
        return form;
    }

    private Long book(String title, Long... courseIds) {
        books.create(bookForm(title, List.of(courseIds)));
        return jdbc.queryForObject("SELECT id FROM t_book WHERE title = ?", Long.class, title);
    }

    @Test
    void manyToManyAssociationsPersistAndCanBeCleared() {
        Long software = major("软件工程"), computer = major("计算机科学与技术");
        Long java = course("Java 程序设计", software, computer, software);
        Long web = course("Web 开发", software);
        Long book = book("Java 教材", java, web, java);
        assertThat(academic.course(java).getMajors()).extracting("id").containsExactlyInAnyOrder(software, computer);
        assertThat(academic.courseIdsForBook(book)).containsExactlyInAnyOrder(java, web);
        assertThat(academic.coursesForBook(book)).extracting(Course::getName).containsExactlyInAnyOrder("Java 程序设计", "Web 开发");
        academic.saveCourse(java, courseForm("Java 基础", List.of(computer)));
        assertThat(academic.course(java).getMajors()).extracting("id").containsExactly(computer);
        assertThat(academic.courseIdsForBook(book)).hasSize(2);
        academic.saveCourse(java, courseForm("Java 基础", List.of()));
        books.update(book, bookForm("Java 教材", List.of()));
        assertThat(academic.course(java).getMajors()).isEmpty();
        assertThat(academic.courseIdsForBook(book)).isEmpty();
        assertThat(books.findById(book).getStock()).isEqualTo(8);
    }

    @Test
    void duplicateAndInvalidNamesAreRejected() {
        major(" 软件工程 ");
        assertThat(academic.majors().getFirst().getName()).isEqualTo("软件工程");
        assertThatThrownBy(() -> major("软件工程")).isInstanceOf(BizException.class).hasMessageContaining("已存在");
        assertThatThrownBy(() -> major("   ")).isInstanceOf(BizException.class);
        course("Java");
        assertThatThrownBy(() -> course(" Java ")).isInstanceOf(BizException.class).hasMessageContaining("已存在");
        assertThatThrownBy(() -> course("x".repeat(101))).isInstanceOf(BizException.class);
    }

    @Test
    void invalidIdsCannotCreatePartialBooksOrCourses() {
        Long existing = book("已有图书");
        assertThatThrownBy(() -> books.create(bookForm("非法关联", List.of(Long.MAX_VALUE)))).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> books.update(existing, bookForm("不应改名", List.of(-1L)))).isInstanceOf(BizException.class);
        assertThat(books.findById(existing).getTitle()).isEqualTo("已有图书");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_book", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> course("非法课程", Long.MAX_VALUE)).isInstanceOf(BizException.class);
        assertThat(academic.courses()).isEmpty();
    }

    @Test
    void referencesBlockDeletionAndDeletingBookCleansOnlyItsLinks() {
        Long major = major("软件工程"), course = course("Java", major), book = book("教材", course);
        assertThatThrownBy(() -> academic.deleteMajor(major)).isInstanceOf(BizException.class).hasMessageContaining("关联");
        assertThatThrownBy(() -> academic.deleteCourse(course)).isInstanceOf(BizException.class).hasMessageContaining("关联");
        books.delete(book);
        assertThat(academic.courseIdsForBook(book)).isEmpty();
        assertThat(academic.course(course).getName()).isEqualTo("Java");
        academic.saveCourse(course, courseForm("Java", List.of()));
        academic.deleteCourse(course);
        academic.deleteMajor(major);
        assertThat(academic.courses()).isEmpty();
        assertThat(academic.majors()).isEmpty();
    }

    @Test
    void failedRelationWriteRollsBackBookAndPreviousRelations() {
        Long java = course("Java"), web = course("Web");
        Long existing = book("原书名", java);
        doThrow(new DataIntegrityViolationException("simulated concurrent relation change"))
                .when(mapper).insertBookCourse(anyLong(), eq(web));
        assertThatThrownBy(() -> books.update(existing, bookForm("不应保存", List.of(java, web))))
                .isInstanceOf(BizException.class);
        assertThat(books.findById(existing).getTitle()).isEqualTo("原书名");
        assertThat(academic.courseIdsForBook(existing)).containsExactly(java);
        assertThatThrownBy(() -> books.create(bookForm("不应创建", List.of(java, web)))).isInstanceOf(BizException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_book", Integer.class)).isEqualTo(1);
    }

    @Test
    void schemaCanRunAgainWithoutLosingExistingData() {
        Long major = major("软件工程"), course = course("Java", major), book = book("保留图书", course);
        new ResourceDatabasePopulator(new ClassPathResource("db/academic-schema.sql")).execute(dataSource);
        assertThat(academic.courseIdsForBook(book)).containsExactly(course);
        assertThat(academic.major(major).getName()).isEqualTo("软件工程");
    }

    @Test
    void adminPagesRenderAndEditFormsRememberSelections() throws Exception {
        Long major = major("软件工程"), course = course("Java 程序设计", major), book = book("Java 教材", course);
        for (String path : List.of("/admin/majors", "/admin/majors/add", "/admin/majors/" + major + "/edit",
                "/admin/courses", "/admin/courses/add", "/admin/courses/" + course + "/edit",
                "/admin/books", "/admin/books/add", "/admin/books/" + book + "/edit")) {
            mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("aria-label=\"当前位置\"")))
                    .andExpect(content().string(containsString("href=\"/admin/majors\"")));
        }
        mvc.perform(get("/admin/courses/" + course + "/edit").sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(content().string(containsString("checked=\"checked\"")));
        mvc.perform(get("/admin/books/" + book + "/edit").sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(content().string(containsString("checked=\"checked\"")));
    }

    @Test
    void emptyListsAndEmptySelectionsWork() throws Exception {
        for (String path : List.of("/admin/majors", "/admin/courses", "/admin/courses/add", "/admin/books/add")) {
            mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("暂无")));
        }
        Long major = major("软件工程"), course = course("Java", major), book = book("教材", course);
        mvc.perform(post("/admin/courses/" + course).sessionAttr(AuthInterceptor.SESSION_KEY, admin())
                        .param("name", "Java").param("_majorIds", "on"))
                .andExpect(redirectedUrl("/admin/courses"));
        mvc.perform(post("/admin/books/" + book).sessionAttr(AuthInterceptor.SESSION_KEY, admin())
                        .param("title", "教材").param("price", "25.50").param("stock", "8").param("_courseIds", "on"))
                .andExpect(redirectedUrl("/admin/books"));
        assertThat(academic.course(course).getMajors()).isEmpty();
        assertThat(academic.courseIdsForBook(book)).isEmpty();
    }

    @Test
    void duplicateAndBindingErrorsStayOnTheirForm() throws Exception {
        Long major = major("软件工程");
        course("Java", major);
        mvc.perform(post("/admin/courses").sessionAttr(AuthInterceptor.SESSION_KEY, admin())
                        .param("name", "Java").param("majorIds", major.toString()))
                .andExpect(view().name("admin/course-form"))
                .andExpect(content().string(containsString("课程名称已存在")))
                .andExpect(content().string(containsString("checked=\"checked\"")));
        mvc.perform(post("/admin/courses").sessionAttr(AuthInterceptor.SESSION_KEY, admin())
                        .param("name", "新课程").param("majorIds", "bad-id"))
                .andExpect(view().name("admin/course-form")).andExpect(model().attributeExists("error"));
        mvc.perform(post("/admin/books").sessionAttr(AuthInterceptor.SESSION_KEY, admin())
                        .param("title", "新书").param("price", "25.50").param("stock", "8").param("courseIds", "bad-id"))
                .andExpect(view().name("admin/book-form")).andExpect(model().attributeExists("error"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/majors", "/admin/courses", "/admin/majors/add", "/admin/courses/add"})
    void nonAdminsCannotReadOrWriteAcademicData(String path) throws Exception {
        mvc.perform(get(path)).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY, new LoginUser(2L, "reader", "READER")))
                .andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post(path).param("name", "不应创建")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post(path).param("name", "不应创建")
                        .sessionAttr(AuthInterceptor.SESSION_KEY, new LoginUser(2L, "reader", "READER")))
                .andExpect(redirectedUrl("/admin/login"));
        assertThat(academic.majors()).isEmpty();
        assertThat(academic.courses()).isEmpty();
    }
}
