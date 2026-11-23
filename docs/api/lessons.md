# Grade horária, registro de aulas e frequência

O administrador distribui as disciplinas nos horários semanais de uma turma. O professor consulta uma data e registra a aula correspondente a um horário previsto. O ID desse registro identifica a aula nas operações de frequência e atividades. [Grade e aulas por data](../../internal/api/schedules.go) · [Operações de aula](../../internal/api/meetings.go)

## Grade semanal e aulas do dia

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /api/classes/{id}/schedule` | ADMIN | ID da turma | `200 {revision,slots}` em ordem de dia da semana e horário, ou `404`. |
| `PUT /api/classes/{id}/schedule` | ADMIN | Grade completa `{revision,slots:[{weekday,slot,disciplineId}]}` | `200` com a revisão incrementada e os horários salvos. Substitui toda a grade semanal. Uma lista vazia limpa a grade. Revisão desatualizada, horário duplicado, dia ou horário fora do intervalo ou disciplina sem vínculo com a turma retornam `400 BUSINESS_RULE_VIOLATION`. |
| `GET /api/classes/{id}/lesson-day` | TEACHER | Parâmetro obrigatório `date=YYYY-MM-DD` na URL | `200 DailyLesson[]`. Exige vínculo ativo com a turma. Inclui os horários ainda sem registro nas disciplinas do professor e as aulas que ele já registrou para a data. Uma alteração posterior na grade preserva a disciplina das aulas registradas. |
| `PUT /api/classes/{id}/lesson-day` | TEACHER | `{date,slot,disciplineId,content,attachments:[{name,path}]}` | `200 {id}`. Na primeira gravação, exige vínculos válidos com a turma e a disciplina, horário correspondente na grade e ausência de sobreposição. Para editar, exige o mesmo professor e a mesma disciplina do registro. Preserva o ID da aula e substitui o conteúdo e os anexos. |

O campo `weekday` vai de 1 a 5, de segunda a sexta. Os horários `0,1,2,3,4` começam, respectivamente, às `07:00,07:50,08:40,09:45,10:35`. Cada aula dura 50 minutos. **DailyLesson** contém `slot,disciplineId,disciplineName,startTime,endTime,meetingId,content,attachments`. Um `meetingId` nulo indica um horário ainda sem registro.

Envie conteúdo não vazio ou pelo menos um anexo. O limite é de 20.000 bytes de conteúdo após remover espaços nas extremidades e de dez anexos por aula. O nome de cada anexo deve conter texto e ter até 255 bytes. Use o caminho `/api/files/lesson-files/<userId>/<filename>` retornado pelo envio feito pelo próprio usuário. Para registrar uma aula apenas com texto, por exemplo, o professor pode enviar `{"date":"2026-09-14","slot":0,"disciplineId":5,"content":"Equações lineares","attachments":[]}`. Em caso de conflito, consulte novamente a grade ou as aulas do dia e confira a seleção antes de reenviar. [Validação da aula](../../internal/api/schedules.go) · [Envio de arquivos](files-and-feed.md)

## Registros de aula e frequência

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `POST /api/class` | TEACHER | Criação manual legada | Após a autenticação, sempre retorna `400 BUSINESS_RULE_VIOLATION`. Use o PUT de registro das aulas do dia. |
| `POST /api/class/search` | ADMIN, TEACHER | `{date,disciplineId,classId,teacherId}` opcional | `200 MeetingSummary[]`. Professores com cadastro docente ativo recebem suas próprias aulas. Ordenação por data mais recente e depois por horário de início, até 1.000 resultados. |
| `GET /api/class/{id}` | ADMIN, TEACHER | ID da aula | `200 MeetingDetail`. Para professores, exige que sejam responsáveis pela aula. Caso contrário, retorna `403`. |
| `PUT /api/class/update/{id}` | ADMIN, TEACHER | Apenas registros legados. `{date,startTime,endTime,disciplineId,classId,teacherId?}` | `200 MeetingDetail`. Envie os horários no formato `HH:MM`, com término posterior ao início. Exige professor e disciplina vinculados à turma e ausência de sobreposição. O professor só pode indicar a si mesmo. Uma aula da grade retorna `400` e deve ser editada pelo registro do dia. |
| `DELETE /api/class/delete/{id}` | ADMIN | ID da aula | `204` ao marcar a aula como excluída. Retorna `404` se a aula já foi excluída ou não existe. Aulas com atividades de notas já liberadas retornam `400 BUSINESS_RULE_VIOLATION`. |
| `POST /api/class/{id}/frequency` | ADMIN, TEACHER | `{entries:[{studentId,status}]}` | `204`. Professores só podem lançar frequência nas próprias aulas. Cada item identifica um aluno atualmente vinculado à turma e usa `P` para presente ou `F` para faltoso. A operação insere ou atualiza o lote inteiro em uma transação. Lote vazio ou item inválido retorna `400`. |

**MeetingSummary** contém `id,date,startTime,endTime,disciplineName,className,teacherName`. A busca e a consulta detalhada retornam os horários no formato `HH:MM:SS`. **MeetingDetail** contém `id,date,startTime,endTime,scheduleSlot,content,attachments,teacher,classInfo`. O campo `scheduleSlot` é nulo em registros legados. `teacher` pode ser nulo ou conter `{id,name,email}` e `classInfo` contém `id,name,academicYear,students`. Cada aluno contém `id,name,email,present`. O valor nulo de frequência indica que o aluno ainda não recebeu marcação naquela aula. [Requisições e respostas de aulas](../../internal/api/meetings.go)
