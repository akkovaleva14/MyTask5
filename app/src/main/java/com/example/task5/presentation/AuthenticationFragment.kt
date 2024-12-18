package com.example.task5.presentation

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.task5.R
import com.example.task5.data.AppDatabase
import com.example.task5.databinding.FragmentAuthenticationBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import java.util.concurrent.TimeUnit

class AuthenticationFragment : Fragment() {
    private var _binding: FragmentAuthenticationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    private val TAG = "AuthenticationFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Initializing view binding")
        _binding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated: Initializing FirebaseAuth instance")
        auth = FirebaseAuth.getInstance()

        binding.sendCodeButton.setOnClickListener {
            val phoneNumber = binding.phoneNumberEditText.text.toString()
            Log.d(TAG, "onSendCodeButtonClick: Phone number entered - $phoneNumber")
            if (TextUtils.isEmpty(phoneNumber)) {
                Toast.makeText(requireContext(), "Введите номер телефона", Toast.LENGTH_SHORT)
                    .show()
                Log.e(TAG, "onSendCodeButtonClick: Phone number is empty")
            } else {
                sendVerificationCode(phoneNumber)
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        Log.d(TAG, "sendVerificationCode: Sending verification code to $phoneNumber")

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted: Verification completed successfully")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "onVerificationFailed: Verification failed - ${e.message}")
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "onCodeSent: Verification code sent successfully")
                    Log.d(TAG, "onCodeSent: Verification ID - $verificationId")
                    val bundle = Bundle().apply {
                        putString("verificationId", verificationId)
                    }
                    try {
                        findNavController().navigate(
                            R.id.action_authenticationFragment_to_verificationFragment,
                            bundle
                        )
                        Log.d(TAG, "onCodeSent: Navigation to VerificationFragment successful")
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "onCodeSent: Navigation error - ${e.message}")
                        Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
        Log.d(TAG, "sendVerificationCode: PhoneAuthProvider.verifyPhoneNumber called")
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "signInWithPhoneAuthCredential: Signing in with phone credential")

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = task.result?.user
                    Log.d(TAG, "signInWithPhoneAuthCredential: Authentication successful for user - ${user?.phoneNumber}")
                } else {
                    Log.e(TAG, "signInWithPhoneAuthCredential: Authentication failed - ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Ошибка аутентификации", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: Cleaning up view binding")
        super.onDestroyView()
        _binding = null
    }
}
