# Explicación de la Arquitectura del Proyecto MegaPosMobile

## 🏗️ Arquitectura: Clean Architecture + MVVM

He implementado una arquitectura en 3 capas que separa responsabilidades y hace el código mantenible y testeable:

```
┌─────────────────────────────────────────────────┐
│         PRESENTATION LAYER (UI)                 │
│  - Jetpack Compose (UI)                         │
│  - ViewModels (Lógica de UI)                    │
│  - States & Events                              │
└────────────┬────────────────────────────────────┘
             │ uses
             ↓
┌─────────────────────────────────────────────────┐
│         DOMAIN LAYER (Reglas de Negocio)        │
│  - Models (entidades puras)                     │
│  - Repository Interfaces                        │
│  - Use Cases (casos de uso)                     │
└────────────┬────────────────────────────────────┘
             │ implements
             ↓
┌─────────────────────────────────────────────────┐
│         DATA LAYER (Fuentes de Datos)           │
│  - Repository Implementations                   │
│  - API (Retrofit)                               │
│  - Database (Room)                              │
│  - DataStore (Preferencias)                     │
└─────────────────────────────────────────────────┘
```

---

## 📦 Capas en Detalle

### 1. **PRESENTATION Layer** (Lo que el usuario ve)

**Ubicación**: `presentation/`

```kotlin
// Estado: Lo que la pantalla muestra
data class LoginState(
    val userCode: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

// Eventos: Lo que el usuario hace
sealed class LoginEvent {
    data class UserCodeChanged(val code: String) : LoginEvent()
    object Login : LoginEvent()
}

// ViewModel: Maneja la lógica de la pantalla
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEvent(event: LoginEvent) { /* ... */ }
}

// Screen: UI declarativa con Compose
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    // UI usando state
}
```

**Conceptos Clave:**
- **Unidirectional Data Flow**: Los datos fluyen en una sola dirección
  ```
  User Action → Event → ViewModel → State → UI
  ```
- **State**: Un objeto inmutable que representa lo que se muestra
- **Events**: Acciones del usuario (clicks, texto ingresado, etc.)
- **ViewModel**: Sobrevive a rotaciones de pantalla

---

### 2. **DOMAIN Layer** (Reglas de Negocio Puras)

**Ubicación**: `domain/`

Esta capa NO depende de Android ni de frameworks externos. Es Kotlin puro.

```kotlin
// Model: Entidad de dominio (sin anotaciones de JSON/Room)
data class Token(
    val accessToken: String
)

// Repository Interface: Contrato (qué se puede hacer)
interface AuthRepository {
    suspend fun login(code: String, password: String): Flow<Resource<Token>>
}

// Use Case: Un caso de uso específico
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String, password: String): Flow<Resource<Token>> {
        // Validaciones de negocio
        if (code.isBlank()) {
            return flow { emit(Resource.Error("Código requerido")) }
        }
        return authRepository.login(code, password)
    }
}
```

**Por qué Use Cases:**
- **Single Responsibility**: Cada Use Case hace UNA cosa
- **Reutilizables**: Pueden usarse desde múltiples ViewModels
- **Testables**: Fácil de probar sin Android
- **Reglas de Negocio Centralizadas**: Validaciones en un solo lugar

---

### 3. **DATA Layer** (Implementación de Datos)

**Ubicación**: `data/`

```kotlin
// DTO: Cómo viene del servidor (con anotaciones JSON)
data class LoginResponseDto(
    @SerializedName("accessToken") val accessToken: String
) {
    fun toDomain(): Token = Token(accessToken = accessToken)
}

// Repository Implementation
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    override suspend fun login(code: String, password: String): Flow<Resource<Token>> = flow {
        emit(Resource.Loading())
        try {
            val response = authApi.login(LoginRequestDto(code, password))
            if (response.isSuccessful) {
                val token = response.body()!!.toDomain()
                sessionManager.saveSession(token.accessToken)
                emit(Resource.Success(token))
            } else {
                emit(Resource.Error("Error"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Sin conexión"))
        }
    }
}
```

