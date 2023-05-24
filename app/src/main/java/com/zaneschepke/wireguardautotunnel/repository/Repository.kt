package com.zaneschepke.wireguardautotunnel.repository

import kotlinx.coroutines.flow.Flow

interface Repository<T> {
    suspend fun save(t : T)
    suspend fun saveAll(t : List<T>)
    suspend fun getById(id : Long) : T?
    suspend fun getAll() : List<T>?
    suspend fun delete(t : T) : Boolean?
    suspend fun count() : Long?

    val itemFlow : Flow<MutableList<T>>

    fun init()
}