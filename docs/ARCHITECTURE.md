# Архитектура

Приложение использует MVVM и однонаправленный поток состояния.

```text
Compose UI -> ViewModel -> Repository -> Retrofit / Room / DataStore
                                  |
                                  +-> domain calculators and validators
```

- UI: Material 3, Navigation Compose, Canvas-графики.
- ViewModel: `StateFlow`, корутины, централизованные состояния загрузки/ошибки/офлайн.
- Data: Retrofit + Kotlin Serialization, Room, Preferences DataStore.
- Domain: модели, валидаторы поиска и калькулятор статистики без Android-зависимостей.
- DI: небольшой ручной контейнер приложения без тяжёлого DI-фреймворка.

Кэш каталога и просмотренных карточек работает по стратегии stale-while-revalidate. Избранное и статусы являются локальным источником истины. Аналитика обновляет только успешно полученные даты.

