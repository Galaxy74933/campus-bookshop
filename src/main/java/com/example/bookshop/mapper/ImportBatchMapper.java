package com.example.bookshop.mapper;

import com.example.bookshop.entity.ImportBatch;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ImportBatchMapper {
    @Insert("INSERT INTO t_import_batch(owner_id,filename,resource_count) VALUES(#{ownerId},#{filename},#{resourceCount})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(ImportBatch batch);
    @Insert("INSERT INTO t_import_batch_book(batch_id,book_id) VALUES(#{batchId},#{bookId})")
    int addBook(@Param("batchId") Long batchId,@Param("bookId") Long bookId);
    @Select("SELECT * FROM t_import_batch WHERE owner_id=#{ownerId} ORDER BY id DESC LIMIT 1")
    ImportBatch latest(Long ownerId);
    @Select("SELECT * FROM t_import_batch WHERE id=#{id} FOR UPDATE")
    ImportBatch lock(Long id);
    @Select("SELECT b.id FROM t_book b JOIN t_import_batch_book i ON i.book_id=b.id WHERE i.batch_id=#{id} ORDER BY b.id FOR UPDATE")
    List<Long> remainingBooks(Long id);
    @Update("UPDATE t_import_batch SET undone_at=CURRENT_TIMESTAMP WHERE id=#{id} AND undone_at IS NULL")
    int markUndone(Long id);
}
