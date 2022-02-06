package com.gb.imageconverter

import android.os.Bundle
import android.view.View
import com.gb.imageconverter.databinding.MainActivityBinding
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter

class MainActivity : MvpAppCompatActivity(), MainView {

    private lateinit var binding: MainActivityBinding
    private val presenter by moxyPresenter { MainPresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fileName.setOnClickListener { pickFile() }
        binding.startButton.setOnClickListener { presenter.startPressed() }
        binding.cancelButton.setOnClickListener { presenter.cancelPressed() }
    }

    override fun setVisibility(control: MainPresenter.Controls, visible: Boolean) {
        val view = when (control) {
            MainPresenter.Controls.CONVERT_BUTTON -> binding.startButton
            MainPresenter.Controls.PROGRESS_BAR -> binding.progress
            MainPresenter.Controls.CANCEL_BUTTON -> binding.cancelButton
            MainPresenter.Controls.SUCCESS_MESSAGE -> binding.success
            MainPresenter.Controls.FAILURE_MESSAGE -> binding.failure
        }
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setConversionProgress(percentage: Int) {
        binding.progress.progress = percentage
    }

    override fun resetFile() {
        binding.fileName.text = ""
    }

    private fun pickFile() {
        val file = "file:///1.jpg"
        binding.fileName.text = file
        presenter.filePicked(file)
    }
}