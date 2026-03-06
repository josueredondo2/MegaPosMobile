# MegaPosMobile Architecture Diagrams

## 1. High-Level Clean Architecture

```mermaid
graph TB
    subgraph PRESENTATION["PRESENTATION LAYER (UI + MVVM)"]
        direction TB
        Screens["Jetpack Compose Screens<br/>LoginScreen, HomeScreen,<br/>BillingScreen, TransactionScreen,<br/>ProcessScreen, ConfigurationScreen,<br/>AdvancedOptionsScreen, TodayTransactionsScreen"]
        ViewModels["ViewModels<br/>LoginVM, HomeVM, BillingVM,<br/>ProcessVM, ConfigurationVM,<br/>AdvancedOptionsVM, TodayTransactionsVM"]
        States["State Classes<br/>BillingState, ProcessState,<br/>PrintState, CatalogDialogState, etc."]
        Events["Event Classes (Sealed)<br/>BillingEvent, HomeEvent,<br/>LoginEvent, ProcessEvent, etc."]

        Screens -->|"observes StateFlow"| ViewModels
        Screens -->|"sends events"| Events
        Events -->|"handled by"| ViewModels
        ViewModels -->|"emits"| States
        States -->|"collected by"| Screens
    end

    subgraph DOMAIN["DOMAIN LAYER (Pure Kotlin)"]
        direction TB
        UseCases["Use Cases<br/>LoginUseCase, LogoutUseCase,<br/>OpenTerminalUseCase, CloseTerminalUseCase,<br/>PrintDocumentsUseCase, CheckVersionUseCase,<br/>PauseTransactionUseCase, VoidItemUseCase,<br/>AbortTransactionUseCase, etc."]
        RepoInterfaces["Repository Interfaces<br/>AuthRepository, BillingRepository,<br/>PaymentRepository, CatalogRepository,<br/>CashierStationRepository, SystemRepository,<br/>AuditRepository"]
        Models["Domain Models<br/>Customer, InvoiceData, Token,<br/>UserPermissions, DataphonePaymentResult,<br/>DataphoneCloseResult, ReaderBrand,<br/>PrinterModel, AddMaterialResult, etc."]

        UseCases -->|"calls"| RepoInterfaces
        UseCases -->|"uses"| Models
        RepoInterfaces -->|"returns"| Models
    end

    subgraph DATA["DATA LAYER (Implementation)"]
        direction TB
        RepoImpl["Repository Implementations<br/>AuthRepositoryImpl, BillingRepositoryImpl,<br/>PaymentRepositoryImpl, CatalogRepositoryImpl,<br/>CashierStationRepositoryImpl, SystemRepositoryImpl,<br/>AuditRepositoryImpl"]

        subgraph REMOTE["Remote (API)"]
            APIs["Retrofit Interfaces<br/>AuthApi, TransactionApi, PaymentApi,<br/>CustomerApi, FelApi, CatalogApi,<br/>CashierStationApi, AuditApi, SystemApi"]
            DTOs["DTOs (40+)<br/>LoginRequestDto, AddMaterialResponseDto,<br/>FinalizeTransactionDto, PaxResponseDto, etc."]
            Interceptor["AuthInterceptor<br/>(JWT token injection)"]
        end

        subgraph LOCAL["Local (Database + Preferences)"]
            RoomDB["Room Database<br/>MegaPosDatabase (schema v8)"]
            DAOs["DAOs<br/>ServerConfigDao<br/>ActiveTransactionDao"]
            Entities["Entities<br/>ServerConfigEntity<br/>ActiveTransactionEntity"]
            DataStore["SessionManager<br/>(DataStore Preferences)<br/>JWT, userId, sessionId, permissions"]
        end

        RepoImpl --> APIs
        RepoImpl --> DAOs
        RepoImpl --> DataStore
        APIs --> DTOs
        Interceptor -->|"adds JWT to"| APIs
        DAOs --> RoomDB
        Entities --> RoomDB
    end

    subgraph DI["DEPENDENCY INJECTION (Hilt)"]
        NetworkModule["NetworkModule<br/>Retrofit, OkHttp, APIs"]
        RepoModule["RepositoryModule<br/>Binds Interfaces → Impls"]
        DatabaseModule["DatabaseModule<br/>Room DB, DAOs"]
    end

    ViewModels -->|"calls"| UseCases
    ViewModels -->|"calls"| RepoInterfaces
    RepoInterfaces -.->|"implemented by"| RepoImpl
    DI -.->|"provides"| ViewModels
    DI -.->|"provides"| RepoImpl
```

## 2. Data Flow: How a User Action Reaches the API and Back

