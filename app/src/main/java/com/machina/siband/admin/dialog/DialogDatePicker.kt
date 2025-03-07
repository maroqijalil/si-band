package com.machina.siband.admin.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

class DialogDatePicker(private val onDialogDateSet: (Calendar) -> Unit) : DialogFragment(),
  DatePickerDialog.OnDateSetListener {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    // Use the current date as the default date in the picker
    val c = Calendar.getInstance()
    val year = c.get(Calendar.YEAR)
    val month = c.get(Calendar.MONTH)
    val day = c.get(Calendar.DAY_OF_MONTH)

    return DatePickerDialog(requireContext(), this, year, month, day)
  }

  override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
    // Do something with the date chosen by the user
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day)
    dismiss()
    onDialogDateSet(calendar)
  }
}