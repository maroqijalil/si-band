package com.machina.siband.user.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.machina.siband.R
import com.machina.siband.databinding.FragmentUserHomeBinding
import com.machina.siband.module.GlideApp
import com.machina.siband.repository.FirebaseStorageRepo
import com.machina.siband.user.recycler.ListRuanganAdapter
import com.machina.siband.user.viewModel.UserHomeViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [UserHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserHomeFragment : Fragment(), AdapterView.OnItemSelectedListener {

  private var _binding: FragmentUserHomeBinding? = null
  private val binding get() = _binding!!

  private val viewModel: UserHomeViewModel by activityViewModels()
  private lateinit var mAdapter: ListRuanganAdapter
  private lateinit var spinner: Spinner

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    _binding = FragmentUserHomeBinding.inflate(layoutInflater, container, false)

    spinner = binding.fragmentUserHomeSpinner
    spinner.onItemSelectedListener = this

    setupRecycler()
    setupObserver()

    return binding.root
  }

  private fun setupRecycler() {
    val mLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    mAdapter = ListRuanganAdapter(this::onItemRuanganClicked)

    binding.fragmentUserHomeRecycler.apply {
      adapter = mAdapter
      layoutManager = mLayoutManager
    }
  }

  private fun setupObserver() {
    // Listening to selectedLantai, update adapter dataset when selectedLantai changed
    viewModel.selectedLantai.observe(viewLifecycleOwner, {
      viewModel.updateLantaiListOnHome(it)
      val storageRef = FirebaseStorageRepo.getMapImageRef(it.nama)
      GlideApp.with(requireContext())
        .load(storageRef)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .into(binding.fragmentUserHomeMap)
    })

    viewModel.listRuangan.observe(viewLifecycleOwner, {
      mAdapter.setData(it)
    })

    // Listening to arrayLantai, update Data for spinner on change
    viewModel.arrayListLantai.observe(viewLifecycleOwner, { arrayListLantai ->
      val mSpinnerAdapter = ArrayAdapter(
        requireContext(),
        R.layout.support_simple_spinner_dropdown_item,
        arrayListLantai
      )
      spinner.apply {
        adapter = mSpinnerAdapter
        setSelection(viewModel.selectedPosition)
      }
    })
  }

  private fun onItemRuanganClicked(name: String) {
    val id = viewModel.selectedLantai.value?.nama
    if (!id.isNullOrEmpty()) {
      val action =
        UserHomeFragmentDirections.actionUserHomeFragmentToDetailRuanganFragment(id, name)
      findNavController().navigate(action)
    }
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    val newLantai = viewModel.listLantai.value?.get(position)
    if (newLantai != null) {
      viewModel.setSelectedLantai(newLantai, position)
    } else {
      Toast.makeText(context, "An error occured please try again", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {

  }

  companion object {
    private val TAG = "UserHomeFragment"

  }

}