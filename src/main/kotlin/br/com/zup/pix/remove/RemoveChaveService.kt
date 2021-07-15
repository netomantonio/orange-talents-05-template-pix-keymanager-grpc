package br.com.zup.pix.remove

import br.com.zup.pix.ChavePixNaoEncontradaException
import br.com.zup.pix.ChavePixRepository
import br.com.zup.shared.validation.ValidUUID
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChaveService(@Inject val repository: ChavePixRepository) {

    @Transactional
    fun remove(
        @NotBlank @ValidUUID(message = "cliente ID com formato inválido") clienteId: String?,
        @NotBlank @ValidUUID(message = "pix ID com formato inválido") pixId: String?
    ){
        val uuidPixId = UUID.fromString(pixId)
        val uuidClienteId = UUID.fromString(clienteId)
        repository.findByIdAndClienteId(uuidPixId, uuidClienteId)
            .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada ou não pertence ao cliente ID")}

        repository.deleteById(uuidPixId)
    }
}
