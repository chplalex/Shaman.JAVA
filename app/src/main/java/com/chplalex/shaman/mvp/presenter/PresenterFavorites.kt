package com.chplalex.shaman.mvp.presenter

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.chplalex.shaman.mvp.model.db.Location
import com.chplalex.shaman.R
import com.chplalex.shaman.utils.*
import com.chplalex.shaman.mvp.presenter.list.IPresenterListFavorites
import com.chplalex.shaman.mvp.view.IViewFavorites
import com.chplalex.shaman.mvp.view.list.IItemViewFavorite
import com.chplalex.shaman.service.api.APP_ID
import com.chplalex.shaman.service.api.OpenWeatherRetrofit
import com.chplalex.shaman.service.db.ShamanDao
import com.chplalex.shaman.ui.App.Companion.instance
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import moxy.MvpPresenter
import javax.inject.Inject
import javax.inject.Named

class PresenterFavorites() : MvpPresenter<IViewFavorites>() {

    @Inject lateinit var retrofit : OpenWeatherRetrofit
    @Inject lateinit var dao : ShamanDao
    @Inject @Named("UI") lateinit var uiScheduler : Scheduler
    @Inject @Named("IO") lateinit var ioScheduler : Scheduler
    @Inject lateinit var sp : SharedPreferences
    @Inject lateinit var disposable : CompositeDisposable
    @Inject lateinit var navController: NavController

    init {
        instance.activityComponent?.inject(this)
    }

    val presenterList = PresenterListFavorites()

    private val locations = mutableListOf<Location>()
    private val lang = sp.getString("pref_lang", "RU")
    private val units = sp.getString("pref_units", "metric")

    inner class PresenterListFavorites() : IPresenterListFavorites {

        override fun bindView(view: IItemViewFavorite) {
            with(locations[view.pos]) {

                val onViewClick = fun(v: View) {
                    Bundle().also {
                        it.putCharSequence(LOCATION_ARG_NAME, name)
                        it.putCharSequence(LOCATION_ARG_COUNTRY, country)
                        Navigation.findNavController(v).navigate(R.id.actionStart, it)
                    }
                }

                val onDeleteButtonClick = fun(_: View) {
                    favorite = false
                    disposable.add(
                        dao.updateLocation(this)
                            .subscribeOn(ioScheduler)
                            .observeOn(uiScheduler)
                            .subscribe({
                                locations.remove(this)
                                viewState.notifyItemRemoved(view.pos)
                            }, {
                                favorite = true
                                viewState.showErrorDB(it)
                            })
                    )

                }

                view.setListenerOnDeleteButton(onDeleteButtonClick)
                view.setListenerOnView(onViewClick)
                view.setName(name)
                view.setCountry(country)
                disposable.add(
                    retrofit.loadWeather(this.fullName(), APP_ID, lang, units)
                        .subscribeOn(ioScheduler)
                        .observeOn(uiScheduler)
                        .subscribe({
                            view.setTemp(it.tempString)
                            view.setIcon(it.imageResource)
                        }, {
                            view.setNoTemp()
                            viewState.showErrorRetrofit(it)
                        })
                )
            }
        }

        override fun getCount() = locations.size
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        loadData()
    }

    private fun loadData() {
        disposable.add(
            dao.favoriteLocations
                .observeOn(uiScheduler)
                .subscribeOn(ioScheduler)
                .subscribe({
                    locations.clear()
                    locations.addAll(it)
                    viewState.notifyDataSetChanged()
                }, {
                    viewState.showErrorDB(it)
                })
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

    fun onActionStart() {
        navController.navigate(R.id.actionStart, null)
    }

    fun Location.fullName(): String {
        if (name.isEmpty()) return ""
        val result = StringBuilder(name)
        if (country.isNotEmpty()) result.append(",$country")
        return result.toString()
    }
}