**Responsabilidades:**
- **API**: Llamadas HTTP con Retrofit
- **Database**: Persistencia local con Room
- **DataStore**: Preferencias/sesión
- **Mapeo**: DTO ↔ Domain Model

---

## 🔧 Inyección de Dependencias con Hilt

Hilt maneja la creación de objetos automáticamente.

### ¿Cómo funciona?

```kotlin
// 1. Marca la Application
@HiltAndroidApp
class MegaPosApplication : Application()

// 2. Marca Activities
@AndroidEntryPoint
class MainActivity : ComponentActivity()

// 3. Define módulos
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}

// 4. Inyecta donde necesites
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase  // Hilt lo provee automáticamente
) : ViewModel()
```

**Ventajas:**
- No más `new` manual
- Fácil cambiar implementaciones
- Ciclos de vida manejados automáticamente
- Testing más fácil (puedes inyectar mocks)

---

## 🔄 Flujo Completo de Login

```
1. Usuario toca "Login"
   ↓
2. LoginScreen → viewModel.onEvent(LoginEvent.Login)
   ↓
3. LoginViewModel → loginUseCase(code, password)
   ↓
4. LoginUseCase → authRepository.login(code, password)
   ↓
5. AuthRepositoryImpl → authApi.login(LoginRequestDto)
   ↓
6. Retrofit → HTTP POST /pos-api/v1/login
   ↓
7. Backend responde → {"accessToken": "jwt..."}
   ↓
8. AuthRepositoryImpl → sessionManager.saveSession(token)
   ↓
9. AuthRepositoryImpl → emit(Resource.Success(token))
   ↓
10. LoginViewModel → _state.update { isLoginSuccessful = true }
   ↓
11. LoginScreen → onLoginSuccess() → navController.navigate()
```

---

## 🎯 Patrón Resource para Manejo de Estados

```kotlin
sealed class Resource<T> {
    class Loading<T> : Resource<T>()
    class Success<T>(val data: T) : Resource<T>()
    class Error<T>(val message: String) : Resource<T>()
}
```

**Por qué es útil:**
```kotlin
loginUseCase(code, password).collect { result ->
    when (result) {
        is Resource.Loading -> {
            // Mostrar spinner
            _state.update { it.copy(isLoading = true) }
        }
        is Resource.Success -> {
            // Navegar
            _state.update { it.copy(isLoginSuccessful = true) }
        }
        is Resource.Error -> {
            // Mostrar error
            _state.update { it.copy(error = result.message) }
        }
    }
}
```

---

## 📱 Componentes Clave del Proyecto

### 1. **SessionManager** (DataStore)
Guarda datos de sesión persistentes:
```kotlin
sessionManager.saveSession(accessToken = "jwt...")
sessionManager.isLoggedIn().collect { isLoggedIn -> }
sessionManager.clearSession()
```

### 2. **AuthInterceptor** (OkHttp)
Agrega automáticamente el token a las peticiones:
```kotlin
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = runBlocking { sessionManager.getAccessToken().first() }
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
```

### 3. **Navigation** (Compose)
```kotlin
NavHost(navController, startDestination = Screen.Login.route) {
    composable(Screen.Login.route) {
        LoginScreen(onLoginSuccess = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        })
    }
}
```

---

## 🚀 Cómo Agregar un Nuevo Endpoint

Ejemplo: **Buscar Cliente**

### Paso 1: DTO (Data Layer)
```kotlin
// data/remote/dto/CustomerDto.kt
data class CustomerDto(
    @SerializedName("partyId") val partyId: Int,
    @SerializedName("name") val name: String
) {
    fun toDomain() = Customer(partyId, name)
}
```

### Paso 2: Model (Domain Layer)
```kotlin
// domain/model/Customer.kt
data class Customer(
    val partyId: Int,
    val name: String
)
```

