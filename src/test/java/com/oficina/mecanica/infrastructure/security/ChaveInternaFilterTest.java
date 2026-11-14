package com.oficina.mecanica.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.infrastructure.security.filter.ChaveInternaFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ChaveInternaFilterTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void chaveCorretaSegueParaOController() throws Exception {
        var resposta = filtrar(new ChaveInternaFilter("segredo", json), "/internal/identidades/clientes/1", "segredo");

        assertThat(resposta.getStatus()).isEqualTo(200);
    }

    @Test
    void chaveAusenteOuDiferenteRetorna401EmProblemDetail() throws Exception {
        var filtro = new ChaveInternaFilter("segredo", json);

        var semChave = filtrar(filtro, "/internal/identidades/clientes/1", null);
        var chaveErrada = filtrar(filtro, "/internal/identidades/clientes/1", "segredO");

        assertThat(semChave.getStatus()).isEqualTo(401);
        assertThat(semChave.getContentType()).startsWith("application/problem+json");
        assertThat(json.readTree(semChave.getContentAsString()).path("detail").asText())
            .isEqualTo("Credencial interna ausente ou inválida.");
        assertThat(chaveErrada.getStatus()).isEqualTo(401);
    }

    @Test
    void semChaveConfiguradaTudoEhRecusado() throws Exception {
        var filtro = new ChaveInternaFilter("", json);

        assertThat(filtrar(filtro, "/internal/identidades/funcionarios/1", "").getStatus()).isEqualTo(401);
        assertThat(filtrar(new ChaveInternaFilter(null, json), "/internal/x", "x").getStatus()).isEqualTo(401);
    }

    @Test
    void rotasForaDeInternalNaoSaoAfetadas() throws Exception {
        var resposta = filtrar(new ChaveInternaFilter("segredo", json), "/api/ordens", null);

        assertThat(resposta.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletResponse filtrar(ChaveInternaFilter filtro, String caminho, String chave)
            throws Exception {
        var requisicao = new MockHttpServletRequest("GET", caminho);
        if (chave != null) {
            requisicao.addHeader(ChaveInternaFilter.CABECALHO, chave);
        }
        var resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao, resposta, new MockFilterChain());
        return resposta;
    }
}
