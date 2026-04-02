package xyz.dnieln7.galleryex.testutil

import io.mockk.MockKVerificationScope
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify

inline fun <reified T : Any> relaxedMockk(): T = mockk(relaxed = true)

fun verifyOnce(
    verifyBlock: MockKVerificationScope.() -> Unit,
) = verify(
    exactly = 1,
    verifyBlock = verifyBlock
)

fun coVerifyOnce(
    verifyBlock: suspend MockKVerificationScope.() -> Unit,
) = coVerify(
    exactly = 1,
    verifyBlock = verifyBlock
)

fun verifyNever(
    verifyBlock: MockKVerificationScope.() -> Unit,
) = coVerify(
    exactly = 0,
    verifyBlock = verifyBlock
)

fun coVerifyNever(
    verifyBlock: suspend MockKVerificationScope.() -> Unit,
) = coVerify(
    exactly = 0,
    verifyBlock = verifyBlock
)
