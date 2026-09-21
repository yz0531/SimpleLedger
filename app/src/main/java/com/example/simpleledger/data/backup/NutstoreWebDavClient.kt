package com.example.simpleledger.data.backup

import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl

class NutstoreBackupException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

class NutstoreWebDavClient(
    credentials: NutstoreCredentials,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val authorization = Credentials.basic(
        credentials.username,
        credentials.password,
        Charsets.UTF_8,
    )
    private val baseUrl = BASE_URL.toHttpUrl()

    fun ensureBackupDirectory() {
        ensureCollection(appFolderUrl(), "创建应用目录失败")
        ensureCollection(backupFolderUrl(), "创建 backup 目录失败")
    }

    private fun ensureCollection(url: okhttp3.HttpUrl, action: String) {
        val request = authenticatedRequest(url)
            .method("MKCOL", null)
            .build()
        execute(request).use { response ->
            if (response.code in setOf(200, 201, 204, 405)) return
            throw response.toBackupException(action)
        }
    }

    fun listBackupFiles(): List<String> {
        val body = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:"><d:prop><d:displayname/></d:prop></d:propfind>
        """.trimIndent().toRequestBody(XML_MEDIA_TYPE)
        val request = authenticatedRequest(backupFolderUrl())
            .header("Depth", "1")
            .method("PROPFIND", body)
            .build()
        execute(request).use { response ->
            if (response.code != 207) throw response.toBackupException("读取云端备份列表失败")
            return WebDavListingParser.fileNames(response.body?.string().orEmpty())
        }
    }

    fun upload(fileName: String, bytes: ByteArray) {
        require(isSafeFileName(fileName)) { "备份文件名无效" }
        val request = authenticatedRequest(fileUrl(fileName))
            .put(bytes.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request).use { response ->
            if (response.code !in 200..299) throw response.toBackupException("上传坚果云备份失败")
        }
    }

    fun delete(fileName: String) {
        require(isSafeFileName(fileName)) { "备份文件名无效" }
        val request = authenticatedRequest(fileUrl(fileName))
            .delete()
            .build()
        execute(request).use { response ->
            if (response.code !in 200..299 && response.code != 404) {
                throw response.toBackupException("清理旧备份失败")
            }
        }
    }

    fun download(fileName: String): ByteArray {
        require(isSafeFileName(fileName)) { "备份文件名无效" }
        val request = authenticatedRequest(fileUrl(fileName)).get().build()
        execute(request).use { response ->
            if (response.code !in 200..299) throw response.toBackupException("下载坚果云备份失败")
            return response.body?.bytes() ?: throw NutstoreBackupException("云端备份内容为空")
        }
    }

    private fun authenticatedRequest(url: okhttp3.HttpUrl): Request.Builder = Request.Builder()
        .url(url)
        .header("Authorization", authorization)
        .header("User-Agent", "SimpleLedger-Android/1.0")

    private fun appFolderUrl() = baseUrl.newBuilder()
        .addPathSegment(APP_FOLDER)
        .addPathSegment("")
        .build()

    private fun backupFolderUrl() = baseUrl.newBuilder()
        .addPathSegment(APP_FOLDER)
        .addPathSegment(BACKUP_FOLDER)
        .addPathSegment("")
        .build()

    private fun fileUrl(fileName: String) = baseUrl.newBuilder()
        .addPathSegment(APP_FOLDER)
        .addPathSegment(BACKUP_FOLDER)
        .addPathSegment(fileName)
        .build()

    private fun execute(request: Request): Response = try {
        client.newCall(request).execute()
    } catch (exception: IOException) {
        throw NutstoreBackupException("无法连接坚果云，请检查网络后重试", exception)
    }

    private fun Response.toBackupException(action: String): NutstoreBackupException {
        val detail = when (code) {
            401, 403 -> "账号或第三方应用密码不正确"
            404 -> "坚果云 WebDAV 地址不可用"
            409 -> "云端目录状态异常，请稍后重试"
            429 -> "坚果云请求过于频繁，请稍后重试"
            in 500..599 -> "坚果云服务暂时不可用（HTTP $code）"
            else -> "HTTP $code"
        }
        return NutstoreBackupException("$action：$detail")
    }

    private fun isSafeFileName(fileName: String): Boolean =
        fileName.isNotBlank() && fileName.none { it == '/' || it == '\\' || it.isISOControl() }

    private companion object {
        const val BASE_URL = "https://dav.jianguoyun.com/dav/"
        const val APP_FOLDER = "SimpleLedger"
        const val BACKUP_FOLDER = "backup"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
    }
}

internal object WebDavListingParser {
    private val hrefRegex = Regex(
        pattern = """<(?:[A-Za-z0-9_-]+:)?href[^>]*>(.*?)</(?:[A-Za-z0-9_-]+:)?href>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun fileNames(xml: String): List<String> = hrefRegex.findAll(xml)
        .mapNotNull { match ->
            val href = match.groupValues[1]
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim()
            val path = runCatching { URI(href).rawPath }.getOrNull() ?: href
            val trimmed = path.trimEnd('/')
            val rawName = trimmed.substringAfterLast('/', missingDelimiterValue = "")
            if (rawName.isEmpty()) null else runCatching {
                URLDecoder.decode(rawName, Charsets.UTF_8.name())
            }.getOrNull()
        }
        .filter { it.endsWith(".json", ignoreCase = true) }
        .distinct()
        .toList()
}
