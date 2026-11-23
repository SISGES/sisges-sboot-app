# Comunicação escolar

## Materiais de estudo

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /api/materials` | Qualquer perfil | Parâmetro `classId` para professores e administradores. `disciplineId` é opcional. | `200 Material[]`, dos mais recentes aos mais antigos, até 1.000. Para o aluno, usa sua turma atual. Para o professor, filtra pela turma informada e pelas disciplinas que ele leciona. |
| `POST /api/materials` | TEACHER | `{disciplineId,classId,title,description?,materialType?,filePath?}` | `201 Material`. Exige título não vazio, vínculo do professor com a turma e disciplina vinculada tanto ao professor quanto à turma. |
| `DELETE /api/materials/{id}` | TEACHER | ID do material | `204` ao marcar o material como excluído pelo professor que o criou. Para um professor identificado, um material inexistente ou de outro autor retorna `400`. |

**Material** contém `id,disciplineId,disciplineName,classId,className,title,description,materialType,filePath,createdAt`. A API de materiais aceita `filePath` como campo de texto. Consulte o contrato de envio de arquivos para conhecer os destinos aceitos pelo armazenamento. [Operações de materiais](../../internal/api/content.go) · [Contrato de envio de arquivos](files-and-feed.md)

## Avisos, curtidas e comentários

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /api/announcements/feed` | Público, com token bearer opcional | Sem filtros | `200 Announcement[]`, dos mais recentes aos mais antigos, até 500. Aplica o período de vigência e `hiddenForRoles`. O acesso anônimo usa a visibilidade de aluno. Um token válido identifica o perfil do solicitante e indica se ele curtiu cada aviso. |
| `GET /api/announcements` | ADMIN | Sem corpo | `200 Announcement[]` com até 500 avisos não excluídos, dos mais recentes aos mais antigos, inclusive fora da vigência. |
| `GET /api/announcements/{id}` | ADMIN | ID do aviso | `200 Announcement` ou `404`. |
| `POST /api/announcements` | ADMIN, TEACHER | `{title,type,content?,imagePath?,targetRoles?,hiddenForRoles?,activeFrom?,activeUntil?,ttlHours?}` | `201 Announcement`. Exige título não vazio e `type` igual a `TEXT` ou `IMAGE`. |
| `PUT /api/announcements/{id}` | ADMIN | Campos opcionais do aviso | `200 Announcement`. Aplica os campos informados com valor não nulo. `hiddenForRoles:[]` limpa a lista de exclusões. Para alterar a expiração, envie `activeUntil`. A atualização ignora `ttlHours`. |
| `DELETE /api/announcements/{id}` | ADMIN | ID do aviso | `204`. Exclui o registro, emite uma notificação de exclusão e tenta remover a imagem do armazenamento. |
| `POST /api/announcements/{id}/like` | Qualquer perfil | Sem corpo | `200 {liked}` com valor booleano. Alterna a curtida do solicitante. Repetir a chamada inverte o resultado. |
| `GET /api/announcements/{id}/likes` | Qualquer perfil | Sem corpo | `200 {count,likedBy:[{id,name}]}` em ordem de criação das curtidas. |
| `GET /api/announcements/{id}/comments` | Qualquer perfil | Sem corpo | `200 Comment[]` em ordem de criação. |
| `POST /api/announcements/{id}/comments` | Qualquer perfil | `{content}` | `201 Comment`. O comentário deve conter texto e ter até 250 bytes antes da remoção dos espaços nas extremidades. |
| `PUT /api/announcements/{id}/comments/{commentID}` | Qualquer perfil, somente o autor | `{content}` | `200 Comment` com o texto sem espaços nas extremidades. O comentário deve estar ativo, pertencer ao aviso informado e ser de autoria do solicitante. |
| `DELETE /api/announcements/{id}/comments/{commentID}` | Qualquer perfil, somente o autor | Sem corpo | `204` ao marcar o comentário como excluído. Um comentário inexistente ou de outro autor retorna `403`. |

**Announcement** contém `id,title,content,type,imagePath,targetRoles,hiddenForRoles,activeFrom,activeUntil,createdAt,likeCount,likedByCurrentUser,commentCount,authorId,authorName,authorProfileImagePath`. Na criação, `activeFrom` assume o horário atual e `targetRoles` inclui os três perfis por padrão. Uma duração de 1, 4, 10, 24, 48 ou 168 horas, se informada, prevalece sobre `activeUntil`. A visibilidade no mural depende da lista de perfis excluídos e da vigência. Nesta versão, `targetRoles` funciona como metadado. Curtidas e comentários usam o ID do aviso e exigem sessão. **Comment** contém `id,content,createdAt,user:{id,name}`. [Avisos e interações](../../internal/api/content.go)

Por exemplo, `{"title":"Assembleia escolar","type":"TEXT","content":"Encontro no auditório.","ttlHours":24}` cria um aviso válido por 24 horas. O mural deixa de exibir o aviso assim que ele expira. Uma rotina periódica exclui os avisos vencidos. [Expiração e notificações do mural](../../internal/api/websocket.go)

## Calendário

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /api/events` | Qualquer perfil | `includePast=true` opcional para ADMIN | `200 Event[]`. Retorna os próximos eventos em ordem cronológica. Administradores podem incluir eventos passados após os próximos, do mais recente ao mais antigo. Limite de 500. |
| `POST /api/events` | ADMIN | `{title,description?,eventAt,audience,classId?}` | `201 Event`. Exige título não vazio e data e hora entre o momento atual e o fim do ano civil vigente em São Paulo. `audience` aceita `ALL`, `TEACHERS` ou `CLASS`. Para uma turma, informe seu ID. |
| `DELETE /api/events/{id}` | ADMIN | ID do evento | `204` na exclusão ou `404` se o evento não existir. |

**Event** contém `id,title,description,eventAt,audience,classId,className,createdById,createdByName,createdAt`. Os campos do criador são preenchidos para administradores. Todos veem os eventos gerais. Professores também veem os eventos destinados a docentes. Eventos de uma turma aparecem para seus alunos e professores vinculados. Administradores veem todos os públicos. Envie `eventAt` no formato RFC 3339 com fuso horário explícito. [Operações de eventos](../../internal/api/content.go) · [Limites de data](../../internal/api/event_date.go)
