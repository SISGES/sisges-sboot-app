# [Componente] — Referência de configuração

Público e escopo — [desenvolvedor ou operador, serviço e ambiente]

Fontes de referência — [código que lê a configuração, manifesto de implantação e revisão da compilação]

## Contrato de configuração

| Configuração | Lida na compilação ou na execução | Unidade, valor padrão e intervalo válido | Dependências obrigatórias | Efeito no serviço |
| --- | --- | --- | --- | --- |
| [Nome] | [Momento da leitura] | [Valor ou requisito] | [Dependência] | [Efeito observável, inclusive para valores inválidos ou vazios] |

## Diferenças entre ambientes

[Compare os padrões da aplicação, os valores da imagem e as substituições feitas na implantação. Explique como fornecer segredos usando referências ou exemplos fictícios.]

## Verificação

[Descreva uma verificação segura e observável e o sintoma esperado em caso de falha. Indique quais alterações exigem recompilação, reinicialização ou rotação coordenada de credenciais.]

## Quando revisar

[Atualize quando mudar a leitura da configuração, os argumentos de compilação, o manifesto de implantação ou o contrato de um componente consumidor.]