### Paso 3: API Interface
```kotlin
// data/remote/api/CustomerApi.kt
interface CustomerApi {
    @GET("customer/{identification}")
    suspend fun getCustomer(@Path("identification") id: String): Response<List<CustomerDto>>
}
```

### Paso 4: Repository Interface (Domain)
```kotlin
// domain/repository/CustomerRepository.kt
interface CustomerRepository {
    suspend fun getCustomer(id: String): Flow<Resource<List<Customer>>>
}
```

### Paso 5: Repository Implementation (Data)
```kotlin
// data/repository/CustomerRepositoryImpl.kt
class CustomerRepositoryImpl @Inject constructor(
    private val api: CustomerApi
) : CustomerRepository {
    override suspend fun getCustomer(id: String) = flow {
        emit(Resource.Loading())
        try {
            val response = api.getCustomer(id)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()!!.map { it.toDomain() }))
            } else {
                emit(Resource.Error("Error"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Sin conexión"))
        }
    }
}
```

### Paso 6: Use Case
```kotlin
// domain/usecase/GetCustomerUseCase.kt
class GetCustomerUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    suspend operator fun invoke(id: String): Flow<Resource<List<Customer>>> {
        if (id.isBlank()) return flow { emit(Resource.Error("ID requerido")) }
        return repository.getCustomer(id)
    }
}
```

### Paso 7: Registrar en Hilt
```kotlin
// di/NetworkModule.kt
@Provides
@Singleton
fun provideCustomerApi(retrofit: Retrofit): CustomerApi =
    retrofit.create(CustomerApi::class.java)

// di/RepositoryModule.kt
@Binds
@Singleton
abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository
```

### Paso 8: ViewModel
```kotlin
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val getCustomerUseCase: GetCustomerUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(CustomerState())
    val state = _state.asStateFlow()

    fun searchCustomer(id: String) {
        viewModelScope.launch {
            getCustomerUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                    is Resource.Success -> _state.update { it.copy(customers = result.data!!) }
                    is Resource.Error -> _state.update { it.copy(error = result.message) }
                }
            }
        }
    }
}
```

### Paso 9: Screen
```kotlin
@Composable
fun CustomerScreen(viewModel: CustomerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column {
        Button(onClick = { viewModel.searchCustomer("123") }) {
            Text("Buscar")
        }

        if (state.isLoading) CircularProgressIndicator()

        state.customers.forEach { customer ->
            Text(customer.name)
        }
    }
}
```

---

## 🎓 Conceptos Importantes para el Futuro

### 1. **Flow vs LiveData**
He usado **Flow** (más moderno):
- Parte de Kotlin Coroutines
- Más potente (operadores como map, filter)
- Mejor para arquitectura limpia

### 2. **suspend functions**
```kotlin
suspend fun login() // Función que puede suspenderse sin bloquear
```
- Solo se pueden llamar desde coroutines
- `viewModelScope.launch { }` crea un coroutine

### 3. **StateFlow vs MutableStateFlow**
```kotlin
private val _state = MutableStateFlow(State())  // Privado, mutable
val state: StateFlow<State> = _state.asStateFlow()  // Público, solo lectura
```

### 4. **@Composable**
Funciones que describen UI:
```kotlin
@Composable
fun MyButton() {
    Button(onClick = {}) { Text("Click") }
}
```

### 5. **Offline-First**
- Room guarda datos localmente
- Sincroniza cuando hay internet
- App funciona sin conexión

---

## 📋 Checklist para Nuevas Features

- [ ] Crear modelo en `domain/model/`
- [ ] Crear DTO en `data/remote/dto/`
- [ ] Agregar endpoint a API interface
- [ ] Crear repository interface en `domain/repository/`
- [ ] Implementar repository en `data/repository/`
- [ ] Crear Use Case en `domain/usecase/`
- [ ] Registrar en módulos Hilt
- [ ] Crear State & Events
- [ ] Crear ViewModel
- [ ] Crear Screen con Compose
- [ ] Agregar a NavGraph

