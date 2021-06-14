package com.machina.siband.admin.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.machina.siband.databinding.FragmentAdminListLantaiBinding

/**
 * A simple [Fragment] subclass.
 * Use the [AdminListLantaiFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AdminListLantaiFragment : Fragment() {

    private var _binding : FragmentAdminListLantaiBinding? = null
    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdminListLantaiBinding.inflate(inflater)


        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AdminListLantai.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AdminListLantaiFragment().apply {

            }
    }
}