package com.example.bookshop.service;

import com.example.bookshop.dto.*;
import com.example.bookshop.entity.*;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:catalog;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.sql.init.data-locations=classpath:db/academic-test-data.sql","logging.level.root=WARN",
 "logging.level.com.example.bookshop.mapper=WARN","logging.level.org.springframework=WARN"})
@AutoConfigureMockMvc
class CatalogFeatureTest {
 @Autowired CategoryService categories;
 @Autowired DemoResourceService demo;
 @Autowired DemoReadingListService readingDemo;
 @Autowired BookService books;
 @Autowired ReadingListService lists;
 @Autowired UserMapper users;
 @Autowired BookMapper bookMapper;
 @Autowired CartItemMapper carts;
 @Autowired OrderService orders;
 @Autowired CartService cartService;
 @Autowired JdbcTemplate jdbc;
 @Autowired DataSource ds;
 @Autowired MockMvc mvc;
 LoginUser admin,reader,other;
 @BeforeEach void reset() throws Exception {
  try(var c=ds.getConnection()) { assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:catalog"); }
  for(String table:List.of("t_reading_list_book","t_reading_list","t_book_category","t_cart_item","t_order_item","t_order","t_book","t_category","t_user","t_app_migration"))
   jdbc.execute("DELETE FROM "+table);
  categories.initializeDefaults();
  admin=user("admin","ADMIN"); reader=user("reader","READER"); other=user("other","READER");
 }
 LoginUser user(String name,String role) {
  User u=new User(); u.setUsername(name); u.setPassword("123456"); u.setRole(role); u.setBalance(new BigDecimal("1000"));
  users.insert(u); return new LoginUser(u.getId(),name,role);
 }
 Long category() { return categories.list().getFirst().getId(); }
 BookForm form(String title,Long category,int stock) {
  BookForm f=new BookForm(); f.setTitle(title); f.setAuthor("测试作者"); f.setPrice(new BigDecimal("25.50")); f.setStock(stock); f.setCategoryId(category); return f;
 }
 Long book(String title,int stock) {
  books.create(form(title,category(),stock));
  return jdbc.queryForObject("SELECT id FROM t_book WHERE title=?",Long.class,title);
 }
 String page(String path,LoginUser user) throws Exception {
  return mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY,user)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
 }
 @Test void categoriesUniqueReferencedAndInitializationOnlyOnce() {
  assertThat(categories.list()).hasSize(8);
  Long id=category(); String original=categories.require(id).getName();
  assertThatThrownBy(()->categories.save(null," "+original+" ")).isInstanceOf(BizException.class);
  categories.save(id,original);
  Long b=book("引用分类",6);
  assertThatThrownBy(()->categories.delete(id)).isInstanceOf(BizException.class).hasMessageContaining("已有图书");
  Long unused=categories.list().getLast().getId(); categories.delete(unused);
  categories.save(id,"已重命名");
  categories.initializeDefaults();
  assertThat(categories.list()).hasSize(7);
  assertThat(books.findById(b).getCategoryName()).isEqualTo("已重命名");
 }
 @Test void oldBooksSurviveRepeatedSchemaAndRequireCategoryOnEdit() {
  Book b=new Book(); b.setTitle("旧教材"); b.setAuthor("旧作者"); b.setPrice(new BigDecimal("10")); b.setStock(7); bookMapper.insert(b);
  new ResourceDatabasePopulator(new ClassPathResource("db/catalog-schema.sql")).execute(ds);
  categories.initializeDefaults();
  assertThat(books.findById(b.getId()).getCategoryName()).isEqualTo("待分类");
  assertThatThrownBy(()->books.update(b.getId(),form("不应修改",null,3))).isInstanceOf(BizException.class);
  assertThat(books.findById(b.getId()).getTitle()).isEqualTo("旧教材");
  BookQuery q=new BookQuery(); q.setCategoryId(0L);
  assertThat(books.search(q,10,false).getRecords()).extracting(Book::getId).containsExactly(b.getId());
  books.update(b.getId(),form("旧教材",category(),7));
  assertThat(books.search(q,10,false).getTotal()).isZero();
 }
 @Test void invalidCategoriesAndAmountsCannotPartiallyWrite() {
  assertThatThrownBy(()->books.create(form("坏分类",Long.MAX_VALUE,3))).isInstanceOf(BizException.class);
  BookForm f=form("坏价格",category(),3); f.setPrice(new BigDecimal("-1"));
  assertThatThrownBy(()->books.create(f)).isInstanceOf(BizException.class);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_book",Integer.class)).isZero();
 }
 @Test void searchCombinesFieldCategorySortingAndPagination() {
  Long c1=category(),c2=categories.list().getLast().getId();
  for(int i=1;i<=12;i++) {
   BookForm f=form("Java "+i,c1,6); f.setPrice(BigDecimal.valueOf(i)); f.setAuthor("Alice"); books.create(f);
  }
  books.create(form("Java Other",c2,6));
  BookQuery q=new BookQuery(); q.setKeyword(" ALICE "); q.setField("author"); q.setCategoryId(c1); q.setSort("priceAsc"); q.setPage(2);
  var result=books.search(q,8,false);
  assertThat(result.getTotal()).isEqualTo(12); assertThat(result.getRecords()).hasSize(4);
  assertThat(result.getRecords().getFirst().getPrice()).isEqualByComparingTo("9");
  q.setSort("priceDesc"); q.setPage(1);
  assertThat(books.search(q,8,false).getRecords().getFirst().getPrice()).isEqualByComparingTo("12");
  q.setField("title"); assertThat(books.search(q,8,false).getTotal()).isZero();
  q.setKeyword("%"); assertThat(books.search(q,8,false).getTotal()).isZero();
  q.setKeyword(""); q.setCategoryId(0L); assertThat(books.search(q,8,false).getRecords()).isEmpty();
 }
 @Test void stockBoundariesAndPurchaseUpdateWarning() {
  book("零",0); book("一",1); book("五",5); Long six=book("六",6);
  BookQuery q=new BookQuery(); q.setStockStatus("low");
  assertThat(books.search(q,10,true).getRecords()).extracting(Book::getTitle).containsExactlyInAnyOrder("一","五");
  q.setStockStatus("empty"); assertThat(books.search(q,10,true).getRecords()).extracting(Book::getTitle).containsExactly("零");
  carts.upsert(reader.getId(),six,1); orders.checkout(reader.getId());
  q.setStockStatus("low"); assertThat(books.search(q,10,true).getTotal()).isEqualTo(3);
  q.setStockStatus("empty"); assertThat(books.search(q,10,false).getTotal()).isEqualTo(4);
 }
 @Test void listOwnershipDuplicateNamesMembershipAndDeletion() {
  Long b=book("书单图书",4);
  Long pub=lists.save(null,"推荐","说明",admin),mine=lists.save(null,"私人","",reader);
  lists.save(null,"私人","",other);
  assertThat(lists.visible(other)).extracting(ReadingList::getId).doesNotContain(mine).contains(pub);
  assertThatThrownBy(()->lists.require(mine,other,false)).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->lists.require(mine,admin,false)).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->lists.add(pub,b,reader)).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->lists.save(mine,"偷改","",other)).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->lists.delete(mine,other)).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->lists.save(null," 私人 ","",reader)).isInstanceOf(BizException.class);
  lists.save(mine,"私人","更新说明",reader);
  lists.add(mine,b,reader);
  assertThatThrownBy(()->lists.add(mine,b,reader)).isInstanceOf(BizException.class);
  lists.add(pub,b,admin); lists.delete(mine,reader);
  assertThat(books.findById(b)).isNotNull();
  assertThat(lists.books(pub,reader)).extracting(Book::getId).containsExactly(b);
  lists.remove(pub,b,admin); assertThat(lists.books(pub,reader)).isEmpty();
 }
 @Test void deleteBookCleansListsAndCartButKeepsOrderSnapshot() {
  Long b=book("保留订单快照",6),list=lists.save(null,"书单","",reader); lists.add(list,b,reader);
  carts.upsert(reader.getId(),b,1); var order=orders.checkout(reader.getId());
  carts.upsert(other.getId(),b,1);
  books.delete(b);
  assertThat(lists.books(list,reader)).isEmpty();
  assertThat(carts.findByUserId(other.getId())).isEmpty();
  assertThat(jdbc.queryForObject("SELECT book_title FROM t_order_item WHERE order_id=?",String.class,order.getId())).isEqualTo("保留订单快照");
 }
 @Test void allPagesRenderWithCorrectNavigationAndActions() throws Exception {
  Long b=book("Java 教材",5),pub=lists.save(null,"公开推荐","说明",admin),mine=lists.save(null,"我的学习","私人说明",reader);
  lists.add(pub,b,admin); lists.add(mine,b,reader);
  for(String p:List.of("/admin/books","/admin/books/add","/admin/books/"+b+"/edit","/admin/categories",
    "/admin/reading-lists","/admin/reading-lists/add","/admin/reading-lists/"+pub,"/admin/reading-lists/"+pub+"/edit","/admin/reading-lists/choose?bookId="+b)) {
   String html=page(p,admin);
   assertThat(html).contains("分类管理","书单管理","aria-label=\"当前位置\"").doesNotContain("专业管理","课程管理","专业课程总览",">我的订单</a>");
  }
  for(String p:List.of("/shop/books","/shop/reading-lists","/shop/reading-lists/add","/shop/reading-lists/"+pub,
    "/shop/reading-lists/"+mine,"/shop/reading-lists/"+mine+"/edit","/shop/reading-lists/choose?bookId="+b)) {
   String html=page(p,reader);
   assertThat(html).contains("学习书单","我的订单","aria-label=\"当前位置\"").doesNotContain(">分类管理</a>",">书单管理</a>");
  }
  assertThat(page("/shop/reading-lists/"+pub,reader)).doesNotContain("编辑名称及说明","从书单移除");
  assertThat(page("/admin/books?stockStatus=low",admin)).contains("库存偏低","Java 教材");
 }
 @Test void paginationKeepsFiltersAndShowsContinuousNumbers() throws Exception {
  for(int i=0;i<12;i++) book("分页书"+i,6);
  String html=page("/shop/books?keyword=测试作者&field=author&categoryId="+category()+"&sort=priceAsc&page=2",reader);
  assertThat(html).contains("<td>9</td>","field=author","sort=priceAsc","categoryId="+category()).doesNotContain("<th>ID</th>");
  assertThat(page("/admin/books?page=2",admin)).contains("<td>11</td>");
 }
 @Test void webRejectsForeignPrivateListsAndReaderWritesToPublic() throws Exception {
  Long mine=lists.save(null,"私密内容","",reader),pub=lists.save(null,"公开","",admin),b=book("图书",5);
  mvc.perform(get("/shop/reading-lists/"+mine).sessionAttr(AuthInterceptor.SESSION_KEY,other)).andExpect(status().isForbidden());
  mvc.perform(get("/admin/reading-lists/"+mine).sessionAttr(AuthInterceptor.SESSION_KEY,admin)).andExpect(status().isForbidden());
  mvc.perform(post("/shop/reading-lists/"+pub+"/books").param("bookId",b.toString()).sessionAttr(AuthInterceptor.SESSION_KEY,reader)).andExpect(status().isForbidden());
  mvc.perform(post("/shop/reading-lists/"+mine+"/delete").sessionAttr(AuthInterceptor.SESSION_KEY,other)).andExpect(status().isForbidden());
  for(String path:List.of("/admin/categories","/admin/reading-lists","/admin/books")) {
   mvc.perform(get(path)).andExpect(redirectedUrl("/admin/login"));
   mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY,reader)).andExpect(redirectedUrl("/admin/login"));
  }
  for(String path:List.of("/admin/majors","/admin/courses","/admin/academic-overview"))
   mvc.perform(get(path).sessionAttr(AuthInterceptor.SESSION_KEY,admin)).andExpect(status().isNotFound());
 }
 @Test void formErrorsStayVisibleAndEmptyPagesRender() throws Exception {
  assertThat(page("/shop/books",reader)).contains("没有找到相关资源");
  assertThat(page("/shop/reading-lists",reader)).contains("暂无公开书单","还没有个人书单");
  mvc.perform(post("/admin/books").sessionAttr(AuthInterceptor.SESSION_KEY,admin)
   .param("title","教材").param("stock","2").param("price","20"))
   .andExpect(view().name("admin/book-form")).andExpect(model().attributeExists("error"));
  mvc.perform(post("/shop/reading-lists").sessionAttr(AuthInterceptor.SESSION_KEY,reader)
   .param("name"," ").param("description","说明"))
   .andExpect(view().name("reading/form")).andExpect(model().attributeExists("error"));
 }

 @Test void resourceMetadataAndMultipleCategoriesPersistWithoutDuplicates() {
  Long main=category(),extra=categories.list().getLast().getId();
  BookForm f=form("交叉学科教材",main,4); f.setCategoryIds(List.of(main,extra,extra));
  f.setLanguage("中外文对照"); f.setAuthorUnit("示例大学"); f.setPublisher("示例出版社");
  f.setPublishDate(java.time.LocalDate.of(2025,9,1)); f.setIdentifier("978-1-23456-789-0"); f.setDescription("内容简介");
  books.create(f); Long id=jdbc.queryForObject("SELECT id FROM t_book WHERE title=?",Long.class,f.getTitle());
  Book b=books.findById(id);
  assertThat(b.getCategoryIds()).containsExactly(extra);
  assertThat(b.getCategoryName()).contains(categories.require(main).getName(),categories.require(extra).getName());
  assertThat(b.getLanguage()).isEqualTo("中外文对照"); assertThat(b.getAuthorUnit()).isEqualTo("示例大学");
  assertThat(b.getPublishDate()).isEqualTo(java.time.LocalDate.of(2025,9,1));
  assertThat(categories.list().stream().filter(c->c.getId().equals(extra)).findFirst().orElseThrow().getBookCount()).isEqualTo(1);
  assertThatThrownBy(()->categories.delete(extra)).isInstanceOf(BizException.class);
  BookQuery q=new BookQuery(); q.setCategoryIds(List.of(main,extra));
  assertThat(books.search(q,10,false).getRecords()).extracting(Book::getId).containsExactly(id);
  f.setCategoryIds(List.of()); f.setLanguage(""); books.update(id,f);
  assertThat(books.findById(id).getCategoryIds()).isEmpty();
  assertThat(books.findById(id).getLanguage()).isEmpty(); categories.delete(extra);
 }
 @Test void unionAcrossFacetsAndKeywordNarrowingMatchUserExample() {
  Long computer=category(),otherCategory=categories.list().getLast().getId();
  BookForm f=form("Java 中文教材",computer,6); f.setLanguage("中文"); books.create(f);
  f=form("Java 外文教材",otherCategory,6); f.setLanguage("外文"); books.create(f);
  f=form("Java 外文计算机",computer,6); f.setLanguage("外文"); books.create(f);
  f=form("不相关中文",otherCategory,6); f.setLanguage("中文"); books.create(f);
  BookQuery q=new BookQuery(); q.setCategoryIds(List.of(computer)); q.setLanguages(List.of("外文"));
  assertThat(books.search(q,10,false).getRecords()).extracting(Book::getTitle)
   .containsExactlyInAnyOrder("Java 中文教材","Java 外文教材","Java 外文计算机");
  q.setKeyword("中文"); assertThat(books.search(q,10,false).getRecords()).extracting(Book::getTitle).containsExactly("Java 中文教材");
  q.setKeyword(""); q.setCategoryIds(List.of()); q.setLanguages(List.of("中文","外文"));
  assertThat(books.search(q,10,false).getTotal()).isEqualTo(4);
 }
 @Test void resourceTypesValidationAndOptionalFields() {
  BookForm f=form("论文可不填销售信息",category(),0); f.setResourceType("论文"); f.setPrice(null); f.setStock(null); f.setAuthor(null);
  books.create(f);
  Book paper=books.search(new BookQuery(),10,false).getRecords().getFirst();
  assertThat(paper.isPurchasable()).isFalse(); assertThat(paper.getPrice()).isEqualByComparingTo("0"); assertThat(paper.getStock()).isZero();
  f.setResourceType("图书"); assertThatThrownBy(()->books.create(f)).isInstanceOf(BizException.class);
  f.setResourceType("不存在类型"); assertThatThrownBy(()->books.create(f)).isInstanceOf(BizException.class);
  f.setResourceType("论文"); f.setLanguage("随意语言"); assertThatThrownBy(()->books.create(f)).isInstanceOf(BizException.class);
  f.setLanguage(""); f.setDescription("长".repeat(2001)); assertThatThrownBy(()->books.create(f)).isInstanceOf(BizException.class);
  assertThat(books.search(new BookQuery(),10,false).getTotal()).isEqualTo(1);
  BookQuery q=new BookQuery(); q.setStockStatus("empty"); assertThat(books.search(q,10,true).getTotal()).isZero();
  q=new BookQuery(); q.setResourceTypes(List.of("论文","期刊")); assertThat(books.search(q,10,false).getTotal()).isEqualTo(1);
 }
 @Test void nonSaleResourcesCannotBeAddedUpdatedOrPaidForEvenWithForgedRequests() throws Exception {
  BookForm f=form("不可售论文",category(),9); f.setResourceType("论文"); books.create(f);
  Long id=books.search(new BookQuery(),10,false).getRecords().getFirst().getId();
  assertThatThrownBy(()->cartService.add(reader.getId(),id,1)).isInstanceOf(BizException.class).hasMessageContaining("仅供浏览");
  mvc.perform(post("/shop/cart/add").sessionAttr(AuthInterceptor.SESSION_KEY,reader).param("bookId",id.toString()).param("quantity","1"))
   .andExpect(redirectedUrl("/shop/books")).andExpect(flash().attribute("error","该资源仅供浏览，不能购买"));
  Long sale=book("正常图书",6); carts.upsert(reader.getId(),sale,1); carts.upsert(reader.getId(),id,1);
  var item=carts.findByUserIdAndBookId(reader.getId(),id);
  assertThatThrownBy(()->cartService.updateQuantity(reader.getId(),item.getId(),1)).isInstanceOf(BizException.class);
  assertThatThrownBy(()->orders.checkout(reader.getId())).isInstanceOf(BizException.class).hasMessageContaining("不能结算");
  assertThat(users.findById(reader.getId()).getBalance()).isEqualByComparingTo("1000");
  assertThat(books.findById(sale).getStock()).isEqualTo(6);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_order",Integer.class)).isZero();
  assertThat(carts.findByUserId(reader.getId())).hasSize(2);
 }
 @Test void changingBookToPaperCleansCartButPreservesHistoricalOrderAndList() {
  Long id=book("原来图书",6),list=lists.save(null,"学习资料","",reader);
  lists.add(list,id,reader); cartService.add(reader.getId(),id,1); orders.checkout(reader.getId());
  cartService.add(reader.getId(),id,1);
  BookForm f=form("现在论文",category(),0); f.setResourceType("论文"); f.setPrice(null); f.setStock(null); books.update(id,f);
  assertThat(carts.findByUserId(reader.getId())).isEmpty();
  assertThat(lists.books(list,reader)).hasSize(1); assertThat(lists.books(list,reader).getFirst().isPurchasable()).isFalse();
  assertThat(jdbc.queryForObject("SELECT book_title FROM t_order_item",String.class)).isEqualTo("原来图书");
  books.delete(id);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_resource_info",Integer.class)).isZero();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_resource_category",Integer.class)).isZero();
 }
 @Test void searchNewFieldsAndResourceDetailsAndForms() throws Exception {
  mvc.perform(post("/admin/books").sessionAttr(AuthInterceptor.SESSION_KEY,admin)
   .param("title","Web 研究").param("resourceType","论文").param("categoryId",category().toString())
   .param("language","外文").param("publisher","研究期刊").param("identifier","10.1234/test")
   .param("authorUnit","示例学校").param("description","<script>alert(1)</script>").param("publishDate","2025-09-01"))
   .andExpect(redirectedUrl("/admin/books"));
  BookQuery q=new BookQuery(); q.setField("publisher"); q.setKeyword("研究期刊");
  Book b=books.search(q,10,false).getRecords().getFirst();
  q.setField("identifier"); q.setKeyword("10.1234/test"); assertThat(books.search(q,10,false).getTotal()).isEqualTo(1);
  q.setKeyword("10.1234"); assertThat(books.search(q,10,false).getTotal()).isZero();
  q.setField("all"); q.setKeyword("研究期刊"); assertThat(books.search(q,10,false).getTotal()).isEqualTo(1);
  String html=page("/shop/books/"+b.getId(),reader);
  assertThat(html).contains("研究期刊","示例学校","2025-09-01","仅供浏览","&lt;script&gt;").doesNotContain("/shop/cart/add");
  assertThat(page("/admin/books/"+b.getId()+"/edit",admin)).contains("Web 研究","10.1234/test","研究期刊");
  assertThat(page("/admin/books/"+b.getId(),admin)).contains("编辑资源");
  Long list=lists.save(null,"论文书单","",admin); lists.add(list,b.getId(),admin);
  assertThat(page("/shop/reading-lists/"+list,reader)).contains("仅供浏览").doesNotContain("/shop/cart/add");
  assertThat(page("/shop/books",reader)).contains("仅供浏览").doesNotContain("/shop/cart/add");
  mvc.perform(post("/admin/books").sessionAttr(AuthInterceptor.SESSION_KEY,admin).param("title","坏日期")
   .param("resourceType","论文").param("categoryId",category().toString()).param("publishDate","bad"))
   .andExpect(view().name("admin/book-form")).andExpect(model().attributeHasFieldErrors("bookForm","publishDate"));
 }
 @Test void multiselectPaginationRoundTripsWithoutLosingFacets() throws Exception {
  for(int i=0;i<12;i++) book("多选分页"+i,6);
  String path="/shop/books?categoryIds="+category()+"&categoryIds="+categories.list().getLast().getId()+"&languages=外文&resourceTypes=论文&resourceTypes=期刊&keyword=多选&field=title";
  String html=page(path,reader);
  var matcher=java.util.regex.Pattern.compile("href=\"([^\"]+)\">下一页").matcher(html);
  assertThat(matcher.find()).isTrue();
  String link=org.springframework.web.util.HtmlUtils.htmlUnescape(matcher.group(1));
  var result=mvc.perform(get(java.net.URI.create(link)).sessionAttr(AuthInterceptor.SESSION_KEY,reader)).andExpect(status().isOk()).andReturn();
  BookQuery bound=(BookQuery)result.getModelAndView().getModel().get("q");
  assertThat(bound.getCategoryIds()).containsExactly(category(),categories.list().getLast().getId());
  assertThat(bound.getLanguages()).containsExactly("外文"); assertThat(bound.getResourceTypes()).containsExactly("论文","期刊");
  assertThat(result.getResponse().getContentAsString()).contains("<td>9</td>");
 }
 @Test void legacyRowsRemainBooksWithUnknownLanguageAfterRepeatedUpgrade() {
  Book b=new Book(); b.setTitle("历史图书"); b.setAuthor(""); b.setPrice(new BigDecimal("20")); b.setStock(2); bookMapper.insert(b);
  new ResourceDatabasePopulator(new ClassPathResource("db/catalog-schema.sql")).execute(ds);
  assertThat(books.findById(b.getId()).isPurchasable()).isTrue();
  assertThat(books.findById(b.getId()).getLanguage()).isEmpty();
  BookQuery q=new BookQuery(); q.setLanguages(List.of("未填写"));
  assertThat(books.search(q,10,false).getRecords()).extracting(Book::getId).containsExactly(b.getId());
  q.setLanguages(List.of()); q.setCategoryIds(List.of(0L));
  assertThat(books.search(q,10,false).getTotal()).isEqualTo(1);
  cartService.add(reader.getId(),b.getId(),1); orders.checkout(reader.getId());
  assertThat(books.findById(b.getId()).getStock()).isEqualTo(1);
 }

 @Test void demoResourcesImportOnceAndPreserveEditsAndDeletions() throws Exception {
  Long existing=book("Python 程序设计入门",42);
  demo.initialize();
  assertThat(books.search(new BookQuery(),100,false).getTotal()).isEqualTo(12);
  assertThat(books.findById(existing).getStock()).isEqualTo(42);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_resource_info WHERE resource_type<>'图书'",Integer.class)).isEqualTo(3);
  Long edited=jdbc.queryForObject("SELECT id FROM t_book WHERE title='Web 应用开发实践'",Long.class);
  books.update(edited,form("我修改后的名称",category(),7));
  books.delete(existing);
  demo.initialize();
  assertThat(books.search(new BookQuery(),100,false).getTotal()).isEqualTo(11);
  assertThat(books.findById(edited).getTitle()).isEqualTo("我修改后的名称");
  BookQuery q=new BookQuery(); q.setLanguages(List.of("外文"));
  assertThat(books.search(q,100,false).getTotal()).isEqualTo(3);
 }

 @Test void demoReadingListsHaveDistinctOwnersAndImportOnlyOnce() throws Exception {
  demo.initialize(); readingDemo.initialize();
  assertThat(lists.visible(admin)).hasSize(3).allMatch(ReadingList::isPublicList);
  assertThat(lists.visible(reader)).hasSize(5);
  assertThat(lists.visible(other)).hasSize(3).allMatch(ReadingList::isPublicList);
  var personal=lists.visible(reader).stream().filter(l->!l.isPublicList()).toList();
  assertThat(personal).hasSize(2);
  for(var list:lists.visible(reader)) assertThat(lists.books(list.getId(),reader)).isNotEmpty();
  Long id=personal.getFirst().getId();
  lists.save(id,"我修改过的书单","我的说明",reader);
  lists.delete(personal.getLast().getId(),reader);
  readingDemo.initialize();
  assertThat(lists.visible(reader)).hasSize(4);
  assertThat(lists.require(id,reader,false).getName()).isEqualTo("我修改过的书单");
  assertThatThrownBy(()->lists.require(id,other,false)).isInstanceOf(ResponseStatusException.class);
 }
}
