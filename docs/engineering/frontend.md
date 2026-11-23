# Guia de engenharia do frontend

O portal escolar é uma aplicação de página única em React 18 e TypeScript. O navegador identifica a rota, verifica a sessão local e o perfil do usuário, monta a página na estrutura comum do portal e chama a API Go por meio de um serviço de domínio. Escreva os textos da interface em português do Brasil e mantenha os nomes das propriedades da API conforme definidos no servidor. [Rotas](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/App.tsx) · [Serviços de domínio](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services)

## Onde implementar cada alteração

| Área | Ponto de entrada | Detalhe da integração |
| --- | --- | --- |
| Telas e navegação | `src/App.tsx`, `src/pages`, `src/config/navConfig.ts` | Registre a rota e decida em quais menus de perfil ela deve aparecer. Uma rota pode existir sem link de navegação. |
| Sessão e acesso | `AuthContext`, `authService`, `ProtectedRoute` e componentes de proteção por perfil | A restauração da sessão aguarda a validação do servidor. `TeacherRoute` permite o acesso de professores e administradores. A API verifica quais operações cada um pode executar. |
| Formato de requisições e respostas | `src/services/api.ts`, serviços de domínio e `src/types/auth.ts` | O cliente compartilhado envia o token de autenticação e padroniza os erros. Há tipos de domínio definidos junto aos serviços. Trate explicitamente respostas de sucesso com corpo vazio. |
| Envio de arquivos e imagens protegidas | `uploadService`, `ProtectedFile`, `UserAvatar` | Envie os dados em formulário multipart, salve o caminho retornado pela API e use o token de acesso para buscar arquivos privados. Confira os destinos permitidos no servidor. |
| Estado que persiste entre rotas | Provedores de contexto em `src/main.tsx` | Os rascunhos da grade horária ficam em memória por usuário e turma. Persistem ao navegar entre rotas e são descartados ao recarregar a página ou trocar de usuário. |
| Estilo, mensagens e controles reutilizáveis | `src/index.css`, `src/themes/colors.ts`, `ThemeContext`, `src/components/ui` | O `ThemeProvider` escolhe a preferência salva ou o tema do sistema, define `data-theme` e gera variáveis CSS. Use os provedores compartilhados de diálogos e notificações para manter a interação consistente. |
| Datas e atualizações em tempo real | `src/utils/eventDateTime.ts`, `src/hooks/useStompFeed.ts` | Preserve o deslocamento de fuso horário ao enviar o instante de um evento. Uma notificação do mural deve disparar uma nova consulta à API. |

[Provedores da aplicação](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/main.tsx) · [Tema](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/ThemeContext.tsx) · [Navegação](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/config/navConfig.ts) · [Identidade visual e uso da marca](https://github.com/SISGES/sisges-react-app/tree/d5d8daf06046040ef7f035f9f0d725f268bc7df2/brand/open-book)

## Acompanhar uma alteração da tela até o banco

Para alterar a grade horária, comece na rota administrativa `/admin/grade-horaria`. A página edita um rascunho por turma, usa `scheduleService` para salvar os horários com o número da revisão e trata a resposta do servidor. A API verifica a turma, as disciplinas e a revisão antes de substituir a grade no PostgreSQL. Teste sair da página e voltar, confirmar a redefinição, salvar com sucesso e tentar salvar uma revisão desatualizada em outra aba. O rascunho do navegador e a grade gravada no banco têm ciclos de vida diferentes. [Tela da grade](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Timetable/Timetable.tsx) · [Provedor de rascunhos](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/TimetableDraftProvider.tsx) · [Implementação da grade na API](../../internal/api/schedules.go)

Ao alterar uma operação da API, confira no servidor o método, o perfil permitido, o corpo e o status HTTP antes de modificar o tipo em TypeScript. Na revisão documentada, o servidor Go valida o token com uma resposta 200 sem corpo e o cliente compartilhado de GET sempre chama `response.json()`. Assim, o login pode funcionar e, ao recarregar a página, a tentativa de restaurar a sessão pode encerrá-la. Um teste de navegador que simula a validação com um objeto JSON pode deixar essa incompatibilidade passar. [Cliente HTTP](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/api.ts) · [Validação no servidor](../../internal/api/users.go)

## Verificar a interface

Na raiz do frontend, execute `npm ci` e `npm run build`. Teste cada fluxo alterado com todos os perfis permitidos e com um perfil sem permissão. Confira a navegação por teclado, a restauração do foco ao fechar diálogos, os nomes acessíveis dos campos, as mensagens de erro, os dois temas e o comportamento em telas estreitas. Para mudanças em HTTP ou arquivos, repita o fluxo com a API real e examine o corpo da resposta e o tipo de conteúdo. [Estratégia de testes](test-strategy.md) · [Ambiente local](development.md)
