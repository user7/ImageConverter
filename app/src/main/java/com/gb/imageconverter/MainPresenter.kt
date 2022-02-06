package com.gb.imageconverter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import moxy.MvpPresenter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalStateException

class MainPresenter : MvpPresenter<MainView>() {
    enum class Controls {
        OUTPUT_FILE,
        CONVERT_BUTTON,
        PROGRESS_BAR,
        CANCEL_BUTTON,
        SUCCESS_MESSAGE,
        FAILURE_MESSAGE,
    }

    private var inputUri: Uri? = null
    private var outputUri: Uri? = null
    private var disposable: Disposable? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        setVisibleControls()
    }

    override fun onDestroy() {
        stopConverter()
        super.onDestroy()
    }

    fun cancelPressed() = handleConversionFailure()

    fun inputFilePicked(uri: Uri) {
        Log.d("==", "input file picked: $uri")
        inputUri = uri
        setVisibleControls(Controls.OUTPUT_FILE)
    }

    fun outputFilePicked(uri: Uri?) {
        Log.d("==", "output file picked: $uri")
        uri ?: return handleConversionFailure()
        outputUri = uri
        setVisibleControls(Controls.CONVERT_BUTTON, Controls.OUTPUT_FILE)
    }

    data class ConversionContext(
        val input: InputStream,
        val inputSize: Long,
        val output: OutputStream
    )

    fun startPressed() {
        setVisibleControls(
            Controls.CANCEL_BUTTON,
            Controls.OUTPUT_FILE,
            Controls.PROGRESS_BAR,
        )
        viewState.setConversionProgress(0)
        Observable
            .create<ConversionContext> { emitter -> openStreams(emitter) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { disposable = it }
            .observeOn(Schedulers.io())
            .flatMap { context -> performConversion(context) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { progress -> viewState.setConversionProgress(progress) },
                { error -> handleConversionFailure() },
                { handleConversionSuccess() }
            )
    }

    private fun handleConversionFailure(error: String? = null) {
        stopConverter()
        viewState.resetFile()
        setVisibleControls(Controls.FAILURE_MESSAGE)
    }

    private fun setVisibleControls(vararg controls: Controls) {
        for (v in Controls.values()) {
            viewState.setVisibility(v, v in controls)
        }
    }

    private fun openStreams(emitter: ObservableEmitter<ConversionContext>) {
        // MainPresenter должен открывать файлы,
        // открытие файлов идёт через contentResolver.openInputStream(uri),
        // а contentResolver живёт в MainActivity.
        // В итоге Presenter зависит от Activity, не знаю, как правильно избавиться.
        val resolver = MainActivity.activityContentResolver
        if (resolver == null) {
            emitter.onError(IllegalStateException("activityContentResolver unavailable"))
            return
        }

        val inputUriTmp = inputUri
        val outputUriTmp = outputUri
        if (inputUriTmp == null || outputUriTmp == null) {
            emitter.onError(IllegalStateException("URIs are unset"))
            return
        }

        val input = resolver.openInputStream(inputUriTmp)
        val output = resolver.openOutputStream(outputUriTmp)
        if (input == null || output == null) {
            emitter.onError(IOException("failed to open files input=$input output=$output"))
            return
        }

        val inputSize = resolver.openAssetFileDescriptor(inputUriTmp, "r")?.length
        if (inputSize == null) {
            emitter.onError(IOException("failed to retrieve file size"))
            return
        }

        emitter.onNext(ConversionContext(input, inputSize, output))
        emitter.onComplete()
    }

    private fun performConversion(context: ConversionContext): Observable<Int> {
        val step = 1000
        return Observable.create { emitter ->
            val buffer = ByteArrayOutputStream()
            val bytes = ByteArray(step)
            var total = 0
            while (context.input.available() > 0) {
                val n = context.input.read(bytes)
                if (n == 0)
                    break
                total += n
                buffer.write(bytes, 0, n)
                Thread.sleep(100) // фиктивная задержка
                emitter.onNext((total * 100 / context.inputSize).toInt())
            }
            val bitmap = BitmapFactory.decodeByteArray(buffer.toByteArray(), 0, total)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, context.output)
            context.input.close()
            context.output.close()
            emitter.onComplete()
        }
    }

    private fun handleConversionSuccess() {
        stopConverter()
        setVisibleControls(
            Controls.CONVERT_BUTTON,
            Controls.OUTPUT_FILE,
            Controls.SUCCESS_MESSAGE,
        )
    }

    private fun stopConverter() {
        disposable?.dispose()
        disposable = null
    }
}