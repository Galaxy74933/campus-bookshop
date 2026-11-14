package com.example.bookshop.service;

import com.example.bookshop.dto.CourseForm;
import com.example.bookshop.dto.MajorForm;
import com.example.bookshop.entity.Course;
import com.example.bookshop.entity.Major;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.AcademicMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AcademicService {
    private final AcademicMapper mapper;
    public AcademicService(AcademicMapper mapper) { this.mapper = mapper; }

    public List<Major> majors() { return mapper.listMajors(); }
    public List<Course> courses() { return mapper.listCourses(); }
    public List<Course> coursesForBook(Long id) { return mapper.coursesForBook(id); }
    public List<Long> courseIdsForBook(Long id) { return mapper.courseIdsForBook(id); }

    public Major major(Long id) {
        Major major = mapper.findMajor(id);
        if (major == null) throw new BizException("专业不存在，请刷新列表");
        return major;
    }

    public Course course(Long id) {
        Course course = mapper.findCourse(id);
        if (course == null) throw new BizException("课程不存在，请刷新列表");
        return course;
    }

    @Transactional
    public void saveMajor(Long id, MajorForm form) {
        String name = validateName(form.getName(), "专业");
        if (id != null) major(id);
        Major major = new Major();
        major.setId(id);
        major.setName(name);
        try {
            if (id == null) mapper.insertMajor(major);
            else if (mapper.updateMajor(major) == 0) throw new BizException("专业不存在，请刷新列表");
        } catch (DuplicateKeyException e) {
            throw new BizException("专业名称已存在，请使用其他名称");
        }
    }

    @Transactional
    public void saveCourse(Long id, CourseForm form) {
        String name = validateName(form.getName(), "课程");
        List<Long> majorIds = normalizedIds(form.getMajorIds());
        for (Long majorId : majorIds) major(majorId);
        if (id != null) course(id);
        Course course = new Course();
        course.setId(id);
        course.setName(name);
        try {
            if (id == null) mapper.insertCourse(course);
            else if (mapper.updateCourse(course) == 0) throw new BizException("课程不存在，请刷新列表");
            mapper.clearCourseMajors(course.getId());
            for (Long majorId : majorIds) mapper.insertCourseMajor(course.getId(), majorId);
        } catch (DuplicateKeyException e) {
            throw new BizException("课程名称已存在，请使用其他名称");
        } catch (DataIntegrityViolationException e) {
            throw new BizException("所选专业已发生变化，请刷新后重试");
        }
    }

    public List<Long> validateCourseIds(List<Long> ids) {
        List<Long> unique = normalizedIds(ids);
        for (Long id : unique) course(id);
        return unique;
    }

    @Transactional
    public void replaceBookCourses(Long bookId, List<Long> ids) {
        List<Long> unique = validateCourseIds(ids);
        mapper.clearBookCourses(bookId);
        try {
            for (Long courseId : unique) mapper.insertBookCourse(bookId, courseId);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("图书或所选课程已发生变化，请刷新后重试");
        }
    }

    @Transactional
    public void deleteMajor(Long id) {
        major(id);
        if (mapper.majorReferenceCount(id) > 0) throw new BizException("该专业已关联课程，请先在课程编辑页解除关联");
        try {
            mapper.deleteMajor(id);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("该专业已被课程关联，不能删除");
        }
    }

    @Transactional
    public void deleteCourse(Long id) {
        course(id);
        if (mapper.courseReferenceCount(id) > 0) throw new BizException("该课程已关联专业或图书，请先解除关联再删除");
        try {
            mapper.deleteCourse(id);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("该课程已被引用，不能删除");
        }
    }

    private String validateName(String raw, String label) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new BizException(label + "名称不能为空");
        if (name.length() > 100) throw new BizException(label + "名称不能超过100个字符");
        return name;
    }

    private List<Long> normalizedIds(List<Long> ids) {
        if (ids == null) return List.of();
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) throw new BizException("请选择有效的关联项");
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
