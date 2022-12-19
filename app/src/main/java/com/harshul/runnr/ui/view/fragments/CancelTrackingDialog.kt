package com.harshul.runnr.ui.view.fragments

import android.app.ActionBar
import android.app.Dialog
import android.os.Bundle
import android.view.Display
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.harshul.runnr.databinding.DialogCancelRunBinding

class CancelTrackingDialog : DialogFragment() {

    private var yesListener: (() -> Unit)? = null

    fun setYesListener(listener: () -> Unit) {
        yesListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialogCancelRun = Dialog(requireContext())
        dialogCancelRun.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogCancelRunBinding.inflate(layoutInflater)
        dialogCancelRun.setContentView(dialogBinding.root)

        dialogBinding.buttonYes.setOnClickListener {
            yesListener?.let { yes ->
                yes()
            }
            dialogCancelRun.dismiss()
        }

        dialogBinding.buttonNo.setOnClickListener { dialogCancelRun.cancel() }

        dialogCancelRun.show()
        val display: Display = requireActivity().windowManager.defaultDisplay
        val width: Int = display.width - 100
        val window: Window? = dialogCancelRun.window
        window?.setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        return dialogCancelRun
    }

}