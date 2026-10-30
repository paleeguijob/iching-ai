package com.iching.ai.di

import com.iching.ai.data.di.dataModule
import com.iching.ai.domain.usecase.DeleteReadingUseCase
import com.iching.ai.domain.usecase.GetHexagramUseCase
import com.iching.ai.domain.usecase.GetHexagramsUseCase
import com.iching.ai.domain.usecase.GetReadingUseCase
import com.iching.ai.domain.usecase.InterpretReadingUseCase
import com.iching.ai.domain.usecase.ObserveJournalUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.ResolveCastUseCase
import com.iching.ai.domain.usecase.SaveReadingUseCase
import com.iching.ai.domain.usecase.SearchHexagramsUseCase
import com.iching.ai.domain.usecase.TossCoinsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.feature.divination.DivinationViewModel
import com.iching.ai.feature.home.HomeViewModel
import com.iching.ai.feature.journal.JournalEntryViewModel
import com.iching.ai.feature.journal.JournalViewModel
import com.iching.ai.feature.library.HexagramDetailViewModel
import com.iching.ai.feature.library.LibraryViewModel
import com.iching.ai.feature.settings.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val domainModule = module {
    factory { TossCoinsUseCase() }
    factoryOf(::ResolveCastUseCase)
    factoryOf(::InterpretReadingUseCase)
    factoryOf(::GetHexagramsUseCase)
    factoryOf(::GetHexagramUseCase)
    factoryOf(::SearchHexagramsUseCase)
    factoryOf(::ObserveJournalUseCase)
    factoryOf(::GetReadingUseCase)
    factoryOf(::SaveReadingUseCase)
    factoryOf(::DeleteReadingUseCase)
    factoryOf(::ObserveSettingsUseCase)
    factoryOf(::UpdateSettingsUseCase)
}

val presentationModule = module {
    viewModelOf(::HomeViewModel)
    viewModel { params -> DivinationViewModel(params.get(), get(), get(), get(), get(), get()) }
    viewModelOf(::LibraryViewModel)
    viewModel { params -> HexagramDetailViewModel(params.get(), get()) }
    viewModelOf(::JournalViewModel)
    viewModel { params -> JournalEntryViewModel(params.get(), get(), get(), get()) }
    viewModelOf(::SettingsViewModel)
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(dataModule, domainModule, presentationModule)
    }
}
