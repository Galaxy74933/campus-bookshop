package com.example.bookshop.service;

import com.example.bookshop.dto.BookForm;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.ResourceMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ResourceImportService {
    public static final List<String> HEADERS=List.of("资源名称","资源类型","主分类","附加分类","作者","语言","来源/出版社","出版日期","ISBN/DOI","作者单位","简介","价格","库存");
    private final BookService books;
    private final CategoryService categories;
    private final ResourceMapper resources;
    private final com.example.bookshop.mapper.ImportBatchMapper batches;
    public ResourceImportService(BookService books,CategoryService categories,ResourceMapper resources,com.example.bookshop.mapper.ImportBatchMapper batches) {
        this.books=books; this.categories=categories; this.resources=resources; this.batches=batches;
    }
    public static class ImportException extends BizException {
        private final List<String> errors;
        public ImportException(List<String> errors) { super("导入失败，未新增任何资源"); this.errors=List.copyOf(errors); }
        public List<String> getErrors() { return errors; }
    }
    @Transactional
    public int importFile(MultipartFile file) {
        return importFile(file,null);
    }
    @Transactional
    public int importFile(MultipartFile file,Long ownerId) {
        if(file.isEmpty() || file.getSize()>5*1024*1024) throw new BizException("请选择不超过 5 MB 的 Excel 文件");
        if(file.getOriginalFilename()==null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new BizException("仅支持 .xlsx 文件，请使用导入模板");
        List<BookForm> forms=new ArrayList<>(); List<String> errors=new ArrayList<>();
        Map<String,Long> categoryIds=new HashMap<>();
        resources.lockImportCategories();
        categories.list().forEach(c->categoryIds.put(c.getName(),c.getId()));
        Set<String> identifiers=new HashSet<>(), identities=new HashSet<>();
        try(var in=file.getInputStream(); var workbook=new XSSFWorkbook(in)) {
            Sheet sheet=workbook.getSheet("资源导入");
            if(sheet==null) throw new BizException("缺少“资源导入”工作表，请使用模板");
            if(sheet.getLastRowNum()>500) throw new BizException("每次最多导入 500 行，请分批上传");
            Row header=sheet.getRow(0);
            for(int i=0;i<HEADERS.size();i++) if(!HEADERS.get(i).equals(value(header,i)))
                throw new BizException("表头不匹配，请保留模板第一行及列顺序");
            for(int i=1;i<=sheet.getLastRowNum();i++) {
                Row row=sheet.getRow(i);
                if(row==null) continue;
                try {
                    List<String> v=new ArrayList<>();
                    for(int c=0;c<HEADERS.size();c++) v.add(value(row,c));
                    if(v.stream().allMatch(String::isBlank)) continue;
                    BookForm f=new BookForm(); f.setTitle(v.get(0)); f.setResourceType(v.get(1));
                    f.setCategoryId(category(categoryIds,v.get(2)));
                    if(!v.get(3).isBlank()) f.setCategoryIds(Arrays.stream(v.get(3).split("[;；]",-1)).map(String::trim).filter(s->!s.isEmpty()).map(s->category(categoryIds,s)).distinct().toList());
                    f.setAuthor(v.get(4)); f.setLanguage(v.get(5)); f.setPublisher(v.get(6));
                    if(!v.get(7).isBlank()) {
                        try { f.setPublishDate(LocalDate.parse(v.get(7))); }
                        catch(RuntimeException e) { throw new BizException("出版日期须为有效日期，格式 yyyy-MM-dd"); }
                    }
                    f.setIdentifier(v.get(8)); f.setAuthorUnit(v.get(9)); f.setDescription(v.get(10));
                    if("图书".equals(f.getResourceType())) {
                        try { if(!v.get(11).isBlank()) f.setPrice(new BigDecimal(v.get(11))); }
                        catch(NumberFormatException e) { throw new BizException("价格须填写数字"); }
                        try { if(!v.get(12).isBlank()) f.setStock(new BigDecimal(v.get(12)).intValueExact()); }
                        catch(ArithmeticException | NumberFormatException e) { throw new BizException("库存须为非负整数"); }
                    } else if(!v.get(11).isBlank() || !v.get(12).isBlank()) throw new BizException("非图书资源不售卖，价格和库存请留空");
                    var book=books.validated(f);
                    String identity=key(f.getResourceType())+"\u0000"+key(f.getTitle())+"\u0000"+key(f.getAuthor());
                    boolean repeated=!identities.add(identity);
                    if(!f.getIdentifier().isBlank()) repeated|=!identifiers.add(key(f.getIdentifier()));
                    if(repeated || resources.importDuplicates(book)>0) throw new BizException("资源重复（ISBN/DOI 相同，或名称、作者和类型均相同），请删除重复行；修改已有资源请使用编辑");
                    forms.add(f);
                } catch(BizException e) { errors.add("第 "+(i+1)+" 行："+e.getMessage()); }
            }
        } catch(BizException e) { throw e; }
        catch(Exception e) { throw new BizException("无法读取 Excel，请检查文件是否损坏、加密或格式不正确"); }
        if(!errors.isEmpty()) throw new ImportException(errors);
        if(forms.isEmpty()) throw new BizException("没有可导入的数据，请从第二行开始填写");
        var batch=new com.example.bookshop.entity.ImportBatch();
        batch.setOwnerId(ownerId);
        String filename=file.getOriginalFilename().replace('\\','/');
        filename=filename.substring(filename.lastIndexOf('/')+1);
        batch.setFilename(filename.substring(0,Math.min(filename.length(),255)));
        batch.setResourceCount(forms.size()); batches.insert(batch);
        for(BookForm f:forms) batches.addBook(batch.getId(),books.create(f));
        return forms.size();
    }
    private static String key(String s) { return s==null ? "" : s.trim().toLowerCase(Locale.ROOT); }
    private static Long category(Map<String,Long> ids,String name) {
        Long id=ids.get(name);
        if(id==null) throw new BizException("分类“"+name+"”不存在，请先创建或填写现有分类名称");
        return id;
    }
    private static String value(Row row,int index) {
        Cell cell=row==null ? null : row.getCell(index);
        if(cell==null) return "";
        // ISO dates are valid OOXML, but POI's transitional schema has no enum for type d.
        if(cell instanceof org.apache.poi.xssf.usermodel.XSSFCell x) {
            var type=x.getCTCell().getDomNode().getAttributes().getNamedItem("t");
            if(type!=null && "d".equals(type.getNodeValue())) {
                if(index!=7) throw new BizException(HEADERS.get(index)+"不能填写日期");
                try { return LocalDate.parse(x.getRawValue().substring(0,10)).toString(); }
                catch(RuntimeException e) { throw new BizException("出版日期无效，请填写 yyyy-MM-dd 日期"); }
            }
        }
        if(cell.getCellType()==CellType.BLANK) return "";
        if(index==7 && cell.getCellType()==CellType.NUMERIC) {
            try {
                if(DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            } catch(RuntimeException e) { throw new BizException("出版日期无效，请填写 yyyy-MM-dd 日期"); }
        }
        return switch(cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            default -> throw new BizException(HEADERS.get(index)+"请填写文本或数字，不支持公式、布尔值或错误单元格");
        };
    }
}
