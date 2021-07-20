package br.com.zup.pix.carrega

import br.com.zup.*
import com.google.protobuf.Timestamp
import java.time.ZoneId

class CarregaChaveResponseConverter {
    fun convert(chaveInfo: ChavePixInfo): CarregaChaveResponse {
        return CarregaChaveResponse.newBuilder()
            .setClienteId(chaveInfo.clienteId?.toString() ?: "")
            .setPixId(chaveInfo.pixId?.toString() ?: "")
            .setChave(CarregaChaveResponse.ChavePix
                .newBuilder()
                .setTipo(TipoChave.valueOf(chaveInfo.tipo.name))
                .setChave(chaveInfo.chave)
                .setConta(CarregaChaveResponse.ChavePix.ContaInfo
                    .newBuilder()
                    .setTipo(TipoConta.valueOf(chaveInfo.tipoConta.name))
                    .setInstituicao(chaveInfo.conta.instituicao)
                    .setNomeTitular(chaveInfo.conta.nomeTitular)
                    .setCpfTitular(chaveInfo.conta.cpfTitular)
                    .setAgencia(chaveInfo.conta.agencia)
                    .setNumeroConta(chaveInfo.conta.numeroConta)
                    .build()
                )
                .setCriadaEm(chaveInfo.registradaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
            )
            .build()
    }

}
