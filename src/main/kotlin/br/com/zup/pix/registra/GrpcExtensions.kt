package br.com.zup.pix.registra

import br.com.zup.*
import br.com.zup.pix.TipoDeChave
import br.com.zup.pix.TipoDeConta

fun RegistraChaveRequest.toModel() : NovaChavePix {
    return NovaChavePix(
        clienteId = clienteId,
        tipo = when (tipoChave) {
            TipoChave.UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoChave.name)
        },
        chave = chave,
        tipoConta = when (tipoConta){
            TipoConta.UNKNOWN_TIPO_CONTA -> null
            else -> TipoDeConta.valueOf(tipoConta.name)
        }
    )
}