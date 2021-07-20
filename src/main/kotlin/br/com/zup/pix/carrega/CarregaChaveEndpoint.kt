package br.com.zup.pix.carrega

import br.com.zup.*
import br.com.zup.integration.bcb.BancoCentralClientPix
import br.com.zup.pix.ChavePixRepository
import br.com.zup.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@ErrorHandler
@Singleton
class CarregaChaveEndpoint(
    @Inject private val repository: ChavePixRepository,
    @Inject private val bcbClient: BancoCentralClientPix,
    @Inject private val validador: Validator
) : KeyManagerCarregaGrpcServiceGrpc.KeyManagerCarregaGrpcServiceImplBase() {

    override fun carregar(
        request: CarregaChaveRequest,
        responseObserver: StreamObserver<CarregaChaveResponse>
    ) {
        val filtro = request.toModel(validador)
        val chaveInfo = filtro.filtra(repository = repository, bcbClient = bcbClient )

        responseObserver.onNext(CarregaChaveResponseConverter().convert(chaveInfo))
        responseObserver.onCompleted()
    }
}