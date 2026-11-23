# Avaliação, boletim e progressão escolar

**Escopo** · Requisitos P-06 e P-07 na implementação com histórico por ciclo (migração V25). O administrador configura a avaliação e conduz o ano letivo em **Notas**. Ele ou o professor da aula lança e libera as notas das atividades. O aluno consulta o próprio boletim.

## Configuração e ciclo letivo

Antes de iniciar o ano, o administrador informa a pontuação máxima e o percentual mínimo de aprovação do ano e de cada trimestre. Os percentuais vão de 0 a 100. Em cada trimestre, as parcelas de provas, atividades e trabalhos são inteiras, positivas e somam o máximo trimestral. Os três máximos trimestrais devem somar o anual. A criação de uma atividade não controla a soma dos pontos já atribuídos às demais atividades da categoria. [Regras de configuração](../../internal/api/grading.go) · [Validação da atividade](../../internal/api/activities.go)

Em **Notas → Ciclo letivo**, o administrador consulta a situação, o período previsto e, durante o ano, o trimestre vigente. **Atualizar ciclo** consulta novamente o estado do servidor. Enquanto essa consulta estiver pendente ou falhar, a edição das regras fica indisponível. As alterações locais nas regras devem ser salvas antes de iniciar o ano.

| Ação | Condições | Resultado |
| --- | --- | --- |
| Iniciar ano letivo | Regras carregadas e salvas, ciclo fora de andamento e término previsto posterior ao dia atual em São Paulo. | Inicia o primeiro trimestre e guarda as regras do ano. Durante o ciclo, a API e a tela bloqueiam alterações nessas regras. Se o ciclo anterior já terminou, o sistema cria um novo ID e preserva o anterior. |
| Encerrar trimestre | Ano em andamento, senha atual do administrador e nenhuma atividade ativa com nota ainda não liberada no trimestre vigente. | O botão avança do primeiro para o segundo trimestre ou do segundo para o terceiro. No terceiro, a tela oferece o encerramento do ano. |
| Encerrar ano letivo | Ano em andamento, senha atual, nenhuma atividade ativa pendente de liberação em todo o ciclo e turmas de destino válidas para as promoções. | Calcula os resultados, registra o boletim de fechamento, efetiva as promoções e marca o ciclo como encerrado na mesma transação. Libera a configuração para o próximo ano. A operação pode ocorrer antes do terceiro trimestre ou da data prevista de término. |

O diálogo de encerramento consulta os professores com pendências para o período selecionado. Se houver nomes, o administrador pode atualizar a consulta depois que os professores liberarem as notas. Falha na consulta ou pendência de liberação impede confirmar. A operação no servidor volta a verificar as pendências e as condições do ciclo. A tela envia o ID esperado do ciclo e, no fechamento, também o trimestre esperado. Se outro administrador já avançou o período, a operação falha e o usuário pode cancelar o diálogo e atualizar o ciclo. [Operações do ciclo](../../internal/api/grading.go) · [Contrato da API de avaliação](../api/assessment.md)

## Da atividade à nota liberada

1. Na aula, abrir **Nova atividade**, escolher o tipo de avaliação e informar título e pontuação máxima positiva. O formulário usa o trimestre vigente para as categorias trimestrais e omite o trimestre na recuperação anual. A aula deve estar dentro das datas do ciclo vigente. A API aceita provas (`PROVA`), atividades (`ATIVIDADE`), trabalhos (`TRABALHO`), recuperação trimestral e recuperação anual. O tipo padrão é `ATIVIDADE`. Na ausência de trimestre, a API usa o trimestre atual para as atividades que o exigem. A recuperação anual não tem trimestre. Para o anexo opcional, o formulário usa o destino `activities` e requer R2 configurado.
2. Abrir o lançamento de notas pela lista de atividades da aula. A criação registra a lista dos alunos ativos da turma naquele momento. Uma transferência posterior preserva a participação na atividade. Os lançamentos aceitam nota nula ou valor entre zero e o máximo da atividade, inclusive. O servidor valida a participação de cada aluno e salva o lote em uma transação.
3. Conferir e salvar as notas antes de liberar a atividade. A liberação não salva edições pendentes no formulário. Ela pode ocorrer com notas em branco. A partir da liberação, o servidor rejeita novas gravações de notas e a exclusão da atividade. O ciclo vigente delimita os lançamentos. Atividades pendentes do ciclo legado podem exigir regularização antes do fechamento.
4. No boletim, uma atividade regular ainda não liberada pode ocupar uma célula com nota nula. Só notas liberadas e não nulas entram na soma. A recuperação tem linha separada. A tela abre no ciclo vigente. O seletor **Ano letivo** permite consultar um ciclo disponível ao aluno e voltar ao atual. Para um ciclo encerrado com resultado, a tela mostra a situação de aprovação e promoção, a pontuação-base e a pontuação final. A seleção usa `GET /api/boletim/me?cycleId=N`. Sem dados de histórico na resposta, o boletim continua exibindo as linhas trimestrais.

