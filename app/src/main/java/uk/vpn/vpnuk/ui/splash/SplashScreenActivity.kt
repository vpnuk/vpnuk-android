/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */
package uk.vpn.vpnuk.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.coroutines.flow.onEach
import uk.vpn.vpnuk.BaseActivity
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.ui.mainScreen.MainActivity
import uk.vpn.vpnuk.ui.quickLaunch.QuickLaunchActivity
import uk.vpn.vpnuk.utils.launchWhenCreated
import uk.vpn.vpnuk.utils.observe

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : BaseActivity() {

    val vm: SplashScreenVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        vm.onCreate()

        observeLiveData()
        initView()
    }

    private fun observeLiveData() {
        vm.viewState.onEach { render(it) }.launchWhenCreated(lifecycleScope)
        vm.oneShotEvents.onEach {
            when(it){
                is SplashScreenVM.OneShotEvent.NavigateToQuickLaunch->{
                    val intent = Intent(this, QuickLaunchActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
                is SplashScreenVM.OneShotEvent.ErrorToast->{
                    Toasty.error(this, it.message, Toasty.LENGTH_SHORT).show()
                }
            }
        }.launchWhenCreated(lifecycleScope)
    }

    private fun render(viewState: SplashScreenVM.ViewState) {
        vSplashScreenActivityTextState.text = viewState.loadingTextToDisplay
    }

    private fun initView() {
        supportActionBar?.hide()
    }
}