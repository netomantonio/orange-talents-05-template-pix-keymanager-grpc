package br.com.zup.pix.lista

import br.com.zup.*
import br.com.zup.pix.ChavePixRepository
import br.com.zup.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ListaChavesEndpoint(
    @Inject private val repository: ChavePixRepository
) : KeyManagerListaGrpcServiceGrpc.KeyManagerListaGrpcServiceImplBase() {

    override fun listar(request: ListaChavesRequest, responseObserver: StreamObserver<ListaChavesResponse>) {

        if (request.clienteId.isNullOrBlank()) throw IllegalArgumentException("Cliente ID deve estar preenchido")

//        val defaultPageable = Pageable.from(0, 10)
//        val provavelClient = itauClient.buscaCliente(request.clienteId)
//
//        if (provavelClient.status.code == HttpStatus.NOT_FOUND.code){
//            throw IllegalStateException("Cliente ID '${request.clienteId}'")
//        }
        val uuidClienteId = UUID.fromString(request.clienteId)

        val chaves = repository.findAllByClienteId(uuidClienteId).map{
            ListaChavesResponse.ChavePix.newBuilder()
                .setPixId(it.id.toString())
                .setTipoChave(br.com.zup.TipoChave.valueOf(it.tipo.name))
                .setChave(it.chave)
                .setTipoConta(br.com.zup.TipoConta.valueOf(it.tipoConta.name))
                .setCriadaEm(
                    it.criadaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                        Timestamp.newBuilder()
                            .setSeconds(createdAt.epochSecond)
                            .setNanos(createdAt.nano)
                        .build()
                    }
                )
            .build()
        }
        responseObserver.onNext(ListaChavesResponse.newBuilder()
            .setClienteId(uuidClienteId.toString())
            .addAllChaves(chaves)
            .build())

        responseObserver.onCompleted()
    }
}