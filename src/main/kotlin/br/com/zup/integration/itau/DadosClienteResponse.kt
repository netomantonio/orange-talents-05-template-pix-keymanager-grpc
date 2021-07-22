package br.com.zup.integration.itau

data class DadosClienteResponse(
    val clienteId: String?,
    val nomeTitular: String?,
    val cpfTitular: String?,
    val instituicao: InstituicaoResponse?
) {


}
