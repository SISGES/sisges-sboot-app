# Modelo de Banco de Dados — SISGES (Sistema de Gestão Escolar)

## 1. Visão geral e escopo (TCC)

Este documento descreve a arquitetura relacional do banco de dados do SISGES, considerando **PostgreSQL**, **JPA/Hibernate** e **Flyway**, com foco nas funcionalidades iniciais:

- Cadastro de alunos com documentos (RG, certidão de nascimento, histórico escolar, etc.) — dados dos documentos; inserção de arquivos será reintroduzida posteriormente
- Papéis de usuário: **administrador**, **professor**, **aluno**
- Inclusão de alunos e professores (apenas administrador)
- Lançamento de frequência em aulas
- Inclusão de alunos e professores em turmas (apenas administrador)
- Criação de turmas (conjunto alunos + professores; apenas administrador)
- Criação de disciplinas (nome, descrição, materiais e lições)

As decisões de modelagem foram tomadas para atender a normalização (3FN), rastreabilidade, auditoria e evolução futura, justificando-as no contexto de um Trabalho de Conclusão de Curso (TCC).

---

## 2. Modelo relacional (entidades e relacionamentos)

### 2.1 Diagrama conceitual (entidades principais)

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────────┐
│    user     │       │ student_responsible│       │  document_type  │
├─────────────┤       ├──────────────────┤       ├─────────────────┤
│ id (PK)     │       │ id (PK)           │       │ id (PK)         │
│ name        │       │ name              │       │ name            │
│ email       │       │ phone             │       │ description     │
│ register    │       │ alternative_phone │       └────────┬────────┘
│ password    │       │ email             │                │
│ birth_date  │       │ alternative_email │                │
│ gender      │       └────────┬──────────┘                │
│ role        │                │                            │
│ created_at  │       ┌────────▼──────────┐       ┌────────▼────────┐
│ updated_at  │       │     student      │       │ student_document│
│ deleted_at  │       ├──────────────────┤       ├─────────────────┤
└──────┬──────┘       │ id (PK)          │       │ id (PK)         │
       │              │ user_id (FK)     │◄──────│ student_id (FK) │
       │              │ responsible_id   │       │ document_type_id│
       │              │ class_id (FK)    │       │ document_number │
       │              └────────┬──────────┘       │ issuing_authority│
       │                       │                 │ issue_date      │
       │              ┌────────▼──────────┐     └─────────────────┘
       │              │   school_class    │
       │              ├──────────────────┤
       │              │ id (PK)          │     ┌─────────────────┐
       │              │ name             │     │   discipline     │
       │              │ academic_year    │     │ id (PK)         │
       │              └────────┬──────────┘     │ name           │
       │                       │                │ description    │
       │              ┌────────▼──────────┐     └────────┬───────┘
       │              │  teacher_class   │              │
       │              ├──────────────────┤     ┌────────▼────────┐
       │              │ teacher_id (FK)   │     │ class_discipline│
       │              │ class_id (FK)    │     ├─────────────────┤
       │              └────────┬──────────┘     │ class_id (FK)   │
       │                       │                │ discipline_id   │
       │              ┌────────▼──────────┐     └────────┬───────┘
       │              │   teacher        │              │
       │              ├──────────────────┤     ┌────────▼────────┐
       │              │ id (PK)          │     │ class_meeting   │
       │              │ user_id (FK)     │     ├─────────────────┤
       │              └──────────────────┘     │ id (PK)         │
       │                                        │ class_id (FK)   │
       │                                        │ discipline_id   │
       │                                        │ meeting_date    │
       │                                        └────────┬────────┘
       │                                                 │
       │                                        ┌────────▼────────┐
       │                                        │   attendance    │
       │                                        ├─────────────────┤
       │                                        │ id (PK)         │
       │                                        │ meeting_id (FK) │
       │                                        │ student_id (FK) │
       │                                        │ present         │
       │                                        └─────────────────┘
       │
       │  discipline_material (discipline_id), lesson (discipline_id)
