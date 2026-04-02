package com.unileste.sisges.bootstrap;

import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "sisges.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeedRunner implements ApplicationRunner {

    private static final String DOMAIN = "@sisges.com";

    private static final String[] GIVEN = {
            "Ana", "Beatriz", "Carlos", "Daniela", "Eduardo", "Fernanda", "Gustavo", "Helena", "Igor", "Juliana",
            "Kleber", "Larissa", "Marcos", "Natália", "Otávio", "Paula", "Rafael", "Sabrina", "Thiago", "Vanessa",
            "Bruno", "Camila", "Diego", "Elisa", "Felipe"
    };

    private static final String[] FAMILY = {
            "Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira", "Lima", "Gomes",
            "Ribeiro", "Carvalho", "Rocha", "Dias", "Barbosa", "Teixeira", "Cavalcante", "Moura", "Nascimento", "Freitas",
            "Martins", "Cardoso", "Correia", "Monteiro", "Araújo"
    };

    private static final String[] RESPONSIBLE_FULL = {
            "Roberto Silva Mendes", "Sandra Oliveira Costa", "Marcos Antônio Pereira", "Luciana Ferreira Lima",
            "Paulo Henrique Souza", "Cláudia Regina Dias", "Fernando Luís Gomes", "Adriana Martins Rocha",
            "Ricardo Alberto Teixeira", "Patrícia Nunes Cardoso", "André Luiz Barbosa", "Juliana Costa Araújo",
            "Felipe Augusto Ribeiro", "Carla Simone Moura", "Rodrigo César Nascimento"
    };

    private final UserRepository userRepository;
    private final StudentResponsibleRepository studentResponsibleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassMeetingRepository classMeetingRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementCommentRepository announcementCommentRepository;
    private final AnnouncementLikeRepository announcementLikeRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRegisterAndDeletedAtIsNull("ADM0001")) {
            return;
        }
        log.info("SISGES homologation seed: loading demo data");

        List<StudentResponsible> responsibles = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            int idx = i + 1;
            StudentResponsible sr = StudentResponsible.builder()
                    .name(RESPONSIBLE_FULL[i])
                    .phone("1199999" + String.format("%04d", idx))
                    .alternativePhone(null)
                    .email("resp" + String.format("%03d", idx) + DOMAIN)
                    .alternativeEmail(null)
                    .build();
            responsibles.add(studentResponsibleRepository.save(sr));
        }

        List<SchoolClass> classes = new ArrayList<>();
        String year = "2026";
        classes.add(schoolClassRepository.save(SchoolClass.builder().name("1º Ano A").academicYear(year).build()));
        classes.add(schoolClassRepository.save(SchoolClass.builder().name("1º Ano B").academicYear(year).build()));
        classes.add(schoolClassRepository.save(SchoolClass.builder().name("2º Ano A").academicYear(year).build()));

        List<String> disciplineNames = List.of(
                "Português",
                "Matemática",
                "Ciências",
                "História",
                "Geografia",
                "Educação Física",
                "Arte",
                "Inglês"
        );
        List<Discipline> disciplines = new ArrayList<>();
        for (String n : disciplineNames) {
            disciplines.add(disciplineRepository.save(Discipline.builder().name(n).description("Componente curricular").build()));
        }

        User admin = User.builder()
                .name("Patrícia Mendes Rocha")
                .email("adm0001" + DOMAIN)
                .register("ADM0001")
                .password(passwordEncoder.encode("admin123"))
                .birthDate(LocalDate.of(1978, 4, 12))
                .gender("FEMALE")
                .userRole("ADMIN")
                .build();
        admin = userRepository.save(admin);

        List<Teacher> teachers = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            String reg = String.format("P%04d", i);
            String tName = GIVEN[(i + 3) % GIVEN.length] + " " + FAMILY[(i * 5) % FAMILY.length] + " " + FAMILY[(i * 7 + 2) % FAMILY.length];
            User u = User.builder()
                    .name(tName)
                    .email(reg.toLowerCase() + DOMAIN)
                    .register(reg)
                    .password(passwordEncoder.encode("professor123"))
                    .birthDate(LocalDate.of(1972 + (i % 18), (i % 12) + 1, 10 + (i % 15)))
                    .gender(i % 2 == 0 ? "FEMALE" : "MALE")
                    .userRole("TEACHER")
                    .build();
            u = userRepository.save(u);
            Teacher t = Teacher.builder().baseData(u).build();
            teachers.add(teacherRepository.save(t));
        }

        for (int d = 0; d < disciplines.size(); d++) {
            Discipline disc = disciplines.get(d);
            disc.getTeachers().add(teachers.get(d % teachers.size()));
            if (d + 1 < teachers.size()) {
                disc.getTeachers().add(teachers.get((d + 1) % teachers.size()));
            }
            disciplineRepository.save(disc);
        }

        for (SchoolClass sc : classes) {
            for (Discipline d : disciplines) {
                sc.getDisciplines().add(d);
            }
            for (int t = 0; t < teachers.size(); t++) {
                if (t % classes.size() == classes.indexOf(sc)) {
                    sc.getTeachers().add(teachers.get(t));
                }
            }
            schoolClassRepository.save(sc);
        }

        List<User> studentUsers = new ArrayList<>();
        for (int i = 1; i <= 150; i++) {
            String reg = String.format("A%05d", i);
            int idx = i - 1;
            String sName = GIVEN[idx % GIVEN.length] + " " + FAMILY[(idx / GIVEN.length) % FAMILY.length];
            User u = User.builder()
                    .name(sName)
                    .email(reg.toLowerCase() + DOMAIN)
                    .register(reg)
                    .password(passwordEncoder.encode("aluno123"))
                    .birthDate(LocalDate.of(2008 + (idx % 5), (idx % 11) + 1, 5 + (idx % 20)))
                    .gender(idx % 2 == 0 ? "FEMALE" : "MALE")
                    .userRole("STUDENT")
                    .build();
            u = userRepository.save(u);
            studentUsers.add(u);
            SchoolClass sc = classes.get((i - 1) % classes.size());
            Student st = Student.builder().baseData(u).currentClass(sc).build();
            st = studentRepository.save(st);
            st.getResponsibles().add(responsibles.get((i - 1) % responsibles.size()));
            studentRepository.save(st);
        }

        LocalDate[] meetingDates = {
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 3, 4)
        };
        int di = 0;
        for (SchoolClass sc : classes) {
            for (LocalDate md : meetingDates) {
                Discipline d = disciplines.get(di % disciplines.size());
                Teacher t = d.getTeachers().isEmpty() ? teachers.get(0) : d.getTeachers().get(0);
                ClassMeeting cm = ClassMeeting.builder()
                        .schoolClass(sc)
                        .discipline(d)
                        .teacher(t)
                        .meetingDate(md)
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(9, 0))
                        .build();
                classMeetingRepository.save(cm);
                di++;
            }
        }

        seedAnnouncements(admin, teachers, studentUsers);

        log.info("SISGES homologation seed: finished (users, turmas, disciplinas, aulas, avisos, comentários, curtidas)");
    }

    private void seedAnnouncements(User admin, List<Teacher> teachers, List<User> studentUsers) {
        LocalDateTime base = LocalDateTime.now().minusDays(21);
        int slot = 0;

        List<SavedPost> posts = new ArrayList<>();

        posts.add(createPost(
                "Calendário escolar — recesso de julho",
                "A coordenação informa que o recesso escolar está previsto de 14 a 25 de julho. Durante o período, o atendimento administrativo será reduzido. Dúvidas podem ser encaminhadas pelo e-mail institucional.",
                "TEXT",
                null,
                admin,
                base,
                slot++));

        posts.add(createPost(
                "Reunião de pais e mestres — 2º bimestre",
                "Convidamos as famílias para a reunião presencial no dia 28/03, às 19h, no auditório. Serão apresentados os resultados parciais e orientações para avaliações finais.",
                "TEXT",
                null,
                teachers.get(0).getBaseData(),
                base,
                slot++));

        posts.add(createPost(
                "Biblioteca: horário estendido",
                "A biblioteca passará a atender também das 17h às 18h30, de segunda a quinta, para empréstimo e devolução de obras. Cadastro de novos leitores na secretaria.",
                "TEXT",
                null,
                teachers.get(3).getBaseData(),
                base,
                slot++));

        posts.add(createPost(
                "Semana de ciências e tecnologia",
                "Entre os dias 07 e 11 de abril ocorrerão mostras de trabalhos, palestras com ex-alunos e oficinas práticas. Inscrições abertas na coordenação até 01/04.",
                "TEXT",
                null,
                admin,
                base,
                slot++));

        posts.add(createPost(
                "Campeonato interclasses — educação física",
                "Divulgamos a tabela dos jogos do turno da tarde. Uniforme completo obrigatório. Hidratação e autorização médica atualizada na secretaria.",
                "IMAGE",
                "https://picsum.photos/id/237/900/900",
                teachers.get(5).getBaseData(),
                base,
                slot++));

        posts.add(createPost(
                "Mostra de arte e cultura",
                "Exposição dos trabalhos de arte dos estudantes do 1º e 2º anos. Visitação aberta à comunidade no sábado, das 9h às 13h.",
                "IMAGE",
                "https://picsum.photos/id/433/900/900",
                teachers.get(7).getBaseData(),
                base,
                slot++));

        posts.add(createPost(
                "Campanha de vacinação — atualização de cartão",
                "Lembramos que o cartão de vacina deve estar em dia para participação em atividades externas. Secretaria disponível para conferência de documentação.",
                "IMAGE",
                "https://picsum.photos/id/292/900/900",
                admin,
                base,
                slot++));

        posts.add(createPost(
                "Acesso ao portal do aluno",
                "Publicamos um guia rápido com login, recuperação de senha e onde consultar boletim e comunicados. Em caso de bloqueio, contate a TI pelo ramal 210.",
                "TEXT",
                null,
                teachers.get(11).getBaseData(),
                base,
                slot++));

        posts.add(createPost(
                "Transporte escolar — alteração de rota",
                "Informamos ajuste pontual na rota do período da manhã a partir de segunda-feira. Lista de pontos atualizada no mural da portaria e no app da escola.",
                "TEXT",
                null,
                admin,
                base,
                slot++));

        posts.add(createPost(
                "Feira do livro 2026",
                "Descontos para estudantes e famílias em títulos selecionados. Parte da renda revertida para aquisição de novos exemplares para a biblioteca.",
                "IMAGE",
                "https://picsum.photos/id/866/900/900",
                teachers.get(2).getBaseData(),
                base,
                slot++));

        posts.add(createPost(
                "Segurança: uso de crachá",
                "Reforçamos que visitantes devem retirar crachá de identificação na portaria e acompanhar fluxo de entrada. Obrigado pela colaboração.",
                "TEXT",
                null,
                teachers.get(9).getBaseData(),
                base,
                slot++));

        for (SavedPost sp : posts) {
            LocalDateTime created = sp.baseTime().plusHours(sp.slot() * 4L);
            jdbcTemplate.update(
                    "UPDATE sisges.announcement SET created_at = ? WHERE id = ?",
                    Timestamp.valueOf(created),
                    sp.announcement().getId());
        }

        String[][] commentTexts = {
                {"Concordo, vou ajustar minha agenda.", "Ótima iniciativa, obrigado pelo aviso."},
                {"Vou comparecer com meus pais.", "Há vaga para acompanhante?"},
                {"Finalmente um horário que ajuda!", "Posso renovar o empréstimo online?"},
                {"Minha turma já está montando o estande.", "Há limite de inscrições por turma?"},
                {"Time do 2º ano vai com tudo!", "Horário confirmado no mural?"},
                {"Adorei a exposição do ano passado.", "Entrada gratuita mesmo?"},
                {"Vou levar o cartão atualizado esta semana.", "A enfermaria faz a conferência?"},
                {"Consegui acessar pelo guia, valeu.", "O link do portal mudou?"},
                {"Moro perto do novo ponto, ótimo.", "A volta no fim da tarde também muda?"},
                {"Já separei a lista de desejos.", "Aceita PIX na feira?"},
                {"Sempre uso o crachá na entrada.", "Visitante precisa agendar antes?"}
        };

        for (int p = 0; p < posts.size(); p++) {
            SavedPost sp = posts.get(p);
            Announcement a = sp.announcement();
            int cIdx = Math.min(p, commentTexts.length - 1);
            int u0 = 5 + p * 3;
            int u1 = 12 + p * 7;
            User author0 = studentUsers.get(u0 % studentUsers.size());
            User author1 = studentUsers.get(u1 % studentUsers.size());
            AnnouncementComment c1 = announcementCommentRepository.save(AnnouncementComment.builder()
                    .announcement(a)
                    .user(author0)
                    .content(commentTexts[cIdx][0])
                    .build());
            AnnouncementComment c2 = announcementCommentRepository.save(AnnouncementComment.builder()
                    .announcement(a)
                    .user(author1)
                    .content(commentTexts[cIdx][1])
                    .build());
            LocalDateTime ac = sp.baseTime().plusHours(sp.slot() * 4L + 1);
            jdbcTemplate.update("UPDATE sisges.announcement_comment SET created_at = ? WHERE id = ?",
                    Timestamp.valueOf(ac), c1.getId());
            jdbcTemplate.update("UPDATE sisges.announcement_comment SET created_at = ? WHERE id = ?",
                    Timestamp.valueOf(ac.plusMinutes(20)), c2.getId());

            int likeStart = p * 11;
            int likeCount = 8 + (p % 7);
            for (int k = 0; k < likeCount; k++) {
                User liker = studentUsers.get((likeStart + k) % studentUsers.size());
                if (announcementLikeRepository.existsByAnnouncementIdAndUserId(a.getId(), liker.getId())) {
                    continue;
                }
                AnnouncementLike like = announcementLikeRepository.save(AnnouncementLike.builder()
                        .announcement(a)
                        .user(liker)
                        .build());
                jdbcTemplate.update("UPDATE sisges.announcement_like SET created_at = ? WHERE id = ?",
                        Timestamp.valueOf(ac.plusMinutes(30 + k)), like.getId());
            }
            if (p % 3 == 0) {
                User tLiker = teachers.get(p % teachers.size()).getBaseData();
                if (!announcementLikeRepository.existsByAnnouncementIdAndUserId(a.getId(), tLiker.getId())) {
                    AnnouncementLike lk = announcementLikeRepository.save(AnnouncementLike.builder()
                            .announcement(a)
                            .user(tLiker)
                            .build());
                    jdbcTemplate.update("UPDATE sisges.announcement_like SET created_at = ? WHERE id = ?",
                            Timestamp.valueOf(ac.plusMinutes(45)), lk.getId());
                }
            }
        }
    }

    private SavedPost createPost(
            String title,
            String content,
            String type,
            String imagePath,
            User author,
            LocalDateTime base,
            int slot
    ) {
        Announcement.AnnouncementBuilder b = Announcement.builder()
                .title(title)
                .content(content)
                .type(type)
                .createdBy(author);
        if ("IMAGE".equals(type) && imagePath != null) {
            b.imagePath(imagePath);
        }
        Announcement saved = announcementRepository.save(b.build());
        return new SavedPost(saved, base, slot);
    }

    private record SavedPost(Announcement announcement, LocalDateTime baseTime, int slot) {}
}
