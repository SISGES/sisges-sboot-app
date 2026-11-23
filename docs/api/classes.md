# Turmas e disciplinas

Uma turma reúne alunos da mesma série. O campo `academicYear` representa a série, por exemplo `6º ano`. Use os IDs de professor e aluno retornados pelas buscas específicas de cada perfil. Os exemplos pressupõem uma sessão com o perfil exigido. [Diretório de pessoas](identity.md)

## Turmas e vínculos

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `POST /api/classes` | ADMIN | `{name,academicYear,studentIds?,teacherIds?,disciplineIds?}` | `200 Class`. Exige nome não vazio e série. Cria a turma e vincula os alunos, professores e disciplinas selecionados em uma transação. Um conflito ao criar a turma retorna `409`. |
| `POST /api/classes/search` | ADMIN, TEACHER | `{name,academicYear}` opcional | `200 [{id,name,academicYear,studentCount,teacherCount}]`. Busca parcial por nome e exata por série. Em ordem de nome, até 1.000 resultados. Professores com cadastro docente ativo recebem suas turmas vinculadas. |
| `GET /api/classes/{id}` | ADMIN, TEACHER | ID da turma | `200 Class`. Para professores, exige vínculo ativo com a turma. Caso contrário, retorna `403`. |
| `DELETE /api/classes/delete/{id}` | ADMIN | ID da turma | `204`. Marca a turma como excluída. Uma turma inexistente ou já excluída retorna `404`. |
| `POST /api/classes/{classID}/student/add/{id}` | ADMIN | ID do aluno, sem corpo | `200 Class`. Define a turma atual do aluno, inclusive quando ele já pertence a outra turma. |
| `POST /api/classes/{classID}/student/remove/{id}` | ADMIN | ID do aluno, sem corpo | `200 Class`. Limpa a turma atual do aluno somente se ela corresponder à turma informada. |
| `POST /api/classes/{classID}/teacher/add/{id}` | ADMIN | ID do professor e `{disciplineIds:[…]}` opcional | `200 Class`. Cria ou reativa o vínculo do professor e vincula as disciplinas selecionadas na mesma transação. |
| `POST /api/classes/{classID}/teacher/remove/{id}` | ADMIN | ID do professor, sem corpo | `200 Class`. Marca o vínculo entre professor e turma como excluído. Os vínculos das disciplinas com a turma permanecem ativos. |
| `POST /api/classes/{classID}/discipline/add/{id}` | ADMIN | ID da disciplina, sem corpo | `200 Class`. Cria ou reativa o vínculo da disciplina. |
| `POST /api/classes/{classID}/discipline/remove/{id}` | ADMIN | ID da disciplina, sem corpo | `200 Class`. Marca o vínculo da disciplina como excluído. |

**Class** contém `id,name,academicYear,students,teachers,disciplines`. Cada aluno contém `id,name,email,present` (a frequência é nula nesta consulta). Cada professor contém `id,name,email` e cada disciplina contém `id,name`. As alterações de vínculo retornam a turma resultante. Algumas operações retornam a turma mesmo quando nenhuma linha é alterada. Confira os vínculos retornados quando precisar confirmar o resultado. [Operações de turma](../../internal/api/classes.go)

Ao adicionar um professor a uma turma existente, consulte primeiro as disciplinas desse professor. Se houver uma única disciplina, uma seleção vazia a vincula automaticamente. Se houver várias, informe ao menos uma. A ausência de disciplinas disponíveis ou uma seleção inválida retorna `400 VALIDATION_ERROR` no campo `disciplineIds`. IDs repetidos em uma seleção válida são considerados uma única vez. Na criação inicial da turma, `teacherIds` e `disciplineIds` são listas independentes. [Vínculo de professor](../../internal/api/class_teacher.go)

## Disciplinas

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /api/disciplines` | ADMIN, TEACHER | Sem filtros | `200 Discipline[]`, até 1.000 disciplinas ativas em ordem de nome. |
| `GET /api/disciplines/{id}` | ADMIN, TEACHER | ID da disciplina | `200 Discipline` ou `404`. |
| `POST /api/disciplines` | ADMIN | `{name,description?,teacherIds?}` | `201 Discipline`. Exige nome não vazio. Nome duplicado retorna `400`. |
| `PUT /api/disciplines/update/{id}` | ADMIN | `{name,description?,teachers?:[{teacherId,vinculado}]}` | `200 Discipline`. O valor `true` vincula o professor e `false` remove o vínculo. Professores omitidos mantêm seus vínculos. Omitir a descrição atribui a ela o valor nulo. |

**Discipline** contém `id,name,description,teachers`. A descrição pode ser nula e cada professor contém `id,name,email`. Para renomear uma disciplina e vincular o professor 17, por exemplo, envie `{"name":"Matemática","description":"Álgebra","teachers":[{"teacherId":17,"vinculado":true}]}`. [Operações de disciplina](../../internal/api/disciplines.go)
