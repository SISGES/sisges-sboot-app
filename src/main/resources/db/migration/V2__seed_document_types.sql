-- SISGES - Dados iniciais: tipos de documento
-- Migration V2

INSERT INTO sisges.document_type (name, description) VALUES
    ('RG', 'Registro Geral (identidade)'),
    ('CPF', 'Cadastro de Pessoa Física'),
    ('Certidão de Nascimento', 'Certidão de nascimento ou equivalente'),
    ('Histórico Escolar', 'Histórico ou declaração de conclusão de série/ano anterior'),
    ('Foto 3x4', 'Foto recente para documentação escolar'),
    ('Comprovante de Residência', 'Comprovante de endereço')
ON CONFLICT (name) DO NOTHING;
