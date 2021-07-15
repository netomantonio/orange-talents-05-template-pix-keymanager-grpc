package br.com.zup.pix

import io.micronaut.validation.validator.constraints.EmailValidator
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator
import javax.validation.ConstraintValidatorContext

enum class TipoDeChave {
    CPF {
        override fun valida(chave: String?, context: ConstraintValidatorContext): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }

            if (!chave.matches("[0-9]+".toRegex())) {
                return false
            }

            return CPFValidator().run {
                initialize(null)
                isValid(chave, context)
            }
        }
    },

    CELULAR {
        override fun valida(chave: String?, context: ConstraintValidatorContext): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }
            return chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex())
        }
    },
    EMAIL {
        override fun valida(chave: String?, context: ConstraintValidatorContext): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }
            return EmailValidator().run {
                initialize(null)
                isValid(chave, context )
            }
        }
    },
    ALEATORIA {
        override fun valida(chave: String?, context: ConstraintValidatorContext) = chave.isNullOrBlank() // não deve se preenchida
    };

    abstract fun valida(chave: String?, context: ConstraintValidatorContext): Boolean
}
