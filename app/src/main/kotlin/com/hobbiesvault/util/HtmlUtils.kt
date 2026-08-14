package com.hobbiesvault.util

import android.text.Html

/**
 * Converte texto HTML (comum em campos de sinopse/descrição vindos de APIs como
 * Google Books e AniList) em texto plano, decodificando entidades e convertendo
 * tags de bloco/quebra de linha em quebras de linha reais.
 */
fun htmlToPlainText(html: String): String =
    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
