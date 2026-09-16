package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V9__SeedFuncionarioHomologacao extends BaseJavaMigration {

    static final String PLACEHOLDER_CPF = "funcionario-seed-cpf";
    static final String PLACEHOLDER_NOME = "funcionario-seed-nome";

    private static final String SQL = """
        INSERT INTO funcionarios (cpf, nome)
        VALUES (?, ?)
        ON CONFLICT (cpf) DO NOTHING
        """;

    @Override
    public void migrate(Context context) throws Exception {
        var placeholders = context.getConfiguration().getPlaceholders();
        var cpf = placeholders.getOrDefault(PLACEHOLDER_CPF, "").replaceAll("\\D", "");
        if (cpf.isEmpty()) {
            return;
        }
        if (cpf.length() != 11) {
            throw new IllegalStateException("CPF do funcionário de homologação deve ter 11 dígitos.");
        }
        var nome = placeholders.getOrDefault(PLACEHOLDER_NOME, "Funcionário de homologação");

        try (var ps = context.getConnection().prepareStatement(SQL)) {
            ps.setString(1, cpf);
            ps.setString(2, nome.isBlank() ? "Funcionário de homologação" : nome);
            ps.executeUpdate();
        }
    }
}
