# Lean Launcher
Лёгкий лаунчер для Android TV (без сети, без зависимостей).

## Сборка
Залейте проект на GitHub: вкладка Actions → Build APK → скачайте app-debug.apk.
Или откройте папку в Android Studio и нажмите Run.

## Установка
    adb connect <IP_ТВ>:5555
    adb install -r app-debug.apk
Нажмите Home и выберите «Lean Launcher» -> «Всегда».

Долгое нажатие OK на приложении открывает его настройки (удаление/остановка).

## Откат
Если нужно вернуть родной Home: Настройки -> Приложения -> Lean Launcher -> удалить,
либо `adb uninstall com.example.leanlauncher`.
