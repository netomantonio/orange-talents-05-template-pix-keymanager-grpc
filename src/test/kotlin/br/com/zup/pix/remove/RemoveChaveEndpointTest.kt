package br.com.zup.pix.remove

import br.com.zup.*
import br.com.zup.integration.bcb.*
import br.com.zup.pix.*
import io.grpc.*
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RemoveChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRemoveGrpcServiceGrpc.KeyManagerRemoveGrpcServiceBlockingStub
) {

    lateinit var CHAVE_EXISTENTE: ChavePix

    @Inject
    lateinit var bcbClient: BancoCentralClientPix

    @BeforeEach
    fun setup() {
        CHAVE_EXISTENTE = repository.save(chave(
            tipo = TipoDeChave.EMAIL,
            chave = "tone@gmail.com",
            clienteId = UUID.randomUUID()
        ))
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve remover chave pix existente`() {

        // cenário
        `when`(bcbClient.remover("tone@gmail.com", DeletePixKeyRequest("tone@gmail.com")))
            .thenReturn(HttpResponse.ok(DeletePixKeyResponse(
                key = "tone@gmail.com",
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                deleteAt = LocalDateTime.now()))
            )

        // ação
        grpcClient.remover(
            RemoveChaveRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build()
        )


        //validação
        val response = repository.findByIdAndClienteId(CHAVE_EXISTENTE.id, CHAVE_EXISTENTE.clienteId).isEmpty
        assertTrue(response)
    }

    @Test

    fun `nao deve remover chave pix quando chave nao existe`() {

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChaveRequest.newBuilder()
                    .setPixId(UUID.randomUUID().toString())
                    .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                    .build()
            )
        }


        //validação
        with(throws){
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente ID", status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix existente quando ocorrer algum erro no servico do BCB`(){
        // cenário
        `when`(bcbClient.remover("tone@gmail.com", DeletePixKeyRequest("tone@gmail.com")))
            .thenReturn(HttpResponse.unprocessableEntity())

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.remover(RemoveChaveRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build())
        }

        // validação
        with(throws){
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao remover chave Pix no Banco Central do Brasil (BCB)", status.description)
        }
    }

    @MockBean(BancoCentralClientPix::class)
    fun bcbClient(): BancoCentralClientPix? {
        return Mockito.mock(BancoCentralClientPix::class.java)
    }

    @Factory
    class RemoveClients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerRemoveGrpcServiceGrpc.KeyManagerRemoveGrpcServiceBlockingStub{
            return KeyManagerRemoveGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun chave(
        tipo: TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipo = tipo,
            chave = chave,
            tipoConta = br.com.zup.pix.TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "UNIBANCO ITAU",
                nomeTitular = "Tone Max",
                cpfTitular = "02712505182",
                agencia = "1218",
                numeroConta = "291900"
            )
        )
    }
}