package com.example.bookshop.service;

import com.example.bookshop.entity.ImportBatch;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.ImportBatchMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportBatchService {
    private final ImportBatchMapper batches;
    private final BookService books;
    public ImportBatchService(ImportBatchMapper batches,BookService books) { this.batches=batches; this.books=books; }
    public ImportBatch latest(Long ownerId) { return batches.latest(ownerId); }
    @Transactional
    public int undo(Long id,Long ownerId) {
        ImportBatch batch=batches.lock(id);
        if(batch==null || !ownerId.equals(batch.getOwnerId())) throw new BizException("找不到你的导入批次");
        if(batch.getUndoneAt()!=null) throw new BizException("该批次已经撤回，请勿重复操作");
        if(!id.equals(batches.latest(ownerId).getId())) throw new BizException("只能撤回最近一次导入，请刷新页面");
        var ids=batches.remainingBooks(id);
        for(Long bookId:ids) books.delete(bookId);
        batches.markUndone(id);
        return ids.size();
    }
}
