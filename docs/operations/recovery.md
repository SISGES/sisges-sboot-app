# Incidentes, backup e restauração do serviço

Público — responsáveis pela implantação, pela engenharia do backend e pelos dados da escola. A triagem toma como referência o comportamento do código. Os procedimentos de backup e resposta são práticas propostas. Os repositórios não definem calendário de backups, escala de plantão, prazo de comunicação de incidentes, objetivo de ponto de recuperação (RPO) ou objetivo de tempo de recuperação (RTO).

## Triagem por sintoma

| Sinal | Primeira verificação | Próxima ação |
| --- | --- | --- |
| API indisponível após a implantação | Verifique o estado do contêiner e procure nos logs de inicialização erros de configuração, conexão com o banco, inicialização do R2 ou aplicação de uma migração específica. | Compare o ambiente e o digest da imagem com a última versão funcional. Registre a versão SQL que falhou e consulte o histórico de migrações antes de tentar novamente. |
| Falha na verificação do banco | `GET /health` testa a conexão com o banco com tempo limite de um segundo. | Confira a conectividade com o PostgreSQL, a capacidade de conexões, a latência e a disponibilidade. Confirme o corpo e a origem da resposta para distinguir a verificação da API da página padrão da SPA. |
| Login inválido aparece como sucesso nas métricas HTTP | O contrato de login retorna HTTP 200 com um erro da aplicação para credenciais inválidas. | Em um teste sintético, verifique o código do resultado e a presença do token. Exclua senhas, tokens e corpos das requisições e respostas dos logs. |
| Apenas as operações de arquivo falham | Inspecione a configuração do armazenamento, o prefixo de destino, o limite de corpo no proxy, o diretório temporário e a resposta do serviço de objetos. | Compare a referência no banco com o inventário de objetos. HTTP 503 no envio pode indicar armazenamento sem configuração. Valide com uma conta sintética autorizada. |
| Gravação da grade rejeitada | Confira a revisão e as disciplinas vinculadas à turma. | Consulte a grade atual e concilie os rascunhos antes de salvar. Repetir a requisição com a mesma revisão desatualizada mantém o conflito. |
| Mural desatualizado | Verifique se uma nova leitura por HTTP funciona e se o navegador alcançou o host correto do WebSocket. | Confira o encaminhamento do upgrade e a distribuição de notificações por processo. Recupere o conteúdo visível com uma nova consulta HTTP. |
| Possível exposição de dados escolares | Registre em canal restrito o horário, a categoria dos dados, o serviço e o alcance. | Restrinja o acesso afetado e envolva o operador responsável e o contato da escola. Oculte os identificadores nas anotações do incidente. Valide o acesso antes de reabrir o fluxo. |

[Verificação de disponibilidade e rotas](../../internal/api/app.go) · [Tratamento de erros](../../internal/api/errors.go) · [Acesso e tratamento de dados](security-privacy.md)

## Ensaio de recuperação do PostgreSQL e do R2

1. Identifique a implantação de origem, o horário do backup, os digests das imagens e a conta de armazenamento. Escolha uma versão compatível do PostgreSQL, um **destino vazio e isolado** e um local protegido para o arquivo de backup. Para conciliar arquivos e registros, coordene uma pausa nas gravações ou outra estratégia que produza uma cópia consistente. Um snapshot transacional do banco, por si só, não estabelece um ponto de recuperação que inclua o R2.
2. Forneça as credenciais da libpq por um meio privado, gere um dump em formato personalizado e inspecione seu conteúdo. Os exemplos de shell Unix pressupõem que o operador já definiu os nomes dos bancos ou as referências de conexão. O diretório de backup deve ter controle de acesso e criptografia conforme a política da implantação.

   ```sh
   umask 077
   pg_dump --format=custom --file="$BACKUP_DIR/sisges.dump" --dbname="$SOURCE_DATABASE"
   pg_restore --list "$BACKUP_DIR/sisges.dump"
   ```

3. Faça um inventário e preserve as chaves e versões ou cópias dos objetos de `post-img/`, `profile-img/` e `lesson-files/`. Guarde também cópias privadas da configuração de ambiente e do manifesto de implantação no gerenciador de segredos da operação. Para restaurar em outra conta de armazenamento, o binário continua exigindo um bucket chamado `sisges-prd`.
4. Prepare o banco de recuperação vazio e restaure a cópia nele. O exemplo omite proprietários e permissões originais para permitir a aplicação dos papéis adequados ao ambiente isolado. Restaure essas permissões separadamente antes de reconectar o serviço.

   ```sh
   pg_restore --exit-on-error --no-owner --no-acl \
     --dbname="$EMPTY_RECOVERY_DATABASE" "$BACKUP_DIR/sisges.dump"
   psql -X --dbname="$EMPTY_RECOVERY_DATABASE" \
     -c 'SELECT version, name FROM sisges.go_schema_migrations ORDER BY version;'
   ```

5. Compare as contagens de turmas, usuários, aulas e notas com o inventário da origem. Confira as migrações históricas bem-sucedidas do Flyway, se houver. Inicie a imagem correspondente da API com a carga de demonstração desabilitada, o banco restaurado e os objetos do mesmo ponto de recuperação. Teste a disponibilidade, o login, a consulta de turma, uma grade horária, o boletim de um aluno e leituras autorizadas de arquivos representativos. Use um ambiente de teste restrito para dados reais da escola.
6. Registre o ponto de recuperação confirmado, o tempo decorrido, os objetos indisponíveis e as divergências entre registros. Antes de direcionar o tráfego de produção ao ambiente restaurado, obtenha a aceitação do responsável pelos dados quanto ao estado recuperado e ao intervalo de gravações perdidas. Preserve a origem anterior durante a conciliação e descarte cópias temporárias conforme a política de retenção aprovada.

O repositório não contém um executor de migrações reversas. Quando uma versão falha, todas as anteriores já concluídas permanecem aplicadas. Restaurar somente o binário ou somente o PostgreSQL depois de uma migração de arquivos pode deixar os componentes em estados incompatíveis. [Executor de migrações](../../internal/api/migrate.go) · [Modelo de dados](../architecture/data-model.md)

## Evidências operacionais

Registre erros de implantação e migração, volume e latência das requisições à API, falhas de autenticação, pressão sobre o pool de conexões, taxa de erros no armazenamento de objetos e tempo desde o último ensaio de restauração bem-sucedido. O responsável pelo serviço deve definir os limites de alerta e quem recebe as notificações. Logs e rastreamentos devem omitir tokens de parâmetros de URL, credenciais, notas de alunos, contatos dos responsáveis e conteúdo de arquivos. A aplicação registra erros de processo e de requisição. Métricas externas, ocultação de dados sensíveis, encaminhamento de alertas e retenção precisam ser verificados na implantação.
