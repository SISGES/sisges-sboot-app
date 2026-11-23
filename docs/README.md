# Manual técnico do SISGES

O SISGES reúne matrícula, grade horária, registro de aulas, frequência, avaliações e comunicação escolar. Consulte o documento de requisitos do produto (PRD) para conhecer os fluxos disponíveis e seus critérios de aceite. Antes de assumir compromissos com uma segunda escola ou oferecer níveis de serviço comerciais, consulte o registro de prontidão do SaaS.

Este manual abrange a API Go e a aplicação React de página única. As revisões de referência são `6b37a18` no backend e `b58d2df` no frontend. **Implementado** indica comportamento presente nessas revisões. **Proposto** indica uma decisão de produto, engenharia ou operação que ainda exige a definição de um responsável e evidências de aceite. Confirme com o operador a configuração efetiva da implantação e com a escola as políticas adotadas. [Revisão do backend](https://github.com/SISGES/sisges-sboot-app/tree/6b37a18f35047a2f6d6e85de57e90444ad488609) · [Revisão do frontend](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc)

## Mapa da documentação

| O que você precisa fazer | Documentos técnicos | Modelo reutilizável |
| --- | --- | --- |
| Definir escopo, usuários, resultados e indicadores do piloto | [Requisitos do produto](product/prd.md) | [PRD](templates/product-requirements.md) |
| Implementar um fluxo escolar ou verificar seu aceite | [Matrícula e administração escolar](product/school-administration.md) · [Aulas e frequência](product/teaching-and-attendance.md) · [Avaliação e progressão escolar](product/grading-and-progression.md) · [Comunicação e portal do aluno](product/communications-and-student-portal.md) | [Especificação funcional](templates/functional-specification.md) |
| Definir expectativas de serviço e planejar o aceite com a carga de uma escola | [Requisitos de qualidade](product/quality-requirements.md) | [Requisitos de qualidade](templates/quality-requirements.md) |
| Avaliar as condições para um piloto hospedado ou lançamento comercial | [Prontidão do SaaS e decisões pendentes](product/saas-readiness.md) | [Registro de prontidão](templates/readiness-register.md) |
| Entender os componentes, os limites de confiança, os fluxos críticos e as restrições de escala | [Arquitetura do sistema](architecture/system.md) | [Arquitetura do sistema](templates/system-architecture.md) |
| Alterar uma tabela ou interpretar o histórico de dados escolares | [Modelo de dados e histórico de migrações](architecture/data-model.md) | [Modelo de dados](templates/data-model.md) |
| Entender por que uma aula registrada mantém sua identidade ao mudar a grade | [ADR 001 — Grade semanal e registro de aulas por data](architecture/adr-001-weekly-timetable.md) | [Registro de decisão arquitetural](templates/decision-record.md) |
| Integrar um cliente ou investigar uma resposta inesperada | [Referência da API, erros e exemplos executáveis](api/README.md) · [Identidade](api/identity.md) · [Turmas](api/classes.md) · [Aulas](api/lessons.md) · [Avaliações](api/assessment.md) · [Comunicação](api/communications.md) · [Arquivos e mural](api/files-and-feed.md) | [Referência da API](templates/api-reference.md) |
| Preparar o ambiente de desenvolvimento ou alterar o cliente web | [Ambiente local](engineering/development.md) · [Engenharia do frontend](engineering/frontend.md) | [Guia de engenharia](templates/engineering-guide.md) |
| Configurar a API, a compilação do cliente e a rede dos contêineres | [Configuração de compilação e execução](operations/configuration.md) | [Referência de configuração](templates/configuration-reference.md) |
| Verificar uma mudança acadêmica ou técnica | [Estratégia de testes e evidências para a entrega](engineering/test-strategy.md) | [Estratégia de testes](templates/test-strategy.md) |
| Analisar acessos, dados sensíveis e lacunas nos controles | [Segurança e privacidade](operations/security-privacy.md) | [Análise de segurança e privacidade](templates/security-privacy.md) |
| Publicar uma imagem, investigar um incidente ou restaurar os dados de uma escola | [Publicação e implantação](operations/release.md) · [Incidentes, cópias de segurança e restauração](operations/recovery.md) | [Procedimento operacional](templates/runbook.md) |
| Ampliar este manual | [Modelo de índice da documentação](templates/documentation-index.md) | [Todos os modelos](templates/) |

## Como manter o manual

1. Escolha o modelo correspondente à tarefa do leitor. Registre o escopo e a revisão do repositório. Substitua as orientações do modelo por fatos do SISGES, exemplos, responsáveis pelas decisões e critérios de aceite observáveis. Escreva em português do Brasil.
2. Ao alterar um fluxo, acompanhe uma chamada real do cliente até o código do servidor, as verificações de autorização e os dados persistidos. Confira os status HTTP, os resultados vazios, a validação, o alcance de cada perfil e as novas tentativas. Identifique o requisito do PRD afetado. Uma tela, a declaração de um serviço ou um teste com respostas simuladas, isoladamente, não comprova que o fluxo integrado funciona.
3. Atualize os contratos relacionados na mesma mudança. Uma migração pode afetar o modelo de dados, a API e a recuperação. Uma nova transição acadêmica pode exigir ajustes no PRD, na especificação funcional, no cliente, nos testes e no registro de prontidão. Identifique decisões documentadas após a implementação e objetivos de serviço ainda em discussão.
4. Verifique os links relativos neste repositório. Para evidências em outro repositório, vincule a revisão consultada. Compare os métodos e caminhos da API com as rotas registradas em Go, os nomes de tabelas com o banco após as migrações e os comandos do frontend com os scripts versionados. Registre na descrição da mudança quais verificações foram executadas.

Este manual substitui a descrição anterior do esquema, o exemplo de pesquisa de usuários, o guia de erros, a proposta de CAPTCHA e autenticação de dois fatores, a análise de lacunas do MVP e as notas de implementação do frontend. As orientações de uso e licenciamento da marca acompanham os respectivos arquivos. [Kit de marca do frontend](https://github.com/SISGES/sisges-react-app/tree/d5d8daf06046040ef7f035f9f0d725f268bc7df2/brand/open-book)
