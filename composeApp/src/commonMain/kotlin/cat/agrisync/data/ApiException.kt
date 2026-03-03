package cat.agrisync.data

class ApiException(
    val statusCode: Int,
    message: String
) : Exception(message)

