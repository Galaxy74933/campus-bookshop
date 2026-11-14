package com.example.bookshop.service;

import com.example.bookshop.dto.BookForm;
import com.example.bookshop.dto.CourseForm;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.dto.MajorForm;
import com.example.bookshop.entity.Course;
import com.example.bookshop.entity.Major;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void repeatedDuplicateNamesAreRejectedBeforeInsert() {
        major("软件工程");
        course("编译原理");
        clearInvocations(mapper);
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> major(" 软件工程 ")).isInstanceOf(BizException.class).hasMessageContaining("已存在");
            assertThatThrownBy(() -> course(" 编译原理 ")).isInstanceOf(BizException.class).hasMessageContaining("已存在");
        }
        verify(mapper, never()).insertMajor(any(Major.class));
        verify(mapper, never()).insertCourse(any(Course.class));
        major("计算机科学与技术");
        course("操作系统");
        assertThat(academic.majors()).hasSize(2);
        assertThat(academic.courses()).hasSize(2);
    }

    @Test
    void editsAllowOwnNameButRejectOtherNamesWithoutChangingRelations() {
        Long software = major("软件工程"), computer = major("计算机科学与技术");
        Long compiler = course("编译原理", software);
        course("操作系统", computer);
        MajorForm form = new MajorForm();
        form.setName(" 软件工程 ");
        academic.saveMajor(software, form);
        academic.saveCourse(compiler, courseForm(" 编译原理 ", List.of(software)));
        form.setName("计算机科学与技术");
        assertThatThrownBy(() -> academic.saveMajor(software, form)).isInstanceOf(BizException.class).hasMessageContaining("已存在");
        assertThatThrownBy(() -> academic.saveCourse(compiler, courseForm("操作系统", List.of(computer))))
                .isInstanceOf(BizException.class).hasMessageContaining("已存在");
        assertThat(academic.major(software).getName()).isEqualTo("软件工程");
        assertThat(academic.course(compiler).getName()).isEqualTo("编译原理");
        assertThat(academic.course(compiler).getMajors()).extracting("id").containsExactly(software);
    }

    @Test
    void concurrentDuplicateInsertStillProducesFriendlyError() {
        doThrow(new DuplicateKeyException("simulated concurrent insert")).when(mapper).insertMajor(any(Major.class));
        doThrow(new DuplicateKeyException("simulated concurrent insert")).when(mapper).insertCourse(any(Course.class));
        assertThatThrownBy(() -> major("软件工程")).isInstanceOf(BizException.class).hasMessageContaining("专业名称已存在");
        assertThatThrownBy(() -> course("编译原理")).isInstanceOf(BizException.class).hasMessageContaining("课程名称已存在");
    }

    private void assertRowNumbers(String html, int first, int count) {
        assertThat(html).contains("<th>序号</th>").doesNotContain("<th>ID</th>");
        List<Integer> numbers = Pattern.compile("<tr[^>]*>\\s*<td[^>]*>(\\d+)</td>")
                .matcher(html).results().map(match -> Integer.valueOf(match.group(1))).toList();
        assertThat(numbers).containsExactlyElementsOf(IntStream.range(first, first + count).boxed().toList());
    }

    @Test
    void academicListsShowContinuousNumbersAndKeepActualIdsInActions() throws Exception {
        jdbc.update("INSERT INTO t_major (id, name) VALUES (101, '软件工程'), (106, '计算机科学与技术')");
        jdbc.update("INSERT INTO t_course (id, name) VALUES (201, '编译原理'), (206, '操作系统')");
        for (String kind : List.of("majors", "courses")) {
            long firstId = kind.equals("majors") ? 101 : 201;
            String path = "/admin/" + kind;
            String html = mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertRowNumbers(html, 1, 2);
            assertThat(html).contains("href=\"" + path + "/" + (firstId + 5) + "/edit\"",
                    "action=\"" + path + "/" + firstId + "/delete\"");
            mvc.perform(post(path + "/" + (firstId + 5) + "/delete").sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                    .andExpect(redirectedUrl(path));
            String afterDelete = mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertRowNumbers(afterDelete, 1, 1);
            assertThat(afterDelete).contains("href=\"" + path + "/" + firstId + "/edit\"");
        }
    }

    @Test
    void bookNumbersContinueAcrossPagesAndRestartForSearchWithIdGaps() throws Exception {
        Long newestId = null;
        for (int i = 1; i <= 12; i++) {
            books.delete(book("临时图书"));
            newestId = book("教材-" + i);
        }
        for (String path : List.of("/admin/books", "/shop/books")) {
            boolean isAdmin = path.startsWith("/admin");
            LoginUser user = isAdmin ? admin() : new LoginUser(2L, "reader", "READER");
            int size = isAdmin ? 10 : 8;
            String firstPage = mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY, user))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertRowNumbers(firstPage, 1, size);
            if (isAdmin) {
                assertThat(firstPage).contains("href=\"/admin/books/" + newestId + "/edit\"");
            } else {
                assertThat(firstPage).contains("name=\"bookId\" value=\"" + newestId + "\"");
            }
            String secondPage = mvc.perform(get(path).param("page", "2").sessionAttr(AuthInterceptor.SESSION_KEY, user))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertRowNumbers(secondPage, size + 1, 12 - size);
            String filtered = mvc.perform(get(path).param("keyword", "教材-12").sessionAttr(AuthInterceptor.SESSION_KEY, user))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertRowNumbers(filtered, 1, 1);
        }
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
    void overviewGroupsSharedCoursesAndIncludesUnlinkedRecords() throws Exception {
        Long software = major("软件工程"), computer = major("计算机科学与技术");
        major("数学专业");
        Long shared = course("Java 程序设计", software, computer);
        course("编译原理", software);
        course("操作系统", computer);
        Long unassigned = course("大学英语");
        String html = mvc.perform(get("/admin/academic-overview").sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andExpect(view().name("admin/academic-overview"))
                .andExpect(model().attribute("majorCount", 3)).andExpect(model().attribute("courseCount", 4))
                .andReturn().getResponse().getContentAsString();
        var groups = Pattern.compile("<tbody[^>]*class=\"overview-group\"[^>]*>(.*?)</tbody>", Pattern.DOTALL)
                .matcher(html).results().map(match -> match.group(1)).toList();
        assertThat(groups).hasSize(3);
        assertThat(groups.get(0)).contains("数学专业", "暂未关联课程");
        assertThat(groups.get(1)).contains("计算机科学与技术", "操作系统", "Java 程序设计", "rowspan=\"2\"").doesNotContain("编译原理");
        assertThat(groups.get(2)).contains("软件工程", "编译原理", "Java 程序设计", "rowspan=\"2\"").doesNotContain("操作系统");
        assertThat(Pattern.compile("共享 · 2个专业").matcher(html).results().count()).isEqualTo(2);
        assertThat(html).contains("未关联专业的课程", "大学英语", "href=\"/admin/courses/" + shared + "/edit\"",
                "href=\"/admin/courses/" + unassigned + "/edit\"", "aria-current=\"page\">专业课程总览");
    }

    @Test
    void overviewSearchKeepsEntireMatchingMajorsAndHandlesEmptyOrUnsafeInput() throws Exception {
        Long software = major("软件工程"), computer = major("计算机科学与技术");
        course("Java 程序设计", software, computer);
        course("编译原理", software);
        course("操作系统", computer);
        course("大学英语");
        String sharedSearch = mvc.perform(get("/admin/academic-overview").param("keyword", " jAvA ")
                        .sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andExpect(model().attribute("keyword", "jAvA"))
                .andReturn().getResponse().getContentAsString();
        assertThat(sharedSearch).contains("软件工程", "计算机科学与技术", "编译原理", "操作系统").doesNotContain("大学英语");
        String majorSearch = mvc.perform(get("/admin/academic-overview").param("keyword", "软件")
                        .sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(majorSearch).contains("编译原理", "Java 程序设计").doesNotContain("计算机科学与技术", "操作系统", "大学英语");
        String orphanSearch = mvc.perform(get("/admin/academic-overview").param("keyword", "英语")
                        .sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(orphanSearch).contains("大学英语", "没有匹配的专业").doesNotContain("编译原理");
        String blankSearch = mvc.perform(get("/admin/academic-overview").param("keyword", "   ")
                        .sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(blankSearch).contains("软件工程", "计算机科学与技术", "大学英语");
        String noMatch = mvc.perform(get("/admin/academic-overview").param("keyword", "<script>alert(1)</script>")
                        .sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(noMatch).contains("没有匹配的专业", "没有匹配的未关联专业课程", "&lt;script&gt;")
                .doesNotContain("<script>");
    }

    @Test
    void overviewReflectsRelationChangesWithoutSeparateDataMaintenance() throws Exception {
        Long software = major("软件工程"), course = course("编译原理");
        academic.saveCourse(course, courseForm("编译原理", List.of(software)));
        String linked = mvc.perform(get("/admin/academic-overview").sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(linked).contains("编译原理", "暂无未关联专业的课程。").doesNotContain("暂未关联课程");
        academic.saveCourse(course, courseForm("编译原理", List.of()));
        String unlinked = mvc.perform(get("/admin/academic-overview").sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(unlinked).contains("暂未关联课程", "编译原理").doesNotContain("暂无未关联专业的课程。");
    }

    @Test
    void adminPagesRenderAndEditFormsRememberSelections() throws Exception {
        Long major = major("软件工程"), course = course("Java 程序设计", major), book = book("Java 教材", course);
        for (String path : List.of("/admin/majors", "/admin/majors/add", "/admin/majors/" + major + "/edit",
                "/admin/courses", "/admin/courses/add", "/admin/courses/" + course + "/edit",
                "/admin/books", "/admin/books/add", "/admin/books/" + book + "/edit", "/admin/academic-overview")) {
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
        for (String path : List.of("/admin/majors", "/admin/courses", "/admin/courses/add", "/admin/books/add", "/admin/academic-overview")) {
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

    @Test
    void overviewIsAdminOnlyAndDoesNotAcceptWrites() throws Exception {
        mvc.perform(get("/admin/academic-overview")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(get("/admin/academic-overview")
                        .sessionAttr(AuthInterceptor.SESSION_KEY, new LoginUser(2L, "reader", "READER")))
                .andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post("/admin/academic-overview").sessionAttr(AuthInterceptor.SESSION_KEY, admin()))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(org.springframework.web.HttpRequestMethodNotSupportedException.class));
        assertThat(academic.majors()).isEmpty();
        assertThat(academic.courses()).isEmpty();
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
