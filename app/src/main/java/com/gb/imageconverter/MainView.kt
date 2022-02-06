package com.gb.imageconverter

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface MainView : MvpView {
    fun setVisibility(control: MainPresenter.Controls, visible: Boolean)
    fun setConversionProgress(percentage: Int)
    fun resetFile()
}
