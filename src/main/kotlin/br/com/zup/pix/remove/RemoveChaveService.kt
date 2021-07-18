package br.com.zup.pix.remove

import br.com.zup.integration.bcb.BancoCentralClientPix
import br.com.zup.integration.bcb.DeletePixKeyRequest
import br.com.zup.pix.ChavePixNaoEncontradaException
import br.com.zup.pix.ChavePixRepository
import br.com.zup.shared.validation.ValidUUID
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChaveService(
    @Inject val repository: ChavePixRepository,
    @Inject val bcbClient: BancoCentralClientPix
) {

    @Transactional
    fun remove(
        @NotBlank @ValidUUID(message = "cliente ID com formato inválido") clienteId: String?,
        @NotBlank @ValidUUID(message = "pix ID com formato inválido") pixId: String?
    ) {
        val uuidPixId = UUID.fromString(pixId)

        val uuidClienteId = UUID.fromString(clienteId)

        val chave = repository.findByIdAndClienteId(uuidPixId, uuidClienteId)
            .orElseThrow {
                ChavePixNaoEncontradaException("Chave Pix não encontrada ou não pertence ao cliente ID")
            }

        val request = DeletePixKeyRequest(chave.chave !!)


        val bcbResponse = bcbClient.remover(key = chave.chave !!, request = request)


        if (bcbResponse.status != HttpStatus.OK && bcbResponse.status != HttpStatus.NOT_FOUND) {
            throw IllegalStateException("Erro ao remover chave Pix no Banco Central do Brasil (BCB)")
        }
        repository.deleteById(uuidPixId)

    }
}
