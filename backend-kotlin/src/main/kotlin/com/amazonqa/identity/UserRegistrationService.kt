package com.amazonqa.identity

import com.amazonqa.audit.AuditService
import com.amazonqa.common.exception.DomainException
import com.amazonqa.security.Role
import com.amazonqa.store.StateStore
import com.amazonqa.store.UserProfileRecord
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class RegisterUserPayload(
    val personType: String? = "PF",
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val cpf: String? = null,
    val cnpj: String? = null,
    val companyName: String? = null,
    val addressZip: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val addressComplement: String? = null,
    val addressNeighborhood: String? = null,
    val addressCity: String? = null,
    val addressState: String? = null,
    val residenceProofFilename: String? = null,
)

data class AdminCreateUserPayload(
    val personType: String? = "PF",
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val cpf: String? = null,
    val cnpj: String? = null,
    val companyName: String? = null,
    val addressZip: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val addressComplement: String? = null,
    val addressNeighborhood: String? = null,
    val addressCity: String? = null,
    val addressState: String? = null,
    val residenceProofFilename: String? = null,
    val role: String? = "user",
)

data class UserUpdatePayload(
    val personType: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val cpf: String? = null,
    val cnpj: String? = null,
    val companyName: String? = null,
    val addressZip: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val addressComplement: String? = null,
    val addressNeighborhood: String? = null,
    val addressCity: String? = null,
    val addressState: String? = null,
    val residenceProofFilename: String? = null,
)

data class AddressUpdatePayload(
    val addressZip: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val addressComplement: String? = null,
    val addressNeighborhood: String? = null,
    val addressCity: String? = null,
    val addressState: String? = null,
)

data class UserRegistrationResponse(
    val id: UUID,
    val fullName: String,
    val email: String,
    val status: String,
    val personType: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val cpf: String?,
    val cnpj: String?,
    val companyName: String?,
    val addressZip: String?,
    val addressStreet: String?,
    val addressNumber: String?,
    val addressComplement: String?,
    val addressNeighborhood: String?,
    val addressCity: String?,
    val addressState: String?,
    val residenceProofFilename: String?,
)

