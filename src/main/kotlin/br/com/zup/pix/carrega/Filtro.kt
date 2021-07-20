package br.com.zup.pix.carrega

import br.com.zup.integration.bcb.BancoCentralClientPix
import br.com.zup.pix.ChavePixNaoEncontradaException
import br.com.zup.pix.ChavePixRepository
import br.com.zup.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {
    /*
    * Deve retornar chave encontrada ou lançar uma exceção de erro de chave não encontrada
    * */

    abstract fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClientPix): ChavePixInfo

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String,
        @field:NotBlank @field:ValidUUID val pixId: String
    ) : Filtro() {
        fun pixIdAsUuid() = UUID.fromString(pixId)
        fun clienteIdAsUuid() = UUID.fromString(clienteId)

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClientPix): ChavePixInfo {
            return repository.findByIdAndClienteId(pixIdAsUuid(), clienteIdAsUuid())
                .map(ChavePixInfo::of)
                .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") }
        }
    }

    @Introspected
    data class PorChave(
        @field:NotBlank @field:Size(max = 77) val chave: String
    ) : Filtro() {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClientPix): ChavePixInfo {
            return repository.findByChave(chave)
                .map(ChavePixInfo::of)
                .orElseGet {
                    LOGGER.info("Consultando chave Pix '$chave' no Banco Central do Brasil (BCB)")

                    val response = bcbClient.findByKey(chave)
                    when (response.status) {
                        HttpStatus.OK -> response.body()?.toModel()
                        else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada")
                    }
                }
        }
    }

    @Introspected
    class Invalido : Filtro() {
        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClientPix): ChavePixInfo {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }
    }
}
