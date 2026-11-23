# ADR 001 — Separação entre a grade semanal e os registros de aula por data

Público e responsabilidade — engenharia dos fluxos acadêmicos e do backend

Situação e escopo — registro retrospectivo da decisão implementada na V24

Revisões de referência — backend `6b37a18`, frontend `b58d2df`

## Contexto

A administração planeja a grade semanal por turma e disciplina. O professor registra o que aconteceu em determinada data, anexa arquivos e usa o identificador daquela aula para lançar frequência e avaliações. Uma mudança na grade precisa preservar a disciplina, o professor e o trabalho acadêmico já registrado. Bases mais antigas também contêm aulas cadastradas manualmente, com horários livres. [Migração V24](../../db/migration/V24__weekly_timetable_and_lesson_records.sql) · [Operações de aula](../../internal/api/meetings.go)

## Decisão implementada

1. Manter a grade semanal vigente de cada turma em `class_schedule`, com chave composta por turma, dia da semana e horário. Os cinco horários correspondem a períodos fixos de 50 minutos, com intervalo das 09:30 às 09:45 e término às 11:25. O banco armazena o número do horário e o código Go define seu início e fim. [Implementação da grade](../../internal/api/schedules.go)
2. Substituir toda a grade em uma transação atômica. O administrador envia a última revisão consultada e os horários desejados. O servidor bloqueia a linha da turma, confere a revisão e os vínculos das disciplinas, substitui os horários e incrementa a revisão. Uma lista vazia limpa a grade para as aulas ainda sem registro. [Gravação da grade](../../internal/api/schedules.go)
3. Montar a visão diária do professor com os horários do dia da semana que ele pode lecionar e ainda não registrou, mais as aulas da grade que ele já registrou naquela data. No primeiro salvamento, bloquear a linha da turma, verificar os vínculos e o horário, rejeitar sobreposições e gravar a aula. Um índice único parcial preserva a identidade da aula ativa por turma, data e horário. Ao salvar novamente com autorização, atualizar apenas o conteúdo e os anexos da mesma linha. [Consulta e registro por dia](../../internal/api/schedules.go) · [Índice no esquema](../../db/migration/V24__weekly_timetable_and_lesson_records.sql)
4. Preservar as tabelas de aula e as chaves estrangeiras de seus registros dependentes. Aulas anteriores sem posição na grade mantêm os horários originais. A rota antiga de criação manual retorna um erro de regra de negócio. Ao tentar editar uma aula da grade pela rota genérica, o cliente recebe orientação para usar o fluxo de registro da grade. [Compatibilidade com aulas antigas](../../internal/api/meetings.go)

## Consequências e limites

Suponha que o professor registre matemática no primeiro horário de uma segunda-feira e que a administração depois remova esse horário. A consulta daquela segunda-feira ainda apresenta a aula de matemática. Uma segunda-feira posterior sem registro já reflete a alteração. Datas passadas sem aula registrada também seguem a nova grade, pois o plano semanal não guarda histórico de vigência. [Consulta das aulas](../../internal/api/schedules.go) · [Teste de preservação do registro](../../internal/api/schedules_test.go)

A grade e os registros de aula disputam o bloqueio da mesma linha da turma, inclusive para datas diferentes. Uma revisão desatualizada da grade resulta em HTTP 400 com `BUSINESS_RULE_VIOLATION`. O cliente precisa consultar a revisão atual e conciliar o rascunho. O conteúdo da aula não tem campo de revisão. Edições sucessivas pelo mesmo professor autorizado podem substituir o conteúdo anterior. O bloqueio e o índice parcial preservam a identidade da aula e seus vínculos. [Gravação da grade e das aulas](../../internal/api/schedules.go) · [Erros da aplicação](../../internal/api/errors.go)

## Quando revisar

Reavaliar a decisão se houver grades com período de vigência, horários ou feriados por escola, verificação de conflitos do professor entre turmas, vários editores para uma aula ou regras próprias de cada escola. Esses requisitos podem mudar a chave da grade, a consulta histórica, os rascunhos da interface ou o contrato de gravação. Atualizar os cenários de regressão da V24, o modelo de dados e a referência da API junto com a mudança. [Cenários com banco de dados](../../internal/api/schedules_test.go) · [Modelo de dados](data-model.md) · [Referência da API](../api/README.md)
