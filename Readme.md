# 📍 YourcClue - Приложение для геолокационных заметок

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-blue)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-SDK-green)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4)](https://developer.android.com/jetpack/compose)
[![MapLibre](https://img.shields.io/badge/MapLibre-GS-blue)](https://maplibre.org/)

**YourcClue** - это приложение для Android с использованием современных технологий, позволяющее пользователям создавать заметки на географической карте, устанавливать связи между ними и управлять ими. Приложение объединяет геолокационные данные с функциями управления заметками для наглядного представления информации.

## 🚀 Возможности

### 🗺️ **Карта и геолокация**
- Интерактивная карта с поддержкой стилей MapLibre
- Определение местоположения пользователя в реальном времени
- Масштабирование и навигация по карте
- Пометки заметок на карте с цветовой кодировкой

### 📝 **Управление заметками**
- Создание заметок в произвольных точках на карте
- Редактирование и удаление существующих заметок
- Прикрепление иконок к заметкам
- Привязка дат к заметкам с цветовой индикацией

### 🔗 **Связывание заметок**
- Возможность создавать связи между заметками
- Визуализация связей линиями на карте
- Градиентное окрашивание связей на основе дат
- Управление связями через интерфейс приложения

### 🎨 **Современный UI/UX**
- Современный интерфейс с Material Design
- Адаптивный дизайн под мобильные устройства
- Попап-окна для редактирования заметок
- Размытие фона для улучшения фокусировки

### 📅 **Управление данными**
- Сохранение заметок локально в формате JSON
- Календарь для выбора даты привязки заметки
- Сортировка заметок по датам
- Фильтрация заметок по календарю

### 🔍 **Поиск**
- Поиск заметок по тексту и датам
- Визуальное выделение найденных заметок
- Не найденные заметки отображаются с пониженной прозрачностью

## 🛠️ Технологический стек

### Android (Kotlin)
- **Фреймворк:** [Jetpack Compose](https://developer.android.com/jetpack/compose) для современного UI
- **Язык:** [Kotlin](https://kotlinlang.org/) с поддержкой Coroutines
- **Навигация:** [Navigation Component](https://developer.android.com/guide/navigation)
- **Хранение данных:** Локальное хранилище JSON с использованием Gson
- **Геолокация:** [Google Play Services Location](https://developers.google.com/location-context/location)

### Карта (MapLibre)
- **Библиотека:** [MapLibre Android SDK](https://maplibre.org/)
- **Стили карты:** Поддержка MapTiler стилей
- **Маркеры:** Пользовательские иконки и цвета для заметок
- **Геометрия:** Работа с точками и линиями на карте

### Дополнительные библиотеки
- **UI компоненты:** [Material Components](https://github.com/material-components/material-components-android)
- **JSON:** [Gson](https://github.com/google/gson) для сериализации
- **Дата/время:** Встроенная поддержка Kotlin для работы с датами

## 📁 Структура проекта

```
YourcClue/
├── app/                    # Основное приложение Android
│   ├── src/main/
│   │   ├── java/com/example/tstproj/ # Исходный код Kotlin
│   │   │   ├── MainActivity.kt      # Основное активити с картой
│   │   │   ├── Note.kt             # Модель заметки
│   │   │   ├── CreateNote.kt       # Компонент создания заметки
│   │   │   ├── JsonStorage.kt      # Хранилище JSON
│   │   │   ├── ColorUtils.kt       # Утилиты для цвета
│   │   │   ├── IconAdapter.kt      # Адаптер иконок
│   │   │   └── ui/                 # Компоненты интерфейса
│   │   ├── res/                   # Ресурсы (стили, изображения, макеты)
│   │   └── AndroidManifest.xml    # Манифест приложения
├── build.gradle.kts        # Сборка Gradle для модуля app
├── settings.gradle.kts     # Конфигурация Gradle для проекта
├── gradle.properties       # Свойства Gradle
└── docs/                  # Документация проекта
```

## 🚀 Быстрый старт

### Предварительные требования

- Android Studio Flamingo или новее
- Android SDK API level 24+
- Kotlin 2.0+
- Gradle 8.0+
- MAPTILER_API_KEY (для работы с картами)

### 1. Клонирование репозитория

```bash
git clone <repository-url>
cd YourcClue
```

### 2. Установка API-ключа MapTiler

1. Зарегистрируйтесь на [MapTiler](https://cloud.maptiler.com/)
2. Получите бесплатный API-ключ
3. Добавьте в `local.properties`:
```properties
MAPTILER_API_KEY=ваш_ключ_api_здесь
```

### 3. Сборка и запуск

1. Откройте проект в Android Studio
2. Синхронизируйте проект с Gradle
3. Подключите физическое устройство или создайте эмулятор
4. Нажмите "Run" или выполните в терминале:
```bash
./gradlew installDebug
```

### 4. Использование приложения

1. Откройте приложение на устройстве
2. Разрешите доступ к геолокации
3. Нажмите на карту, чтобы установить маркер
4. Введите текст заметки и сохраните
5. Используйте долгое нажатие для редактирования заметок
6. Свяжите заметки между собой с помощью опции "Связать заметку"
7. Используйте панель поиска вверху для поиска заметок по содержанию или дате

## 📚 Документация API

### Функции приложения

#### MainActivity
- `onMapReady()` - Инициализация карты и установка стиля
- `addMarker()` - Добавление маркера на карту с цветовой кодировкой
- `showMarkersAndLinks()` - Отображение всех заметок и связей
- `drawLineBetweenNotes()` - Отрисовка линии между связанными заметками
- `saveNote()` - Сохранение заметки в локальное хранилище
- `showInfoWindow()` - Показ информационного окна для заметки
- `performSearch()` - Выполнение поиска по заметкам
- `applyAlphaToColor()` - Применение прозрачности к цвету

#### JsonStorage
- `readJsonFromFile()` - Чтение JSON-данных из файла
- `writeJsonToFile()` - Запись JSON-данных в файл
- `loadNotes()` - Загрузка списка заметок

#### ColorUtils
- `getColorForDate()` - Получение цвета на основе даты заметки

## 🐳 Docker (для тестирования)

В настоящее время приложение не использует Docker, но вы можете запустить эмулятор Android в Docker:

```bash
docker run --rm -it -p 5555:5555 -v /tmp/.X11-unix:/tmp/.X11-unix -e DISPLAY=$DISPLAY ReactiveCircus/android-emulator:11.0
```

## 🧪 Тестирование

### Тесты приложения

```bash
# Запустить локальные тесты
./gradlew test

# Запустить инструментальные тесты
./gradlew connectedAndroidTest

# Посмотреть покрытие кода
./gradlew createDebugCoverageReport
```

## 📝 Разработка

### Настройка среды разработки

1. Установите Android Studio последней версии
2. Установите JDK 11 или выше
3. Клонируйте репозиторий
4. Откройте проект в Android Studio
5. Добавьте MAPTILER_API_KEY в `local.properties`

### Запуск в режиме разработки

1. Откройте проект в Android Studio
2. Выберите устройство для запуска (эмулятор или физическое устройство)
3. Нажмите "Run" или выполните `./gradlew installDebug`

### Форматирование кода

```bash
# Форматирование кода Kotlin
./gradlew ktlintFormat

# Проверка стиля кода
./gradlew ktlintCheck

# Форматирование с помощью Spotless
./gradlew spotlessApply
```

## 🔧 Конфигурация

### Конфигурация приложения

Измените `app/local.properties`:

```properties
MAPTILER_API_KEY=ваш_ключ_api_здесь
```

### Сборка релиза

```bash
# Сборка APK релиза
./gradlew assembleRelease

# Подпись APK (требуется ключ)
./gradlew assembleRelease -Pandroid.injected.signing.store.file=путь_к_кеystore -Pandroid.injected.signing.store.password=пароль -Pandroid.injected.signing.key.alias=алиас -Pandroid.injected.signing.key.password=пароль
```

## 📄 Документация

- **Техническое задание:** [SRS](./docs/SRS.md)
- **Дизайн-документ:** [Design](./docs/DESIGN.md)
- **Диаграммы классов:** [Class Diagrams](./docs/CLASS_DIAGRAMS.md)
- **Диаграммы развертывания:** [Deployment Diagrams](./docs/DEPLOYMENT_DIAGRAMS.md)
- **Описание состояний приложения:** [App States](./docs/app_states.md)

## 🤝 Сотрудничество

1. Сделайте форк репозитория
2. Создайте ветку для новой функции (`git checkout -b feature/AmazingFeature`)
3. Зафиксируйте изменения (`git commit -m 'Add amazing feature'`)
4. Отправьте в ветку (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## 📝 Лицензия

Этот проект распространяется под лицензией MIT. См. файл [LICENSE](./LICENSE) для получения дополнительной информации.

## 🙏 Благодарности

- [MapLibre](https://maplibre.org/) - Библиотека для интерактивных карт
- [Android Jetpack](https://developer.android.com/jetpack) - Компоненты для разработки Android
- [Kotlin](https://kotlinlang.org/) - Язык программирования
- [Material Design](https://material.io/) - Дизайн-система
- [Gson](https://github.com/google/gson) - Библиотека для работы с JSON

## 🔮 Планируемые улучшения

- [ ] Интеграция облачного хранилища (Firebase)
- [ ] Синхронизация данных между устройствами
- [ ] Поддержка оффлайн-режима
- [ ] Аналитика использования
- [ ] Экспорт заметок в различные форматы (PDF, GPX)
- [ ] Поддержка вложения изображений к заметкам
- [ ] Совместная работа над заметками
- [ ] Поддержка 3D режима карты
- [ ] Импорт данных из других приложений
- [ ] Продвинутые фильтры и поисковые функции