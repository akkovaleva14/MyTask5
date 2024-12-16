package com.example.task5

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

    // Logging tag
    private val TAG = "AuthenticationFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.sendCodeButton.setOnClickListener {
            val phoneNumber = binding.phoneNumberEditText.text.toString()
            if (TextUtils.isEmpty(phoneNumber)) {
                Toast.makeText(requireContext(), "Введите номер телефона", Toast.LENGTH_SHORT)
                    .show()
                Log.e(TAG, "Phone number is empty")
            } else {
                sendVerificationCode(phoneNumber)
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        Log.d(TAG, "Sending verification code to $phoneNumber")

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "Verification completed successfully")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Verification failed: ${e.message}")
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "Code sent: $verificationId")
                    val bundle = Bundle().apply {
                        putString("verificationId", verificationId)
                    }
                    try {
                        findNavController().navigate(
                            R.id.action_authenticationFragment_to_verificationFragment,
                            bundle
                        )
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Navigation error: ${e.message}")
                        Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "Signing in with phone auth credential")

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = task.result?.user
                    Log.d(TAG, "Authentication successful for user: ${user?.phoneNumber}")
                    navigateToNextFragment()
                } else {
                    Log.e(TAG, "Authentication failed: ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Ошибка аутентификации", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun navigateToNextFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, StationsListFragment())
            .commit()
        Log.d(TAG, "Navigating to next fragment")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
