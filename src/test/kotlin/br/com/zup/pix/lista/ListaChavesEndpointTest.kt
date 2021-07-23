package br.com.zup.pix.lista

import br.com.zup.*
import br.com.zup.integration.bcb.BancoCentralClientPix
import br.com.zup.integration.itau.ContasClientesItauClient
import br.com.zup.pix.*
import io.grpc.*
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*

@MicronautTest(transactional = false)
internal class ListaChavesEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerListaGrpcServiceGrpc.KeyManagerListaGrpcServiceBlockingStub,
    val itauClient: ContasClientesItauClient
) {

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setUp() {
        repository.save(chave(TipoDeChave.EMAIL, "tone@max.com", CLIENTE_ID))
        repository.save(chave(TipoDeChave.ALEATORIA, "randomkey-1", UUID.randomUUID()));
        repository.save(chave(TipoDeChave.ALEATORIA, "randomkey-2", CLIENTE_ID))
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve listar todas as chaves do cliente`() {
        // cenário
        val clienteId = CLIENTE_ID.toString()
        `when`(itauClient.buscaCliente(clienteId))
            .thenReturn(HttpResponse.ok())

        // ação
        val response = grpcClient.listar(
            ListaChavesRequest.newBuilder().setClienteId(clienteId).build()
        )

        with(response.chavesList) {
            assertThat(this, hasSize(2))
            assertThat(
                this.map { Pair(it.tipoChave, it.chave) }.toList(),
                containsInAnyOrder(
                    Pair(TipoChave.ALEATORIA, "randomkey-2"),
                    Pair(TipoChave.EMAIL, "tone@max.com")
                )
            )
        }
    }

    @Test
    fun `nao deve listar as chaves do cliente quando cliente nao possuir chaves`() {
        // cenário
        val clienteSemChave = UUID.randomUUID().toString()
        `when`(itauClient.buscaCliente(clienteSemChave))
            .thenReturn(HttpResponse.ok())

        // ação
        val response = grpcClient.listar(ListaChavesRequest.newBuilder()
            .setClienteId(clienteSemChave)
            .build()
        )

        // validação
        assertEquals(0, response.chavesCount)

    }

    @Test
    fun `nao deve listar todas as chaves do cliente quando clienteId for invalido`() {
        // cenário
        val clienteIdInvalido = ""

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.listar(ListaChavesRequest
                .newBuilder()
                .setClienteId(clienteIdInvalido)
                .build()
            )
        }

        // validação
        with(throws) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Cliente ID deve estar preenchido", status.description)
        }
    }

    @Factory
    class ListaClients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerListaGrpcServiceGrpc.KeyManagerListaGrpcServiceBlockingStub {
            return KeyManagerListaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(ContasClientesItauClient::class)
    fun itauClient(): ContasClientesItauClient? {
        return Mockito.mock(ContasClientesItauClient::class.java)
    }

    private fun chave(
        tipo: TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID()
    ): ChavePix {
        return ChavePix(
            clienteId,
            tipo,
            chave,
            TipoDeConta.CONTA_CORRENTE,
            ContaAssociada(
                "ITAU UNIBANCO",
                "Tone Max",
                "02712505182",
                "1876",
                "505900"
            )
        )

    }
}