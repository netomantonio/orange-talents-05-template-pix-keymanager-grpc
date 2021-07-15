package br.com.zup.pix.remove

import br.com.zup.*
import br.com.zup.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemoveChaveEndpoint(@Inject private val service: RemoveChaveService): KeyManagerRemoveGrpcServiceGrpc
.KeyManagerRemoveGrpcServiceImplBase(){

    override fun remover(request: RemoveChaveRequest, responseObserver: StreamObserver<RemoveChaveResponse>) {

        service.remove(clienteId = request.clienteId, pixId = request.pixId)

        responseObserver.onNext(RemoveChaveResponse.newBuilder().setInfo("Removed successful").build())

        responseObserver.onCompleted()
    }
}