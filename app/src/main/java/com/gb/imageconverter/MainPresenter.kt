package com.gb.imageconverter

import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import moxy.MvpPresenter
import java.util.concurrent.TimeUnit

class MainPresenter : MvpPresenter<MainView>() {
    enum class Controls {
        CONVERT_BUTTON,
        PROGRESS_BAR,
        CANCEL_BUTTON,
        SUCCESS_MESSAGE,
        FAILURE_MESSAGE,
    }

    private var fileName: String? = null
    private var disposable: Disposable? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        setVisibleControls()
    }

    override fun onDestroy() {
        stopConverter()
        super.onDestroy()
    }

    fun startPressed() {
        setVisibleControls(
            Controls.CANCEL_BUTTON,
            Controls.PROGRESS_BAR
        )
        viewState.setConversionProgress(0)
        Observable.interval(100, TimeUnit.MILLISECONDS)
            .doOnSubscribe { disposable = it }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { tick -> handleProgress(tick.toInt() * 4) /* approx 2 seconds */ }
            .doOnError { cancelPressed() }
            .subscribe()
    }

    private fun handleProgress(progress: Int) {
        Log.d("==", "progress $progress")
        if (progress <= 100) {
            viewState.setConversionProgress(progress)
        } else {
            stopConverter()
            setVisibleControls(
                Controls.CONVERT_BUTTON,
                Controls.SUCCESS_MESSAGE
            )
        }
    }

    private fun stopConverter() {
        disposable?.dispose()
        disposable = null
    }

    fun cancelPressed() {
        stopConverter()
        viewState.resetFile()
        setVisibleControls(Controls.FAILURE_MESSAGE)
    }

    fun filePicked(file: String) {
        fileName = file
        setVisibleControls(Controls.CONVERT_BUTTON)
    }

    private fun setVisibleControls(vararg controls: Controls) {
        for (v in Controls.values()) {
            viewState.setVisibility(v, v in controls)
        }
    }
}
