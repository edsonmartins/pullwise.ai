package com.pullwise.intellij.auth

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class AuthManager {

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("Pullwise", "API Token")
    )

    fun getToken(): String? {
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    fun setToken(token: String) {
        PasswordSafe.instance.set(credentialAttributes, Credentials("pullwise", token))
    }

    fun clearToken() {
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    fun isAuthenticated(): Boolean {
        val token = getToken()
        return token != null && token.isNotBlank()
    }

    companion object {
        val instance: AuthManager
            get() = ApplicationManager.getApplication().getService(AuthManager::class.java)
    }
}
