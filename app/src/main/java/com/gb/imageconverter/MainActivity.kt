package com.gb.imageconverter

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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

        binding.inputFile.setOnClickListener { runInputFilePicker() }
        binding.outputFile.setOnClickListener { runOutputFilePicker() }
        binding.startButton.setOnClickListener { presenter.startPressed() }
        binding.cancelButton.setOnClickListener { presenter.cancelPressed() }
        activityContentResolver = contentResolver
    }

    override fun onDestroy() {
        super.onDestroy()
        activityContentResolver = null
    }

    override fun setVisibility(control: MainPresenter.Controls, visible: Boolean) {
        val view = when (control) {
            MainPresenter.Controls.OUTPUT_FILE -> binding.outputFile
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
        binding.inputFile.text = ""
    }

    private val pickInputFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                binding.inputFile.text = uri.toString()
                presenter.inputFilePicked(uri)
            }
        }

    private fun runInputFilePicker() {
        pickInputFileLauncher.launch(arrayOf("image/*"))
    }

    private val pickOutputFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
            binding.outputFile.text = uri.toString()
            presenter.outputFilePicked(uri)
        }

    private fun runOutputFilePicker() {
        pickOutputFileLauncher.launch("out.png")
    }

    // хак для доступа к contentResolver из Presenter
    companion object {
        var activityContentResolver: ContentResolver? = null
    }
}