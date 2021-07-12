package br.com.zup.pix.registra

import br.com.zup.integration.itau.ContasClientesItauClient
import br.com.zup.pix.*
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(@Inject val repository: ChavePixRepository,
                          @Inject val itauClient: ContasClientesItauClient
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registra(@Valid novaChavePix: NovaChavePix): ChavePix {
        if (repository.existsByChave(novaChavePix.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChavePix.chave}' existente")

        val response = itauClient.buscaContaTipo(novaChavePix.clienteId!!, novaChavePix.tipoConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        val chave = novaChavePix.toModel(conta)
        repository.save(chave)

        return chave
    }

}
