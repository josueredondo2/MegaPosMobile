# Explicación de la Arquitectura del Proyecto MegaPosMobile

## 🏗️ Arquitectura: Clean Architecture + MVVM

El proyecto implementa una arquitectura en 3 capas que separa responsabilidades y hace el código mantenible y testeable:

```
┌─────────────────────────────────────────────────┐
│         PRESENTATION LAYER (UI)                 │
│  - Jetpack Compose (UI)                         │
│  - ViewModels (Lógica de UI)                    │
│  - States & Events                              │
│  - Responsive Design System                     │
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
    val dimensions = LocalDimensions.current // Responsive design
    // UI usando state y dimensions
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

## 🎨 Sistema de Diseño Responsive

**Ubicación**: `ui/theme/Dimensions.kt`

El proyecto implementa un sistema de diseño adaptativo que detecta el tamaño de la pantalla y ajusta automáticamente dimensiones, fuentes, y espaciado.

### Tipos de Dispositivo

| Tipo | Ancho de Pantalla | Características |
|------|-------------------|-----------------|
| **PHONE** | < 600dp | Teléfonos 5.5" - Dimensiones estándar |
| **PHABLET** | 600-839dp | Tablets pequeñas - Dimensiones medianas |
| **TABLET** | >= 840dp | Tablets 10.1" - Dimensiones grandes, contenido centrado |

### Uso en Pantallas

```kotlin
@Composable
fun MyScreen() {
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier
            .widthIn(max = dimensions.maxContentWidth) // Ancho máximo en tablets
            .padding(horizontal = dimensions.horizontalPadding)
    ) {
        Text(
            text = "Título",
            fontSize = dimensions.fontSizeTitle
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight)
        ) {
            Text(
                text = "Acción",
                fontSize = dimensions.fontSizeExtraLarge
            )
        }
    }
}
```

### Dimensiones Adaptativas

```kotlin
data class Dimensions(
    // Padding
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,

    // Content
    val maxContentWidth: Dp,      // Infinity en phones, 600dp en tablets
    val horizontalPadding: Dp,     // 32dp phones, 64dp tablets

    // Spacing
    val spacerSmall: Dp,
    val spacerMedium: Dp,
    val spacerLarge: Dp,

    // Font sizes
    val fontSizeSmall: TextUnit,   // 12sp phones, 16sp tablets
    val fontSizeMedium: TextUnit,  // 14sp phones, 18sp tablets
    val fontSizeTitle: TextUnit,   // 24sp phones, 36sp tablets
    val fontSizeHeader: TextUnit,  // 28sp phones, 40sp tablets

    // Components
    val buttonHeight: Dp,          // 56dp phones, 64dp tablets
    val textFieldHeight: Dp,
    val iconSizeSmall: Dp,
    val headerHeight: Dp,          // 80dp phones, 120dp tablets

    // Logo
    val logoFontSize: TextUnit,
    val sloganFontSize: TextUnit
)
```

---

## 🎨 Sistema de Temas y Colores

**Ubicación**: `ui/theme/`

### Colores de MegaSuper

```kotlin
// Color.kt
val MegaSuperRed = Color(0xFFC62828)
val MegaSuperRedDark = Color(0xFFB71C1C)
val MegaSuperRedLight = Color(0xFFEF5350)
val MegaSuperWhite = Color(0xFFFFFFFF)
val MegaSuperGray = Color(0xFF757575)
```

### Tema de la App

```kotlin
// Theme.kt
private val MegaSuperColorScheme = lightColorScheme(
    primary = MegaSuperRed,
    onPrimary = MegaSuperWhite,
    secondary = MegaSuperRedLight,
    tertiary = MegaSuperRedDark
)

@Composable
fun MegaPosMobileTheme(content: @Composable () -> Unit) {
    ProvideDimensions {  // Inyecta el sistema responsive
        MaterialTheme(
            colorScheme = MegaSuperColorScheme,
            typography = Typography,
            content = content
        )
    }
}
```

---

## 🗄️ Base de Datos Local (Room)

**Ubicación**: `data/local/`

### Configuración del Servidor

El proyecto guarda la configuración del servidor en la base de datos local para permitir configuración dinámica.

```kotlin
// Entity
@Entity(tableName = "server_config")
data class ServerConfigEntity(
    @PrimaryKey val id: Int = 1,
    val serverUrl: String,       // URL del API
    val serverName: String,       // Hostname del dispositivo
    val isActive: Boolean = true,
    val lastConnected: Long? = null
)

