@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "BACKEND_PORT_START=18082"
set "FRONTEND_PORT_START=5174"
set "DEMO_STATION_CODE=1-A6501-C001-S001"
set "BACKEND_PORT=%BACKEND_PORT_START%"
set "FRONTEND_PORT=%FRONTEND_PORT_START%"
set "DB_URL=jdbc:postgresql://localhost:5432/cnpc_promotion"
set "DB_USERNAME=cnpc"
set "DB_PASSWORD=cnpc"
set "POSTGRES_DB=cnpc_promotion"
set "POSTGRES_USER=cnpc"
set "POSTGRES_PASSWORD=cnpc"
set "POSTGRES_PORT=5432"

echo ========================================
echo CNPC Smart Retail - start all
echo ========================================
echo Root: %ROOT%
echo.

call :require_cmd mvn Maven
if errorlevel 1 goto fail

call :require_cmd npm Node.js/npm
if errorlevel 1 goto fail

if not exist "%BACKEND_DIR%\pom.xml" (
  echo [ERROR] Backend pom.xml not found: %BACKEND_DIR%\pom.xml
  goto fail
)

if not exist "%FRONTEND_DIR%\package.json" (
  echo [ERROR] Frontend package.json not found: %FRONTEND_DIR%\package.json
  goto fail
)

where docker >nul 2>nul
if errorlevel 1 (
  echo [WARN] Docker was not found. PostgreSQL auto-start is skipped.
  echo [WARN] If backend fails, start PostgreSQL manually first.
) else (
  echo [1/4] Starting PostgreSQL with docker compose...
  if "%DRY_RUN%"=="1" (
    echo DRY_RUN docker compose -f "%ROOT%docker-compose.yml" up -d postgres
  ) else (
    docker compose -f "%ROOT%docker-compose.yml" up -d postgres
    if errorlevel 1 (
      echo [WARN] Docker compose failed. Check Docker Desktop.
    ) else (
      call :wait_postgres
    )
  )
)

echo.
echo [2/4] Resolving backend service...
set "BACKEND_PORT="
for /l %%p in (%BACKEND_PORT_START%,1,18120) do (
  if not defined BACKEND_PORT (
    netstat -ano | findstr /R /C:":%%p .*LISTENING" >nul 2>nul
    if not errorlevel 1 (
      call :backend_compatible %%p
      if not errorlevel 1 set "BACKEND_PORT=%%p"
    )
  )
)
if defined BACKEND_PORT (
  set "REUSE_BACKEND=1"
  echo [INFO] Compatible checkout backend found on port !BACKEND_PORT!.
) else (
  call :port_in_use %BACKEND_PORT_START%
  if not errorlevel 1 (
    echo [WARN] Port %BACKEND_PORT_START% is occupied by an old or incompatible service.
    echo [WARN] A new backend will be started on another port.
  )
  set "BACKEND_PORT="
  for /l %%p in (%BACKEND_PORT_START%,1,18120) do (
    if not defined BACKEND_PORT (
      netstat -ano | findstr /R /C:":%%p .*LISTENING" >nul 2>nul
      if errorlevel 1 set "BACKEND_PORT=%%p"
    )
  )
  if not defined BACKEND_PORT (
    echo [ERROR] No free backend port found in range %BACKEND_PORT_START%-18120.
    goto fail
  )
)

echo.
echo [INFO] Cleaning stale frontend instances from this project...
if "%DRY_RUN%"=="1" (
  echo DRY_RUN stop project Vite listeners on ports 5173-5199
) else (
  call :cleanup_frontends
)

set "FRONTEND_PORT=%FRONTEND_PORT_START%"
if not "%DRY_RUN%"=="1" (
  call :port_in_use %FRONTEND_PORT%
  if not errorlevel 1 (
    echo [ERROR] Customer demo port %FRONTEND_PORT% is occupied by another program.
    echo [ERROR] Close that program, then run start-all.bat again.
    goto fail
  )
)

set "BACKEND_URL=http://127.0.0.1:%BACKEND_PORT%"
set "FRONTEND_URL=http://127.0.0.1:%FRONTEND_PORT%/operation-campaigns"
set "CHECKOUT_URL=http://127.0.0.1:%FRONTEND_PORT%/checkout"
set "FRONTEND_API_URL=http://127.0.0.1:%FRONTEND_PORT%/api/checkout/capabilities"

echo Backend:  %BACKEND_URL%
echo Demo:     %FRONTEND_URL%
echo Checkout: %CHECKOUT_URL%

if "%REUSE_BACKEND%"=="1" goto after_backend
echo.
echo [2/4] Launching backend window on port %BACKEND_PORT%...
if "%DRY_RUN%"=="1" (
  echo DRY_RUN start backend in "%BACKEND_DIR%" on port %BACKEND_PORT%
) else (
  start "CNPC Backend :%BACKEND_PORT%" /D "%BACKEND_DIR%" cmd /k "set DB_URL=%DB_URL%&& set DB_USERNAME=%DB_USERNAME%&& set DB_PASSWORD=%DB_PASSWORD%&& call mvn -DskipTests -Dspring-boot.run.profiles=dev-db -Dspring-boot.run.arguments=--server.port=%BACKEND_PORT% spring-boot:run"
  call :wait_backend
  if errorlevel 1 (
    echo [ERROR] Backend did not become checkout-ready.
    echo [ERROR] Check the CNPC Backend window for the startup error.
    goto fail
  )
)
:after_backend

