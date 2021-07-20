package br.com.zup.pix.registra

import br.com.zup.*
import br.com.zup.integration.bcb.*
import br.com.zup.integration.itau.*
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
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RegristraChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRegistraGrpcServiceGrpc.KeyManagerRegistraGrpcServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: ContasClientesItauClient

    @Inject
    lateinit var bcbClient: BancoCentralClientPix


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

        `when`(bcbClient.registrar(createPixKeyRequest()))
            .thenReturn(HttpResponse.created(createPixKeyResponse()))


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

    @Test
    fun `nao deve registrar chave pix quando nao for possivel registrar chave no BCB`(){

        // cenário
        `when`(itauClient.buscaContaTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.registrar(createPixKeyRequest()))
            .thenReturn(HttpResponse.badRequest())

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistraChaveRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.EMAIL)
                .setChave("tone@gmail.com")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        // validação
        with(throws) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)", status.description)
        }
    }


    @MockBean(ContasClientesItauClient::class)
    fun itauClient(): ContasClientesItauClient? {
        return Mockito.mock(ContasClientesItauClient::class.java)
    }

    @MockBean(BancoCentralClientPix::class)
    fun bcbClient(): BancoCentralClientPix? {
        return Mockito.mock(BancoCentralClientPix::class.java)
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
            titular = TitularResponse("Tone Max", "02712505182")
        )
    }

    private fun createPixKeyRequest(): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = PixKeyType.EMAIL,
            key = "tone@gmail.com",
            bankAccount = bankAccount(),
            owner = owner()
        )
    }

    private fun createPixKeyResponse(): CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = PixKeyType.EMAIL,
            key = "tone@gmail.com",
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(
            participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
            branch = "1218",
            accountNumber = "291900",
            accountType = BankAccount.AccountType.CACC
        )
    }

    private fun owner(): Owner {
        return Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Tone Max",
            taxIdNumber = "02712505182"
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
            tipoConta = TipoDeConta.CONTA_CORRENTE,
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