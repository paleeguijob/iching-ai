package com.iching.ai.di

import com.iching.ai.data.api.ApiSession
import com.iching.ai.data.di.dataModule
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.usecase.ClaimPushPromptUseCase
import com.iching.ai.domain.usecase.DeleteReadingUseCase
import com.iching.ai.domain.usecase.FilterJournalUseCase
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.RecordDailyViewUseCase
import com.iching.ai.domain.usecase.GetHexagramsUseCase
import com.iching.ai.domain.usecase.GetReadingUseCase
import com.iching.ai.domain.usecase.ObserveJournalUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.domain.usecase.SearchHexagramsUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.feature.daily.DailyViewModel
import com.iching.ai.feature.divination.DivinationViewModel
import com.iching.ai.feature.home.HomeViewModel
import com.iching.ai.feature.journal.JournalEntryViewModel
import com.iching.ai.feature.journal.JournalViewModel
import com.iching.ai.feature.library.HexagramDetailViewModel
import com.iching.ai.feature.library.LibraryViewModel
import com.iching.ai.feature.paywall.PaywallViewModel
import com.iching.ai.feature.settings.SettingsViewModel
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FirebaseApiSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val domainModule = module {
    factory { TossCoinsUseCase() }
    factoryOf(::ResolveCastUseCase)
    factoryOf(::GetHexagramsUseCase)
    factoryOf(::GetHexagramUseCase)
    factoryOf(::GetDailyHexagramUseCase)
    factoryOf(::RecordDailyViewUseCase)
    factoryOf(::ClaimPushPromptUseCase)
    factoryOf(::SearchHexagramsUseCase)
    factoryOf(::ObserveJournalUseCase)
    factoryOf(::GetReadingUseCase)
    factoryOf(::SaveReadingUseCase)
    factory { DeleteReadingUseCase(get(), get<OracleApi>()::deleteReading) }
    factory { FilterJournalUseCase() }
    factoryOf(::ObserveSettingsUseCase)
    factoryOf(::UpdateSettingsUseCase)
}

/** Native SDK bridges (Firebase, RevenueCat, AdMob) and the session built on them. */
fun platformModule(bridges: NativeBridges): Module = module {
    single { bridges }
    single { bridges.auth }
    single { FirebaseApiSession(get()) }
    single<ApiSession> { get<FirebaseApiSession>() }
    single {
        AppSession(
            bridges = get(),
            apiSession = get(),
            api = get(),
            journal = get(),
            settings = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        )
    }
}

val presentationModule = module {
    viewModelOf(::HomeViewModel)
    viewModel {
        val session = get<AppSession>()
        DailyViewModel(
            getDaily = get(),
            recordView = get(),
            claimPushPrompt = get(),
            log = session::log,
            pushSupported = { session.dailyPushSupported },
            enablePush = { session.setDailyPush(true) },
        )
    }
    viewModel { params ->
        DivinationViewModel(
            question = params.values.filterIsInstance<String>().first(),
            category = params.values.filterIsInstance<QuestionCategory>().firstOrNull(),
            tossCoins = get(),
            resolveCast = get(),
            saveReading = get(),
            api = get(),
            session = get(),
            observeSettings = get(),
        )
    }
    viewModelOf(::LibraryViewModel)
    viewModel { params -> HexagramDetailViewModel(params.get(), get()) }
    viewModelOf(::JournalViewModel)
    viewModel { params -> JournalEntryViewModel(params.get(), get(), get(), get(), get(), get()) }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PaywallViewModel)
}

fun initKoin(bridges: NativeBridges, config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule(bridges), dataModule, domainModule, presentationModule)
    }
}