if not exist "%FRONTEND_DIR%\node_modules" (
  echo.
  echo [3/4] Installing frontend dependencies...
  if "%DRY_RUN%"=="1" (
    echo DRY_RUN npm install in "%FRONTEND_DIR%"
  ) else (
    pushd "%FRONTEND_DIR%"
    call npm install
    set "NPM_INSTALL_ERROR=!ERRORLEVEL!"
    popd
    if not "!NPM_INSTALL_ERROR!"=="0" (
      echo [ERROR] npm install failed.
      goto fail
    )
  )
)

echo.
echo [3/4] Launching frontend window on port %FRONTEND_PORT%...
echo [INFO] Frontend proxy target: %BACKEND_URL%
if "%DRY_RUN%"=="1" (
  echo DRY_RUN start frontend in "%FRONTEND_DIR%" on port %FRONTEND_PORT%
) else (
  start "CNPC Frontend :%FRONTEND_PORT%" /D "%FRONTEND_DIR%" cmd /k "set VITE_BACKEND_URL=%BACKEND_URL%&& call npm run dev -- --host 0.0.0.0 --port %FRONTEND_PORT% --strictPort"
)

echo.
echo [4/4] Verifying frontend and checkout proxy...
call :open_frontend
if errorlevel 1 goto fail

echo.
echo Start command finished.
echo Customer demo: %FRONTEND_URL%
echo Checkout:      %CHECKOUT_URL%
echo Health:       %BACKEND_URL%/actuator/health
echo Capabilities: %BACKEND_URL%/api/checkout/capabilities
echo.
echo Close services by closing the backend/frontend command windows.
if not "%NO_PAUSE%"=="1" pause
exit /b 0

:require_cmd
where %1 >nul 2>nul
if errorlevel 1 (
  echo [ERROR] %2 command not found. Please install it and add it to PATH.
  exit /b 1
)
exit /b 0

:port_in_use
netstat -ano | findstr /R /C:":%1 .*LISTENING" >nul 2>nul
if errorlevel 1 exit /b 1
exit /b 0

:backend_compatible
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-RestMethod -TimeoutSec 2 'http://127.0.0.1:%1/api/checkout/capabilities'; if ($r.success -and $r.data.service -eq 'cnpc-promotion-retail' -and $r.data.apiVersion -eq 'checkout-v2' -and $r.data.calculate -and $r.data.confirm) { exit 0 } } catch {}; exit 1" >nul 2>nul
if errorlevel 1 exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-RestMethod -TimeoutSec 2 'http://127.0.0.1:%1/api/stations/%DEMO_STATION_CODE%'; if ($r.success -and $r.data.stationCode -eq '%DEMO_STATION_CODE%') { exit 0 } } catch {}; exit 1" >nul 2>nul
exit /b %ERRORLEVEL%

:cleanup_frontends
powershell -NoProfile -ExecutionPolicy Bypass -Command "$root = [IO.Path]::GetFullPath('%FRONTEND_DIR%').TrimEnd('\'); $listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -ge 5173 -and $_.LocalPort -le 5199 }; foreach ($listener in $listeners) { $process = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $listener.OwningProcess) -ErrorAction SilentlyContinue; if ($process -and $process.Name -eq 'node.exe' -and $process.CommandLine -like ('*' + $root + '*vite*')) { Write-Host ('[INFO] Stopping stale frontend on port ' + $listener.LocalPort + ' (PID ' + $listener.OwningProcess + ')'); Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue } }"
exit /b 0

:wait_postgres
echo [1/4] Waiting for PostgreSQL healthcheck...
set "PG_HEALTH="
for /l %%i in (1,1,30) do (
  for /f "delims=" %%s in ('docker inspect -f "{{.State.Health.Status}}" cnpc-promotion-postgres 2^>nul') do set "PG_HEALTH=%%s"
  if "!PG_HEALTH!"=="healthy" (
    echo [1/4] PostgreSQL is healthy.
    exit /b 0
  )
  timeout /t 2 /nobreak >nul
)
echo [WARN] PostgreSQL did not become healthy within 60 seconds. Backend will still launch.
exit /b 0

:wait_backend
echo [2/4] Waiting for checkout backend on port %BACKEND_PORT%...
for /l %%i in (1,1,60) do (
  call :backend_compatible %BACKEND_PORT%
  if not errorlevel 1 (
    echo [2/4] Checkout backend is ready.
    exit /b 0
  )
  timeout /t 2 /nobreak >nul
)
exit /b 1

:open_frontend
if "%DRY_RUN%"=="1" (
  echo DRY_RUN verify and open "%FRONTEND_URL%"
  exit /b 0
)
echo Waiting for customer demo page and checkout proxy...
for /l %%i in (1,1,60) do (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $page = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 '%FRONTEND_URL%'; $api = Invoke-RestMethod -TimeoutSec 2 '%FRONTEND_API_URL%'; if ($page.StatusCode -eq 200 -and $api.success -and $api.data.apiVersion -eq 'checkout-v2') { exit 0 } } catch {}; exit 1" >nul 2>nul
  if not errorlevel 1 (
    echo Frontend and checkout proxy are ready.
    if "%NO_OPEN%"=="1" (
      echo Browser auto-open is disabled. Open manually: %FRONTEND_URL%
    ) else (
      echo Opening browser: %FRONTEND_URL%
      start "" "%FRONTEND_URL%"
    )
    exit /b 0
  )
  timeout /t 1 /nobreak >nul
)
echo [ERROR] Frontend or checkout proxy was not ready within 60 seconds.
echo [ERROR] Check the CNPC Frontend window and backend port mapping.
exit /b 1

:fail
echo.
echo Startup failed. See the message above.
if not "%NO_PAUSE%"=="1" pause
exit /b 1
