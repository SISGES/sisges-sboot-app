# Grade horária, registro de aulas e frequência

**Escopo** · Especificação funcional dos requisitos P-03 a P-05. O administrador define a grade semanal de disciplinas. O professor registra o conteúdo ministrado em cada data e faz a chamada da turma.

## Da grade ao registro da aula

| Etapa | Pré-condições e ação | Resultado ou exceção |
| --- | --- | --- |
| Planejar | Em `/admin/grade-horaria`, o administrador seleciona a turma e distribui as disciplinas vinculadas pelos horários da semana. | O rascunho do usuário permanece na memória do navegador durante a navegação entre telas. Recarregar a página ou sair da conta descarta o rascunho. |
| Salvar a grade | Enviar a lista completa de horários e a revisão retornada na última consulta. Os dias vão de segunda a sexta (1 a 5), com horários de 0 a 4. | Se a revisão coincidir, a operação substitui a lista de uma só vez e incrementa a revisão, inclusive ao salvar uma grade vazia. A API rejeita horários duplicados, disciplinas sem vínculo e revisões desatualizadas. Consultar a grade novamente antes de conciliar alterações simultâneas. |
| Consultar o dia | O professor escolhe uma de suas turmas e a data no registro diário. | A API reúne os horários ainda sem registro que o professor pode ministrar e as aulas da grade já registradas por ele naquela data. Se não houver horários correspondentes, retorna uma lista vazia. |
| Registrar a aula | Enviar data, horário, disciplina e conteúdo, anexos ou ambos. Para um novo registro, o professor deve estar vinculado à turma e à disciplina prevista. | O primeiro envio cria a aula naquela data. Ao salvar de novo o mesmo horário de sua autoria com a mesma disciplina, o professor atualiza o conteúdo e os anexos no próprio registro. Uma aula do modelo anterior que se sobreponha ao intervalo impede a criação. |
| Consultar uma aula | A equipe abre o registro pela tela Aulas. | O professor pode consultar as próprias aulas. O administrador pode consultar e excluir registros. Aulas antigas sem horário de grade ainda podem ser editadas. A API rejeita a criação manual de novas aulas. |

Os cinco horários são 07:00–07:50, 07:50–08:40, 08:40–09:30, 09:45–10:35 e 10:35–11:25. Alterações posteriores na grade afetam os horários ainda sem registro. Uma aula já registrada preserva a disciplina original, o conteúdo e o ID. A API de registro diário permite a gravação apenas ao professor, embora a proteção da rota de professor no frontend também admita administradores. [Grade e registro diário](../../internal/api/schedules.go) · [Operações de aula](../../internal/api/meetings.go) · [Ciclo de vida do rascunho](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/TimetableDraftProvider.tsx)

## Frequência

A tela da aula mostra a lista atual de alunos da turma. No navegador, um aluno sem lançamento aparece inicialmente como presente. A interface envia uma atualização individual quando a marcação muda. Assim, os valores exibidos por padrão são insuficientes para comprovar que a chamada foi concluída. O professor da aula ou o administrador pode enviar um ou mais lançamentos com `P` (presente) ou `F` (faltoso). A API verifica se cada aluno pertence atualmente à turma da aula e grava os lançamentos em uma transação. Um novo envio atualiza a marcação de cada aluno informado. O portal agrupa por disciplina as faltas explicitamente registradas. Uma aula sem marcação não soma falta. [Gravação da frequência](../../internal/api/meetings.go) · [Tela de detalhes da aula](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/AulaDetail/AulaDetail.tsx) · [Consulta de faltas do aluno](../../internal/api/students.go)

## Anexos da aula e materiais de estudo

**Registro de aula.** É obrigatório informar texto ou ao menos um anexo. Após remover espaços nas extremidades, o conteúdo admite até 20.000 bytes. Cada registro comporta até dez referências de anexo. Com o R2 configurado, o professor pode enviar para `lesson-files` arquivos de imagem, PDF, texto ou Office em formatos aceitos, com até 10 MiB cada. A referência do anexo deve usar o caminho de quem fez o envio na conta atual. A autorização de download também verifica o vínculo com a aula referenciada para os membros da turma que têm direito de acesso. [Validação da aula](../../internal/api/schedules.go) · [Armazenamento](../../internal/api/storage.go) · [Tela de registro diário](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/CreateAula/CreateAula.tsx)

**Material de estudo.** Em `/materiais`, o professor escolhe turma e disciplina, informa um título e pode acrescentar uma descrição. A criação verifica os vínculos entre professor e turma, professor e disciplina, e turma e disciplina. O aluno vê os materiais da turma atual. Somente o professor que criou o material pode excluí-lo. O seletor de arquivo opcional usa o destino `materials`, rejeitado pela operação de envio em Go. O material somente com texto pode ser publicado sem armazenamento de objetos. [Operações de materiais](../../internal/api/content.go) · [Tela de materiais](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Materials/Materials.tsx)

## Cenários de aceite

| Dado | Quando | Então |
| --- | --- | --- |
| Dois administradores consultaram a revisão 2 | O primeiro salva a revisão 3 e o segundo envia a revisão anterior | O segundo recebe um erro de regra de negócio e a alteração do primeiro é preservada. |
| Um professor vinculado, um horário de segunda-feira e a disciplina atribuída | O professor registra o conteúdo, faz a chamada e depois edita o conteúdo da aula | O ID da aula e os lançamentos de frequência são preservados. |
| Um horário já registrado cuja disciplina foi alterada na grade | O professor que fez o registro reabre aquela data | A aula salva mantém a disciplina original. |
| Uma aula com dois alunos da turma e um aluno de outra turma | O responsável pela chamada inclui o aluno externo em um lote de lançamentos | O lote falha e nenhuma das alterações é gravada. |
| Um professor vinculado à turma e à disciplina | Ele publica um material com título e sem arquivo | O aluno matriculado consegue vê-lo. A exclusão pelo professor que o criou retira o material da lista de itens ativos. |

## Quando revisar

Ao mudar a duração ou os horários de aula, a matrícula na turma, as regras de arquivos ou os valores de frequência, revisar em conjunto a validação de horários, o editor da grade, o registro diário e o portal do aluno. [Testes do fluxo de horários](../../internal/api/schedules_test.go)