// DAO
@Dao
interface ServerConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerConfig(config: ServerConfigEntity)

    @Query("SELECT * FROM server_config WHERE isActive = 1 LIMIT 1")
    fun getActiveServerConfig(): Flow<ServerConfigEntity?>

    @Query("SELECT * FROM server_config WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServerConfigSync(): ServerConfigEntity?
}

// Database
@Database(
    entities = [ServerConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MegaPosDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao
}
```

---

## 🔧 Inyección de Dependencias con Hilt

### Configuración de Red con URL Dinámica

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        sessionManager: SessionManager,
        serverConfigDao: ServerConfigDao
    ): AuthInterceptor {
        return AuthInterceptor(sessionManager, serverConfigDao)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL) // Base URL placeholder
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

---

## 🔐 AuthInterceptor Dinámico

**Ubicación**: `data/remote/interceptor/AuthInterceptor.kt`

El interceptor lee la configuración de la base de datos en cada request para usar la URL y hostname configurados.

```kotlin
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val serverConfigDao: ServerConfigDao
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Obtener configuración de la base de datos
        val serverConfig = runBlocking {
            serverConfigDao.getActiveServerConfigSync()
        } ?: throw IOException("Configuración del servidor no encontrada")

        // Obtener hostname desde la DB
        val hostname = serverConfig.serverName.takeIf { it.isNotBlank() }
            ?: throw IOException("Hostname no configurado")

        // Construir nueva URL con la configuración de la DB
        val configuredBaseUrl = serverConfig.serverUrl.toHttpUrlOrNull()
            ?: throw IOException("URL del servidor no configurada")

        val newUrl = originalRequest.url.newBuilder()
            .scheme(configuredBaseUrl.scheme)
            .host(configuredBaseUrl.host)
            .port(configuredBaseUrl.port)
            .build()

        // Para login: solo hostname
        if (newRequest.url.encodedPath.endsWith("login")) {
            return chain.proceed(
                newRequest.newBuilder()
                    .url(newUrl)
                    .header("x-Hostname", hostname)
                    .build()
            )
        }

        // Para otros endpoints: token + hostname
        val token = runBlocking { sessionManager.getAccessToken().first() }

        return chain.proceed(
            newRequest.newBuilder()
                .url(newUrl)
                .header("Authorization", "Bearer $token")
                .header("x-Hostname", hostname)
                .build()
        )
    }
}
```

**Ventajas:**
- ✅ URL configurable sin recompilar la app
- ✅ Hostname personalizado por dispositivo
- ✅ Sin valores hardcodeados
- ✅ Errores claros si falta configuración

---

## 🔄 Flujo Completo de la App

### 1. Primera Vez (Sin Configuración)

```
1. App inicia
   ↓
2. AuthInterceptor intenta obtener config → null
   ↓
3. Usuario ve LoginScreen
   ↓
4. Usuario toca "Configuración"
   ↓
5. Navega a ConfigurationScreen
   ↓
6. Usuario ingresa:
   - URL: http://192.168.1.100:6060
   - Hostname: android-pos-01
   ↓
7. ConfigurationViewModel → serverConfigDao.insertServerConfig()
   ↓
8. Configuración guardada en Room
   ↓
9. Usuario regresa a LoginScreen
   ↓
10. Ahora puede hacer login
```

### 2. Login Normal (Con Configuración)

```
1. Usuario ingresa código y contraseña
   ↓
2. LoginScreen → viewModel.onEvent(LoginEvent.Login)
   ↓
3. LoginViewModel → loginUseCase(code, password)
   ↓
4. LoginUseCase → authRepository.login()
   ↓
5. AuthRepositoryImpl → authApi.login()
   ↓
6. AuthInterceptor intercepta request:
   - Lee serverConfig de Room DB
   - Reemplaza URL: http://192.168.1.100:6060/pos-api/v1/login
   - Agrega header: x-Hostname: android-pos-01
   ↓
7. Retrofit → HTTP POST a servidor configurado
   ↓
8. Backend responde → {"accessToken": "jwt..."}
   ↓
9. AuthRepositoryImpl → sessionManager.saveSession(token)
   ↓
