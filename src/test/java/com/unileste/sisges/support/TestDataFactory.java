package com.unileste.sisges.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unileste.sisges.controller.dto.auth.LoginRequest;
import com.unileste.sisges.controller.dto.auth.LoginResponse;
import com.unileste.sisges.model.Discipline;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.StudentResponsible;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.DisciplineRepository;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.StudentResponsibleRepository;
import com.unileste.sisges.repository.TeacherRepository;
import com.unileste.sisges.repository.UserRepository;
import com.unileste.sisges.security.JwtService;
import com.unileste.sisges.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
@RequiredArgsConstructor
public class TestDataFactory {

    public static final String DEFAULT_PASSWORD = "testpass123";

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final StudentResponsibleRepository studentResponsibleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final DisciplineRepository disciplineRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public User createAdmin(String suffix) {
        return createUser("Admin " + suffix, "admin-" + suffix + "@test.sisges.local",
                "ADM" + suffix.toUpperCase(), "ADMIN");
    }

    public User createTeacher(String suffix) {
        User user = createUser("Teacher " + suffix, "teacher-" + suffix + "@test.sisges.local",
                "TCH" + suffix.toUpperCase(), "TEACHER");
        Teacher teacher = Teacher.builder().baseData(user).build();
        teacherRepository.save(teacher);
        return user;
    }

    public User createStudent(String suffix, SchoolClass schoolClass) {
        User user = createUser("Student " + suffix, "student-" + suffix + "@test.sisges.local",
                "STU" + suffix.toUpperCase(), "STUDENT");
        StudentResponsible responsible = StudentResponsible.builder()
                .name("Resp " + suffix)
                .phone("11999990000")
                .alternativePhone("11999990001")
                .email("resp-" + suffix + "@test.sisges.local")
                .alternativeEmail("resp2-" + suffix + "@test.sisges.local")
                .build();
        responsible = studentResponsibleRepository.save(responsible);
        Student student = Student.builder()
                .baseData(user)
                .currentClass(schoolClass)
                .responsibles(new ArrayList<>())
                .build();
        student.getResponsibles().add(responsible);
        studentRepository.save(student);
        if (schoolClass != null) {
            schoolClass.getStudents().add(student);
            schoolClassRepository.save(schoolClass);
        }
        return user;
    }

    public SchoolClass createSchoolClass(String suffix) {
        SchoolClass schoolClass = SchoolClass.builder()
                .name("Turma " + suffix)
                .academicYear("6º ano")
                .students(new ArrayList<>())
                .teachers(new ArrayList<>())
                .disciplines(new ArrayList<>())
                .build();
        return schoolClassRepository.save(schoolClass);
    }

    public Discipline createDiscipline(String suffix) {
        Discipline discipline = Discipline.builder()
                .name("Disciplina " + suffix)
                .description("Descrição teste")
                .build();
        return disciplineRepository.save(discipline);
    }

    public void linkTeacherToClass(Teacher teacher, SchoolClass schoolClass) {
        SchoolClass managedClass = schoolClassRepository.findById(schoolClass.getId()).orElseThrow();
        Teacher managedTeacher = teacherRepository.findById(teacher.getId()).orElseThrow();
        boolean alreadyLinked = managedClass.getTeachers().stream()
                .anyMatch(t -> t.getId().equals(managedTeacher.getId()));
        if (!alreadyLinked) {
            managedClass.getTeachers().add(managedTeacher);
            managedTeacher.getClasses().add(managedClass);
            schoolClassRepository.save(managedClass);
        }
    }

    public void linkDisciplineToClass(Discipline discipline, SchoolClass schoolClass) {
        SchoolClass managedClass = schoolClassRepository.findById(schoolClass.getId()).orElseThrow();
        Discipline managedDiscipline = disciplineRepository.findById(discipline.getId()).orElseThrow();
        boolean alreadyLinked = managedClass.getDisciplines().stream()
                .anyMatch(d -> d.getId().equals(managedDiscipline.getId()));
        if (!alreadyLinked) {
            managedClass.getDisciplines().add(managedDiscipline);
            managedDiscipline.getSchoolClasses().add(managedClass);
            schoolClassRepository.save(managedClass);
        }
    }

    public void linkTeacherToDiscipline(Teacher teacher, Discipline discipline) {
        Discipline managedDiscipline = disciplineRepository.findById(discipline.getId()).orElseThrow();
        Teacher managedTeacher = teacherRepository.findById(teacher.getId()).orElseThrow();
        boolean alreadyLinked = managedDiscipline.getTeachers().stream()
                .anyMatch(t -> t.getId().equals(managedTeacher.getId()));
        if (!alreadyLinked) {
            managedDiscipline.getTeachers().add(managedTeacher);
            managedTeacher.getDisciplines().add(managedDiscipline);
            disciplineRepository.save(managedDiscipline);
        }
    }

    public Teacher findTeacherByUserId(Integer userId) {
        return teacherRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                .orElseThrow();
    }

    public Student findStudentByUserId(Integer userId) {
        return studentRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                .orElseThrow();
    }

    public String tokenFor(User user) {
        return jwtService.generateToken(new UserPrincipal(user));
    }

    public String loginToken(MockMvc mockMvc, String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder().email(email).password(password).build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);
        return response.getAccessToken();
    }

    public TestScenario seedScenario(String suffix) {
        SchoolClass schoolClass = createSchoolClass(suffix);
        Discipline discipline = createDiscipline(suffix);
        User admin = createAdmin(suffix);
        User teacherUser = createTeacher(suffix);
        User studentUser = createStudent(suffix, schoolClass);
        Teacher teacher = findTeacherByUserId(teacherUser.getId());
        linkTeacherToClass(teacher, schoolClass);
        linkDisciplineToClass(discipline, schoolClass);
        linkTeacherToDiscipline(teacher, discipline);
        return new TestScenario(suffix, admin, teacherUser, studentUser, teacher,
                findStudentByUserId(studentUser.getId()), schoolClass, discipline);
    }

    private User createUser(String name, String email, String register, String role) {
        User user = User.builder()
                .name(name)
                .email(email)
                .register(register)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender("MALE")
                .userRole(role)
                .build();
        return userRepository.save(user);
    }

    public record TestScenario(
            String suffix,
            User admin,
            User teacherUser,
            User studentUser,
            Teacher teacher,
            Student student,
            SchoolClass schoolClass,
            Discipline discipline
    ) {
    }
}
