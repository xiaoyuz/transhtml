package com.xiaoyuz.transhtml

import com.huaban.analysis.jieba.JiebaSegmenter
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.util.StringUtils

private const val DEPTH = 6
private const val LIMIT_COUNT = 100
private const val HEAD_EMPTY_LINES = 1
private const val END_LIMIT_CHAR_COUNT = 20

enum class ContentType {
    NONE, P, IMG
}

data class ContentNode(var type: ContentType = ContentType.NONE, var value: String = "")

data class Article(var title: String? = null, var content: String? = null, var contentWithTags: String? = null,
                   var publishTime: Long? = null) {
    constructor(url: String): this() {
        val doc = Jsoup.connect(url).get()
        val body = doc.body()
        body.select("script").remove()
        body.select("style").remove()
        title = doc.title()
        getContent(body)
        val a = 0
    }

    private fun getContent(body: Element) {
        val bodyHtmlText = body.html().replace("""(?is)<!--.*?-->""".toRegex(), "")
        val orgLines = bodyHtmlText.split("\n")
        val lines = orgLines.map {
            it.replace("""(?is)</p>|<br.*?/>""".toRegex(), "[crlf]")
                    .replace("""(?is)<.*?>""".toRegex(), "").trim()
        }
        val sb = StringBuilder()
        val orgSb = StringBuilder()

        val contentInfos = mutableListOf<String>()
        val contentType = mutableListOf<ContentNode>()

        var preTextLen = 0
        var startPos = -1
        val jiebaSegmenter = JiebaSegmenter()
        for (i in 0 until lines.size - DEPTH) {
            var len = 0
            for (j in 0 until DEPTH) {
                len += jiebaSegmenter.sentenceProcess(lines[i + j]).size
            }
            if (startPos == -1) {
                if (preTextLen > LIMIT_COUNT && len > 0) {
                    var emptyCount = 0
                    for (j in i - 1 downTo 1) {
                        if (StringUtils.isEmpty(lines[j])) emptyCount++ else emptyCount = 0
                        if (emptyCount == HEAD_EMPTY_LINES) {
                            startPos = j + HEAD_EMPTY_LINES
                            break
                        }
                    }
                    if (startPos == -1) startPos = i
                    for (j in startPos..i) {
                        orgSb.append(orgLines[j])
                        contentInfos.add(lines[j])
                        val lineNode = Jsoup.parse(orgLines[j])
                        contentType.add(when {
                            lineNode.select("img").isNotEmpty() -> ContentNode(ContentType.IMG,
                                    lineNode.select("img").attr("src"))
                            lineNode.select("p").isNotEmpty() -> ContentNode(ContentType.P, "")
                            else -> ContentNode()
                        })
                    }
                }
            } else {
                if (len <= END_LIMIT_CHAR_COUNT && preTextLen < END_LIMIT_CHAR_COUNT) {
                    startPos = -1
                }
                orgSb.append(orgLines[i])
                contentInfos.add(lines[i])
                val lineNode = Jsoup.parse(orgLines[i])
                contentType.add(when {
                    lineNode.select("img").isNotEmpty() -> ContentNode(ContentType.IMG,
                            lineNode.select("img").attr("src"))
                    lineNode.select("p").isNotEmpty() -> ContentNode(ContentType.P, "")
                    else -> ContentNode()
                })
            }
            preTextLen = len
        }
        contentInfos.forEachIndexed { index, s ->
            if (contentType[index].type == ContentType.P) {
                sb.append(s)
            } else if (contentType[index].type == ContentType.IMG) {
                sb.append("[crlf]")
                sb.append(JSONObject(contentType[index]).toString())
                sb.append("[crlf]")
            }
        }
        val result = sb.toString()
        content = result.replace("[crlf]", "\n")
        contentWithTags = orgSb.toString()
    }
}