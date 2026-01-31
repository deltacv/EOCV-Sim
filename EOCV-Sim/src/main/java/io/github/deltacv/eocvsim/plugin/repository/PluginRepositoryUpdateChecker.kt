/*
 * Copyright (c) 2024 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.deltacv.eocvsim.plugin.repository

import io.github.deltacv.common.util.loggerOf
import io.github.deltacv.common.util.ParsedVersion
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

private val logger by loggerOf("PluginRepositoryUpdateChecker")

data class ParsedArtifact(
    val groupId: String,
    val artifactId: String,
    val version: String
)

fun parseArtifact(coordinates: String): ParsedArtifact {
    val components = coordinates.split(":")
    if (components.size != 3) {
        logger.warn("Invalid artifact ID format. Expected 'group:artifact:version'.")
        return ParsedArtifact("", "", "")
    }

    val (group, artifact, version) = components

    return ParsedArtifact(group, artifact, version)
}

fun findArtifactRootUrl(repositoryUrl: String, artifactId: String): String? {
    val (group, artifact, _) = parseArtifact(artifactId)
    val groupPath = group.replace('.', '/')
    val pluginUrl = "$repositoryUrl$groupPath/$artifact/"

    return pluginUrl
}

fun findArtifactLatest(repositoryUrl: String, artifactId: String): ParsedVersion? {
    val pluginUrl = findArtifactRootUrl(repositoryUrl, artifactId)
    if (pluginUrl == null) {
        println("Failed to find plugin root URL for $artifactId")
        return null
    }

    val mavenMetadataUrl = "${pluginUrl}maven-metadata.xml"

    return try {
        // Fetch and parse the maven-metadata.xml
        val metadataXml = URL(mavenMetadataUrl).readText()

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(metadataXml.byteInputStream())

        val latest = doc.getElementsByTagName("latest").item(0)?.textContent

        return ParsedVersion(latest!!)
    } catch (e: Exception) {
        logger.warn("Failed to get plugin versions for $artifactId in $repositoryUrl", e)
        null
    }
}

fun checkForUpdates(
    coordinates: String,
    repoUrls: Array<String>
): ParsedVersion? {
    val (group, artifact, version) = coordinates.split(":")
    val artifactId = "$group:$artifact:$version"

    val currentVersion = ParsedVersion(version)

    for (repoUrl in repoUrls) {
        val latestVersion = findArtifactLatest(repoUrl, artifactId) ?: continue

        logger.info("Latest version of $group:$artifact is $latestVersion")

        if (latestVersion > currentVersion) {
            return latestVersion
        }
    }

    return null
}