```mermaid
sequenceDiagram
    participant U as User
    participant S as Screen (Compose)
    participant VM as ViewModel
    participant UC as UseCase
    participant R as Repository
    participant API as Retrofit API
    participant DB as Room / DataStore

    U->>S: Taps button / types input
    S->>VM: onEvent(SomeEvent)
    VM->>VM: _state.update { loading }
    VM->>UC: invoke(params)
    UC->>R: repositoryMethod(params)
    R->>API: HTTP request (via Retrofit)
    API-->>R: Response (DTO)
    R->>R: dto.toDomain()
    R-->>UC: Flow<Resource<T>>
    UC-->>VM: Resource.Success(data)
    VM->>VM: _state.update { data }
    VM->>DB: Save locally (if needed)
    S->>S: Recomposes with new state
    S-->>U: Updated UI
```

## 3. Navigation Graph

```mermaid
graph LR
    Login["Login Screen"]
    Home["Home Screen"]
    Config["Configuration Screen"]
    AdvOpts["Advanced Options Screen"]
    TodayTx["Today Transactions"]

    subgraph BillingGraph["Billing Graph (Shared ViewModel)"]
        Billing["Billing Screen"]
        Transaction["Transaction Screen"]
    end

    Payment["Payment Process Screen"]

    Login -->|"login success"| Home
    Home -->|"Facturación"| BillingGraph
    Home -->|"Configuración"| Config
    Home -->|"Opciones Avanzadas"| AdvOpts
    Home -->|"Transacciones Hoy"| TodayTx
    Billing <-->|"shared BillingViewModel"| Transaction
    Transaction -->|"finalize"| Payment
    Payment -->|"success → new tx"| Billing
    Payment -->|"back"| Transaction
```

## 4. Peripheral Integration (Driver/Factory Pattern)

```mermaid
graph TB
    subgraph DATAPHONE["Dataphone System"]
        DM["DataphoneManager<br/>(orchestrator)"]
        DDF["DataphoneDriverFactory"]

        subgraph Drivers["Drivers"]
            PAX["EmbeddedDataphoneService<br/>(PAX A920 via BAC KinPos)"]
            HTTP["HttpDataphoneService<br/>(ZEBRA via HTTP)"]
            SIM["Simulated Path<br/>(SIMULADO - mock results)"]
        end

        DM -->|"isPaxEmbedded?"| PAX
        DM -->|"isSimulated?"| SIM
        DM -->|"else (ZEBRA)"| HTTP
        HTTP --> DDF
        DDF -->|"creates"| PaxBacDriver["PaxBacDriver<br/>(80+ response codes)"]
    end

    subgraph PRINTER["Printer System"]
        PM["PrinterManager<br/>(orchestrator)"]
        PDF["PrinterDriverFactory"]

        subgraph PrintServices["Services"]
            NetPrint["NetworkPrinterService<br/>(TCP port 9100)"]
            BTPrint["BluetoothPrinterService<br/>(SPP UUID)"]
        end

        PM -->|"usePrinterIp?"| NetPrint
        PM -->|"else"| BTPrint
        NetPrint --> PDF
        BTPrint --> PDF
        PDF -->|"creates"| ZebraDriver["ZebraZQ511Driver<br/>(ZPL commands)"]
    end

    subgraph SCANNER["Scanner System"]
        SM["ScannerManager"]
        SDF["ScannerDriverFactory"]

        subgraph ScanDrivers["Drivers"]
            ZebraScan["ZebraScannerDriver<br/>(DataWedge intents)"]
            PaxScan["PaxScannerDriver"]
        end

        SM --> SDF
        SDF -->|"ZEBRA"| ZebraScan
        SDF -->|"PAX"| PaxScan
    end

    Config2["ServerConfigEntity<br/>(Room DB)"] -->|"readerBrand"| DM
    Config2 -->|"usePrinterIp<br/>printerIp<br/>bluetoothAddress"| PM
    Config2 -->|"readerBrand"| SM
```

## 5. Billing Flow (Detailed)

