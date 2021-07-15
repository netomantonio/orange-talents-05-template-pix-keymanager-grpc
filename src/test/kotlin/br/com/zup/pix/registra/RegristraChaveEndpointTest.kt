package br.com.zup.pix.registra

import br.com.zup.*
import br.com.zup.integration.itau.ContasClientesItauClient
import br.com.zup.pix.*
import br.com.zup.util.violations
import io.grpc.*
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RegristraChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRegistraGrpcServiceGrpc.KeyManagerRegistraGrpcServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: ContasClientesItauClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup(){
        repository.deleteAll()
    }

    @Test
    fun `deve cadastrar uma nova chave pix`(){
        // cenário
        `when`(itauClient.buscaContaTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))


        // ação
        val response = grpcClient.registrar(RegistraChaveRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoChave(TipoChave.EMAIL)
            .setChave("tone@gmail.com")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build()
        )

        // validação
        with(response) {
            assertEquals(CLIENTE_ID.toString(), clientId)
            assertNotNull(pixId)
        }

    }

    @Test
    fun `nao deve registrar chave pix quando chave existente`() {
        // cenário
        repository.save(chave(
            tipo = TipoDeChave.CPF,
            chave = "02712505182",
            clienteId = CLIENTE_ID
        ))

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistraChaveRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setChave("02712505182")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        // validação
        with(throws){
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix '02712505182' existente", status.description)
        }

    }

    @Test
    fun `nao deve registrar chave pix quando nao encontrar dados da conta cliente`() {
        // cenário
        `when`(itauClient.buscaContaTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistraChaveRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setChave("02712505182")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        with(throws){
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itau", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando parametros forem invalidos`() {
        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistraChaveRequest.newBuilder().build())
        }

        with(throws){
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(violations(), containsInAnyOrder(
                Pair("clienteId", "não deve estar em branco"),
                Pair("clienteId", "não é um formato válido de UUID"),
                Pair("tipoConta", "não deve ser nulo"),
                Pair("tipo", "não deve ser nulo"),

            ))
        }
    }

    @MockBean(ContasClientesItauClient::class)
    fun itauClient(): ContasClientesItauClient? {
        return Mockito.mock(ContasClientesItauClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerRegistraGrpcServiceGrpc.KeyManagerRegistraGrpcServiceBlockingStub{
            return KeyManagerRegistraGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun dadosDaContaResponse(): DadosContaResponse {
        return DadosContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", ContaAssociada.ITAU_UNIBANCO_ISPB),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Tone Max", "12345678910")
        )
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
                nomeTitular = "Rafael Ponte",
                cpfTitular = "63657520325",
                agencia = "1218",
                numeroConta = "291900"
            )
        )
    }
}