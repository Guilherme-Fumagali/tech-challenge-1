package com.oficina.mecanica.application.port;

public interface PublicadorMensagens {

    void publicar(ComandoAEnviar comando);
}
