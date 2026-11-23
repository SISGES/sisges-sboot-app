# Avaliação, boletim e progressão escolar

**Escopo** · Especificação funcional dos requisitos P-06 e P-07. O administrador configura as notas e gerencia o ciclo letivo. O administrador ou o professor da aula gerencia o lançamento de notas de uma atividade. Cada aluno consulta o próprio boletim.

## Configuração e transições do ciclo letivo

Na tela de notas, o administrador define a pontuação máxima e o percentual mínimo para o ano e para cada um dos três trimestres. Cada trimestre distribui pontos inteiros positivos entre provas (`PROVA`), atividades (`ATIVIDADE`) e trabalhos (`TRABALHO`). As três parcelas devem somar a pontuação máxima do trimestre, e a soma dos três máximos trimestrais deve corresponder ao máximo anual. O portal exibe um percentual fixo de aprovação de 70%. O cálculo do boletim e da progressão aplica esse percentual mesmo quando os mínimos configurados são diferentes. Ao criar uma atividade, a API também não verifica se a soma das pontuações já atribuídas à categoria ultrapassa sua parcela. [Configuração de notas](../../internal/api/grading.go) · [Cálculos do boletim](../../internal/api/report.go) · [Tela de notas](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/GradingConfig/GradingConfig.tsx)

| Ação do administrador | Condições | Mudança de estado observada |
| --- | --- | --- |
| Iniciar o ano | Data final futura e ciclo fora de `IN_PROGRESS` | Atualiza o primeiro registro de ciclo existente para `IN_PROGRESS`, seleciona o trimestre 1, registra as datas de início e define `gradingLocked=true`. A operação que altera a configuração de notas não consulta esse indicador. |
| Encerrar o trimestre | Ciclo em andamento, senha atual do administrador e nenhuma atividade ativa sem liberação identificada na consulta de professores com pendências naquele trimestre | Avança um trimestre até o limite de 3. Executar a transição no trimestre 3 mantém o valor 3. |
| Encerrar o ano | Ciclo em andamento, senha atual do administrador, nenhuma liberação pendente encontrada em todos os trimestres e turmas de destino válidas para as promoções | Processa resultados e promoções, depois marca o ciclo como `FINISHED`. A API aceita o encerramento antes do trimestre 3 ou da data final. |

A consulta de liberações pendentes identifica professores pelo nome a partir dos vínculos entre atividades, aulas, professores e usuários. O frontend de referência implementa os métodos do serviço de ciclo letivo, mas as telas acessíveis por rota não os chamam. O gerenciamento do ciclo é um fluxo da API. [Operações do ciclo](../../internal/api/grading.go) · [Cliente do ciclo letivo](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/academicCycleService.ts)

## Da atividade à nota liberada

1. Com o ciclo em andamento, criar uma atividade vinculada a uma aula. O título e a pontuação máxima positiva são obrigatórios. Além das três categorias regulares, a API aceita recuperação trimestral e anual. Se o tipo for omitido, o padrão é `ATIVIDADE`. O trimestre, quando informado, deve estar entre 1 e 3. Uma atividade regular precisa ter trimestre para aparecer na linha correspondente do boletim.
2. Abrir o lançamento de notas com o ID da atividade. A lista contém os alunos ativos atualmente matriculados na turma da aula. É possível salvar uma nota nula ou um valor de zero à pontuação máxima da atividade, inclusive. A API valida cada lançamento e grava o lote em uma transação.
3. Liberar as notas salvas. A partir da liberação, as notas ficam disponíveis apenas para consulta na tela de lançamento. É possível liberar uma atividade com notas em branco. Antes de confirmar, a equipe deve conferir as notas ausentes conforme a política da escola. A solicitação de liberação não salva alterações pendentes no formulário.
4. O aluno abre `/boletim`. As atividades regulares ainda não liberadas podem ocupar células com nota nula. Somente notas liberadas e não nulas entram nas somas. As notas de recuperação aparecem em uma linha separada. O boletim usa a turma atual do aluno, sem seletor de ano letivo. [Operações de atividade](../../internal/api/activities.go) · [Boletim do aluno](../../internal/api/report.go) · [Tela de lançamento de notas](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/ActivityGradebook/ActivityGradebook.tsx)

