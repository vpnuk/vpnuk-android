/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import uk.vpn.vpnuk.data.repository.LocalRepository
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var localRepository: LocalRepository

    private val destroySubscription = CompositeDisposable()

    fun Disposable.addToDestroySubscriptions() {
        destroySubscription.add(this)
    }

    override fun onDestroy() {
        destroySubscription.clear()
        super.onDestroy()
    }

    open fun showProgress() {

    }

    open fun hideProgress() {

    }

    fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun <T>Single<T>.addProgressTracking() =
        doOnSubscribe {
            showProgress()
        }.doOnEvent { _, _ ->
            hideProgress()
        }

    fun Completable.addProgressTracking() =
        doOnSubscribe {
            showProgress()
        }.doOnEvent {
            hideProgress()
        }
}