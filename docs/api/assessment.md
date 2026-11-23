# Avaliação, ciclo letivo e boletins

O ID da atividade identifica uma avaliação vinculada a uma aula. O ID de aluno identifica a linha de lançamento de nota. O boletim é calculado para a turma atual do aluno. [Operações de atividades](../../internal/api/activities.go) · [Cálculo do boletim](../../internal/api/report.go)

## Atividades e notas

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /api/activities/meeting/{id}` | ADMIN, TEACHER | ID da aula | `200 Activity[]`. Lista as atividades ativas associadas à aula ativa informada, das mais recentes às mais antigas, até 1.000. A consulta filtra pelo ID da aula. |
| `GET /api/activities/my` | STUDENT | Sem corpo | `200 Activity[]` para a turma atual do aluno. Inclui os dados das atividades com notas liberadas ou pendentes, das mais recentes às mais antigas, até 1.000. |
| `POST /api/activities` | ADMIN, TEACHER | `{classMeetingId,title,maxPoints,activityType?,trimesterNumber?,description?,filePath?}` | `201 Activity`. O ciclo deve estar em `IN_PROGRESS`. Se o solicitante for professor, a aula deve ser dele. Exige título não vazio, pontuação máxima positiva e tipo válido. `trimesterNumber` pode ser nulo ou variar de 1 a 3. |
| `GET /api/activities/{id}/gradebook` | ADMIN, TEACHER | ID da atividade | `200 Gradebook`. A rota é atendida pelo padrão `GET /api/activities/{rest...}`. Outros sufixos tratados por esse curinga retornam `404`. |
| `PUT /api/activities/{id}/grades` | ADMIN, TEACHER | `{entries:[{studentId,score}]}` | `200 Gradebook`. Exige um lote não vazio e uma atividade com notas ainda não liberadas. Insere ou atualiza notas apenas de alunos da turma atual da atividade. `score` pode ser nulo ou estar entre zero e `maxPoints`, inclusive. Um item inválido cancela todo o lote. |
| `POST /api/activities/{id}/release` | ADMIN, TEACHER | Sem corpo | `200 Activity`. Marca as notas como liberadas e registra a data e o usuário da primeira liberação. É possível liberar notas mesmo com campos em branco. Gravações de notas posteriores retornam `400`. |
| `DELETE /api/activities/{id}` | ADMIN, TEACHER | ID da atividade | `204` ao marcar a atividade como excluída. |

Para consultar o lançamento, gravar ou liberar notas ou excluir a atividade, o professor precisa ser o responsável pela aula da atividade. O descumprimento dessa regra retorna `400 BUSINESS_RULE_VIOLATION`. Administradores podem gerenciar atividades em todas as aulas. Os tipos são `PROVA`, `ATIVIDADE` (padrão na criação), `TRABALHO`, `RECUPERACAO_TRIMESTRE` e `RECUPERACAO_ANUAL`. [Validações das atividades](../../internal/api/activities.go)

**Activity** contém `id,classMeetingId,title,description,filePath,activityType,trimesterNumber,maxPoints,released,releasedAt,createdAt`. **Gradebook** contém `activityId,classMeetingId,title,activityType,trimesterNumber,maxPoints,released,students`. Cada aluno contém `studentId,userId,studentName,score`. Para uma atividade de 10 pontos, `{"entries":[{"studentId":12,"score":8.5},{"studentId":14,"score":null}]}` lança 8,5 pontos para um aluno e limpa a nota do outro.

## Configuração de notas e transições do ciclo

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /api/grading-config` | ADMIN, TEACHER | Sem corpo | `200 GradingConfig`. |
| `PUT /api/grading-config` | ADMIN | Objeto `GradingConfig` completo | `200 GradingConfig`. O máximo anual deve ser a soma dos máximos trimestrais. Em cada trimestre, os pontos das três categorias devem ser positivos e somar o máximo do trimestre. A atualização não consulta `gradingLocked`. |
| `GET /api/academic-cycle` | Qualquer perfil | Sem corpo | `200 Cycle`. |
| `POST /api/academic-cycle/start-year` | ADMIN | `{yearEndDate:"YYYY-MM-DD"}` | `200 Cycle`. Exige uma data futura para o término. Altera o ciclo existente para `IN_PROGRESS`, registra a data inicial e define o trimestre 1 e `gradingLocked:true`. Um ciclo já em andamento retorna `400`. |
| `GET /api/academic-cycle/pending-releases` | ADMIN | Parâmetro opcional `trimester=1`, `2` ou `3` na URL | `200 {trimester,teachers}`. Retorna os nomes distintos dos professores com atividades pendentes de liberação. Sem filtro, consulta todos os trimestres e retorna `trimester:null`. |
| `POST /api/academic-cycle/end-trimester` | ADMIN | `{password}` do administrador autenticado | `200 Cycle`. Exige ano letivo em andamento e ausência de liberações pendentes no trimestre atual. Avança um trimestre até o limite de 3. |
| `POST /api/academic-cycle/end-year` | ADMIN | `{password}` do administrador autenticado | `200 Cycle`. Exige ano letivo em andamento e ausência de liberações pendentes em todos os trimestres. Processa os resultados e as promoções de série e então define `FINISHED`. A falta de uma turma de destino pode impedir o encerramento com `400`. |
| `GET /api/boletim/me` | STUDENT | Sem corpo | `200 Report` para o aluno autenticado. Um registro de aluno inexistente retorna `404`. Alunos sem turma recebem os máximos configurados e trimestres sem atividades. |

