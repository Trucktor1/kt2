import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger


@Serializable
data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val year: Int
)


@Serializable
data class BookCreateDto(
    val title: String,
    val author: String,
    val year: Int
)


@Serializable
data class YearPatchDto(
    val year: Int
)


@Serializable
data class ErrorDto(
    val error: String
)


@Serializable
data class CountDto(
    val count: Int
)


@Serializable
data class StatsDto(
    val count: Int,
    val earliestYear: Int?,
    val latestYear: Int?
)


val bookStorage = mutableListOf(
    Book(
        id = 1,
        title = "Война и мир",
        author = "Лев Толстой",
        year = 1869
    ),

    Book(
        id = 2,
        title = "Преступление и наказание",
        author = "Фёдор Достоевский",
        year = 1866
    ),

    Book(
        id = 3,
        title = "Мастер и Маргарита",
        author = "Михаил Булгаков",
        year = 1967
    )
)


val idGenerator = AtomicInteger(
    bookStorage.maxOf { it.id } + 1
)


fun Application.module() {

    // Подключаем JSON.
    install(ContentNegotiation) {
        json()
    }


    routing {

        route("/api/v1") {


            route("/books") {

                get {

                    call.respond(bookStorage)
                }

                get("/search") {

                    val authorFilter =
                        call.request.queryParameters["author"]


                    if (authorFilter.isNullOrBlank()) {

                        call.respond(bookStorage)

                        return@get
                    }


                    val matchedBooks = bookStorage.filter { book ->

                        book.author.contains(
                            authorFilter,
                            ignoreCase = true
                        )
                    }


                    call.respond(matchedBooks)
                }

                get("/count") {

                    call.respond(
                        CountDto(
                            count = bookStorage.size
                        )
                    )
                }

                get("/stats") {

                    val minYear =
                        bookStorage.minOfOrNull { it.year }

                    val maxYear =
                        bookStorage.maxOfOrNull { it.year }


                    call.respond(
                        StatsDto(
                            count = bookStorage.size,
                            earliestYear = minYear,
                            latestYear = maxYear
                        )
                    )
                }

                post {

                    val payload =
                        call.receive<BookCreateDto>()



                    if (payload.title.isBlank()) {

                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorDto(
                                error = "Название книги не может быть пустым"
                            )
                        )

                        return@post
                    }


                    if (payload.author.isBlank()) {

                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorDto(
                                error = "Автор книги не может быть пустым"
                            )
                        )

                        return@post
                    }


                    val newId =
                        idGenerator.getAndIncrement()


                    val newBook = Book(
                        id = newId,
                        title = payload.title,
                        author = payload.author,
                        year = payload.year
                    )


                    bookStorage.add(newBook)



                    call.respond(
                        HttpStatusCode.Created,
                        newBook
                    )
                }


                get("/{id}") {

                    val rawId =
                        call.parameters["id"]


                    val bookId =
                        rawId?.toIntOrNull()



                    if (bookId == null) {

                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorDto(
                                error = "ID должен быть целым числом"
                            )
                        )

                        return@get
                    }


                    val foundBook =
                        bookStorage.find { book ->

                            book.id == bookId
                        }



                    if (foundBook == null) {

                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorDto(
                                error = "Книга с id=$bookId не найдена"
                            )
                        )

                        return@get
                    }


                    call.respond(foundBook)
                }


                put("/{id}") {

                    val rawId =
                        call.parameters["id"]


                    val bookId =
                        rawId?.toIntOrNull()


                    if (bookId == null) {

                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorDto(
                                error = "ID должен быть целым числом"
                            )
                        )

                        return@put
                    }


                    val targetIndex =
                        bookStorage.indexOfFirst { book ->

                            book.id == bookId
                        }


                    if (targetIndex == -1) {

                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorDto(
                                error = "Книга с id=$bookId не найдена"
                            )
                        )

                        return@put
                    }


                    val payload =
                        call.receive<BookCreateDto>()


                    val replacementBook = Book(
                        id = bookId,
                        title = payload.title,
                        author = payload.author,
                        year = payload.year
                    )


                    bookStorage[targetIndex] = replacementBook


                    call.respond(replacementBook)
                }


                delete("/{id}") {

                    val rawId =
                        call.parameters["id"]


                    val bookId =
                        rawId?.toIntOrNull()


                    if (bookId == null) {

                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorDto(
                                error = "ID должен быть целым числом"
                            )
                        )

                        return@delete
                    }


                    val wasRemoved =
                        bookStorage.removeIf { book ->

                            book.id == bookId
                        }


                    if (!wasRemoved) {

                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorDto(
                                error = "Книга с id=$bookId не найдена"
                            )
                        )

                        return@delete
                    }


                    call.respond(
                        HttpStatusCode.NoContent
                    )
                }


                patch("/{id}/year") {

                    val rawId =
                        call.parameters["id"]


                    val bookId =
                        rawId?.toIntOrNull()


                    if (bookId == null) {

                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorDto(
                                error = "ID должен быть целым числом"
                            )
                        )

                        return@patch
                    }


                    val targetIndex =
                        bookStorage.indexOfFirst { book ->

                            book.id == bookId
                        }


                    if (targetIndex == -1) {

                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorDto(
                                error = "Книга с id=$bookId не найдена"
                            )
                        )

                        return@patch
                    }


                    val payload =
                        call.receive<YearPatchDto>()


                    val patchedBook =
                        bookStorage[targetIndex].copy(
                            year = payload.year
                        )


                    bookStorage[targetIndex] = patchedBook


                    call.respond(patchedBook)
                }
            }
        }
    }
}