# Requisitos de produto do SISGES

**Público** · Gestão escolar, produto, engenharia e garantia da qualidade (QA)

**Referência de implementação** · A situação dos requisitos descreve o frontend e o backend preparados em conjunto com a migração V25. A implantação em produção, a adoção pelas escolas e as metas de nível de serviço exigem evidências próprias.

## Objetivo e usuários

O administrador cadastra pessoas, organiza turmas e disciplinas e atribui aulas aos professores. O professor registra a aula prevista na grade e faz a chamada. O aluno consulta as faltas registradas, os comunicados da escola e as notas liberadas. O banco de dados mantém uma escola lógica, com um único ciclo letivo vigente e histórico por ciclo. As regras de avaliação ficam registradas no início de cada ano. A estratégia para atender várias escolas independentes continua em aberto.

| Papel | O que precisa fazer | Onde atua |
| --- | --- | --- |
| Administrador | Preparar turmas, configurar as regras de notas, acompanhar aulas e publicar eventos e avisos | Telas administrativas, lista de aulas, gestão de eventos e mural do Início |
| Professor | Consultar horários atribuídos, registrar conteúdo e frequência, distribuir material de estudo, lançar e liberar notas | Aulas, Materiais, mural do Início e rota de lançamento de notas |
| Aluno | Encontrar colegas e professores, consultar atividades, acompanhar faltas e ver as notas liberadas | Início, Minha turma, Faltas e Boletim |
| Responsável | Informar seus dados de contato na matrícula do aluno | Cadastro de responsável vinculado ao aluno. Os papéis que permitem entrar no sistema são ADMIN, TEACHER e STUDENT. |

Na interface, a *turma* agrupa os alunos, a *disciplina* identifica o componente curricular e a *aula* é um encontro em uma data específica. A *frequência* registra presenças e faltas, e o *boletim* reúne as notas do aluno. O campo `academicYear` da turma identifica a série. O ciclo letivo representa o ano que a escola está administrando. [Navegação da interface](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/config/navConfig.ts) · [Rotas HTTP](../../internal/api/app.go)

## Requisitos e critérios de aceite

“Implementado” indica que o componente que faz a chamada e a operação que a recebe dão suporte ao fluxo. Os cenários das especificações abaixo podem servir para verificar uma versão com dados escolares fictícios.

| ID | Funcionalidade e critério de aceite | Situação | Especificação |
| --- | --- | --- | --- |
| P-01 | O administrador cadastra um aluno com um novo responsável. O e-mail escolar retornado permite entrar e abrir o portal do aluno. | Implementado. O cadastro também permite buscar um responsável existente por meio da API administrativa. Ao restaurar a sessão, o cliente aceita a resposta vazia de sucesso da validação. | [Administração escolar](school-administration.md) |
| P-02 | O administrador vincula um aluno, professores e disciplinas a uma turma. É possível consultar a composição atual da turma e seus vínculos. | Implementado | [Administração escolar](school-administration.md) |
| P-03 | O administrador publica a grade semanal de disciplinas. Uma tentativa de salvar uma revisão desatualizada preserva a mais recente. | Implementado | [Aulas e frequência](teaching-and-attendance.md) |
| P-04 | O professor registra uma aula em um horário atribuído e lança a frequência. Salvar novamente preserva o ID da aula, a frequência e as atividades. | Implementado. Os anexos dependem do armazenamento de objetos. | [Aulas e frequência](teaching-and-attendance.md) |
| P-05 | O professor publica material para uma disciplina e uma turma às quais está vinculado. O aluno matriculado vê o material no Início. | Implementado para texto e anexos. O envio de arquivos depende de R2 configurado, e a leitura depende de autorização para o material. | [Aulas e frequência](teaching-and-attendance.md) |
| P-06 | A equipe cria uma atividade com pontuação, salva as notas e faz a liberação. O aluno vê no boletim somente as notas liberadas. | Implementado no fluxo da aula. O formulário informa a pontuação e a lista de atividades abre o lançamento de notas. As notas liberadas entram no boletim do aluno. | [Avaliação e progressão](grading-and-progression.md) |
| P-07 | O administrador configura a avaliação, avança os trimestres e encerra o ano após resolver as liberações pendentes. O sistema registra os resultados e as promoções cabíveis. | Implementado em Notas. O fechamento exige a senha do administrador e a resolução das pendências. Um novo ano tem identidade própria. Resultados e regras anteriores permanecem vinculados ao respectivo ciclo. | [Avaliação e progressão](grading-and-progression.md) |
| P-08 | A equipe publica avisos com prazo de exibição, os usuários autenticados interagem com eles e o administrador agenda eventos para públicos específicos. | Implementado. Mural e eventos têm regras de público distintas. | [Comunicação e portal do aluno](communications-and-student-portal.md) |
| P-09 | O aluno consulta o diretório da turma atual, as faltas por disciplina, os materiais e o próprio boletim. | Implementado. O aluno pode mudar de ano letivo no boletim quando há histórico. Algumas consultas dependem de matrícula em uma turma e da liberação das notas. | [Comunicação e portal do aluno](communications-and-student-portal.md) |

## Indicadores de sucesso

Os indicadores a seguir são propostas para avaliar um piloto escolar. A escola e a equipe de produto precisam acordar metas numéricas e a frequência dos relatórios. As aplicações fornecem registros operacionais para os cálculos. Não há painel de indicadores do piloto nas versões analisadas.

| Pergunta | Medida proposta e fonte | Interpretação |
| --- | --- | --- |
| Os professores conseguem concluir a chamada? | Para uma semana letiva acordada, comparar os horários previstos em cada data com as aulas registradas. Depois, comparar a quantidade de alunos da turma com os lançamentos de frequência por aula. | Usar registros confirmados pela escola para descontar aulas canceladas, feriados e mudanças de matrícula. A grade semanal pode mudar. |
| A equipe consegue concluir os lançamentos no prazo? | Contar as atividades ativas de cada trimestre que ainda aguardam liberação na data de fechamento acordada. Consultar as liberações pendentes na API para identificar os professores a contatar. | Uma atividade liberada pode conter notas em branco. Acompanhar esses casos separadamente com acesso autorizado à tela de lançamento. |
| Os alunos encontram a informação acadêmica de que precisam? | Em um piloto supervisionado, pedir a um aluno matriculado que localize as faltas de uma disciplina indicada e uma nota liberada. Registrar a conclusão da tarefa sem coletar notas para análise de uso. | Observar quando o aluno precisa de ajuda para distinguir nota pendente, nota em branco e zero. |

## Dependências e decisões

A escola fornece as listas de alunos, os vínculos de disciplinas, a política de avaliação e as turmas de destino para a progressão. A engenharia fornece o banco com as migrações aplicadas, API e cliente configurados e armazenamento de objetos para os recursos com arquivos. Antes do lançamento comercial, produto e direção escolar precisam definir o escopo da matrícula, as regras de avaliação e o histórico acadêmico. A operação precisa definir as responsabilidades por recuperação e suporte. [Registro de preparação para SaaS](saas-readiness.md)

## Quando revisar

Revisar os requisitos afetados sempre que mudar um papel, uma rota do portal, uma transição acadêmica, um destino de arquivos, a organização dos períodos letivos ou o modelo de isolamento entre escolas. Na mesma alteração, atualizar os cenários de aceite e o registro de preparação para SaaS.
