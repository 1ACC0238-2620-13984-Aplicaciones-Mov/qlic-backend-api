package com.wasd.qlic.iam.application.commands

/** US05: request to create a subscriber account. */
data class RegisterAccountCommand(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val acceptTerms: Boolean
)

/** US06: request to authenticate a subscriber. */
data class AuthenticateCommand(
    val email: String,
    val password: String
)