10. LoginViewModel → _state.update { isLoginSuccessful = true }
   ↓
11. LoginScreen → navController.navigate(Screen.Home)
```

---

## 📱 Pantallas del Proyecto

### 1. ConfigurationScreen

**Ubicación**: `presentation/configuration/`

Pantalla para configurar la URL del servidor y el hostname del dispositivo.

```kotlin
@Composable
fun ConfigurationScreen(
    viewModel: ConfigurationViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current

    Column {
        // Header rojo con logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MegaSuperRed)
                .height(dimensions.headerHeight)
        ) {
            Row {
                Image(
                    painter = painterResource(R.drawable.logo_megasuper),
                    modifier = Modifier.height(dimensions.headerHeight * 0.6f)
                )
                Text("Version: 1.0", color = MegaSuperWhite)
            }
        }

        // Campos de configuración
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = { viewModel.onEvent(ConfigurationEvent.ServerUrlChanged(it)) },
            label = { Text("Dirección POS API") }
        )

        OutlinedTextField(
            value = state.hostname,
            onValueChange = { viewModel.onEvent(ConfigurationEvent.HostnameChanged(it)) },
            label = { Text("Host Name") }
        )

        Button(onClick = { viewModel.onEvent(ConfigurationEvent.Save) }) {
            Text("Guardar")
        }
    }
}
```

**State:**
```kotlin
data class ConfigurationState(
    val serverUrl: String = "",
    val hostname: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
```

**Events:**
```kotlin
sealed class ConfigurationEvent {
    data class ServerUrlChanged(val url: String) : ConfigurationEvent()
    data class HostnameChanged(val hostname: String) : ConfigurationEvent()
    data object Save : ConfigurationEvent()
    data object ClearError : ConfigurationEvent()
}
```

### 2. LoginScreen

**Ubicación**: `presentation/login/`

Pantalla de inicio de sesión con botón de configuración.

**Características:**
- Header rojo con logo de MegaSuper
- Campos de código de usuario y contraseña
- Botón de login
- Botón de configuración
- Diseño responsive

### 3. HomeScreen

**Ubicación**: `presentation/home/`

Pantalla principal después del login (pendiente de implementar funcionalidades).

---

## 🎯 Patrón Resource para Manejo de Estados

```kotlin
sealed class Resource<T> {
    class Loading<T> : Resource<T>()
    class Success<T>(val data: T) : Resource<T>()
    class Error<T>(val message: String) : Resource<T>()
}
```

**Uso:**
```kotlin
loginUseCase(code, password).collect { result ->
    when (result) {
        is Resource.Loading -> {
            _state.update { it.copy(isLoading = true) }
        }
        is Resource.Success -> {
            _state.update { it.copy(isLoginSuccessful = true) }
        }
        is Resource.Error -> {
            _state.update { it.copy(error = result.message) }
        }
    }
}
```

---

## 📁 Estructura de Archivos del Proyecto

```
app/src/main/
├── java/com/devlosoft/megaposmobile/
│   ├── core/
│   │   └── common/
│   │       └── Constants.kt
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/
│   │   │   │   └── ServerConfigDao.kt
│   │   │   ├── database/
│   │   │   │   └── MegaPosDatabase.kt
│   │   │   ├── entity/
│   │   │   │   └── ServerConfigEntity.kt
│   │   │   └── preferences/
│   │   │       └── SessionManager.kt
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   │   └── AuthApi.kt
│   │   │   ├── dto/
│   │   │   │   ├── LoginRequestDto.kt
│   │   │   │   └── LoginResponseDto.kt
│   │   │   └── interceptor/
│   │   │       └── AuthInterceptor.kt
│   │   └── repository/
│   │       └── AuthRepositoryImpl.kt
│   ├── di/
│   │   ├── DatabaseModule.kt
│   │   ├── NetworkModule.kt
│   │   └── RepositoryModule.kt
│   ├── domain/
│   │   ├── model/
│   │   │   └── Token.kt
│   │   ├── repository/
│   │   │   └── AuthRepository.kt
│   │   └── usecase/
│   │       └── LoginUseCase.kt
│   ├── presentation/
│   │   ├── configuration/
│   │   │   ├── ConfigurationEvent.kt
│   │   │   ├── ConfigurationScreen.kt
│   │   │   ├── ConfigurationState.kt
│   │   │   └── ConfigurationViewModel.kt
│   │   ├── login/
│   │   │   ├── LoginEvent.kt
│   │   │   ├── LoginScreen.kt
│   │   │   ├── LoginState.kt
│   │   │   └── LoginViewModel.kt
│   │   ├── home/
│   │   │   └── HomeScreen.kt
│   │   └── navigation/
│   │       ├── NavGraph.kt
│   │       └── Screen.kt
│   ├── ui/
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Dimensions.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── MainActivity.kt
└── res/
    └── drawable/
        └── logo_megasuper.png  (tu logo aquí)
```

---

## 🚀 Cómo Agregar una Nueva Pantalla

### Ejemplo: Pantalla de Búsqueda de Clientes

#### Paso 1: Crear State & Events

```kotlin
// presentation/customer/CustomerState.kt
data class CustomerState(
    val searchId: String = "",
    val customers: List<Customer> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// presentation/customer/CustomerEvent.kt
sealed class CustomerEvent {
    data class SearchIdChanged(val id: String) : CustomerEvent()
    data object Search : CustomerEvent()
    data object ClearError : CustomerEvent()
}
```

#### Paso 2: Crear ViewModel

```kotlin
// presentation/customer/CustomerViewModel.kt
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val getCustomerUseCase: GetCustomerUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(CustomerState())
    val state: StateFlow<CustomerState> = _state.asStateFlow()

    fun onEvent(event: CustomerEvent) {
        when (event) {
            is CustomerEvent.SearchIdChanged -> {
                _state.update { it.copy(searchId = event.id) }
            }
            is CustomerEvent.Search -> searchCustomer()
            is CustomerEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun searchCustomer() {
        viewModelScope.launch {
            getCustomerUseCase(_state.value.searchId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                customers = result.data ?: emptyList(),
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                error = result.message,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }
}
```

#### Paso 3: Crear Screen

```kotlin
// presentation/customer/CustomerScreen.kt
@Composable
fun CustomerScreen(
    viewModel: CustomerViewModel = hiltViewModel(),
    onCustomerSelected: (Customer) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = dimensions.maxContentWidth)
            .padding(horizontal = dimensions.horizontalPadding)
    ) {
        OutlinedTextField(
            value = state.searchId,
            onValueChange = { viewModel.onEvent(CustomerEvent.SearchIdChanged(it)) },
            label = { Text("ID Cliente", fontSize = dimensions.fontSizeMedium) },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.textFieldHeight)
        )

        Spacer(modifier = Modifier.height(dimensions.spacerMedium))

        Button(
            onClick = { viewModel.onEvent(CustomerEvent.Search) },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight),
            enabled = !state.isLoading
        ) {
            Text("Buscar", fontSize = dimensions.fontSizeExtraLarge)
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        LazyColumn {
            items(state.customers) { customer ->
                CustomerItem(
                    customer = customer,
                    onClick = { onCustomerSelected(customer) }
                )
            }
        }
    }
}
```

#### Paso 4: Agregar a Navigation

```kotlin
// navigation/Screen.kt
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Configuration : Screen("configuration")
    data object Customer : Screen("customer")  // Nueva
}

// navigation/NavGraph.kt
composable(route = Screen.Customer.route) {
    CustomerScreen(
        onCustomerSelected = { customer ->
            // Navegar a otra pantalla
        }
    )
}
```

---

## 💡 Mejores Prácticas Implementadas

### 1. No Usar Valores Hardcodeados


✅ **Correcto:**
```kotlin
// Leer de base de datos
val config = serverConfigDao.getActiveServerConfigSync()
.baseUrl(config.serverUrl)
.header("x-Hostname", config.serverName)
```

### 2. Diseño Responsive

❌ **Incorrecto:**
```kotlin
Text(text = "Título", fontSize = 24.sp)
Spacer(modifier = Modifier.height(16.dp))
```

✅ **Correcto:**
```kotlin
val dimensions = LocalDimensions.current
Text(text = "Título", fontSize = dimensions.fontSizeTitle)
Spacer(modifier = Modifier.height(dimensions.spacerMedium))
```

### 3. Manejo de Errores Claro

```kotlin
// AuthInterceptor arroja errores descriptivos
throw IOException("Configuración del servidor no encontrada. Por favor configure la URL y el hostname en Configuración.")
```

### 4. Estados Inmutables

```kotlin
// Siempre usa .copy() para actualizar state
_state.update { it.copy(isLoading = true) }
// Nunca mutación directa
```

### 5. Separación de Responsabilidades

- **Screen**: Solo UI
- **ViewModel**: Lógica de UI + coordinación
- **UseCase**: Reglas de negocio
- **Repository**: Acceso a datos

---

## 🎓 Recursos de Configuración

### Constantes Importantes

```kotlin
// core/common/Constants.kt
object Constants {
    const val DATABASE_NAME = "megapos_database"
    const val PREFERENCES_NAME = "megapos_preferences"

    // DataStore keys
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_USER_CODE = "user_code"
    const val KEY_USER_NAME = "user_name"
    const val KEY_SESSION_ID = "session_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_SERVER_URL = "server_url"
}
```

### Assets y Recursos

**Logo de MegaSuper:**
- Ubicación: `res/drawable/logo_megasuper.png`
- Formato: PNG con fondo transparente
- Tamaño recomendado: 400x120px
- Se usa en el header de las pantallas

**Densidades opcionales:**
```
drawable-mdpi/logo_megasuper.png    (200x60px)
drawable-hdpi/logo_megasuper.png    (300x90px)
drawable-xhdpi/logo_megasuper.png   (400x120px)
drawable-xxhdpi/logo_megasuper.png  (600x180px)
drawable-xxxhdpi/logo_megasuper.png (800x240px)
```

---

## 🐛 Troubleshooting

### Error: "Configuración del servidor no encontrada"

**Causa**: No se ha configurado la URL y hostname

**Solución**:
1. Ir a la pantalla de Configuración
2. Ingresar URL del API (ej: `http://192.168.1.100:6060`)
3. Ingresar Hostname (ej: `android-pos-01`)
4. Guardar

### Error: "Cannot access database on main thread"

**Solución**: Siempre usar `suspend` functions
```kotlin
viewModelScope.launch {
    val config = serverConfigDao.getActiveServerConfigSync()
}
```

### Error: Logo no aparece

**Causa**: Imagen no está en la carpeta correcta

**Solución**:
1. Verificar que existe: `app/src/main/res/drawable/logo_megasuper.png`
2. Si usas XML placeholder, reemplázalo con PNG
3. Clean & Rebuild project

### UI no se adapta en tablet

**Causa**: No se está usando el sistema de dimensiones

**Solución**:
```kotlin
val dimensions = LocalDimensions.current
// Usar dimensions.* en lugar de valores fijos
```

---

## 📋 Checklist de Nueva Feature

- [ ] Crear modelo en `domain/model/`
- [ ] Crear DTO en `data/remote/dto/`
- [ ] Agregar endpoint a API interface
- [ ] Crear repository interface en `domain/repository/`
- [ ] Implementar repository en `data/repository/`
- [ ] Crear Use Case en `domain/usecase/`
- [ ] Registrar en módulos Hilt (`di/`)
- [ ] Crear State en `presentation/[feature]/`
- [ ] Crear Events en `presentation/[feature]/`
- [ ] Crear ViewModel en `presentation/[feature]/`
- [ ] Crear Screen con Compose usando `LocalDimensions`
- [ ] Agregar route a `Screen.kt`
- [ ] Agregar composable a `NavGraph.kt`
- [ ] Probar en phone y tablet

---

## 🎯 Próximos Pasos Recomendados

1. **Implementar más endpoints** del backend
2. **Agregar validación de campos** más robusta
3. **Implementar cache offline** con Room
4. **Agregar tests unitarios** para ViewModels y UseCases
5. **Implementar manejo de errores** más específico (códigos de error del backend)
6. **Agregar animaciones** y transiciones
7. **Implementar refresh de token** automático
8. **Agregar modo oscuro** (opcional)
9. **Implementar sincronización** en background

---

## 📚 Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Arquitectura**: Clean Architecture + MVVM
- **Inyección de Dependencias**: Hilt
- **Base de Datos**: Room
- **Preferencias**: DataStore
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines + Flow
- **State Management**: StateFlow
- **Navigation**: Compose Navigation

---

**¿Preguntas? Revisa este documento o consulta el código directamente en las rutas indicadas.**
