package com.sergo379.gamedex.data

import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException

object ApiErrorMapper {
    fun message(error: Throwable): String = when (error) {
        is SocketTimeoutException -> "Сервер не ответил вовремя. Попробуйте ещё раз."
        is HttpException -> when (error.code()) {
            401, 403 -> "RAWG отклонил API-ключ. Проверьте ключ в настройках."
            429 -> "Лимит запросов RAWG исчерпан. Повторите позже."
            in 500..599 -> "Сервис RAWG временно недоступен."
            else -> "Не удалось получить данные (HTTP ${error.code()})."
        }
        is IOException -> "Нет подключения к интернету. Показываем сохранённые данные, если они есть."
        else -> "Не удалось обработать ответ источника данных."
    }
}

