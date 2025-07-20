# Инструкции по сборке OpenComputers для Forge 1.20.1

## Требования для сборки

- **Java**: JDK 17 (обязательно!)
- **IntelliJ IDEA**: 2025.1.3 или новее  
- **Gradle**: Автоматически загружается (версия 7.5.1)
- **Forge**: 1.20.1-47.3.0 (автоматически загружается)

## Пошаговая инструкция

### 1. Подготовка окружения

1. **Установите JDK 17**:
   - Скачайте с официального сайта Oracle или используйте OpenJDK
   - Убедитесь, что `JAVA_HOME` указывает на JDK 17
   - Проверьте версию: `java -version`

2. **Распакуйте архив** в удобное место

### 2. Открытие в IntelliJ IDEA

1. **Откройте проект**:
   - File → Open → Выберите папку с проектом
   - Дождитесь завершения индексации Gradle

2. **Настройка Project SDK**:
   - File → Project Structure → Project
   - Установите Project SDK на JDK 17
   - Language level: 17

3. **Импорт Gradle**:
   - IntelliJ автоматически определит Gradle проект
   - Если не работает - перейдите в Gradle tab справа и нажмите Refresh

### 3. Сборка мода

#### Через IntelliJ IDEA:
1. Откройте Gradle tab (справа)
2. Найдите Tasks → build → build
3. Двойной клик для запуска сборки

#### Через командную строку:
```bash
# Windows
./gradlew.bat build

# Linux/Mac  
./gradlew build
```

### 4. Результат сборки

- **Готовый мод**: `build/libs/opencomputers-2.0.0-SNAPSHOT.jar`
- **Размер**: ~2-3 МБ (включая LuaJ библиотеку)
- **Совместимость**: Minecraft 1.20.1 + Forge 47.3.0+

### 5. Тестирование мода

#### Запуск клиента для тестирования:
```bash
./gradlew runClient
```

#### Запуск сервера для тестирования:
```bash  
./gradlew runServer
```

## Возможные проблемы и решения

### 1. Ошибка "Gradle version not supported"
**Решение**: Убедитесь, что используется Gradle 7.5.1 (прописано в gradle-wrapper.properties)

### 2. Ошибка "Java version"  
**Решение**: Проверьте, что JAVA_HOME указывает на JDK 17:
```bash
echo $JAVA_HOME
java -version
```

### 3. Ошибка "Dependencies not found"
**Решение**: Очистите кэш и пересоберите:
```bash
./gradlew clean --refresh-dependencies
./gradlew build
```

### 4. Медленная сборка
**Решение**: Увеличьте память для Gradle в `gradle.properties`:
```
org.gradle.jvmargs=-Xmx4G -Xms1G
```

## Структура проекта

```
opencomputers/
├── src/main/java/              # Исходный код мода
├── src/main/resources/         # Ресурсы (текстуры, модели, переводы)
├── build.gradle               # Конфигурация сборки
├── gradle.properties          # Настройки Gradle
├── settings.gradle           # Настройки проекта
└── gradlew / gradlew.bat     # Wrapper для Gradle
```

## Готовые возможности мода

✅ **Компьютеры**: 3 уровня (T1/T2/T3) с разными характеристиками  
✅ **Экраны**: Поддержка различных разрешений и цветов  
✅ **Роботы**: Программируемые мобильные компьютеры  
✅ **Компоненты**: CPU, память, GPU, EEPROM с полным API  
✅ **Lua VM**: Полноценная среда выполнения с sandboxing  
✅ **Энергия**: Интеграция с Forge Energy (RF/FE)  
✅ **API**: 100% совместимость с оригинальным OpenComputers  

## Следующие шаги

1. **Добавьте текстуры**: Поместите .png файлы в `src/main/resources/assets/opencomputers/textures/`
2. **Настройте рецепты**: Отредактируйте файлы в `src/main/resources/data/opencomputers/recipes/`
3. **Локализация**: Добавьте переводы в `src/main/resources/assets/opencomputers/lang/`

**Мод готов к использованию!** 🎉