package com.walley.app.data.remote

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DriveFile(val id: String, val name: String, val createdTime: String? = null)

@Serializable
private data class DriveFileListResponse(val files: List<DriveFile> = emptyList())

@Serializable
private data class DriveFileMetadata(val name: String, val parents: List<String> = listOf("appDataFolder"))

private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

/**
 * Minimal Google Drive REST v3 client scoped to the app's private `appDataFolder` — a per-app,
 * per-account space invisible in the user's normal Drive UI. Follows this app's existing
 * plain-HttpURLConnection convention (see [FrankfurterApi]) rather than pulling in Retrofit or
 * Google's own, much heavier, Drive API client library.
 */
class GoogleDriveApi @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listFiles(accessToken: String): List<DriveFile> = withContext(Dispatchers.IO) {
        val url = URL(
            "$DRIVE_FILES_URL?spaces=appDataFolder&fields=files(id,name,createdTime)" +
                "&orderBy=createdTime%20desc&pageSize=100"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<DriveFileListResponse>(body).files
        } finally {
            connection.disconnect()
        }
    }

    suspend fun uploadFile(accessToken: String, fileName: String, mimeType: String, content: ByteArray): DriveFile =
        withContext(Dispatchers.IO) {
            val boundary = "walley-${UUID.randomUUID()}"
            val metadataJson = json.encodeToString(DriveFileMetadata.serializer(), DriveFileMetadata(name = fileName))

            val body = ByteArrayOutputStream().apply {
                write("--$boundary\r\n".toByteArray())
                write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
                write(metadataJson.toByteArray())
                write("\r\n--$boundary\r\n".toByteArray())
                write(content)
                write("\r\n--$boundary--".toByteArray())
            }.toByteArray()

            val connection = (URL(DRIVE_UPLOAD_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                connectTimeout = 30_000
                readTimeout = 30_000
            }
            try {
                connection.outputStream.use { it.write(body) }
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString(DriveFile.serializer(), responseBody)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun downloadFile(accessToken: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val connection = (URL("$DRIVE_FILES_URL/$fileId?alt=media").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 30_000
            readTimeout = 30_000
        }
        try {
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun deleteFile(accessToken: String, fileId: String): Unit = withContext(Dispatchers.IO) {
        val connection = (URL("$DRIVE_FILES_URL/$fileId").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            connection.responseCode
            Unit
        } finally {
            connection.disconnect()
        }
    }
}
