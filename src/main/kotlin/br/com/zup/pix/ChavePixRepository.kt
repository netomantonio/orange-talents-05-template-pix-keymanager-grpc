package br.com.zup.pix

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, UUID> {

    fun existsByChave(chave: String?): Boolean

    fun findByChave(chave: String): Optional<ChavePix>

    fun findByIdAndClienteId(id: UUID?, clienteId: UUID?): Optional<ChavePix>

    fun findAllByClienteId(uuidClienteId: UUID) : List<ChavePix>
}
