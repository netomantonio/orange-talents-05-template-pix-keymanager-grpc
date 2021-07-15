package br.com.zup.pix.registra

import br.com.zup.pix.*
import br.com.zup.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.*

@ValidPixKey
@Introspected
data class NovaChavePix(


    @field:NotBlank @field:ValidUUID val clienteId: String?,
    @field:NotNull val tipo: TipoDeChave?,
    @field:Size(max = 77) val chave: String?,
    @field:NotNull val tipoConta: TipoDeConta?
) {
    fun toModel(conta: ContaAssociada): ChavePix {
        return ChavePix(
            clienteId = UUID.fromString(this.clienteId),
            tipo = TipoDeChave.valueOf(this.tipo!!.name),
            chave = if (this.tipo == TipoDeChave.ALEATORIA) UUID.randomUUID().toString() else this.chave,
            tipoConta = TipoDeConta.valueOf(this.tipoConta!!.name),
            conta = conta
        )
    }

}
