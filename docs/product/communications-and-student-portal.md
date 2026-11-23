# Comunicação escolar e portal do aluno

**Escopo** · Especificação funcional dos requisitos P-08 e P-09. Avisos são publicações com período de exibição. Eventos são compromissos de calendário com data e público definidos. O portal do aluno também reúne informações acadêmicas da turma.

## Avisos e eventos

| Ação | Papel | Comportamento do produto |
| --- | --- | --- |
| Publicar aviso | Administrador ou professor | Criar no mural do Início uma publicação com título, em texto ou imagem. Por padrão, a exibição começa no momento da criação. Os prazos de exibição (TTL) aceitos na criação são 1, 4, 10, 24, 48 ou 168 horas. Quando há TTL, o prazo conta a partir do início da exibição. O envio de imagens requer R2. |
| Definir visibilidade | Autor da publicação | A consulta do mural aplica o período de exibição e exclui os papéis em `hiddenForRoles`. O servidor armazena `targetRoles`, mas a consulta do mural não usa esse campo para selecionar leitores. A API e o painel web exigem autenticação. |
| Interagir | Usuário autenticado | Curtir ou desfazer a curtida, com uma curtida por usuário e aviso. É possível ver quem curtiu, listar comentários e criar, editar ou excluir os próprios comentários. Um novo comentário deve conter texto e ter até 250 bytes. |
| Gerenciar ou expirar aviso | Administrador ou rotina em segundo plano | O administrador pode editar ou excluir um aviso, inclusive se for de autoria de um professor. O aviso deixa de aparecer nas consultas do mural assim que expira. Uma rotina executada a cada minuto exclui os registros expirados e tenta remover as imagens associadas. O aplicativo consulta o mural ao abrir a tela e repete a consulta a cada 60 segundos enquanto a aba está visível. Notificações recebidas por WebSocket também disparam uma nova consulta. [Fluxo de atualização do mural](../architecture/system.md) |
| Agendar evento | Administrador | Informar título e data com horário, entre o momento atual e o fim do ano civil corrente no fuso de São Paulo. Os públicos são `ALL`, `TEACHERS` ou `CLASS`. Um evento de turma exige o ID da turma. A API de gestão de eventos permite criar, listar e excluir. |
| Consultar calendário | Usuário autenticado | O administrador pode incluir eventos passados. Os demais usuários veem eventos futuros destinados a todos, aos professores se tiverem esse papel, ou a uma turma em que lecionam ou estudam. |

As regras de visibilidade do mural definem quais avisos entram na lista. As rotas de interação aplicam a mesma visibilidade e verificam a autoria nas alterações de comentários. Comunicados escolares que exijam público restrito precisam de uma política explícita de visibilidade em todas as formas de leitura. [Operações de conteúdo](../../internal/api/content.go) · [Expiração e atualização do mural](../../internal/api/websocket.go) · [Regras de data dos eventos](../../internal/api/event_date.go) · [Interface do mural](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/components/AnnouncementFeed/AnnouncementFeed.tsx)

## Telas do aluno e ausência de dados

| Tela | Informações e limites |
| --- | --- |
| Início | O painel autenticado carrega o mural de avisos, o painel de eventos, os materiais e os dados das atividades da turma. Uma atividade pode aparecer antes da liberação das notas. A API atual não oferece fluxo para entrega de trabalhos ou tarefas pelo aluno. |
| Minha turma (`/minha-turma`) | Exibe turma atual, série, colegas e professores vinculados. A resposta inclui nomes e e-mails escolares. Sem uma turma ativa, a operação de consulta retorna erro de recurso não encontrado. |
| Faltas (`/faltas`) | Agrupa as faltas registradas do aluno por disciplina e inclui aulas anteriores que continuem ativas e tenham registro de falta. Disciplinas sem falta registrada não geram linha de contagem. A tela carrega a turma atual e as faltas em conjunto. |
| Boletim (`/boletim`) | Exibe três linhas trimestrais, os totais liberados, as notas de recuperação e a indicação de direito à recuperação anual. Para aluno cadastrado sem turma atual, a API retorna os máximos configurados e listas trimestrais de atividades vazias. |
| Conta (`/account`) | Todos os papéis podem consultar a própria conta. Os campos de perfil do aluno são somente de leitura na interface e na API. |

[Consultas do aluno](../../internal/api/students.go) · [Boletim](../../internal/api/report.go) · [Seções da tela inicial do aluno](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/components/StudentHomeSection/StudentHomeSection.tsx) · [Avaliação e progressão](grading-and-progression.md)

## Cenários de aceite

| Dado | Quando | Então |
| --- | --- | --- |
| Um aviso ativo que inclui STUDENT entre os papéis ocultos | Um aluno atualiza o mural do Início | O aviso não aparece na lista retornada. |
| Um professor cria um aviso em texto | O administrador altera o título | A consulta seguinte mostra o novo título. O professor não tem permissão para editar o aviso. |
| Dois usuários autenticados e um comentário do primeiro | O segundo tenta editar o comentário | A atualização falha e o conteúdo original é preservado. |
| Um evento futuro de público CLASS para a turma B | Um aluno da turma A consulta os eventos e depois um aluno da turma B faz a mesma consulta | Somente o aluno da turma B vê esse evento. |
| Um aluno tem duas faltas e uma presença na mesma disciplina | O aluno abre Faltas | A disciplina exibe duas faltas. |

## Quando revisar

Ao alterar papéis ou públicos, conferir em conjunto a visibilidade do mural e das interações. Ao mudar o preenchimento ou a exibição de eventos, conferir os limites de data no fuso de São Paulo. Quando uma tela do aluno mudar, revisar com a equipe pedagógica os efeitos de transferências de turma e da liberação de notas.
