package br.com.zup.pix.registra

import br.com.zup.integration.bcb.BancoCentralClientPix
import br.com.zup.integration.bcb.CreatePixKeyRequest
import br.com.zup.integration.itau.ContasClientesItauClient
import br.com.zup.pix.*
import br.com.zup.shared.grpc.handlers.ConstraintViolationExceptionHandler
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(@Inject val repository: ChavePixRepository,
                          @Inject val itauClient: ContasClientesItauClient,
                          @Inject val bcbClient: BancoCentralClientPix
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registra(@Valid novaChavePix: NovaChavePix): ChavePix {
        if (repository.existsByChave(novaChavePix.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChavePix.chave}' existente")


        val itauResponse = itauClient.buscaContaTipo(novaChavePix.clienteId!!, novaChavePix.tipoConta!!.name)
        val conta = itauResponse.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")
        val chave = novaChavePix.toModel(conta)

        repository.save(chave)


        val bcbRequest = CreatePixKeyRequest.of(chave).also {
            LOGGER.info("Registrando chave Pix no Banco Central do Brasil (BCB): $it")
        }

        val bcbResponse = bcbClient.registrar(bcbRequest)
        if (bcbResponse.status != HttpStatus.CREATED)
            throw IllegalStateException("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)")

        chave.atualiza(bcbResponse.body()!!.key)

        return chave
    }

}
