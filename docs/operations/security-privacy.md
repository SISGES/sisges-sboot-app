# Revisão de segurança e privacidade

Esta revisão reúne os controles observáveis no navegador, no serviço Go e nas migrações SQL. A equipe responsável pela publicação pode usá-la para solicitar evidências antes que uma escola confie registros identificáveis de alunos ao serviço. Os controles da hospedagem e as políticas da escola precisam de verificação própria.

## Dados, acesso e ciclo de vida

| Dados | Onde ficam | O que verificar sobre acesso e ciclo de vida |
| --- | --- | --- |
| Identidade da conta, hash da senha, papel e perfil | PostgreSQL. O navegador guarda o perfil do usuário conectado e o token de portador no armazenamento local. | Em chamadas HTTP protegidas, a API consulta a conta ativa e compara o e-mail e o papel. A saída da conta limpa o estado do cliente. Avalie o uso de dispositivos compartilhados e as necessidades de expiração e revogação. |
| Alunos, responsáveis e listas de turma | Tabelas de usuários, alunos, responsáveis e vínculos | Os registros podem incluir datas de nascimento, nomes, telefones e e-mails de responsáveis. Responsáveis são registros de contato. As consultas ao diretório e ao próprio perfil atendem públicos distintos. |
| Frequência, notas, recuperação e promoção | Aulas, frequência, atividades, notas e resultados anuais no PostgreSQL | O histórico acadêmico é sensível. Preserve os limites de acesso por papel, vínculo do professor com a turma e identidade do aluno. Confira as funções usadas para consulta, gravação e emissão de boletins. |
| Avisos, curtidas, comentários e eventos | Conteúdo e relacionamentos no PostgreSQL, com caminhos opcionais de imagens no R2 | O mural exige autenticação e aplica o período de vigência e a lista de papéis ocultos às consultas e interações. Antes de publicar conteúdo sensível, confira os públicos aprovados pela escola. |
| Imagens de avisos, fotos de perfil e documentos de aula | Objetos no R2, referenciados pelos registros da aplicação | As rotas de arquivo autenticam o leitor. Nos anexos de aula, verificam também a autoria do envio ou o vínculo com o registro. As imagens seguem outras regras de leitura. Uma chave de objeto opaca, sozinha, não define a política de privacidade. |
| Backups, logs e cópias no navegador ou dispositivo | Infraestrutura gerida pela operação e armazenamento do cliente | Defina quem pode acessar, exportar ou excluir cópias, quais identificadores podem aparecer nos logs e como considerar exclusões anteriores durante uma restauração. |

[Verificações de sessão](../../internal/api/auth.go) · [Operações de conteúdo](../../internal/api/content.go) · [Arquivos](../../internal/api/storage.go) · [Modelo de dados](../architecture/data-model.md)

## Controles implementados

- No login, o serviço compara a senha com o hash bcrypt e assina tokens HS256 com uma chave de pelo menos 32 bytes. Em uma requisição protegida, confere a assinatura, a validade e a conta ativa. A troca da própria senha exige a senha atual. O cadastro de contas é uma operação administrativa. [Usuários](../../internal/api/users.go) · [Autenticação](../../internal/api/auth.go)
- O middleware de autorização confere os papéis permitidos em cada rota. Algumas operações também verificam o vínculo com o registro, como o lançamento de aulas da grade e a leitura de seus anexos. Os controles de tela orientam a navegação. O servidor controla o acesso aos dados. [Rotas](../../internal/api/app.go) · [Aulas da grade](../../internal/api/schedules.go)
- O envio de arquivos tem limite de tamanho e verificações de extensão e destino. O serviço atribui as chaves dos objetos, assina as requisições ao armazenamento e transmite os downloads em fluxo contínuo. Documentos de aula são entregues como anexos. A verificação de extensão por si só não inspeciona o conteúdo. [Armazenamento](../../internal/api/storage.go)
- A imagem de backend executa um binário estático com usuário numérico sem privilégios de root e um diretório temporário privado com permissão de escrita. O processo recebe as credenciais do banco, a chave de assinatura e as credenciais do R2. As configurações `VITE_*` do frontend são incorporadas aos arquivos públicos. [Imagem do backend](../../Dockerfile) · [Configuração](configuration.md)

## Decisões e evidências para aceitação

| Área | Limite atual | Responsabilidade proposta e evidência esperada |
| --- | --- | --- |
| Isolamento entre escolas | Registros, configuração de notas e acesso ao ciclo acadêmico usam um contexto escolar compartilhado. | Produto e engenharia definem o modelo de isolamento. Antes de oferecer uma instância compartilhada, verificam, com dados de duas escolas independentes, o isolamento de diretórios, boletins, arquivos, backups e acesso administrativo. |
| Abuso de credenciais e recuperação de acesso | A aplicação oferece login por senha. As rotas versionadas não oferecem cadastro de MFA, limitação de tentativas de login, recuperação de acesso por autoatendimento ou revogação individual de tokens de renovação. | O responsável pelo serviço documenta a recuperação e a remoção de acesso da equipe, avalia autenticação adicional em operações sensíveis e limitação de tentativas, e verifica os fluxos de cadastro, perda do dispositivo e revogação. CAPTCHA e MFA foram propostas de uma análise anterior. |
| Navegador e transporte | A SPA persiste tokens de portador, e a abertura da conexão do mural envia um token na consulta da URL. A operação responde pela terminação TLS, pela ocultação de dados nos logs do proxy e pela rotação de chaves. | A operação verifica HTTPS/WSS, restringe as origens CORS, oculta campos sensíveis, ensaia a rotação e registra o efeito em sessões e sockets ativos. |
| Privilégio mínimo e ciclo de vida dos arquivos | As permissões variam conforme o recurso. Gravar a referência no banco e gravar ou remover o objeto são operações separadas. | A engenharia revisa o alcance efetivo de cada operação de leitura e escrita, acrescenta testes de acesso negado onde faltam e concilia arquivos expirados ou sem referência com as regras de retenção. |
| Evidências e retenção | O esquema permite exclusão lógica de vários tipos de registro e inclui uma tabela de ações de usuário. As rotas Go não implementam trilha de auditoria completa, processo de exportação de dados ou calendário de retenção. | O responsável pelos dados da escola define as categorias de registro e os procedimentos de retenção, exportação, correção e exclusão, inclusive das cópias arquivadas. A engenharia verifica uma solicitação representativa e registra a evidência de conclusão. |
| Resposta a abuso e a incidentes | A aplicação registra erros. Os repositórios não definem plantão nem política de incidentes. | A operação define os responsáveis pelo escalonamento, mantém um registro de incidentes com acesso restrito e ensaia a restauração dos dados afetados. [Procedimento de incidentes](recovery.md) |

Revise este documento quando mudarem a autenticação, o isolamento entre escolas, os prefixos de arquivos, as regras de público-alvo ou a retenção. Para qualquer declaração externa de segurança ou aprovação regulatória, a organização responsável precisa verificar sua implantação e seus procedimentos operacionais.