---

## 🛠️ Herramientas Importantes

- **Logcat**: Ver logs en Android Studio
- **Network Profiler**: Ver llamadas HTTP
- **Database Inspector**: Ver datos de Room
- **Layout Inspector**: Depurar UI Compose

---

## 📚 Recursos Adicionales

### Documentación Oficial
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin Flow](https://developer.android.com/kotlin/flow)
- [Room](https://developer.android.com/training/data-storage/room)
- [Retrofit](https://square.github.io/retrofit/)

### Tutoriales
- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [State in Compose](https://developer.android.com/jetpack/compose/state)

---

## 💡 Tips y Mejores Prácticas

### 1. Naming Conventions
- **ViewModel**: `LoginViewModel`, `CustomerViewModel`
- **UseCase**: `LoginUseCase`, `GetCustomerUseCase`
- **Repository**: `AuthRepository`, `CustomerRepository`
- **DTO**: `LoginRequestDto`, `CustomerDto`
- **Screen**: `LoginScreen`, `CustomerScreen`
- **State**: `LoginState`, `CustomerState`
- **Event**: `LoginEvent`, `CustomerEvent`

### 2. Organización de Archivos
Agrupa por feature, no por tipo:
```
✅ Correcto:
presentation/
  login/
    LoginScreen.kt
    LoginViewModel.kt
    LoginState.kt
    LoginEvent.kt
  customer/
    CustomerScreen.kt
    CustomerViewModel.kt

❌ Incorrecto:
presentation/
  screens/
    LoginScreen.kt
    CustomerScreen.kt
  viewmodels/
    LoginViewModel.kt
    CustomerViewModel.kt
```

### 3. Evita God Objects
- Mantén ViewModels pequeños y enfocados
- Un ViewModel por pantalla
- Un UseCase hace UNA cosa

### 4. Testing
```kotlin
// Test de Use Case
@Test
fun `login with empty code returns error`() = runTest {
    val useCase = LoginUseCase(mockRepository)

    useCase("", "password").collect { result ->
        assert(result is Resource.Error)
        assertEquals("Código requerido", result.message)
    }
}
```

### 5. Logging
```kotlin
// En desarrollo
if (BuildConfig.DEBUG) {
    Log.d("LoginViewModel", "Login successful: $token")
}
```

---

## 🐛 Troubleshooting Común

### Error: "Cannot access database on main thread"
**Solución**: Usa `suspend` functions o Flow
```kotlin
// ❌ Incorrecto
val data = database.dao().getData()

// ✅ Correcto
viewModelScope.launch {
    val data = database.dao().getData()
}
```

### Error: "lateinit property has not been initialized"
**Solución**: Usa Hilt o inicializa en `onCreate`

### Error: "No value for X in state"
**Solución**: Provee valores por defecto en el State
```kotlin
data class LoginState(
    val userCode: String = "",  // ✅ Default
    val password: String = ""
)
```

### Error: "java.lang.IllegalStateException: Flow invariant is violated"
**Solución**: No uses `flow.collect {}` dos veces en el mismo Flow
```kotlin
// ❌ Incorrecto
val flow = repository.getData()
flow.collect { }
flow.collect { }  // Error!

// ✅ Correcto
repository.getData().collect { }  // Nueva instancia
```

---

## 🎯 Próximos Pasos

1. **Implementar más endpoints** usando `IMPLEMENTATION_GUIDE.md`
2. **Agregar tests** unitarios y de integración
3. **Mejorar UI** con animaciones y transiciones
4. **Implementar modo offline** completo con Room
5. **Agregar manejo de errores** más robusto
6. **Implementar refresh tokens** para sesiones largas
7. **Agregar analytics** y crash reporting

---

**¿Preguntas? Revisa `IMPLEMENTATION_GUIDE.md` para ejemplos específicos de cada endpoint.**
