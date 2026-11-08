package com.iching.ai.di

import com.iching.ai.data.api.ApiSession
import com.iching.ai.data.di.dataModule
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.campaignIdFor
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
import com.iching.ai.domain.usecase.SaveOutcomeUseCase
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
import com.iching.ai.feature.year.YearViewModel
import com.iching.ai.platform.NativeBridges
import com.iching.ai.session.AppSession
import com.iching.ai.session.FirebaseApiSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.feature.siamsi.SiamSiDrawGate
import com.iching.ai.feature.siamsi.SiamSiHomeViewModel
import com.iching.ai.feature.siamsi.SiamSiViewModel
import com.iching.ai.platform.secureRandom
import kotlin.time.Clock
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
    factory { SaveOutcomeUseCase(get(), get<OracleApi>()::saveOutcome) }
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
    single {
        val session = get<AppSession>()
        SiamSiDrawGate(
            settings = get(),
            ads = get<NativeBridges>().ads,
            status = session.status,
            online = session.online,
            log = session::log,
            today = { bangkokEpochDay(Clock.System.now().toEpochMilliseconds()) },
        )
    }
}

val presentationModule = module {
    viewModelOf(::HomeViewModel)
    viewModel {
        val session = get<AppSession>()
        SiamSiHomeViewModel(get(), get(), get(), get(), session.status, session.online, session::log)
    }
    viewModel {
        val session = get<AppSession>()
        DailyViewModel(
            getDaily = get(),
            recordView = get(),
            claimPushPrompt = get(),
            log = session::log,
            pushSupported = { session.dailyPushSupported },
            enablePush = { session.setDailyPush(true) },
            syncStreak = session::syncStreak,
        )
    }
    viewModel {
        val session = get<AppSession>()
        YearViewModel(
            api = get(),
            getHexagram = get(),
            observeSettings = get(),
            log = session::log,
            campaignId = { year -> campaignIdFor(session.status.value.campaign, year) },
            campaignActive = { session.status.value.campaign?.active },
            syncStreak = session::syncStreak,
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
            resumeReadingId = params.values.filterIsInstance<Long>().firstOrNull(),
            getReading = get(),
            yearOf = params.values.filterIsInstance<Int>().firstOrNull(),
        )
    }
    viewModelOf(::LibraryViewModel)
    viewModel { params -> HexagramDetailViewModel(params.get(), get()) }
    viewModel { params ->
        SiamSiViewModel(
            route = params.get(),
            repository = get(),
            api = get(),
            session = get(),
            journal = get(),
            settings = get(),
            random = secureRandom(),
            today = { bangkokEpochDay(Clock.System.now().toEpochMilliseconds()) },
        )
    }
    viewModelOf(::JournalViewModel)
    viewModel { params -> JournalEntryViewModel(params.get(), get(), get(), get(), get(), get(), get<AppSession>()::log) }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PaywallViewModel)
}

fun initKoin(bridges: NativeBridges, config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule(bridges), dataModule, domainModule, presentationModule)
    }
}