```

### 2.2 Tabelas resumidas

| Tabela | Descrição |
|--------|-----------|
| **users** | Usuários do sistema (admin, professor, aluno). Autenticação e dados comuns. |
| **student_responsible** | Responsável legal pelo aluno (pai/mãe/tutor). |
| **student** | Perfil de aluno: vincula user, responsável e turma atual. |
| **document_type** | Catálogo de tipos de documento (RG, certidão, histórico etc.). |
| **student_document** | Documentos do aluno (tipo, número, órgão emissor, data); sem armazenamento de arquivo por enquanto. |
| **teacher** | Perfil de professor: vincula user. |
| **school_class** | Turma por ano (nome ex.: "1º ano", ano letivo); ensino fundamental e médio, sem período. |
| **teacher_class** | Associação N:N professor–turma. |
| **discipline** | Disciplina (nome, descrição). |
| **class_discipline** | Disciplinas ministradas em cada turma. |
| **discipline_material** | Materiais da disciplina (título, descrição, tipo); sem arquivo por enquanto. |
| **lesson** | Lições/aulas teóricas da disciplina (ordem, título, descrição). |
| **class_meeting** | Ocorrência de aula (turma + disciplina + data). |
| **attendance** | Frequência: presente/ausente por aluno por aula. |
| **user_logs** | Log de ações do usuário (user_id, action_id: 0=DELETE, 1=CREATE, 2=UPDATE, 3=LOGIN, 4=LOGOUT). |

Todas as tabelas utilizam **soft delete** (`deleted_at`).

---

## 3. Justificativas de modelagem (TCC)

### 3.1 Single table para usuário (user) e papel (role)

- **Decisão:** Uma única tabela `users` com atributo `role` (ADMIN, TEACHER, STUDENT).
- **Justificativa:** Reduz duplicação de dados de login (e-mail, senha, nome, data de nascimento). Permite um único fluxo de autenticação e autorização por papel. Para TCC, a simplicidade e a aderência a padrões de segurança (um usuário = um login) justificam a escolha; em trabalhos futuros pode-se evoluir para RBAC com tabela de papéis e permissões.

### 3.2 Entidades student e teacher separadas de user

- **Decisão:** `student` e `teacher` referenciam `users` (1:1). Dados específicos (ex.: responsável, turma) ficam nas entidades de perfil.
- **Justificativa:** Segue o padrão “perfil por contexto”: o mesmo conceito de usuário (autenticação) é estendido por papéis sem poluir a tabela de usuários. Facilita regras de negócio (ex.: só aluno tem documentos escolares; só professor tem vínculo com turmas) e futuras extensões (ex.: secretário, coordenador).

### 3.3 Documentos do aluno em tabela própria com tipo parametrizável

- **Decisão:** Tabelas `document_type` (catálogo) e `student_document` (número, órgão emissor, data de emissão). Inserção de arquivos será reintroduzida em versão futura.
- **Justificativa:** Atende à necessidade de RG, certidão, histórico etc. sem criar uma coluna por tipo (flexibilidade e normalização). O tipo parametrizável permite incluir novos documentos sem alterar o esquema, relevante para normas educacionais e para argumentação no TCC sobre extensibilidade e reuso.

### 3.4 Turma (school_class) e associações professor–turma e aluno–turma

- **Decisão:** `school_class` com nome (ex.: "1º ano", "2º ano") e ano letivo; sem período, adequado a escola pública de ensino fundamental e médio. Aluno com FK `class_id` (turma atual). Professor–turma em tabela de associação `teacher_class` (N:N).
- **Justificativa:** Reflete a realidade escolar: uma turma agrupa muitos alunos e vários professores. Um aluno possui uma turma atual por vez; professores podem lecionar em várias turmas. A associação N:N entre professor e turma é modelada explicitamente para consultas e regras de permissão (ex.: apenas admin altera vínculos).

### 3.5 Disciplina independente e ligada à turma

- **Decisão:** `discipline` (nome, descrição); `class_discipline` associa turma e disciplina; `discipline_material` e `lesson` pertencem à disciplina.
- **Justificativa:** Disciplina é um conceito reutilizável (ex.: “Matemática”) aplicado a várias turmas. Materiais e lições pertencem à disciplina, não à turma, evitando duplicação. A tabela `class_discipline` indica quais disciplinas são oferecidas em cada turma, permitindo depois associar professor por disciplina por turma, se necessário.

### 3.6 Ocorrência de aula (class_meeting) e frequência (attendance)

- **Decisão:** `class_meeting` (turma, disciplina, data) e `attendance` (class_meeting, student, present).
- **Justificativa:** Frequência é lançada por “aula” (ocorrência em uma data), e não apenas por disciplina. Isso permite múltiplas aulas da mesma disciplina na mesma turma em datas diferentes e um registro de presença/ausência por aluno por aula, alinhado a requisitos legais e pedagógicos citáveis no TCC.

### 3.7 Soft delete e auditoria

- **Decisão:** Uso de `deleted_at` em **todas** as tabelas (users, school_class, student, teacher, discipline, student_document, discipline_material, lesson, class_meeting, attendance, student_responsible, document_type e nas associações teacher_class e class_discipline).
- **Justificativa:** Preserva histórico para fins legais e auditoria, evita quebra de integridade referencial e permite relatórios históricos, argumentos importantes na fundamentação do TCC.

### 3.8 Log de ações (user_logs)

- **Decisão:** Tabela `user_logs` com `user_id`, `action_id` (0=DELETE, 1=CREATE, 2=UPDATE, 3=LOGIN, 4=LOGOUT) e `created_at`. Enum `LogAction` no backend define as ações.
- **Justificativa:** Auditoria de quem fez qual ação e quando; suporte a requisitos de rastreabilidade e segurança em ambiente escolar.

### 3.9 Uso de schema PostgreSQL (sisges)

- **Decisão:** Objetos criados no schema `sisges`.
- **Justificativa:** Separação lógica do domínio da aplicação; facilita backups, permissões e coexistência com outros sistemas no mesmo banco, se necessário.

---

## 4. DDL SQL (resumo)

O DDL completo está nas migrations Flyway em `src/main/resources/db/migration/`:

- **V1__create_schema_and_tables.sql** — schema, tabelas principais, FKs e índices.
- **V2__seed_document_types.sql** — dados iniciais de tipos de documento.
- **V3__school_class_year_only_soft_delete_user_logs_remove_file_path.sql** — turma só por ano (sem período), soft delete em todas as tabelas, criação de user_logs, remoção de colunas de arquivo (file_path).

As tabelas seguem o modelo descrito na seção 2; chaves primárias são `id` (serial/identity); FKs com `ON DELETE` adequado (ex.: RESTRICT em vínculos críticos, SET NULL em `student.class_id` quando a turma for desativada, conforme regra de negócio).

---

## 5. Sugestão de entidades JPA

As entidades JPA estão em `com.unileste.sisges.model` e espelham o modelo relacional:

| Entidade | Tabela | Descrição |
|----------|--------|-----------|
| `User` | users | Usuário do sistema (role: ADMIN, TEACHER, STUDENT). |
| `StudentResponsible` | student_responsible | Responsável legal pelo aluno. |
| `DocumentType` | document_type | Catálogo de tipos de documento. |
| `SchoolClass` | school_class | Turma (nome, ano letivo, período). |
| `Discipline` | discipline | Disciplina (nome, descrição, materiais e lições). |
| `Student` | student | Perfil aluno (user, responsável, turma atual). |
| `StudentDocument` | student_document | Documentos do aluno (tipo, número, arquivo). |
| `Teacher` | teacher | Perfil professor (user). |
| `DisciplineMaterial` | discipline_material | Material da disciplina (título, arquivo, tipo). |
| `Lesson` | lesson | Lição da disciplina (ordem, título, descrição). |
| `ClassMeeting` | class_meeting | Ocorrência de aula (turma + disciplina + data). |
| `Attendance` | attendance | Frequência (presente/ausente por aluno por aula). |
| `UserLog` | user_logs | Log de ações (user_id, action_id conforme enum LogAction). |

Relacionamentos N:N são mapeados com `@ManyToMany` e `@JoinTable`: `SchoolClass` ↔ `Teacher` (teacher_class), `SchoolClass` ↔ `Discipline` (class_discipline). Use `FetchType.LAZY` nas associações para evitar N+1 e ajuste cascatas conforme as regras de negócio.

---

## 6. Convenções para JPA/Hibernate

- **Schema:** `sisges` (configurável em `spring.jpa.properties.hibernate.default_schema` ou `@Table(schema = "sisges")`).
- **Nomenclatura:** tabelas em `snake_case`; colunas em `snake_case`; uso de `@Column(name = "...")` quando o nome da coluna difere do atributo.
- **Flyway:** controle total do DDL; uso de `spring.jpa.hibernate.ddl-auto=validate` em produção para não sobrescrever o banco.
- **Identidade:** `GenerationType.IDENTITY` para compatibilidade com PostgreSQL `SERIAL`/`GENERATED BY DEFAULT AS IDENTITY`.

---

## 7. Referências (sugestão para TCC)

- Date, C. J. *An Introduction to Database Systems*. 8th ed. Addison-Wesley.
- Elmasri, R.; Navathe, S. B. *Sistemas de Banco de Dados*. 6. ed. Pearson.
- Documentação oficial: PostgreSQL, JPA/Hibernate, Flyway (versionamento e boas práticas).
