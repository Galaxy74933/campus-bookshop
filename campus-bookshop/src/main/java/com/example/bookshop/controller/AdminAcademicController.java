package com.example.bookshop.controller;

import com.example.bookshop.dto.CourseForm;
import com.example.bookshop.dto.MajorForm;
import com.example.bookshop.entity.Course;
import com.example.bookshop.entity.Major;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.service.AcademicService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminAcademicController {
    private final AcademicService academicService;
    public AdminAcademicController(AcademicService academicService) { this.academicService = academicService; }

    @GetMapping("/majors")
    public String majors(Model model) {
        List<Major> majors = academicService.majors();
        model.addAttribute("majors", majors);
        model.addAttribute("majorCourses", groupCourses(majors, academicService.courses()));
        return "admin/majors";
    }

    @GetMapping("/academic-overview")
    public String overview(@RequestParam(defaultValue = "") String keyword, Model model) {
        List<Major> majors = academicService.majors();
        List<Course> courses = academicService.courses();
        Map<Long, List<Course>> majorCourses = groupCourses(majors, courses);
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        // Keep every course in a matching major so search never hides part of its curriculum.
        List<Major> visibleMajors = majors.stream()
                .filter(major -> matches(major.getName(), query)
                        || majorCourses.get(major.getId()).stream().anyMatch(course -> matches(course.getName(), query)))
                .toList();
        List<Course> unassignedCourses = courses.stream()
                .filter(course -> course.getMajors().isEmpty() && matches(course.getName(), query)).toList();
        model.addAttribute("majors", visibleMajors);
        model.addAttribute("majorCourses", majorCourses);
        model.addAttribute("unassignedCourses", unassignedCourses);
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("majorCount", majors.size());
        model.addAttribute("courseCount", courses.size());
        return "admin/academic-overview";
    }

    private boolean matches(String name, String query) {
        return name.toLowerCase(Locale.ROOT).contains(query);
    }

    private Map<Long, List<Course>> groupCourses(List<Major> majors, List<Course> courses) {
        Map<Long, List<Course>> majorCourses = new LinkedHashMap<>();
        for (Major major : majors) majorCourses.put(major.getId(), new ArrayList<>());
        for (Course course : courses) {
            for (Major major : course.getMajors()) {
                List<Course> associatedCourses = majorCourses.get(major.getId());
                if (associatedCourses != null) associatedCourses.add(course);
            }
        }
        return majorCourses;
    }

    @GetMapping("/majors/add")
    public String addMajor(Model model) {
        model.addAttribute("majorForm", new MajorForm());
        return "admin/major-form";
    }

    @GetMapping("/majors/{id}/edit")
    public String editMajor(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Major major = academicService.major(id);
            MajorForm form = new MajorForm();
            form.setId(id);
            form.setName(major.getName());
            model.addAttribute("majorForm", form);
            return "admin/major-form";
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/majors";
        }
    }

    @PostMapping({"/majors", "/majors/{id}"})
    public String saveMajor(@PathVariable(required = false) Long id,
                            @Valid @ModelAttribute("majorForm") MajorForm form, BindingResult result,
                            Model model, RedirectAttributes ra) {
        form.setId(id);
        if (result.hasErrors()) {
            model.addAttribute("error", "请输入1～100个字符的专业名称");
            return "admin/major-form";
        }
        try {
            academicService.saveMajor(id, form);
            ra.addFlashAttribute("msg", "专业已保存");
            return "redirect:/admin/majors";
        } catch (BizException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/major-form";
        }
    }

    @PostMapping("/majors/{id}/delete")
    public String deleteMajor(@PathVariable Long id, RedirectAttributes ra) {
        try {
            academicService.deleteMajor(id);
            ra.addFlashAttribute("msg", "专业已删除");
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/majors";
    }

    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("courses", academicService.courses());
        return "admin/courses";
    }

    @GetMapping("/courses/add")
    public String addCourse(Model model) {
        model.addAttribute("courseForm", new CourseForm());
        return courseForm(model);
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourse(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            Course course = academicService.course(id);
            CourseForm form = new CourseForm();
            form.setId(id);
            form.setName(course.getName());
            form.setMajorIds(course.getMajors().stream().map(Major::getId).toList());
            model.addAttribute("courseForm", form);
            return courseForm(model);
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/courses";
        }
    }

    @PostMapping({"/courses", "/courses/{id}"})
    public String saveCourse(@PathVariable(required = false) Long id,
                             @Valid @ModelAttribute("courseForm") CourseForm form, BindingResult result,
                             Model model, RedirectAttributes ra) {
        form.setId(id);
        if (result.hasErrors()) {
            model.addAttribute("error", "请填写有效的课程名称并选择有效的专业");
            return courseForm(model);
        }
        try {
            academicService.saveCourse(id, form);
            ra.addFlashAttribute("msg", "课程及专业关联已保存");
            return "redirect:/admin/courses";
        } catch (BizException e) {
            model.addAttribute("error", e.getMessage());
            return courseForm(model);
        }
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes ra) {
        try {
            academicService.deleteCourse(id);
            ra.addFlashAttribute("msg", "课程已删除");
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    private String courseForm(Model model) {
        model.addAttribute("majors", academicService.majors());
        return "admin/course-form";
    }
}