**Integração do cliente.** O formulário de atividade da aula na versão de referência omite `maxPoints`. A API rejeita o envio mesmo quando há título e ciclo ativo. O destino opcional `activities` para envio de arquivo também não tem suporte. A rota de lançamento `/atividades/:id/notas` abre para uma atividade existente, mas a lista de atividades da aula não oferece um link para ela. Um teste de interface que intercepta a API é insuficiente para comprovar a criação de ponta a ponta. [Formulário da atividade](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/AulaDetail/AulaDetail.tsx) · [Cliente de atividades](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/activityService.ts) · [Destinos de armazenamento](../../internal/api/storage.go)

## Recuperação e promoção

- **Direito à recuperação.** No trimestre, é preciso haver ao menos uma atividade regular, todas as atividades regulares devem estar liberadas e o total precisa ser inferior a 70% da pontuação máxima trimestral. Para indicar direito à recuperação anual, o portal exige o cumprimento da condição de liberação nos três trimestres e um total anual inferior a 70% do máximo anual.
- **Cálculo no fechamento do ano.** Em um trimestre com ao menos uma atividade regular, todas liberadas e total abaixo do mínimo, a última recuperação trimestral liberada pode elevar o total ao mínimo. A nota exigida na recuperação é 70 mais a quantidade de pontos que falta. Em um trimestre que vale 30 pontos, um aluno com 18 precisa chegar a 21. A nota exigida na recuperação é 73. A recuperação anual aplica a mesma regra de diferença de pontos depois dos ajustes trimestrais. O sistema registra a pontuação-base, a nota final e o resultado da aprovação.
- **Turma de destino.** O aluno aprovado avança para a próxima série reconhecida. Entre várias turmas de destino, o sistema prefere uma turma de mesmo nome e, em seguida, usa a ordem dos nomes. Ao aprovar um aluno da última série, o sistema remove seu vínculo com a turma atual. O aluno reprovado permanece na turma. Uma série inválida ou a falta de uma turma de destino obrigatória desfaz a transação de resultados. [Processamento de fim de ano](../../internal/api/report.go)

**Limite do histórico.** O processamento considera até 10.000 alunos ativos vinculados a turmas e deixa de executar se já existir *qualquer* resultado para o ID do primeiro ciclo. Iniciar outro ano reutiliza esse registro. As consultas de avaliação também selecionam a turma sem separar os registros por ano. Para que a escola possa confiar no histórico e nas promoções de anos sucessivos, é preciso definir como o modelo de dados e as regras representarão um segundo ano independente. [Estado do ciclo](../../internal/api/grading.go) · [Processamento de resultados e boletim](../../internal/api/report.go)

## Cenários de aceite

| Dado | Quando | Então |
| --- | --- | --- |
| Um máximo anual válido de 100 e máximos trimestrais de 30, 30 e 40 | O administrador altera uma parcela de categoria e a soma deixa de corresponder ao máximo do trimestre | A API rejeita a configuração. |
| Uma atividade não liberada que vale 10 e um aluno da turma | O professor salva a nota 8 | O lançamento contém 8 e a célula do boletim do aluno permanece nula até a liberação. |
| Uma atividade liberada que vale 10 pontos | O professor tenta corrigir a nota | A gravação retorna um erro de regra de negócio. |
| Um ciclo em andamento e uma atividade ainda não liberada, vinculada ao professor no trimestre atual | O administrador confirma o encerramento do trimestre | O ciclo permanece no mesmo trimestre e a resposta identifica o professor com a pendência. |
| Um aluno aprovado em uma série que não é a última e nenhuma turma da série seguinte | O administrador encerra o ano | A transação de resultados falha, nenhuma promoção é efetivada e o ciclo continua em andamento. |

## Quando revisar

Revisar a política aprovada pela escola e repetir os exemplos de liberação, recuperação e encerramento do ano sempre que mudar a composição das notas, a identificação do ciclo, o histórico de matrícula, os percentuais mínimos ou o formulário de atividade.
