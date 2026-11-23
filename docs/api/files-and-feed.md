# Arquivos e notificações do mural

## Arquivos binários

| Operação | Acesso | Requisição | Resposta e comportamento |
| --- | --- | --- | --- |
| `POST /api/upload` | ADMIN, TEACHER | Requisição multipart com a parte `file`, o nome do arquivo e `subdir` | `200 {"path":"/api/files/…"}`. Use esse caminho, relativo ao servidor da API, no registro correspondente. Envie um arquivo não vazio por chamada, com até 10 MiB. |
| `GET /api/files/{key...}` | Qualquer perfil, com verificação adicional para anexos de aula | Consulte o caminho retornado com o token bearer no cabeçalho | `200` com fluxo binário, Content-Type armazenado e `Cache-Control: private, max-age=300`. Um objeto inexistente ou armazenamento não configurado retorna `404`. |

Informe o destino do envio como parâmetro da URL. Se usar um campo multipart, coloque `subdir` **depois** da parte `file`. A operação localiza o primeiro arquivo com nome e, em seguida, lê os demais campos do formulário. O limite para a requisição multipart completa é de 11 MiB. [Implementação do envio](../../internal/api/storage.go)

| `subdir` | Extensões aceitas | Caminho e regras de acesso |
| --- | --- | --- |
| `post-img`, `profile-img` | `.png .jpg .jpeg .gif .webp` | Nome aleatório sob o prefixo escolhido. A leitura exige autenticação. |
| `lesson-files` | Extensões de imagem acima e `.pdf .txt .doc .docx .xls .xlsx .ppt .pptx` | Nome aleatório sob `lesson-files/<userId>/`. O administrador e o usuário que enviou o arquivo podem baixá-lo. Para os demais, o caminho exato precisa constar em uma aula ativa que o usuário lecione ou a cuja turma pertença como aluno atual. A resposta identifica o arquivo como anexo e usa `nosniff`. |

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

1. Envie `CONNECT\naccept-version:1.2\n\n\0`. A resposta é `CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0`.
2. Envie `SUBSCRIBE\nid:feed\ndestination:/topic/feed\n\n\0`.
3. Ao receber `MESSAGE` em `/topic/feed`, leia o corpo JSON, por exemplo `{"type":"ANNOUNCEMENT_UPDATED","announcementId":42}`. Os tipos são `ANNOUNCEMENT_CREATED`, `ANNOUNCEMENT_UPDATED` e `ANNOUNCEMENT_DELETED`. A expiração também pode emitir uma notificação de exclusão.
4. Consulte novamente `GET /api/announcements/feed` com o token da sessão para obter a lista atual visível para o usuário. Curtidas e comentários têm consultas REST próprias.

A entrega ocorre dentro de cada processo, sem garantia de recebimento. O servidor aceita até 256 conexões do mural por processo, mantém uma fila de oito notificações de saída por cliente e descarta novas notificações quando a fila está cheia. Os quadros recebidos têm limite de 64 KiB e o prazo de leitura é de dois minutos. Após reconectar, assine o tópico novamente e recarregue o mural para recuperar as alterações perdidas. [Distribuição de notificações](../../internal/api/websocket.go) · [Contrato REST de avisos](communications.md)
