package br.com.zup.pix

import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(
        name = "uk_chave_pix",
        columnNames = ["chave"]
    )]
)
class
ChavePix(
    @field:NotNull @Column(nullable = false) val clienteId: UUID,
    @field:NotNull @Enumerated(EnumType.STRING) @Column(nullable = false) val tipo: TipoDeChave,
    @field:NotBlank @Column(unique = true, nullable = false) var chave: String?,
    @field:NotNull @Enumerated(EnumType.STRING) @Column(nullable = false) val tipoConta: TipoDeConta,
    @field:Valid @Embedded val conta: ContaAssociada
) {
    fun atualiza(key: String) {
        this.chave = key;
    }

    @Id
    @GeneratedValue
    val id: UUID? = null

    @Column(nullable = false)
    val criadaEm: LocalDateTime = LocalDateTime.now()

}
