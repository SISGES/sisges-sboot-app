# Arquivos e notificações do mural

## Arquivos binários

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `POST /api/upload` | ADMIN, TEACHER | Requisição multipart com a parte `file`, o nome do arquivo e `subdir` | `200 {"path":"/api/files/…"}`. Use esse caminho, relativo ao servidor da API, no registro correspondente. Envie um arquivo não vazio por chamada, com até 10 MiB. |
| `GET /api/files/{key...}` | Qualquer perfil, com verificação de visibilidade para imagens de avisos e anexos escolares | Consulte o caminho retornado com o token bearer no cabeçalho | `200` com fluxo binário, Content-Type armazenado e cache privado de até 300 segundos. Para imagens de avisos, usa `Cache-Control: private, no-store`. Um objeto inexistente ou armazenamento não configurado retorna `404`. |

Informe o destino do envio como parâmetro da URL. Se usar um campo multipart, coloque `subdir` **depois** da parte `file`. A operação localiza o primeiro arquivo com nome e, em seguida, lê os demais campos do formulário. O limite para a requisição multipart completa é de 11 MiB. [Implementação do envio](../../internal/api/storage.go)

| `subdir` | Extensões aceitas | Caminho e regras de acesso |
| --- | --- | --- |
| `post-img` | `.png .jpg .jpeg .gif .webp` | Nome aleatório sob `post-img/<userId>/`. Para alunos e professores, a leitura exige um aviso ativo e visível para seu perfil que referencie a imagem. Na ausência desse vínculo, retorna `404`. O administrador pode consultar a imagem de um aviso oculto para manutenção. |
| `profile-img` | `.png .jpg .jpeg .gif .webp` | Nome aleatório sob o prefixo de perfil. A leitura exige autenticação. |
| `lesson-files` | Extensões de imagem acima e `.pdf .txt .doc .docx .xls .xlsx .ppt .pptx` | Nome aleatório sob `lesson-files/<userId>/`. O administrador e o usuário que enviou o arquivo podem baixá-lo. Para os demais, o caminho exato precisa constar em uma aula ativa que o usuário lecione ou a cuja turma pertença como aluno atual. A resposta identifica o arquivo como anexo e usa `nosniff`. |
| `materials` | As mesmas extensões de `lesson-files` | O envio gera `materials/<userId>/<arquivo>`. Publique o caminho no campo `filePath` do material. Após a publicação, alunos da turma atual e professores vinculados à turma e à disciplina podem baixar o anexo de um material ativo. |
| `activities` | As mesmas extensões de `lesson-files` | O envio gera `activities/<userId>/<arquivo>`. Publique o caminho no campo `filePath` da atividade. Em uma aula e atividade ativas, o professor da aula e os alunos que constam na lista de notas daquela atividade podem baixar o arquivo. |

Administradores podem ler esses arquivos. Para um novo material ou atividade, o servidor só aceita o caminho gerado para o destino correto e o ID do usuário que está publicando. Um arquivo enviado e ainda não publicado fica indisponível aos alunos. A exclusão lógica do registro encerra o acesso dos alunos pelo registro.

Para imagens de avisos legados, preserve o nome do arquivo ao copiar os objetos de `announcements/` para `post-img/` no R2. A leitura aceita ambos os prefixos e identifica a visibilidade pela referência armazenada no aviso.

Para anexos legados, a API reconhece referências gravadas como `/api/files/<chave>`, `/uploads/<chave>` ou a própria chave. O cliente solicita a leitura em `/api/files/<chave>`. As chaves antigas de `materials/`, `activities/` e `general/` precisam existir no R2 com o mesmo nome. Se os objetos estiverem no MinIO anterior ou no sistema de arquivos local, copie os objetos correspondentes para o bucket antes de usar os links. A leitura consulta os vínculos do registro existente. O destino `general` serve apenas para leitura de referências legadas.

No exemplo, um professor envia o arquivo local `worksheet.pdf` com um token já obtido. Uma resposta como `{"path":"/api/files/lesson-files/27/0123456789abcdef0123456789abcdef.pdf"}` permite referenciar o arquivo no registro da aula com `{"name":"worksheet.pdf","path":"<caminho retornado>"}`. [Registro da aula](lessons.md)

```sh
curl -H "Authorization: Bearer $TOKEN" -F 'file=@worksheet.pdf' \
  'http://localhost:8080/api/upload?subdir=lesson-files'
```

A ausência de arquivo, destino inválido, extensão não permitida ou tamanho inválido normalmente retornam `400 {"error":"…"}`. Se o armazenamento não estiver configurado, o envio retorna `503 {"error":"Armazenamento não configurado"}`. Uma falha ao gravar o arquivo pode retornar `500 {"error":"Erro ao salvar arquivo"}`. O cliente também deve tratar o formato comum de erro em falhas de autenticação ou erros internos. [Operações de armazenamento](../../internal/api/storage.go)

## Notificações de avisos

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `GET /ws` | Qualquer perfil | Solicite a mudança de protocolo para WebSocket em `/ws?token=<accessToken>` no servidor da API | `101 Switching Protocols`. Um token inválido retorna `401`. Com uma sessão válida, a ausência do cabeçalho de upgrade ou da chave do WebSocket retorna `400`. |

Use `wss` com TLS e mantenha o token da URL fora dos registros de log. Envie cada quadro abaixo como uma mensagem de texto WebSocket. Substitua `\n` por uma quebra de linha LF e `\0` por um único byte NUL. A operação implementa a assinatura de um tópico do mural com um subconjunto de STOMP. [Implementação do WebSocket](../../internal/api/websocket.go)

1. Envie `CONNECT\naccept-version:1.2\n\n\0`. A resposta é `CONNECTED\nversion:1.2\nheart-beat:0,10000\n\n\0`.
2. Envie `SUBSCRIBE\nid:feed\ndestination:/topic/feed\n\n\0`.
3. Ao receber `MESSAGE` em `/topic/feed` com o ID da assinatura no cabeçalho `subscription` e um `message-id`, leia o corpo JSON, por exemplo `{"type":"ANNOUNCEMENT_UPDATED","announcementId":42}`. Os tipos são `ANNOUNCEMENT_CREATED`, `ANNOUNCEMENT_UPDATED` e `ANNOUNCEMENT_DELETED`. A expiração também pode emitir uma notificação de exclusão.
4. Consulte novamente `GET /api/announcements/feed` com o token da sessão para obter a lista atual visível para o usuário. Curtidas e comentários têm consultas REST próprias.
5. Para cancelar a assinatura, envie `UNSUBSCRIBE\nid:feed\n\n\0` com o mesmo ID. O servidor negocia o recebimento de sinais de atividade do cliente a cada dez segundos quando o cliente oferece esse recurso.

A entrega ocorre dentro de cada processo, sem garantia de recebimento. O servidor aceita até 256 conexões do mural por processo, mantém uma fila de oito notificações de saída por cliente e descarta novas notificações quando a fila está cheia. Os quadros recebidos têm limite de 64 KiB e o prazo de leitura é de dois minutos. Após reconectar, assine o tópico novamente e recarregue o mural para recuperar as alterações perdidas. [Distribuição de notificações](../../internal/api/websocket.go) · [Contrato REST de avisos](communications.md)
