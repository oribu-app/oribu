package app.oribu.service

/**
 * Failure cases the UI can localize (auth, rate limiting, not-found, generic HTTP errors).
 * Thrown by API service classes instead of a plain [Exception] with a hardcoded message — the
 * message here is for logs only; the UI resolves [reason] to a translated string via
 * `app.oribu.ui.components.localizedApiErrorMessage` and never shows this text directly.
 */
class ApiException(
    val provider: String,
    val reason: ApiErrorReason,
    val httpCode: Int? = null,
) : Exception("$provider $reason${httpCode?.let { " ($it)" } ?: ""}")

enum class ApiErrorReason {
    UNAUTHORIZED,
    RATE_LIMITED,
    NOT_FOUND,
    HTTP_ERROR,
}
