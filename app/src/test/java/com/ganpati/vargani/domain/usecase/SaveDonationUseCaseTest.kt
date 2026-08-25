package com.ganpati.vargani.domain.usecase

import com.ganpati.vargani.domain.model.Donation
import com.ganpati.vargani.domain.model.PaymentMode
import com.ganpati.vargani.domain.repository.DonationRepository
import com.ganpati.vargani.domain.usecase.donation.SaveDonationUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SaveDonationUseCaseTest {

    private lateinit var repository: DonationRepository
    private lateinit var useCase: SaveDonationUseCase

    private val validDonation = Donation(
        id = 0L,
        receiptNo = "GV-0001",
        name = "Test Donor",
        mobile = "9876543210",
        amount = 500.0,
        paymentMode = PaymentMode.CASH,
        collector = "Collector A",
        dateEpochMillis = 1_700_000_000_000L,
        timeEpochMillis = 1_700_000_000_000L,
    )

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = SaveDonationUseCase(repository)
    }

    @Test
    fun invoke_blankName_throws() = runTest {
        val donation = validDonation.copy(name = "  ")

        val result = runCatching { useCase(donation) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Name required")
        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun invoke_zeroAmount_throws() = runTest {
        val donation = validDonation.copy(amount = 0.0)

        val result = runCatching { useCase(donation) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Amount must be positive")
    }

    @Test
    fun invoke_negativeAmount_throws() = runTest {
        val donation = validDonation.copy(amount = -100.0)

        val result = runCatching { useCase(donation) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Amount must be positive")
    }

    @Test
    fun invoke_duplicateReceipt_throws() = runTest {
        coEvery { repository.isReceiptUnique("GV-0001", 0L) } returns false

        val result = runCatching { useCase(validDonation) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Receipt not unique")
        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun invoke_validNewDonation_insertsAndReturnsId() = runTest {
        coEvery { repository.isReceiptUnique("GV-0001", 0L) } returns true
        coEvery { repository.insert(any()) } returns 42L

        val id = useCase(validDonation)

        assertThat(id).isEqualTo(42L)
        coVerify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun invoke_validExistingDonation_updatesAndReturnsId() = runTest {
        val existing = validDonation.copy(id = 7L)
        coEvery { repository.isReceiptUnique("GV-0001", 7L) } returns true
        coEvery { repository.update(any()) } returns Unit

        val id = useCase(existing)

        assertThat(id).isEqualTo(7L)
        coVerify(exactly = 1) { repository.update(existing) }
        coVerify(exactly = 0) { repository.insert(any()) }
    }
}