**GradingConfig** é um objeto sem níveis internos. Envie o inteiro `yearMaxPoints` e o percentual numérico `yearMinPercentage`. Para cada `N` igual a 1, 2 e 3, envie `trimesterNMaxPoints`, `trimesterNMinPercentage`, `trimesterNPointsProvas`, `trimesterNPointsAtividades` e `trimesterNPointsTrabalhos` (por exemplo, `trimester2PointsProvas`). Apenas os percentuais admitem valores fracionários. Leia o objeto com GET antes de editar e enviar a configuração completa. [Configuração e transições](../../internal/api/grading.go)

**Cycle** contém `status` (`NOT_STARTED`, `IN_PROGRESS` ou `FINISHED`) e `yearStartDate,yearEndDate,currentTrimester,gradingLocked,yearStartedAt,yearFinishedAt`. Datas e horários podem ser nulos. Liberações pendentes ou senha incorreta durante uma transição retornam `400 BUSINESS_RULE_VIOLATION`. O encerramento do ano pode alterar a turma dos alunos e não há operação HTTP para desfazê-lo. O processamento usa o ID do ciclo existente e é ignorado se já houver resultados anuais para esse ID. [Operações do ciclo](../../internal/api/grading.go) · [Processamento de fim de ano](../../internal/api/report.go)

**Report** contém `fixedApprovalPercentage` (70), `yearMaxPoints,totalReleasedScore,eligibleForYearRecovery,trimesters,recoveryRow`. Há três objetos de trimestre, cada um com `trimester,trimesterMaxPoints,activities,totalReleasedScore,allActivitiesReleased,eligibleForRecovery`. Cada atividade tem `activityId,title,activityType,maxPoints,score,released`. Notas pendentes são nulas. As notas liberadas das atividades regulares compõem os totais. A linha de recuperação contém `trimesterRecoveryScores` (três notas, possivelmente nulas) e `yearRecoveryScore`. O aluno fica elegível à recuperação trimestral se houver pelo menos uma atividade regular, todas estiverem liberadas e o total estiver abaixo de 70% do máximo trimestral. A recuperação anual exige essa condição de liberação nos três trimestres e total inferior a 70% do máximo anual. [Boletim do aluno](../../internal/api/report.go)
