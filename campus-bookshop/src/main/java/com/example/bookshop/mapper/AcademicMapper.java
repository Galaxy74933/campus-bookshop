package com.example.bookshop.mapper;

import com.example.bookshop.entity.Major;
import com.example.bookshop.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AcademicMapper {
    List<Major> listMajors();
    Major findMajor(@Param("id") Long id);
    Long majorIdByName(@Param("name") String name);
    int insertMajor(Major major);
    int updateMajor(Major major);
    int deleteMajor(@Param("id") Long id);
    int majorReferenceCount(@Param("id") Long id);
    List<Course> listCourses();
    Course findCourse(@Param("id") Long id);
    Long courseIdByName(@Param("name") String name);
    int insertCourse(Course course);
    int updateCourse(Course course);
    int deleteCourse(@Param("id") Long id);
    int courseReferenceCount(@Param("id") Long id);
    int clearCourseMajors(@Param("courseId") Long courseId);
    int insertCourseMajor(@Param("courseId") Long courseId, @Param("majorId") Long majorId);
    List<Long> courseIdsForBook(@Param("bookId") Long bookId);
    List<Course> coursesForBook(@Param("bookId") Long bookId);
    int clearBookCourses(@Param("bookId") Long bookId);
    int insertBookCourse(@Param("bookId") Long bookId, @Param("courseId") Long courseId);
}