[Atividades e lançamentos](../../internal/api/activities.go) · [Consulta do boletim](../../internal/api/report.go) · [Arquivos e autorização](../api/files-and-feed.md)

## Recuperação e promoção

- **Direito à recuperação.** No trimestre, é preciso haver ao menos uma atividade regular, todas devem estar liberadas e a soma precisa ser inferior à pontuação mínima daquele trimestre. O portal pode indicar direito à recuperação anual se os três trimestres atenderem à condição de liberação e a base anual, com os ajustes de recuperação trimestral aplicáveis, ficar abaixo do mínimo anual.
- **Pontuação.** O mínimo em pontos corresponde ao máximo do período multiplicado pelo percentual configurado e dividido por 100, arredondado para duas casas decimais. Quando o aluno está abaixo do mínimo, a recuperação precisa atingir o percentual do período somado à diferença em pontos. Por exemplo, num trimestre de 30 pontos com mínimo de 70%, o mínimo vale 21. Se o aluno tem 18, precisa obter 73 na recuperação para elevar a pontuação trimestral a 21. O fechamento aplica a mesma regra à recuperação anual após os ajustes trimestrais. O total de notas regulares liberadas e a base ajustada podem, por isso, ser diferentes.
- **Turma de destino.** O aluno aprovado avança para a próxima série reconhecida. Entre várias turmas da série, o sistema prefere a de mesmo nome e depois ordena por nome e ID. Na última série, o aluno aprovado fica sem turma atual. O aluno reprovado permanece na turma. Série inválida ou falta de uma turma de destino obrigatória desfaz o fechamento inteiro. [Cálculos e progressão](../../internal/api/report.go)

## Histórico e limites da migração

Cada novo ciclo tem regras próprias. O fechamento processa os alunos ativos com turma ativa que ainda não têm resultado para aquele ciclo. O resultado já gravado de um aluno não impede o processamento dos demais. O boletim salvo no fechamento é preservado depois da promoção ou de alterações na configuração do ano seguinte.

Para ciclos já iniciados antes da V25, a migração mantém o mínimo efetivo de 70% aplicado pelo cálculo anterior. Quando há evidência de reutilização de um ciclo, a migração preserva os resultados anteriores. Só vincula atividades antigas a um ano quando a data da aula identifica um único ciclo. Atividades ambíguas permanecem consultáveis pela aula. Datas de início, listas de alunos e boletins que nunca foram gravados não podem ser reconstituídos com precisão. [Migração do histórico](../../db/migration/V25__academic_cycle_history.sql)

## Cenários de aceite

| Dado | Quando | Então |
| --- | --- | --- |
| Um máximo anual de 100 e máximos trimestrais de 30, 30 e 40 | O administrador altera uma parcela e a soma deixa de corresponder ao máximo do trimestre | A API rejeita a configuração. |
| Um ciclo em andamento | O administrador consulta as regras ou tenta alterá-las diretamente pela API | A tela permite consultar os valores, e o servidor rejeita a gravação. |
| Uma atividade ainda não liberada que vale 10 e um aluno na lista da atividade | O professor salva 8 | O lançamento contém 8. A célula correspondente do boletim permanece nula até a liberação. Após a liberação, a tentativa de corrigir a nota falha. |
| Uma atividade do trimestre vigente pendente de liberação | O administrador abre o diálogo de encerramento do trimestre | O professor aparece nas pendências, o fechamento fica indisponível e o ciclo permanece no trimestre vigente. |
| Um aluno aprovado em uma série que não é a última e nenhuma turma de destino | O administrador confirma o encerramento anual | O fechamento falha, nenhuma promoção é efetivada e o ciclo continua em andamento. |
| Um ano encerrado com boletim salvo | O administrador inicia outro ano e o aluno seleciona o ano anterior no boletim | Os ciclos têm IDs distintos e o resultado do ciclo encerrado permanece disponível ao aluno. |

## Quando revisar

Revisar com a equipe pedagógica os exemplos de liberação, recuperação, transferência e fechamento quando mudarem a composição das notas, a identificação dos ciclos, a política de recuperação ou o formulário de atividade.
