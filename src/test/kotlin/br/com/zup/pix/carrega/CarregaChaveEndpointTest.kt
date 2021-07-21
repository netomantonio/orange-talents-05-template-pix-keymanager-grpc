package br.com.zup.pix.carrega

import br.com.zup.CarregaChaveRequest
import br.com.zup.KeyManagerCarregaGrpcServiceGrpc
import br.com.zup.integration.bcb.*
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
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class CarregaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerCarregaGrpcServiceGrpc.KeyManagerCarregaGrpcServiceBlockingStub
) {
    @Inject
    lateinit var bcbClient: BancoCentralClientPix

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setUp() {
        repository.save(chave(tipo = TipoDeChave.EMAIL, chave = "tone@gmail.com", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.CPF, chave = "02712505182", clienteId = UUID.randomUUID()))
        repository.save(chave(tipo = TipoDeChave.ALEATORIA, chave = "randomica", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.CELULAR, chave = "+5541988535194", clienteId = CLIENTE_ID))
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }


    @Test
    fun `deve carregar chave por pixId e clienteId`() {
        // cenáro

        val chaveExistente = repository.findByChave("+5541988535194").get()

        // ação
        val response = grpcClient.carregar(
            CarregaChaveRequest.newBuilder()
                .setPixId(
                    CarregaChaveRequest.FiltroPorPixId.newBuilder()
                        .setPixId(chaveExistente.id.toString())
                        .setClienteId(chaveExistente.clienteId.toString())
                        .build()
                ).build()
        )

        // validação

        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)

        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando filtro invalido`() {

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.carregar(
                CarregaChaveRequest.newBuilder()
                    .setPixId(
                        CarregaChaveRequest.FiltroPorPixId.newBuilder()
                            .setPixId("")
                            .setClienteId("")
                            .build()
                    ).build()
            )
        }

        // validação

        with(throws) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            MatcherAssert.assertThat(
                violations(),
                Matchers.containsInAnyOrder(
                    Pair("pixId", "não deve estar em branco"),
                    Pair("pixId", "não é um formato válido de UUID"),
                    Pair("clienteId", "não é um formato válido de UUID"),
                    Pair("clienteId", "não deve estar em branco")
                )
            )
        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando registro nao existir`() {
        // ação
        val pixIdNaoExistente = UUID.randomUUID().toString()
        val clienteIdNaoExistente = UUID.randomUUID().toString()
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.carregar(
                CarregaChaveRequest.newBuilder()
                    .setPixId(
                        CarregaChaveRequest.FiltroPorPixId.newBuilder()
                            .setPixId(pixIdNaoExistente)
                            .setClienteId(clienteIdNaoExistente)
                            .build()
                    ).build()
            )
        }

        // validação
        with(throws) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro existir localmente`() {
        // cenário
        val chaveExistente = repository.findByChave("tone@gmail.com").get()

        // ação
        val response = grpcClient.carregar(CarregaChaveRequest.newBuilder().setChave("tone@gmail.com").build())

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro nao existir localmente mas existir no BCB`() {
        // cenário
        val bcbResponse = pixKeyDetailsResponse()
        `when`(bcbClient.findByKey("user.from@santander.bank"))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

        // ação
        val response = grpcClient.carregar(
            CarregaChaveRequest.newBuilder()
                .setChave("user.from@santander.bank")
                .build()
        )

        // validação
        with(response) {
            assertEquals("", this.pixId)
            assertEquals("", this.clienteId)
            assertEquals(bcbResponse.keyType.name, this.chave.tipo.name)
            assertEquals(bcbResponse.key, this.chave.chave)
        }
    }


    @Test
    fun `nao deve carregar chave por valor da chave quando registro nao existir localmente nem no BCB`() {
        // cenário
        `when`(bcbClient.findByKey("this.not@existing.key"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.carregar(
                CarregaChaveRequest.newBuilder()
                    .setChave("this.not@existing.key")
                    .build()
            )
        }

        // validação
        with(throws) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave quando filtro invalido`() {
        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.carregar(
                CarregaChaveRequest.newBuilder()
                    .setChave("")
                    .build()
            )
        }

        // validação

        with(throws) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            MatcherAssert.assertThat(
                violations(),
                Matchers.containsInAnyOrder(
                    Pair("chave", "não deve estar em branco")
                )
            )
        }
    }

    @Test
    fun `nao deve carregar chave quando filtro invalido`() {
        // ação
        val throws = assertThrows<StatusRuntimeException> {
            grpcClient.carregar(
                CarregaChaveRequest.newBuilder().build()
            )
        }

        // validação

        with(throws) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }


    @MockBean(BancoCentralClientPix::class)
    fun bcbClient(): BancoCentralClientPix? {
        return Mockito.mock(BancoCentralClientPix::class.java)
    }

    @Factory
    class CarregaClients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerCarregaGrpcServiceGrpc.KeyManagerCarregaGrpcServiceBlockingStub {
            return KeyManagerCarregaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
        return PixKeyDetailsResponse(
            keyType = PixKeyType.EMAIL,
            key = "tone@gmail.com",
            bankAccount = BankAccount(
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                branch = "18767",
                accountNumber = "505900",
                accountType = BankAccount.AccountType.CACC
            ),
            owner = Owner(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Tone Max",
                taxIdNumber = "02712505182"
            ),
            createdAt = LocalDateTime.now()
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