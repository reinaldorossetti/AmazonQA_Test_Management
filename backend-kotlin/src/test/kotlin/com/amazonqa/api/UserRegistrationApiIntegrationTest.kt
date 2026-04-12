package com.amazonqa.api

import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class UserRegistrationApiIntegrationTest : ApiIntegrationTestBase() {
    @Test
    fun `public user registration creates user profile`() {
        val email = "register-${System.nanoTime()}@example.com"

        val payload =
            mapOf(
                "person_type" to "PF",
                "first_name" to "Alice",
                "last_name" to "Silva",
                "email" to email,
                "phone" to "+55 11 99999-0000",
                "password" to "Secret@123",
                "cpf" to "11122233344",
                "address_zip" to "01000-000",
                "address_street" to "Rua Augusta",
                "address_number" to "123",
                "address_complement" to "Apto 12",
                "address_neighborhood" to "Centro",
                "address_city" to "Sao Paulo",
                "address_state" to "SP",
                "residence_proof_filename" to "residence-proof-pf.pdf",
            )

        val userId =
            Given {
                contentType(ContentType.JSON)
                accept(ContentType.JSON)
                body(payload)
            }.`when`()
                .post("/api/v1/users/register")
                .then()
                .statusCode(200)
                .body("data.email", equalTo(email))
                .body("data.firstName", equalTo("Alice"))
                .body("data.lastName", equalTo("Silva"))
                .extractId()

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId/profile")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))
            .body("data.personType", equalTo("PF"))
            .body("data.firstName", equalTo("Alice"))
            .body("data.lastName", equalTo("Silva"))
            .body("data.email", equalTo(email))
            .body("data.phone", equalTo("+55 11 99999-0000"))
            .body("data.cpf", equalTo("11122233344"))
            .body("data.addressZip", equalTo("01000-000"))
            .body("data.addressStreet", equalTo("Rua Augusta"))
            .body("data.addressNumber", equalTo("123"))
            .body("data.addressComplement", equalTo("Apto 12"))
            .body("data.addressNeighborhood", equalTo("Centro"))
            .body("data.addressCity", equalTo("Sao Paulo"))
            .body("data.addressState", equalTo("SP"))
            .body("data.residenceProofFilename", equalTo("residence-proof-pf.pdf"))
    }

    @Test
    fun `admin can create and update full user profile`() {
        val email = "admin-create-${System.nanoTime()}@example.com"

        val userId =
            givenAdmin()
                .body(
                    mapOf(
                        "person_type" to "PJ",
                        "first_name" to "QA",
                        "last_name" to "Company",
                        "email" to email,
                        "phone" to "+55 21 98888-0000",
                        "password" to "Admin@123",
                        "cnpj" to "12345678000199",
                        "company_name" to "QA Factory",
                        "address_zip" to "20000-100",
                        "address_street" to "Av Atlantica",
                        "address_number" to "500",
                        "address_complement" to "Sala 2",
                        "address_neighborhood" to "Copacabana",
                        "address_city" to "Rio de Janeiro",
                        "address_state" to "RJ",
                        "residence_proof_filename" to "residence-proof-pj.pdf",
                        "role" to "tester",
                    ),
                ).`when`()
                .post("/api/v1/admin/users/full")
                .then()
                .statusCode(200)
                .body("data.email", equalTo(email))
                .body("data.personType", equalTo("PJ"))
                .extractId()

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId/profile")
            .then()
            .statusCode(200)
            .body("data.id", equalTo(userId))
            .body("data.personType", equalTo("PJ"))
            .body("data.firstName", equalTo("QA"))
            .body("data.lastName", equalTo("Company"))
            .body("data.email", equalTo(email))
            .body("data.phone", equalTo("+55 21 98888-0000"))
            .body("data.cnpj", equalTo("12345678000199"))
            .body("data.companyName", equalTo("QA Factory"))
            .body("data.addressZip", equalTo("20000-100"))
            .body("data.addressStreet", equalTo("Av Atlantica"))
            .body("data.addressNumber", equalTo("500"))
            .body("data.addressComplement", equalTo("Sala 2"))
            .body("data.addressNeighborhood", equalTo("Copacabana"))
            .body("data.addressCity", equalTo("Rio de Janeiro"))
            .body("data.addressState", equalTo("RJ"))
            .body("data.residenceProofFilename", equalTo("residence-proof-pj.pdf"))

        givenAdmin()
            .body(
                mapOf(
                    "address_zip" to "01310-100",
                    "address_street" to "Av Paulista",
                    "address_number" to "1000",
                    "address_complement" to "Conjunto 10",
                    "address_neighborhood" to "Bela Vista",
                    "address_city" to "Sao Paulo",
                    "address_state" to "SP",
                ),
            ).`when`()
            .patch("/api/v1/admin/users/$userId/address")
            .then()
            .statusCode(200)
            .body("data.personType", equalTo("PJ"))
            .body("data.firstName", equalTo("QA"))
            .body("data.lastName", equalTo("Company"))
            .body("data.email", equalTo(email))
            .body("data.companyName", equalTo("QA Factory"))
            .body("data.addressZip", equalTo("01310-100"))
            .body("data.addressStreet", equalTo("Av Paulista"))
            .body("data.addressNumber", equalTo("1000"))
            .body("data.addressComplement", equalTo("Conjunto 10"))
            .body("data.addressNeighborhood", equalTo("Bela Vista"))
            .body("data.addressCity", equalTo("Sao Paulo"))
            .body("data.addressState", equalTo("SP"))

        givenAdmin()
            .`when`()
            .get("/api/v1/admin/users/$userId/profile")
            .then()
            .statusCode(200)
            .body("data.personType", equalTo("PJ"))
            .body("data.firstName", equalTo("QA"))
            .body("data.lastName", equalTo("Company"))
            .body("data.email", equalTo(email))
            .body("data.phone", equalTo("+55 21 98888-0000"))
            .body("data.cnpj", equalTo("12345678000199"))
            .body("data.companyName", equalTo("QA Factory"))
            .body("data.addressZip", equalTo("01310-100"))
            .body("data.addressStreet", equalTo("Av Paulista"))
            .body("data.addressNumber", equalTo("1000"))
            .body("data.addressComplement", equalTo("Conjunto 10"))
            .body("data.addressNeighborhood", equalTo("Bela Vista"))
            .body("data.addressCity", equalTo("Sao Paulo"))
            .body("data.addressState", equalTo("SP"))
            .body("data.residenceProofFilename", equalTo("residence-proof-pj.pdf"))
    }
}