```mermaid
stateDiagram-v2
    [*] --> BillingScreen: Navigate from Home

    state BillingScreen {
        [*] --> CheckRecovery: init (if !skipRecoveryCheck)
        CheckRecovery --> HasTransaction: Recovery found
        CheckRecovery --> NoTransaction: No recovery
        NoTransaction --> SelectCustomer: User searches
        SelectCustomer --> ClickStart: "Iniciar Transacción"
        HasTransaction --> ClickUpdate: "Actualizar Cliente"
    }

    ClickStart --> ValidateSession
    ClickUpdate --> ValidateSession

    ValidateSession --> SessionExpired: Dead session
    ValidateSession --> CheckDocType: Session alive

    SessionExpired --> [*]: Redirect to Login

    CheckDocType --> ValidateFEL: FC (Factura Electrónica)
    CheckDocType --> NavigateToTx: CO (Tiquete Electrónico)

    ValidateFEL --> SelectActivity: Show economic activities
    SelectActivity --> NavigateToTx: Confirm activity

    state TransactionScreen {
        [*] --> ScanArticle
        ScanArticle --> AddArticle: Barcode / manual
        AddArticle --> APICreatesTx: First article (transactionCode blank)
        AddArticle --> APIAddItem: Subsequent articles
        APICreatesTx --> ShowItems: transactionCode set
        APIAddItem --> ShowItems
        ShowItems --> ScanArticle: Add more
        ShowItems --> Finalize: Click "Finalizar"
    }

    NavigateToTx --> TransactionScreen

    Finalize --> PaymentProcess

    state PaymentProcess {
        [*] --> ProcessPayment: DataphoneManager
        ProcessPayment --> PrintDocuments: Payment success
        ProcessPayment --> PaymentError: Payment failed
        PrintDocuments --> Success: Printed
        PrintDocuments --> PrintError: Print failed
        PaymentError --> RetryPayment: User retries
        RetryPayment --> ProcessPayment
    }

    Success --> BillingScreen: "Nueva transacción" (resetState=true)
    TransactionScreen --> BillingScreen: Back (change customer)
```

## 6. Resource Wrapper Pattern

```mermaid
graph TD
    API["API Call / DB Query"]
    Loading["Resource.Loading()"]
    Success["Resource.Success(data)"]
    Error["Resource.Error(message)"]

    API -->|"emit first"| Loading
    Loading -->|"call succeeds"| Success
    Loading -->|"call fails"| Error

    Success -->|"ViewModel updates"| StateUpdate["_state.update { ... }"]
    Error -->|"ViewModel updates"| ErrorState["_state.update { error = msg }"]
    Loading -->|"ViewModel updates"| LoadingState["_state.update { isLoading = true }"]

    StateUpdate --> UI["Screen recomposes"]
    ErrorState --> UI
    LoadingState --> UI
```

## 7. Project File Structure

```
app/src/main/java/com/devlosoft/megaposmobile/
│
├── MainActivity.kt                    # Single Activity (Compose)
├── MegaPosApplication.kt             # Hilt Application
│
├── presentation/                      # UI LAYER
│   ├── navigation/
│   │   ├── NavGraph.kt               # All routes & navigation
│   │   └── Routes.kt                 # @Serializable route definitions
│   ├── shared/components/            # Reusable UI components
│   ├── login/                        # Login feature
│   ├── home/                         # Home dashboard
│   ├── billing/                      # Billing + Transaction (nested graph)
│   │   ├── state/                    # Sub-state classes
│   │   └── components/              # Billing-specific components
│   ├── process/                      # Payment processing
│   ├── configuration/                # Server config
│   ├── advancedoptions/              # Device settings
│   └── todaytransactions/            # Transaction history
│
├── domain/                            # DOMAIN LAYER
│   ├── model/                        # Domain models (pure Kotlin)
│   ├── repository/                   # Repository interfaces
│   └── usecase/                      # Business logic use cases
│       └── billing/                  # Billing-specific use cases
│
├── data/                              # DATA LAYER
│   ├── remote/
│   │   ├── api/                      # Retrofit interfaces
│   │   │   └── interceptor/          # JWT AuthInterceptor
│   │   └── dto/                      # Data Transfer Objects (40+)
│   ├── local/
│   │   ├── database/                 # Room DB (MegaPosDatabase)
│   │   ├── dao/                      # Data Access Objects
│   │   ├── entity/                   # Room entities
│   │   └── preferences/             # DataStore (SessionManager)
│   └── repository/                   # Repository implementations
│
├── core/                              # PERIPHERAL & UTILITIES
│   ├── dataphone/                    # Dataphone drivers & services
│   │   └── drivers/                  # PaxBacDriver
│   ├── printer/                      # Printer drivers & services
│   │   └── drivers/                  # ZebraZQ511Driver
│   ├── scanner/                      # Scanner drivers & services
│   │   └── drivers/                  # Zebra & PAX drivers
│   ├── session/                      # InactivityManager
│   ├── common/                       # Resource, ApiConfig, Constants
│   ├── constants/                    # FieldLengths
│   ├── extensions/                   # Kotlin extensions
│   ├── state/                        # Peripheral state enums
│   └── util/                         # Bluetooth, Network, JWT utils
│
├── di/                                # HILT MODULES
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── DatabaseModule.kt
│
└── ui/theme/                          # Compose Theme
```
