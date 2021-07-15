package br.com.zup.pix.registra

import br.com.zup.*
import br.com.zup.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegristraChaveEndpoint(@Inject private val service: NovaChavePixService)
    : KeyManagerRegistraGrpcServiceGrpc.KeyManagerRegistraGrpcServiceImplBase(){

    override fun registrar(
        request: RegistraChaveRequest,
        responseObserver: StreamObserver<RegistraChaveResponse>) {
        val novaChave = request.toModel()
        val chaveCriada = service.registra(novaChave)

        responseObserver.onNext(
            RegistraChaveResponse.newBuilder()
            .setClientId(chaveCriada.clienteId.toString())
            .setPixId(chaveCriada.id.toString())
            .build()
        )
        responseObserver.onCompleted()
    }

}