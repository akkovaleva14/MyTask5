package com.example.task5.presentation

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.task5.R
import com.example.task5.databinding.FragmentVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider

class VerificationFragment : Fragment() {
    private var _binding: FragmentVerificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String // Объявляем переменную

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Получаем аргумент из Bundle
        verificationId = arguments?.getString("verificationId") ?: ""

        binding.verifyButton.setOnClickListener {
            val code = binding.verificationCodeEditText.text.toString()
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(requireContext(), "Введите код подтверждения", Toast.LENGTH_SHORT).show()
            } else {
                verifyCode(code)
            }
        }
    }

    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Успешная аутентификация
                    navigateToNextFragment()
                } else {
                    Toast.makeText(requireContext(), "Ошибка аутентификации", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToNextFragment() {
        // Логика навигации к следующему фрагменту
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, StationsListFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}