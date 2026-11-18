package com.example.bookshop.service;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.entity.*;
import com.example.bookshop.mapper.*;
import com.example.bookshop.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service
public class ReadingListService {
    private final ReadingListMapper mapper;
    private final BookMapper books;
    public ReadingListService(ReadingListMapper mapper,BookMapper books) { this.mapper=mapper; this.books=books; }
    private boolean admin(LoginUser u) { return u != null && "ADMIN".equals(u.getRole()); }
    private void requireUser(LoginUser u) {
        if (u == null || (!admin(u) && !"READER".equals(u.getRole()))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    public boolean editable(ReadingList r,LoginUser u) {
        return u != null && (r.isPublicList() ? admin(u) : !admin(u) && r.getOwnerId().equals(u.getId()));
    }
    public List<ReadingList> visible(LoginUser u) {
        requireUser(u);
        return mapper.visible(u.getId()).stream().filter(r -> r.isPublicList() || editable(r,u)).toList();
    }
    public ReadingList require(Long id,LoginUser u,boolean write) {
        requireUser(u);
        ReadingList r=mapper.find(id);
        return authorize(r,u,write);
    }
    private ReadingList authorize(ReadingList r,LoginUser u,boolean write) {
        if (r==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"书单不存在");
        if (write ? !editable(r,u) : !r.isPublicList() && !editable(r,u))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"无权访问此书单");
        return r;
    }
    public List<Book> books(Long id,LoginUser u) { require(id,u,false); return mapper.books(id); }
    @Transactional
    public Long save(Long id,String rawName,String rawDescription,LoginUser u) {
        requireUser(u);
        ReadingList r;
        if (id==null) {
            r=new ReadingList(); r.setOwnerId(u.getId()); r.setPublicList(admin(u));
        } else r=authorize(mapper.lock(id),u,true);
        String name=rawName==null ? "" : rawName.trim();
        String description=rawDescription==null ? "" : rawDescription.trim();
        if (name.isEmpty() || name.length()>100) throw new BizException("书单名称须为1～100个字符");
        if (description.length()>500) throw new BizException("书单说明不能超过500个字符");
        Long existing=mapper.named(r.getOwnerId(),name);
        if (existing!=null && !existing.equals(id)) throw new BizException("该创建者已有同名书单");
        r.setName(name); r.setDescription(description);
        try { if (id==null) mapper.insert(r); else mapper.update(r); }
        catch (DataIntegrityViolationException e) { throw new BizException("书单名称重复或数据已变化，请刷新后重试"); }
        return r.getId();
    }
    @Transactional
    public void delete(Long id,LoginUser u) {
        requireUser(u); authorize(mapper.lock(id),u,true); mapper.delete(id);
    }
    @Transactional
    public void add(Long id,Long bookId,LoginUser u) {
        requireUser(u); authorize(mapper.lock(id),u,true);
        if (bookId==null || books.findById(bookId)==null) throw new BizException("图书不存在");
        if (mapper.contains(id,bookId)>0) throw new BizException("该图书已在书单中");
        try { mapper.add(id,bookId); }
        catch (DataIntegrityViolationException e) { throw new BizException("图书已加入或数据已变化，请刷新后重试"); }
    }
    @Transactional
    public void remove(Long id,Long bookId,LoginUser u) {
        requireUser(u); authorize(mapper.lock(id),u,true); mapper.remove(id,bookId);
    }
}

