# Administração escolar

**Escopo** · Especificação funcional dos requisitos P-01 e P-02. As equipes de produto e QA podem usar os cenários para validar o cadastro e a composição das turmas.

## Papéis e pré-condições

Um administrador já cadastrado cria as demais contas em `/admin/register`. O acesso requer e-mail escolar e senha. Em caso de sucesso, a API retorna um token Bearer e o papel do usuário. Ao recarregar a página, a versão de referência do cliente tenta interpretar como JSON a resposta vazia da validação e apaga a sessão armazenada. O usuário precisa entrar novamente. A API exige autenticação de administrador para novos cadastros. [Restauração de sessão](../architecture/system.md) · [Operações de conta](../../internal/api/users.go) · [Controle de acesso por papel](../../internal/api/app.go) · [Sessão no frontend](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/AuthContext.tsx)

## Cadastro e alterações de conta

1. Informar nome, senha inicial, data de nascimento, gênero e papel. Para um aluno, informar o ID de um responsável já cadastrado ou os dados de contato de um novo responsável. A turma atual é opcional.
2. O cadastro gera um identificador com prefixo A para aluno, P para professor e ADM para administrador. O identificador em minúsculas seguido de `@sisges.com` forma o e-mail de acesso. Essa operação gera a identificação da conta sem criar uma caixa de correio. O sistema grava o usuário e os registros e vínculos de aluno, professor ou responsável aplicáveis na mesma transação.
3. O administrador pode buscar alunos e professores. A consulta específica do aluno retorna a turma atual, a frequência e os responsáveis vinculados. As operações do diretório usam o ID de aluno ou professor quando indicado. A consulta geral de dados de usuário usa o ID da conta.
4. Qualquer usuário autenticado pode consultar a própria conta. Administradores e professores podem alterar nome, imagem de perfil e senha. A troca exige a senha atual e uma nova senha com pelo menos oito bytes. O cadastro inicial aceita senhas a partir de seis bytes. Nesse fluxo, o aluno só pode consultar a conta. [Cadastro e perfil](../../internal/api/users.go) · [Dados do aluno](../../internal/api/students.go) · [Tela da conta](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Account/Account.tsx)

**Limites da integração.** O formulário de cadastro chama `POST /api/responsibles/search` para preencher o seletor de responsáveis existentes. Essa operação não existe no roteador e o formulário transforma a falha de busca em uma lista vazia. O backend aceita o ID de um responsável conhecido. O frontend também permite que professores abram `/users/:id`, mas a operação de consulta geral de usuário é exclusiva do administrador. [Formulário de cadastro](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/RegisterUser/RegisterUser.tsx) · [Chamadas do frontend](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/userService.ts) · [Rotas da API](../../internal/api/app.go)

As listas de alunos e professores enviam o ID do perfil acadêmico para a tela geral de dados do usuário. Como essa tela consulta pelo ID da conta, selecionar uma linha pode abrir outra conta ou retornar um erro de registro inexistente quando os identificadores divergem. Nas integrações com o diretório, usar o ID de cada perfil na consulta específica de aluno ou professor. [Lista de alunos](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Students/Students.tsx) · [Lista de professores](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Teachers/Teachers.tsx) · [Tela de dados do usuário](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/UserDetail/UserDetail.tsx)

## Turmas e vínculos

O administrador cria uma turma com nome e série. Pode incluir IDs iniciais de alunos, professores e disciplinas. Cada aluno tem uma única turma atual. Adicioná-lo a outra turma transfere o vínculo. Removê-lo da turma atual deixa o cadastro sem turma. Um professor pode atuar em várias turmas e disciplinas. A exclusão de uma turma registra a data e a hora da exclusão. [Operações de turma](../../internal/api/classes.go) · [Gestão de disciplinas](../../internal/api/disciplines.go)

Ao adicionar um professor a uma turma **existente**, a operação de vínculo consulta suas disciplinas ativas. Se houver uma única disciplina, o sistema a seleciona automaticamente. Se houver várias, pede uma seleção. A operação rejeita um professor sem disciplina ou uma disciplina sem vínculo com ele. O sistema grava o vínculo do professor e as disciplinas selecionadas para a turma em uma transação. Na criação inicial de uma turma, a operação aceita listas separadas de professores e disciplinas. A grade verifica os vínculos entre professor e turma, professor e disciplina, e turma e disciplina para identificar as aulas que cada professor pode registrar. [Vínculo de professor](../../internal/api/class_teacher.go) · [Casos de teste do vínculo](../../internal/api/class_teacher_test.go) · [Validação da grade](../../internal/api/schedules.go)

## Cenários de aceite

| Dado | Quando | Então |
| --- | --- | --- |
| Um administrador e um novo aluno com contato do responsável | O administrador conclui o cadastro | O sistema retorna o identificador de acesso e vincula o responsável ao registro do aluno. |
| Um cadastro de aluno sem ID de responsável e sem novos dados de contato | O administrador envia o formulário | O sistema retorna um erro de regra de negócio antes de criar o usuário. |
| Uma sessão válida de aluno | O aluno abre a própria conta e tenta atualizar o perfil | A consulta funciona e a atualização retorna HTTP 403. |
| Um professor com duas disciplinas e uma turma sem vínculo com nenhuma delas | O administrador vincula o professor e seleciona uma das disciplinas | Os dados da turma incluem o professor e a disciplina escolhida. Omitir a seleção causa falha antes da gravação. |
| Um aluno matriculado na turma A | O administrador o adiciona à turma B | O aluno aparece na lista da turma B e o portal passa a mostrar B como turma atual. |

## Quando revisar

Ao alterar cadastros ou composição de turmas, revisar as regras de matrícula, a busca de responsáveis, o acesso por papel, as transferências de alunos, a seleção de professores e os valores de série aceitos. Coordenar mudanças na lista de alunos com as equipes que mantêm a grade, os materiais e o lançamento de notas.
