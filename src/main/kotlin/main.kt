import java.io.BufferedWriter
import java.io.File
import java.util.*
import kotlin.io.path.nameWithoutExtension

fun main(args: Array<String>) {
    WebsiteGenerator(File(args[0]), File(args[1])).generate()
}

class WebsiteGenerator(private val initialDirectory: File, private val outputDirectory: File) {
    fun generate() {
        handleFolder(initialDirectory)
    }

    private fun handleFolder(folder: File) {
        val outputFile = File("${outputDirectory.path}${folder.relativePath()}")
        if (outputFile.mkdirs()) {
            // TODO figure out why an exception is thrown even though the folder is generated
            // throw IOException("Failed to create directory ${outputFile.path}")
        }
        folder
            .listFiles()
            ?.forEach { file ->
                if (file.isDirectory) {
                    handleFolder(file)
                } else if (file.extension == "zd") {
                    handleFile(file)
                } else {
                    copyFile(file)
                }
            }
    }

    private fun handleFile(file: File) {
        val outputFile = File("${outputDirectory.path}${file.relativePath().removeSuffix(".zd")}.html")
        outputFile.bufferedWriter().use { out ->
            out.write(
                """
            <!DOCTYPE html>
            <html lang="en">
            
            """.trimIndent()
            )
            val lines = file.readLines()
            val title = lines.first { it.dropWhile { it.isWhitespace() }.startsWith("#") }
                .dropWhile { it.isWhitespace() || it == '#' }
            writeHeader(out, title)
            out.write(
                """<h3 class="article-title">$title</h3>
                    
                """.trimMargin()
            )

            out.write(
                """
                <div class="text-container">
                <p>
                
            """.trimIndent()
            )

            lines
                .dropWhile { !it.isHeader() }
                .drop(1)
                .dropWhile { it.isBlank() }
                .forEach { line ->
                    if (line.startsWith("//")) {
                        // Do nothing it's a comment
                    } else if (line.startsWith("#")) {
                        val numHashtags = line.takeWhile { it == '#' }.count()
                        val title = line.dropWhile { it == '#' }
                        out.write(
                            """
                        </p>
                        <h$numHashtags>$title</h$numHashtags>
                        <p>
                        
                    """.trimIndent()
                        )
                    } else if (line.isBlank()) {
                        out.write(
                            """
                        
                        <br/><br/>
                        
                        
                    """.trimIndent()
                        )
                    } else if (line.startsWith("[")) {
                        val text = line.drop(1).takeWhile { it != ']' }
                        val link = line.dropWhile { it != ']' }.drop(1).takeWhile { it != ')' }
                        out.write("""<a href="$link">$text</a>""")
                        out.write("\n")
                    } else {
                        // This is an actual line of text
                        out.write("$line\n")
                    }
                }

            writeIndex(file, out)
            writeFooter(out)
            out.write(
                """
                </body>
            </html>
            """.trimIndent()
            )
        }
    }

    private fun writeIndex(file: File, out: BufferedWriter) {
        // Index is only written for files that have the same name as the parent folder
        if (file.parentFile.toPath().nameWithoutExtension == file.toPath().nameWithoutExtension) {
            // Check all folders inside it. For each folder create a header and inside that header put a list of files that contained in the folder
            file.parentFile.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                out.write(
                    """
                        
            <h2>${folder.toPath().nameWithoutExtension.sanitizeFolderName()}</h2>
            
        """.trimIndent()
                )

                folder.listFiles()?.filter { it.isFile }?.forEach { file ->
                    out.write(
                        """
                            
            <ul><a href="${file.relativePath().takeWhile { it != '.' }}.html"><h3>${file.extractTitle()}</h3></a></ul>
            
        """.trimIndent()
                    )
                }
            }
        }
    }

    private fun writeHeader(out: BufferedWriter, title: String) {
        out.write(
            """
    <head>
        <meta charset="utf-8"/>
        <title>$title</title>
        <link href="/utils/style.css" rel="stylesheet" />
        <link href="/utils/header.css" rel="stylesheet" />
        <link href="/utils/footer.css" rel="stylesheet" />
    </head>
    <body>
        <header class="top-header">
            <nav>
                <ul>
                    <li><a href="/">Home</a></li>
                    <li><a href="/rants/rants.html">Rants</a></li>
                    <li><a href="/projects/projects.html">Projects</a></li>
                </ul>
            </nav>
        </header>
        
        """.trimIndent()
        )
    }

    private fun writeFooter(out: BufferedWriter) {
        out.write(
            """
            </p>
        </div>
        <a href="#" class="back-to-top">Back to Top</a>

        <footer class="footer">
            <nav>
                <ul>
                    <li><a target="_blank" href="https://www.twitch.com"><img src="/images/twitch.svg" alt="My Twitch Channel"/></a></li>
                    <li><a target="_blank" href="https://www.youtube.com"><img src="/images/youtube.png" alt="My Youtube Channel"/></a></li>
                    <li><a target="_blank" href="https://www.reddit.com"><img src="/images/reddit.svg" alt="My Reddit Account"/></a></li>
                    <li><a target="_blank" href="https://www.twitter.com"><img src="/images/twitter.svg" alt="My Twitter Account"/></a></li>
                </ul>
            </nav>
        </footer>
        
        """.trimIndent()
        )
    }

    private fun copyFile(file: File) {
        val outputFile = File("${outputDirectory.path}${file.relativePath()}")
        file.copyTo(outputFile, overwrite = true)
    }

    private fun File.relativePath(): String {
        return path.removePrefix(initialDirectory.path)
    }

    private fun File.extractTitle(): String {
        return readLines().first { it.dropWhile { it.isWhitespace() }.startsWith("#") }.dropWhile { it == '#' }.trim()
    }

    private fun String.isHeader(): Boolean {
        return this.dropWhile { it.isWhitespace() }.startsWith("#")
    }

    private fun String.sanitizeFolderName(): String {
        return split("_").joinToString(" ") { replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
            .trim()
    }
}