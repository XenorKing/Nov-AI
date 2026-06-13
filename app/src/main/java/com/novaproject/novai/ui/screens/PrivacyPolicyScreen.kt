package com.novaproject.novai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaproject.novai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = NovDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Политика конфиденциальности",
                        color = NovTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = NovTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicySection(
                title = "1. Общие положения",
                body = "Настоящая Политика конфиденциальности описывает, как приложение NovAI (далее — «Приложение») собирает, использует и защищает ваши персональные данные. Используя Приложение, вы соглашаетесь с условиями данной Политики.\n\nПоследнее обновление: 12 июня 2026 г."
            )

            PolicySection(
                title = "2. Какие данные мы собираем",
                body = "— Адрес электронной почты и отображаемое имя (никнейм) при регистрации.\n" +
                       "— История переписки с ИИ-ассистентом в рамках диалогов.\n" +
                       "— Настройки параметров ИИ, сохранённые вами.\n" +
                       "— Технические данные: версия приложения, тип устройства, язык системы (анонимно).\n\n" +
                       "Мы не собираем реальное имя, номер телефона, геолокацию или платёжные данные."
            )

            PolicySection(
                title = "3. Как используются данные",
                body = "— Обеспечение работы функций авторизации и персонализации.\n" +
                       "— Хранение истории диалогов для удобства пользователя.\n" +
                       "— Передача сообщений ИИ-модели через защищённый прокси-сервер для получения ответов.\n" +
                       "— Анализ ошибок и улучшение стабильности Приложения.\n\n" +
                       "Ваши сообщения обрабатываются ИИ-моделью в режиме реального времени и не используются для обучения модели."
            )

            PolicySection(
                title = "4. Хранение данных",
                body = "Данные учётной записи и история диалогов хранятся в Firebase Firestore (Google LLC) на серверах в соответствии с политиками Google Cloud.\n\n" +
                       "Вы можете удалить историю переписки непосредственно в Приложении. При удалении учётной записи все связанные данные удаляются в течение 30 дней."
            )

            PolicySection(
                title = "5. Передача данных третьим лицам",
                body = "Мы не продаём и не передаём ваши персональные данные третьим лицам в коммерческих целях.\n\n" +
                       "Ваши сообщения передаются ИИ-модели через защищённый прокси-сервер исключительно для формирования ответа. Данные не сохраняются на стороне прокси после обработки запроса."
            )

            PolicySection(
                title = "6. Безопасность",
                body = "Мы применяем технические и организационные меры для защиты ваших данных:\n" +
                       "— Все соединения используют протокол HTTPS/TLS.\n" +
                       "— Аутентификация осуществляется через Firebase Authentication.\n" +
                       "— Правила доступа к базе данных ограничивают просмотр данных только владельцем аккаунта.\n\n" +
                       "Несмотря на принимаемые меры, ни одна система не обеспечивает абсолютную защиту."
            )

            PolicySection(
                title = "7. Права пользователя",
                body = "Вы имеете право:\n" +
                       "— Получить информацию о хранящихся данных.\n" +
                       "— Изменить отображаемое имя в разделе «Профиль».\n" +
                       "— Удалить отдельные диалоги или всю историю переписки.\n" +
                       "— Удалить аккаунт, что повлечёт удаление всех связанных данных.\n\n" +
                       "По вопросам обработки данных обращайтесь: novaprojecthelp@mail.ru"
            )

            PolicySection(
                title = "8. Использование детьми",
                body = "Мы не собираем намеренно персональные данные лиц младше 13 лет. Если вам стало известно, что несовершеннолетний зарегистрировался в Приложении, свяжитесь с нами по адресу novaprojecthelp@mail.ru — мы незамедлительно удалим аккаунт."
            )

            PolicySection(
                title = "9. Изменения политики",
                body = "Мы можем периодически обновлять данную Политику конфиденциальности. О существенных изменениях вы будете уведомлены через Приложение или по email.\n\n" +
                       "Дата вступления в силу актуальной версии всегда указана в начале документа. Продолжение использования Приложения после обновления означает ваше согласие с новой редакцией."
            )

            // Contacts section with clickable links
            Card(
                colors = CardDefaults.cardColors(containerColor = NovCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "10. Контакты",
                        color = NovCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Если у вас есть вопросы, жалобы или запросы, связанные с конфиденциальностью:",
                        color = NovTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(14.dp))

                    ContactLink(
                        icon = Icons.Default.AlternateEmail,
                        label = "Email",
                        value = "novaprojecthelp@mail.ru",
                        onClick = { uriHandler.openUri("mailto:novaprojecthelp@mail.ru") }
                    )
                    Spacer(Modifier.height(10.dp))
                    ContactLink(
                        icon = Icons.AutoMirrored.Filled.Send,
                        label = "Telegram",
                        value = "@NovaProjectNews",
                        onClick = { uriHandler.openUri("https://t.me/NovaProjectNews") }
                    )
                    Spacer(Modifier.height(10.dp))
                    ContactLink(
                        icon = Icons.Default.Link,
                        label = "ВКонтакте",
                        value = "vk.ru/club238958808",
                        onClick = { uriHandler.openUri("https://vk.ru/club238958808") }
                    )
                }
            }

            // Footer
            Text(
                text = "Nova Project, 2026. Все права защищены.",
                color = NovTextSecondary.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun ContactLink(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = NovCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = NovTextSecondary, fontSize = 11.sp)
            Text(value, color = NovTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NovCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                color = NovCyan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                color = NovTextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
