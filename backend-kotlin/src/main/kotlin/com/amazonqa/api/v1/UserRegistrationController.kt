package com.amazonqa.api.v1

import com.amazonqa.common.api.ApiEnvelope
import com.amazonqa.common.api.ResponseFactory
import com.amazonqa.identity.AddressUpdatePayload
import com.amazonqa.identity.AdminCreateUserPayload
import com.amazonqa.identity.RegisterUserPayload
import com.amazonqa.identity.UserRegistrationService
import com.amazonqa.identity.UserUpdatePayload
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class UserRegistrationController(
    private val userRegistrationService: UserRegistrationService,
) {
    @PostMapping("/users/register")
    fun register(
        @RequestBody request: RegisterUserRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            userRegistrationService.registerUser(
                RegisterUserPayload(
                    personType = request.personType,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    phone = request.phone,
                    password = request.password,
                    cpf = request.cpf,
                    cnpj = request.cnpj,
                    companyName = request.companyName,
                    addressZip = request.addressZip,
                    addressStreet = request.addressStreet,
                    addressNumber = request.addressNumber,
                    addressComplement = request.addressComplement,
                    addressNeighborhood = request.addressNeighborhood,
                    addressCity = request.addressCity,
                    addressState = request.addressState,
                    residenceProofFilename = request.residenceProofFilename,
                ),
            ),
            servletRequest,
        )

    @PostMapping("/admin/users/full")
    @PreAuthorize("hasRole('ADMIN')")
    fun adminCreateUser(
        @RequestBody request: AdminCreateUserRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            userRegistrationService.adminCreateUser(
                AdminCreateUserPayload(
                    personType = request.personType,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    phone = request.phone,
                    password = request.password,
                    cpf = request.cpf,
                    cnpj = request.cnpj,
                    companyName = request.companyName,
                    addressZip = request.addressZip,
                    addressStreet = request.addressStreet,
                    addressNumber = request.addressNumber,
                    addressComplement = request.addressComplement,
                    addressNeighborhood = request.addressNeighborhood,
                    addressCity = request.addressCity,
                    addressState = request.addressState,
                    residenceProofFilename = request.residenceProofFilename,
                    role = request.role,
                ),
            ),
            servletRequest,
        )

    @GetMapping("/admin/users/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    fun getProfile(
        @PathVariable userId: UUID,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> = ResponseFactory.ok(userRegistrationService.getProfile(userId), servletRequest)

    @PatchMapping("/admin/users/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateProfile(
        @PathVariable userId: UUID,
        @RequestBody request: UserUpdateRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            userRegistrationService.updateUserProfile(
                userId,
                UserUpdatePayload(
                    personType = request.personType,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    phone = request.phone,
                    cpf = request.cpf,
                    cnpj = request.cnpj,
                    companyName = request.companyName,
                    addressZip = request.addressZip,
                    addressStreet = request.addressStreet,
                    addressNumber = request.addressNumber,
                    addressComplement = request.addressComplement,
                    addressNeighborhood = request.addressNeighborhood,
                    addressCity = request.addressCity,
                    addressState = request.addressState,
                    residenceProofFilename = request.residenceProofFilename,
                ),
            ),
            servletRequest,
        )

    @PatchMapping("/admin/users/{userId}/address")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateAddress(
        @PathVariable userId: UUID,
        @RequestBody request: AddressUpdateRequest,
        servletRequest: HttpServletRequest,
    ): ApiEnvelope<Any> =
        ResponseFactory.ok(
            userRegistrationService.updateAddress(
                userId,
                AddressUpdatePayload(
                    addressZip = request.addressZip,
                    addressStreet = request.addressStreet,
                    addressNumber = request.addressNumber,
                    addressComplement = request.addressComplement,
                    addressNeighborhood = request.addressNeighborhood,
                    addressCity = request.addressCity,
                    addressState = request.addressState,
                ),
            ),
            servletRequest,
        )
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RegisterUserRequest(
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

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AdminCreateUserRequest(
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

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UserUpdateRequest(
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

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AddressUpdateRequest(
    val addressZip: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val addressComplement: String? = null,
    val addressNeighborhood: String? = null,
    val addressCity: String? = null,
    val addressState: String? = null,
)
