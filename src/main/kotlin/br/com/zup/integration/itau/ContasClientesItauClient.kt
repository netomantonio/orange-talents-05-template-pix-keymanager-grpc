package br.com.zup.integration.itau

import br.com.zup.pix.registra.DadosContaResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${itau.contas.url}")
interface ContasClientesItauClient {

    @Get("/api/v1/clientes/{clienteId}/contas{?tipo}")
    fun buscaContaTipo(@PathVariable clienteId: String, @QueryValue tipo: String): HttpResponse<DadosContaResponse>

}
