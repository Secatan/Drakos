package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.SaleEntity
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncSale_authenticatedOwner_createsReadsAndVoidsSale() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore, auth)
        val docId = "sale_${UUID.randomUUID()}"

        val sale = SaleEntity(
            id = 42L,
            branchId = 1L,
            cashSessionId = 5L,
            userId = 3L,
            receiptNumber = "TK-001-042",
            subtotal = 3600.0,
            discountAmount = 0.0,
            vatAmount = 624.79,
            totalAmount = 3600.0,
            paymentMethod = PaymentMethod.CASH,
            status = "COMPLETED",
            idempotencyKey = UUID.randomUUID().toString()
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncSale(sale, "Sucursal Centro", "Lucía", customDocId = docId)
        }
        assertTrue(createResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCloudSaleById(aliceUid, docId).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals("TK-001-042", fetched?.receiptNumber)
        assertEquals("COMPLETED", fetched?.status)

        val voidResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncSale(sale.copy(status = "VOIDED"), "Sucursal Centro", "Lucía", customDocId = docId)
        }
        assertTrue(voidResult.isSuccess)

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCloudSaleById(aliceUid, docId).getOrThrow()
        }
        assertEquals("VOIDED", updated?.status)
    }

    @Test
    fun observeCloudSales_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore, auth)
        val docId = "sale_${UUID.randomUUID()}"

        val sale = SaleEntity(
            id = 77L,
            branchId = 2L,
            cashSessionId = 9L,
            userId = 4L,
            receiptNumber = "TK-002-077",
            subtotal = 1800.0,
            totalAmount = 1800.0,
            paymentMethod = PaymentMethod.MP_QR,
            status = "COMPLETED",
            idempotencyKey = UUID.randomUUID().toString()
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncSale(sale, "Sucursal Palermo", "Carlos", customDocId = docId).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCloudSales(aliceUid).first { list -> list.any { it.docId == docId } }
        }
        assertTrue(emitted.any { it.docId == docId && it.receiptNumber == "TK-002-077" })
    }

    @Test
    fun getCloudSaleById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore, auth)
        val docId = "sale_${UUID.randomUUID()}"

        val sale = SaleEntity(
            id = 88L,
            branchId = 1L,
            cashSessionId = 3L,
            userId = 1L,
            receiptNumber = "TK-001-088",
            subtotal = 2400.0,
            totalAmount = 2400.0,
            paymentMethod = PaymentMethod.DEBIT,
            status = "COMPLETED",
            idempotencyKey = UUID.randomUUID().toString()
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.syncSale(sale, "Sucursal Centro", "Roberto", customDocId = docId).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore, auth)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getCloudSaleById(aliceUid, docId).getOrThrow()
            }
            fail("Expected PERMISSION_DENIED for cross-user sale read")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun observeCloudSales_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CloudSyncRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCloudSales("alice_unauth_target").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated listener")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun syncExpenseAndCompanySettings_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore, auth)

        val expDocId = "exp_${UUID.randomUUID()}"
        val expenseRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncExpense(
                ExpenseEntity(
                    id = 99L,
                    branchId = 1L,
                    category = ExpenseCategory.SERVICIOS,
                    amount = 14500.0,
                    description = "Pago internet fibra",
                    paymentSource = "BANK",
                    createdByUserId = 1L
                ),
                customDocId = expDocId
            )
        }
        assertTrue(expenseRes.isSuccess)

        val settingsRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncCompanySettings(
                CompanySettingsEntity(
                    businessName = "KioscoPOS Test S.A.",
                    cuit = "30-71234567-8",
                    taxCondition = "IVA Responsable Inscripto",
                    defaultVatRate = 21.0,
                    cashDropAlertThreshold = 50000.0,
                    cashAuditTolerance = 450.0
                )
            )
        }
        assertTrue(settingsRes.isSuccess)

        val cloudCfg = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCloudCompanySettings(aliceUid).first { it != null }
        }
        assertEquals("KioscoPOS Test S.A.", cloudCfg?.businessName)
        assertEquals(450.0, cloudCfg?.cashAuditTolerance ?: 0.0, 0.01)
    }

    @Test
    fun syncStaffProfileAndDevices_authenticatedOwner_roundTripsSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore, auth)

        val staff = com.example.data.entity.UserProfileEntity(
            id = 50L,
            username = "marta.k03",
            fullName = "Marta Kiosco 3",
            pinCode = com.example.domain.security.PinHasher.hash("marta.k03", "6789"),
            role = com.example.domain.model.UserRole.EMPLOYEE,
            assignedBranchId = 3L,
            isActive = true
        )
        val device = com.example.data.entity.AuthorizedDeviceEntity(
            id = 10L,
            deviceId = "INVITE-DRK-7711",
            deviceName = "Moto G54 Sucursal 3",
            assignedBranchId = 3L,
            status = "PENDING",
            pairingCode = "DRK-7711",
            approvedByUserName = "Roberto Dueño"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            val (uSynced, dSynced) = repository.syncAllStaffAndDevices(listOf(staff), listOf(device)).getOrThrow()
            assertEquals(1, uSynced)
            assertEquals(1, dSynced)
        }

        val (cloudStaff, cloudDevices) = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchCloudStaffAndDevicesOnce().getOrThrow()
        }
        assertTrue(cloudStaff.any { it.username == "marta.k03" && it.assignedBranchId == 3L && it.isActive })
        assertTrue(cloudDevices.any { it.deviceId == "INVITE-DRK-7711" && it.pairingCode == "DRK-7711" })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_kiosco@test.com"
        const val BOB_EMAIL = "bob_kiosco@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
