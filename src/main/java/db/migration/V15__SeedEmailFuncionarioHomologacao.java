package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V15__SeedEmailFuncionarioHomologacao extends BaseJavaMigration {

    static final String PLACEHOLDER_CPF = V9__SeedFuncionarioHomologacao.PLACEHOLDER_CPF;
    static final String PLACEHOLDER_EMAIL = "funcionario-seed-email";

    private static final String SQL = "UPDATE funcionarios SET email = ? WHERE cpf = ?";

    @Override
    public void migrate(Context context) throws Exception {
        var placeholders = context.getConfiguration().getPlaceholders();
        var cpf = placeholders.getOrDefault(PLACEHOLDER_CPF, "").replaceAll("\\D", "");
        var email = placeholders.getOrDefault(PLACEHOLDER_EMAIL, "").trim();
        if (cpf.isEmpty() || email.isEmpty()) {
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalStateException("E-mail do funcionário de homologação inválido.");
        }
        try (var ps = context.getConnection().prepareStatement(SQL)) {
            ps.setString(1, email);
            ps.setString(2, cpf);
            ps.executeUpdate();
        }
    }
}
