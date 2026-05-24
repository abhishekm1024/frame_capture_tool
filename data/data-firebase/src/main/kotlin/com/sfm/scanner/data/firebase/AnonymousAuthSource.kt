package com.sfm.scanner.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.sfm.scanner.core.common.Result
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AnonymousAuthSource @Inject constructor(
    private val auth: FirebaseAuth,
) {
    suspend fun signIn(): Result<String> {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return Result.Success(currentUser.uid)
        }
        return suspendCancellableCoroutine { cont ->
            val task = auth.signInAnonymously()
            task.addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    cont.resume(Result.Success(uid))
                } else {
                    cont.resume(Result.Failure(IllegalStateException("Firebase returned null UID after anonymous sign-in")))
                }
            }
            task.addOnFailureListener { exception ->
                cont.resume(Result.Failure(exception))
            }
        }
    }
}
