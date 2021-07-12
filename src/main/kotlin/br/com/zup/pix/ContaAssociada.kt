package br.com.zup.pix

import javax.persistence.Embeddable

@Embeddable
class ContaAssociada(
    instituicao: String,
    nomeTitular: String,
    cpfTitular: String,
    agencia: String,
    numeroConta: String
) {

}
