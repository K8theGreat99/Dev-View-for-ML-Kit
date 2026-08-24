package com.k8thegreat.devview.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {

    @Query("SELECT * FROM samples ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples WHERE id = :id")
    fun observeById(id: String): Flow<SampleEntity?>

    @Query("SELECT * FROM samples WHERE id = :id")
    suspend fun findById(id: String): SampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: SampleEntity)

    @Query("DELETE FROM samples WHERE id = :id")
    suspend fun deleteById(id: String)
}
