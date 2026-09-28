package app.oribu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.oribu.R
import app.oribu.service.ApiErrorReason
import app.oribu.service.ApiException

/** Resolves any search/API failure to a localized, user-facing message — never raw exception text. */
@Composable
fun localizedApiErrorMessage(error: Throwable): String =
    when (error) {
        is ApiException -> {
            when (error.reason) {
                ApiErrorReason.UNAUTHORIZED -> stringResource(R.string.api_error_unauthorized, error.provider)
                ApiErrorReason.RATE_LIMITED -> stringResource(R.string.api_error_rate_limited, error.provider)
                ApiErrorReason.NOT_FOUND -> stringResource(R.string.api_error_not_found, error.provider)
                ApiErrorReason.HTTP_ERROR -> stringResource(R.string.api_error_http, error.provider, error.httpCode ?: 0)
            }
        }

        else -> {
            stringResource(R.string.api_error_generic)
        }
    }
