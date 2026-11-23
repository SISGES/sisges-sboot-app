# Identidade e diretório de pessoas

Consulte o diretório específico do perfil para obter o ID de professor ou aluno usado em vínculos, frequência e notas. O ID de usuário identifica a conta de acesso. O registro de professor ou aluno tem ID próprio, que pode ser numericamente diferente. [Usuários e professores](../../internal/api/users.go) · [Alunos](../../internal/api/students.go)

## Autenticação e contas

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `POST /api/auth/login` | Público | `{email,password}` | `200 {accessToken,tokenType:"Bearer",user}`. A busca por e-mail desconsidera espaços nas extremidades e diferenças entre maiúsculas e minúsculas. Credenciais inválidas retornam HTTP 200 com um objeto de erro cujo `status` vale 401. |
| `POST /api/auth/register` | ADMIN | `{name,password,birthDate,gender,role}` e, para alunos, os campos descritos abaixo | `201 User`. Gera a matrícula e o e-mail em uma única transação. |
| `GET /api/auth/validate` | Qualquer perfil | Sem corpo | `200` com corpo vazio. |
| `GET /api/users/me` | Qualquer perfil | Sem corpo | `200 User` com os dados da conta identificada pelo token. |
| `PATCH /api/users/me` | ADMIN, TEACHER | Um ou mais de `name`, `password`, `currentPassword` e `profileImagePath` | `200 User`. Para trocar a senha, informe a senha atual correta e uma nova senha com pelo menos oito bytes. O nome deve ter no mínimo dois bytes após a remoção de espaços nas extremidades e no máximo 255 bytes antes dela. Um caminho de imagem vazio remove a imagem do perfil. Alunos recebem `403`. |
| `GET /api/users/{id}` | ADMIN | ID de usuário | `200 User` ou `404`. |
| `POST /api/users/search` | ADMIN | `{name,email,register,gender,initialDate,finalDate}` opcional | `200 [{id,name,email,role}]`, até 1.000 contas ativas em ordem de nome. Nome, e-mail e matrícula aceitam correspondência parcial sem distinção entre maiúsculas e minúsculas. Gênero exige correspondência exata. As duas datas delimitam o nascimento e são inclusivas. |

**User** contém `id`, `name`, `email`, `register`, `role`, `birthDate`, `gender` e `profileImagePath`, que pode ser nulo. O cadastro exige nome não vazio, senha de pelo menos seis bytes, data de nascimento válida, gênero e um dos três perfis. O servidor gera uma matrícula com prefixo `A`, `P` ou `ADM` e um e-mail em letras minúsculas no domínio `@sisges.com`. Para `STUDENT`, envie `responsibleId` de um responsável existente ou `responsibleData` com `name`, `phone`, `email` e, opcionalmente, `alternativePhone` e `alternativeEmail`. Quando ambos são fornecidos, prevalecem os dados do novo responsável. O campo `classId` é opcional. Um ID de responsável inexistente retorna `404`. [Campos e validações do cadastro](../../internal/api/users.go)

Por exemplo, um administrador autenticado pode enviar o corpo abaixo para `POST /api/users/search` para buscar contas pelo nome e intervalo de nascimento. Para exibir apenas alunos, filtre o `role` no cliente. A operação não oferece esse filtro.

```json
{"name":"Ana","initialDate":"2010-01-01","finalDate":"2011-12-31"}
```

## Professores e alunos

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `POST /api/teachers/search` | ADMIN | `{name,email}` opcional | `200 [{id,name,email}]` com IDs de professor. Correspondência parcial sem distinção entre maiúsculas e minúsculas, ordem de nome e limite de 1.000. |
| `GET /api/teachers/me` | TEACHER | Sem corpo | `200 Teacher`, identificado a partir da conta autenticada. |
| `GET /api/teachers/{id}` | ADMIN | ID de professor | `200 Teacher` ou `404`. |
| `POST /api/students/search` | ADMIN | `{name,email}` opcional | `200 [{id,name,email}]` com IDs de aluno. Mesmos filtros, ordenação e limite da busca de professores. |
| `GET /api/students/{id}` | ADMIN | ID de aluno | `200 Student` ou `404`. Inclui até 1.000 registros de frequência, da data de aula mais recente à mais antiga. |
| `GET /api/students/me/turma` | STUDENT | Sem corpo | `200 {className,academicYear,classmates,teachers}` para a turma ativa do aluno. Retorna `404` se não houver turma ativa. A lista de colegas inclui o próprio aluno. |
| `GET /api/students/me/faltas-por-disciplina` | STUDENT | Sem corpo | `200 [{disciplineName,absenceCount}]`. Conta as faltas registradas para o aluno por disciplina, em ordem de nome. A lista fica vazia se não houver faltas registradas. |

**Teacher** contém `id,name,email,register,birthDate,gender,classes`. Cada turma contém `{id,name,academicYear}`. **Student** tem os mesmos campos de identificação, `currentClass` (pode ser nulo), `responsibles` (`id,name,phone,email`) e `attendances` (`present` e `classMeeting:{disciplineName,meetingDate,createdAt}`). Na consulta da própria turma, cada professor tem `id,name,email`. Os registros dos colegas incluem também `present`, nulo nessa consulta. [Detalhes do professor](../../internal/api/users.go) · [Detalhes e consultas do aluno](../../internal/api/students.go)