@Service
class UserRegistrationService(
    private val userService: UserService,
    private val stateStore: StateStore,
    private val auditService: AuditService,
    private val jdbcTemplateProvider: ObjectProvider<JdbcTemplate>,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun registerUser(payload: RegisterUserPayload): UserRegistrationResponse {
        val email = payload.email?.trim().orEmpty()
        if (email.isBlank()) {
            throw DomainException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "Email is required", "email")
        }
        val password = payload.password.orEmpty()
        if (password.isBlank()) {
            throw DomainException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "Password is required", "password")
        }

        val fullName = fullNameWithFallback(payload.firstName, payload.lastName, email)
        val user = userService.createUser(fullName = fullName, email = email, role = Role.GUEST)
        val profile = buildProfile(user.id, payload, password)

        stateStore.userProfiles[user.id] = profile
        upsertProfile(profile)
        auditService.logCreate("USER_PROFILE", user.id.toString(), "USER_REGISTERED")

        return toResponse(user.id)
    }

    fun adminCreateUser(payload: AdminCreateUserPayload): UserRegistrationResponse {
        val email = payload.email?.trim().orEmpty()
        if (email.isBlank()) {
            throw DomainException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "Email is required", "email")
        }
        val password = payload.password.orEmpty()
        if (password.isBlank()) {
            throw DomainException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "Password is required", "password")
        }

        val fullName = fullNameWithFallback(payload.firstName, payload.lastName, email)
        val user = userService.createUser(fullName = fullName, email = email, role = mapRole(payload.role))
        val profile =
            UserProfileRecord(
                userId = user.id,
                personType = payload.personType ?: "PF",
                firstName = payload.firstName,
                lastName = payload.lastName,
                phone = payload.phone,
                cpf = payload.cpf,
                cnpj = payload.cnpj,
                companyName = payload.companyName,
                addressZip = payload.addressZip,
                addressStreet = payload.addressStreet,
                addressNumber = payload.addressNumber,
                addressComplement = payload.addressComplement,
                addressNeighborhood = payload.addressNeighborhood,
                addressCity = payload.addressCity,
                addressState = payload.addressState,
                residenceProofFilename = payload.residenceProofFilename,
                passwordHash = passwordEncoder.encode(password),
            )

        stateStore.userProfiles[user.id] = profile
        upsertProfile(profile)
        auditService.logCreate("USER_PROFILE", user.id.toString(), "ADMIN_USER_CREATED")

        return toResponse(user.id)
    }

    fun getProfile(userId: UUID): UserRegistrationResponse = toResponse(userId)

    fun updateUserProfile(
        userId: UUID,
        payload: UserUpdatePayload,
    ): UserRegistrationResponse {
        val current = stateStore.userProfiles[userId] ?: UserProfileRecord(userId = userId)

        payload.personType?.let { current.personType = it }
        payload.firstName?.let { current.firstName = it }
        payload.lastName?.let { current.lastName = it }
        payload.phone?.let { current.phone = it }
        payload.cpf?.let { current.cpf = it }
        payload.cnpj?.let { current.cnpj = it }
        payload.companyName?.let { current.companyName = it }
        payload.addressZip?.let { current.addressZip = it }
        payload.addressStreet?.let { current.addressStreet = it }
        payload.addressNumber?.let { current.addressNumber = it }
        payload.addressComplement?.let { current.addressComplement = it }
        payload.addressNeighborhood?.let { current.addressNeighborhood = it }
        payload.addressCity?.let { current.addressCity = it }
        payload.addressState?.let { current.addressState = it }
        payload.residenceProofFilename?.let { current.residenceProofFilename = it }

        val fullName = composeFullName(current.firstName, current.lastName)
        userService.updateUser(userId = userId, fullName = fullName, email = payload.email)
        current.updatedAt = Instant.now()

        stateStore.userProfiles[userId] = current
        upsertProfile(current)
        auditService.logUpdate("USER_PROFILE", userId.toString(), "USER_PROFILE_UPDATED")

        return toResponse(userId)
    }

    fun updateAddress(
        userId: UUID,
        payload: AddressUpdatePayload,
    ): UserRegistrationResponse {
        val current = stateStore.userProfiles[userId] ?: UserProfileRecord(userId = userId)

        payload.addressZip?.let { current.addressZip = it }
        payload.addressStreet?.let { current.addressStreet = it }
        payload.addressNumber?.let { current.addressNumber = it }
        payload.addressComplement?.let { current.addressComplement = it }
        payload.addressNeighborhood?.let { current.addressNeighborhood = it }
        payload.addressCity?.let { current.addressCity = it }
        payload.addressState?.let { current.addressState = it }
        current.updatedAt = Instant.now()

        stateStore.userProfiles[userId] = current
        upsertProfile(current)
        auditService.logUpdate("USER_PROFILE", userId.toString(), "USER_ADDRESS_UPDATED")

        return toResponse(userId)
    }

    private fun toResponse(userId: UUID): UserRegistrationResponse {
        val user = userService.getUser(userId)
        val profile = stateStore.userProfiles[userId] ?: UserProfileRecord(userId = userId)

        return UserRegistrationResponse(
            id = user.id,
            fullName = user.fullName,
            email = user.email,
            status = user.status.name,
            personType = profile.personType,
            firstName = profile.firstName,
            lastName = profile.lastName,
            phone = profile.phone,
            cpf = profile.cpf,
            cnpj = profile.cnpj,
            companyName = profile.companyName,
            addressZip = profile.addressZip,
            addressStreet = profile.addressStreet,
            addressNumber = profile.addressNumber,
            addressComplement = profile.addressComplement,
            addressNeighborhood = profile.addressNeighborhood,
            addressCity = profile.addressCity,
            addressState = profile.addressState,
            residenceProofFilename = profile.residenceProofFilename,
        )
    }

    private fun fullNameWithFallback(
        firstName: String?,
        lastName: String?,
        emailFallback: String?,
    ): String {
        val composed = composeFullName(firstName, lastName)
        return when {
            !composed.isNullOrBlank() -> composed
            !emailFallback.isNullOrBlank() -> emailFallback.substringBefore('@')
            else -> "User"
        }
    }

    private fun composeFullName(
        firstName: String?,
        lastName: String?,
    ): String? {
        val composed = listOfNotNull(firstName?.trim()?.takeIf { it.isNotBlank() }, lastName?.trim()?.takeIf { it.isNotBlank() }).joinToString(" ").trim()
        return composed.ifBlank { null }
    }

    private fun buildProfile(
        userId: UUID,
        payload: RegisterUserPayload,
        password: String,
    ): UserProfileRecord =
        UserProfileRecord(
            userId = userId,
            personType = payload.personType ?: "PF",
            firstName = payload.firstName,
            lastName = payload.lastName,
            phone = payload.phone,
            cpf = payload.cpf,
            cnpj = payload.cnpj,
            companyName = payload.companyName,
            addressZip = payload.addressZip,
            addressStreet = payload.addressStreet,
            addressNumber = payload.addressNumber,
            addressComplement = payload.addressComplement,
            addressNeighborhood = payload.addressNeighborhood,
            addressCity = payload.addressCity,
            addressState = payload.addressState,
            residenceProofFilename = payload.residenceProofFilename,
            passwordHash = passwordEncoder.encode(password),
        )

    private fun mapRole(roleRaw: String?): Role =
        when (roleRaw?.trim()?.lowercase()) {
            "admin" -> Role.ADMIN
            "leader" -> Role.LEADER
            "tester" -> Role.TESTER
            "guest", "user", null, "" -> Role.GUEST
            else -> Role.GUEST
        }

    private fun upsertProfile(profile: UserProfileRecord) {
        val jdbcTemplate = jdbcTemplateProvider.ifAvailable ?: return
        jdbcTemplate.update(
            """
            INSERT INTO user_profiles (
                user_id,
                person_type,
                first_name,
                last_name,
                phone,
                cpf,
                cnpj,
                company_name,
                address_zip,
                address_street,
                address_number,
                address_complement,
                address_neighborhood,
                address_city,
                address_state,
                residence_proof_filename,
                password_hash,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id) DO UPDATE SET
                person_type = EXCLUDED.person_type,
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                phone = EXCLUDED.phone,
                cpf = EXCLUDED.cpf,
                cnpj = EXCLUDED.cnpj,
                company_name = EXCLUDED.company_name,
                address_zip = EXCLUDED.address_zip,
                address_street = EXCLUDED.address_street,
                address_number = EXCLUDED.address_number,
                address_complement = EXCLUDED.address_complement,
                address_neighborhood = EXCLUDED.address_neighborhood,
                address_city = EXCLUDED.address_city,
                address_state = EXCLUDED.address_state,
                residence_proof_filename = EXCLUDED.residence_proof_filename,
                password_hash = COALESCE(EXCLUDED.password_hash, user_profiles.password_hash),
                updated_at = EXCLUDED.updated_at
            """.trimIndent(),
            profile.userId,
            profile.personType,
            profile.firstName,
            profile.lastName,
            profile.phone,
            profile.cpf,
            profile.cnpj,
            profile.companyName,
            profile.addressZip,
            profile.addressStreet,
            profile.addressNumber,
            profile.addressComplement,
            profile.addressNeighborhood,
            profile.addressCity,
            profile.addressState,
            profile.residenceProofFilename,
            profile.passwordHash,
            Timestamp.from(profile.createdAt),
            Timestamp.from(profile.updatedAt),
        )
    }
}
